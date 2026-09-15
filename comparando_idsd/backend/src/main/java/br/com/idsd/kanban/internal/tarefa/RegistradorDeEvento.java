package br.com.idsd.kanban.internal.tarefa;

import br.com.idsd.kanban.internal.tempo.ReconstrutorDeProjecao;
import br.com.idsd.kanban.shared.EventoBoardPublisher;
import br.com.idsd.kanban.shared.ProblemaDetalhado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.UUID;
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

    private final EventoBoardPublisher publicador;

    @PersistenceContext
    private EntityManager em;

    public RegistradorDeEvento(EventoBoardPublisher publicador) {
        this.publicador = publicador;
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
        exigirJanelaDeEscrita(novo.projetoId());

        var evento = new EventoTarefa(novo, proximaSequencia(novo.projetoId()), Instant.now());
        em.persist(evento);
        // O identificador so existe depois do INSERT, e a rotina de reconstrucao
        // reordena o log por ele. Sem o flush aqui, o aplicador de intervalos
        // trabalharia sobre um evento que ainda nao tem posicao na ordem total.
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
     */
    private long proximaSequencia(UUID projetoId) {
        Object numero = em.createNativeQuery(
                        "UPDATE projeto SET seq_atual = seq_atual + 1"
                                + " WHERE id = :projetoId RETURNING seq_atual")
                .setParameter("projetoId", projetoId)
                .getSingleResult();
        return ((Number) numero).longValue();
    }

    /**
     * Recusa a escrita se a reconstrucao da projecao estiver em curso no projeto.
     *
     * <p>{@code pg_try_advisory_xact_lock} e nao {@code pg_advisory_xact_lock}: a
     * escrita nao espera a janela terminar, ela e recusada. Esperar faria a
     * requisicao pendurar pelo tempo de uma operacao administrativa que pode
     * varrer o log inteiro do projeto, e o {@code 409} diz a verdade — o pedido
     * esta correto e o estado e que nao comporta.
     *
     * <p>O bloqueio e tomado aqui e nao na rota porque este e o ponto por onde
     * toda escrita passa; na rota seria uma linha a ser lembrada em cada uma.
     */
    private void exigirJanelaDeEscrita(UUID projetoId) {
        Object livre = em.createNativeQuery("SELECT pg_try_advisory_xact_lock(:chave)")
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
     * para impedir. Excecao aqui nao desfaz o commit — ja aconteceu —, e por isso
     * o adaptador real de EPIC-08 nao pode deixar falha de rede escapar deste
     * gancho sem tratar.
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
                publicador.publicar(anuncio);
            }
        });
    }
}
