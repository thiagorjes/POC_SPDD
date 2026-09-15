package br.com.idsd.kanban.internal.tarefa;

import br.com.idsd.kanban.internal.tempo.ReconstrutorDeProjecao;
import br.com.idsd.kanban.shared.EventoBoardPublisher;
import br.com.idsd.kanban.shared.ProblemaDetalhado;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * O caminho unico de escrita no anel de verdade.
 *
 * <p>Toda escrita sobre tarefa do sistema passa por aqui, e a razao de existir um
 * so ponto e que tres garantias que ninguem verifica a cada uso dependem de nunca
 * serem esquecidas: a sequencia vem do banco, o log entra na mesma transacao da
 * projecao, e a publicacao sai depois do commit. Espalhadas por dez servicos, as
 * tres decaem no primeiro que as reimplementar.
 *
 * <p><b>Nao aplica intervalos.</b> A tabela de arquivos da TASK-02.4 diz "delega
 * intervalos", e a suite congelada diz o contrario: {@code TomadaServiceTest} e
 * {@code ImpedimentoServiceTest} injetam {@code AplicadorDeIntervalos} <b>no
 * servico de dominio</b> e verificam, com {@code RegistradorDeEvento} mockado,
 * que ele nao e chamado nas repeticoes idempotentes — asserção que so tem sentido
 * se quem chama o aplicador for o servico. A suite esta fora do alcance desta
 * etapa e prevalece; a divergencia esta registrada no historico da task.
 *
 * <p>A classe nao e {@code final} e os metodos nao sao estaticos porque a suite
 * congelada a substitui por mock.
 */
@Service
public class RegistradorDeEvento {

    private static final Logger LOG = LoggerFactory.getLogger(RegistradorDeEvento.class);

    /**
     * Teto do documento de {@code dados} (ACH-15).
     *
     * <p>Generoso de proposito: o conteudo legitimo e motivo, desfecho e titulo, e
     * nenhum deles chega perto. O numero nao esta aqui para apertar o caso comum e
     * sim para que exista um teto, numa tabela sem poda e sem retificacao.
     */
    static final int TAMANHO_MAXIMO_DE_DADOS = 8 * 1024;

    private final EventoBoardPublisher publicador;
    private final ObjectMapper conversor;

    @PersistenceContext
    private EntityManager em;

    public RegistradorDeEvento(EventoBoardPublisher publicador, ObjectMapper conversor) {
        this.publicador = publicador;
        this.conversor = conversor;
    }

    /**
     * Grava o evento e agenda o anuncio dele.
     *
     * <p>Sem {@code @Transactional}: quem abre a transacao e a rota, e abrir
     * outra aqui — ou pior, propagar por {@code REQUIRES_NEW} — separaria o log
     * da projecao em duas transacoes, que e o defeito que o criterio 3 desta task
     * existe para pegar. O metodo <b>exige</b> transacao em curso e recusa rodar
     * fora de uma.
     */
    public EventoTarefa registrar(EventoTarefa.Novo novo) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "evento gravado fora de transacao: log e projecao deixariam de commitar juntos");
        }
        exigirCoerencia(novo);
        exigirDadosAceitaveis(novo.dados());
        exigirJanelaDeEscrita(novo.projetoId());

        var evento = new EventoTarefa(novo, proximaSequencia(novo.projetoId()), Instant.now());
        em.persist(evento);
        // O flush e aqui para que a violacao de (projeto_id, seq) — se houver —
        // chegue como falha desta escrita, e nao como falha de outra coisa no fim
        // da transacao, longe do INSERT que a causou. O comentario anterior dizia
        // que o aplicador de intervalos precisa do identificador, e nao precisa:
        // ele nao o le (ACH-09).
        em.flush();

        agendarPublicacao(evento);
        return evento;
    }

    /**
     * {@code UPDATE projeto SET seq_atual = seq_atual + 1 ... RETURNING seq_atual}
     * (SDR-004).
     *
     * <p>Tres propriedades, e nenhuma e gratuita. O numero vem do <b>banco</b>, e
     * nao de contador em memoria: com duas instancias escrevendo no mesmo projeto,
     * contador por pod produz {@code seq} duplicado, e duplicata faz uma lacuna
     * parecer continuidade — destrui a rede de seguranca de RNF-002, e foi o que
     * superou o ADR-004 neste ponto. O {@code UPDATE} <b>serializa</b> as escritas
     * daquele projeto, e so delas: contencao aceita conscientemente, porque o
     * board de um projeto nao e caminho de escrita de alta concorrencia. E o
     * rollback <b>nao deixa buraco</b>, porque o incremento reverte com a
     * transacao — que e a razao de o contador ser coluna e nao {@code SEQUENCE}.
     *
     * <p>Consulta nativa porque JPQL nao tem {@code RETURNING}, e a alternativa —
     * ler, somar em Java, gravar — e {@code MAX(seq)+1} com outro nome: duas
     * transacoes leem o mesmo valor e produzem o mesmo numero.
     *
     * <p><b>O {@code UPDATE} vai dentro de uma CTE</b> (ACH-06). Passar o
     * {@code UPDATE ... RETURNING} direto a {@code createNativeQuery} e depois
     * chamar {@code getSingleResult} funciona por acidente: o Hibernate classifica
     * a consulta pelo verbo inicial, e o caminho de leitura sobre um comando que
     * ele considera de escrita nao e contrato. Envolvido em
     * {@code WITH ... SELECT}, o comando <b>e</b> um select para todos os efeitos,
     * e o efeito colateral do {@code UPDATE} continua acontecendo — semantica que
     * o PostgreSQL garante. Nao e estilo: e o ponto de que todo o caminho de
     * escrita do sistema depende, e ele nao pode depender de comportamento nao
     * especificado de uma versao de biblioteca.
     *
     * <p>Projeto inexistente devolve <b>zero linhas</b>, e sai como {@code 404} e
     * nao como {@code 500} (ACH-19): pedido sobre recurso que nao existe nao e
     * defeito do servidor.
     */
    private long proximaSequencia(UUID projetoId) {
        var linhas = em.createNativeQuery(
                        "WITH incremento AS ("
                                + " UPDATE projeto SET seq_atual = seq_atual + 1"
                                + " WHERE id = :projetoId RETURNING seq_atual)"
                                + " SELECT seq_atual FROM incremento")
                .setParameter("projetoId", projetoId)
                .getResultList();
        if (linhas.isEmpty()) {
            throw new ProblemaDetalhado.Falha(
                    HttpStatus.NOT_FOUND,
                    "projeto-nao-encontrado",
                    "Projeto nao encontrado",
                    "Nao ha projeto com o identificador informado.");
        }
        return ((Number) linhas.get(0)).longValue();
    }

    /**
     * Confere que o evento fala de uma tarefa que pertence ao projeto declarado
     * (ACH-12).
     *
     * <p>Sem esta guarda, um {@code Novo} montado pelo construtor cheio com o
     * {@code projetoId} de outro projeto grava no log alheio <b>e consome a
     * sequencia dele</b>. O dano nao e so o registro no lugar errado: {@code seq}
     * e o que permite ao cliente detectar lacuna (RNF-002), e numero consumido por
     * escrita que nunca sera anunciada naquele projeto e uma lacuna permanente,
     * indistinguivel de mensagem perdida — resincronizacao eterna no board de quem
     * nada fez.
     *
     * <p><b>O que esta guarda nao cobre</b>: {@code atorId} continua sendo
     * parametro livre, nunca confrontado com o principal autenticado — e ele e a
     * autoria do unico registro de auditoria do sistema. Conferi-lo aqui acoplaria
     * o caminho unico de escrita ao contexto de seguranca web e o quebraria para o
     * job e para o arnes, que legitimamente nao tem principal. O lugar e a borda,
     * que nasce em TASK-02.5. Achado remanescente, destino {@code /tasks}.
     */
    private void exigirCoerencia(EventoTarefa.Novo novo) {
        UUID projetoDaTarefa = em.createQuery(
                        "select t.projetoId from Tarefa t where t.id = :tarefaId", UUID.class)
                .setParameter("tarefaId", novo.tarefaId())
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new ProblemaDetalhado.Falha(
                        HttpStatus.NOT_FOUND,
                        "tarefa-nao-encontrada",
                        "Tarefa nao encontrada",
                        "Nao ha tarefa com o identificador informado."));
        if (!projetoDaTarefa.equals(novo.projetoId())) {
            throw new IllegalStateException(
                    "evento declara projeto " + novo.projetoId()
                            + " para tarefa que pertence a " + projetoDaTarefa);
        }
    }

    /**
     * Recusa documento malformado ou grande demais (ACH-15).
     *
     * <p>A coluna e {@code jsonb} e o banco ja recusaria o texto que nao for JSON
     * — mas recusaria como erro de driver no fim da transacao, longe de quem o
     * produziu, e sem teto de tamanho nenhum. Aqui a recusa e do escritor, que e o
     * unico ponto por onde todo evento passa.
     *
     * <p>O peso desta validacao nao esta na forma e sim no destino: o log e
     * imutavel por decisao de arquitetura (SDR-001, RNF-008), de modo que nao ha
     * caminho de retificacao nem de eliminacao para o que entrar aqui. A regra
     * "nunca dado de cliente" ({@code IDSD 4.10.1}) segue sendo disciplina de
     * chamador e nao mecanismo — nenhuma validacao de forma distingue um motivo
     * escrito pelo servico de um colado do corpo da requisicao. Esse mecanismo
     * pertence a borda, e a borda nasce em TASK-02.5.
     */
    private void exigirDadosAceitaveis(String dados) {
        if (dados == null) {
            return;
        }
        if (dados.length() > TAMANHO_MAXIMO_DE_DADOS) {
            throw new IllegalArgumentException(
                    "documento de evento acima do teto: " + dados.length() + " caracteres");
        }
        try {
            conversor.readTree(dados);
        } catch (JsonProcessingException malformado) {
            throw new IllegalArgumentException("documento de evento nao e JSON valido", malformado);
        }
    }

    /**
     * Recusa a escrita se a reconstrucao da projecao estiver em curso no projeto.
     *
     * <p><b>Modo compartilhado</b>, e essa e a correcao de ACH-01. A versao
     * anterior tomava {@code pg_try_advisory_xact_lock}, que e <b>exclusivo</b>:
     * dois escritores concorrentes no mesmo projeto nao o obtem ao mesmo tempo, de
     * modo que o segundo recebia {@code 409 reconstrucao-em-curso} sem que
     * reconstrucao alguma estivesse em curso. O criterio 1 desta task falhava por
     * construcao, e o efeito ia alem de quebrar o criterio: qualquer participante
     * do projeto impedia os demais de escrever apenas mantendo transacoes de
     * escrita abertas, sem privilegio nenhum alem do que ja tem.
     *
     * <p>A relacao que o desenho exige e assimetrica — escritores convivem entre
     * si e nenhum convive com a reconstrucao —, e e exatamente o que compartilhado
     * contra exclusivo expressa. O {@link ReconstrutorDeProjecao} continua tomando
     * o modo exclusivo, que espera os compartilhados sairem e barra os que
     * chegarem depois.
     *
     * <p>{@code try} e nao a versao que espera: a escrita nao espera a janela
     * terminar, ela e recusada. Esperar faria a requisicao pendurar pelo tempo de
     * uma operacao administrativa que pode varrer o log inteiro do projeto, e o
     * {@code 409} diz a verdade — o pedido esta correto e o estado e que nao
     * comporta.
     *
     * <p>O bloqueio e tomado aqui e nao na rota porque este e o ponto por onde
     * toda escrita passa; na rota seria uma linha a ser lembrada em cada uma.
     */
    private void exigirJanelaDeEscrita(UUID projetoId) {
        Object livre = em.createNativeQuery(
                        "SELECT pg_try_advisory_xact_lock_shared(:chave)")
                .setParameter("chave", ReconstrutorDeProjecao.chaveDeBloqueio(projetoId))
                .getSingleResult();
        if (!Boolean.TRUE.equals(livre)) {
            throw new ProblemaDetalhado.Falha(
                    HttpStatus.CONFLICT,
                    "reconstrucao-em-curso",
                    "Projecao em reconstrucao",
                    "A projecao deste projeto esta sendo reconstruida. Tente de novo em instantes.");
        }
    }

    /**
     * Enfileira o anuncio para depois do commit.
     *
     * <p>{@code afterCommit} e nao {@code afterCompletion}: o segundo roda tambem
     * no rollback, e anunciar o que foi revertido e o caso que esta ordem existe
     * para impedir.
     *
     * <p><b>A excecao morre aqui</b> (ACH-13). O commit ja aconteceu e nao ha como
     * desfaze-lo, mas a excecao que escapa deste gancho sobe pelo encerramento da
     * transacao e sai como {@code 500} — dizendo ao cliente que falhou o que de
     * fato foi gravado, e convidando-o a repetir. Numa escrita sobre log imutavel,
     * a repeticao grava o evento de novo. A protecao fica em quem registra a
     * sincronizacao e nao em cada implementacao da porta: confiar em toda
     * implementacao futura lembrar disso e a forma de a garantia decair no
     * primeiro adaptador que a esquecer — e o de EPIC-08 fala com a rede.
     *
     * <p>Falha de anuncio nao e perda: {@code seq} e contiguo por projeto, e o
     * cliente que nao recebe o evento ve a lacuna e resincroniza (RNF-002). E o
     * mecanismo que existe exatamente para isto.
     */
    private void agendarPublicacao(EventoTarefa evento) {
        var anuncio = new EventoBoardPublisher.EventoPublicado(
                evento.getProjetoId(),
                evento.getTarefaId(),
                evento.getTipo().name(),
                evento.getSeq(),
                evento.getOcorridoEm());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    publicador.publicar(anuncio);
                } catch (RuntimeException falha) {
                    LOG.error(
                            "Evento gravado e nao anunciado: projeto={} tarefa={} seq={}."
                                    + " O cliente detecta a lacuna por seq e resincroniza.",
                            anuncio.projetoId(), anuncio.tarefaId(), anuncio.seq(), falha);
                }
            }
        });
    }
}
