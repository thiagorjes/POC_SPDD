package br.com.idsd.kanban.internal.tarefa;

import br.com.idsd.kanban.internal.projeto.Etapa;
import br.com.idsd.kanban.internal.projeto.EtapaService;
import br.com.idsd.kanban.internal.projeto.RaiaRepositorio;
import br.com.idsd.kanban.internal.tempo.AplicadorDeIntervalos;
import br.com.idsd.kanban.shared.RegraDeNegocioViolada;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Criacao de tarefa — RF-004.
 *
 * <p>A tarefa nasce nas <b>tres</b> dimensoes de RN-002 ao mesmo tempo: na etapa
 * de menor ordem do fluxo vigente, em {@link Condicao#AGUARDANDO_TOMADA} e sem
 * impedimento — e nasce ja contando duas series de tempo, porque a espera de
 * tomada e serie propria desde o primeiro instante e nao um recorte da
 * permanencia (RN-008, DDR-006).
 *
 * <p><b>Os colaboradores estao divididos entre construtor e injecao de campo, e a
 * divisao nao e escolha de quem implementa.</b> {@code CriacaoDeTarefaServiceTest}
 * e suite congelada e monta {@code new CriacaoDeTarefaService(etapaService,
 * registradorDeEvento)} — dois argumentos. O aplicador de intervalos, o
 * {@code EntityManager}, o conversor e o repositorio de raia entram por campo para que essa assinatura
 * continue valendo. Nao ha perda de verificabilidade: o que o teste unitario
 * exercita e a recusa, que acontece <b>antes</b> de qualquer um dos tres ser
 * tocado.
 *
 * <p>Quem chama o aplicador de intervalos e este servico, e nao
 * {@link RegistradorDeEvento} — a mesma divisao que {@code TomadaService} e
 * {@code ImpedimentoService} recebem da suite congelada (ver o Javadoc do
 * registrador).
 */
@Service
public class CriacaoDeTarefaService {

    /** Teto de {@code titulo} na borda — ACH-05. Cartao e rotulo, nao documento. */
    public static final int TAMANHO_MAXIMO_DO_TITULO = 200;

    /** Teto de {@code descricao} na borda — ACH-05. */
    public static final int TAMANHO_MAXIMO_DA_DESCRICAO = 4000;

    private final EtapaService etapas;
    private final RegistradorDeEvento registrador;

    @Autowired
    private RaiaRepositorio raias;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private AplicadorDeIntervalos aplicador;

    @Autowired
    private ObjectMapper conversor;

    public CriacaoDeTarefaService(EtapaService etapas, RegistradorDeEvento registrador) {
        this.etapas = etapas;
        this.registrador = registrador;
    }

    /**
     * Cria a tarefa e devolve o cartao dela.
     *
     * <p>{@code atorId} e parametro e nao vem da requisicao: quem o resolve e a
     * borda, a partir do principal autenticado (ACH-12). Ver
     * {@link NovaTarefaRequisicao}.
     *
     * <p>A ordem dos passos e regra e nao estilo. As duas recusas acontecem antes
     * de qualquer escrita — evento gravado numa criacao recusada nao teria como ser
     * desfeito, porque o log e imutavel (SDR-001, RNF-008). Depois delas, a linha de
     * {@code tarefa} e gravada e descarregada <b>antes</b> do evento, porque o
     * registrador confere que a tarefa do evento pertence ao projeto declarado e
     * para isso precisa encontra-la.
     */
    @Transactional(timeout = 10)
    public CartaoResposta criar(UUID projetoId, NovaTarefaRequisicao pedido, UUID atorId) {
        Objects.requireNonNull(pedido, "a requisicao e obrigatoria");
        String titulo = exigirTitulo(pedido.titulo());
        String descricao = exigirDescricaoNoTeto(pedido.descricao());

        // SDR-005 — antes da leitura do fluxo, e nao depois (ACH-03). Sem ele, a
        // criacao le a etapa de menor ordem e um `PUT /etapas` concorrente conta
        // zero tarefas nela, a arquiva e comita; a criacao entao insere numa etapa
        // ja fora do fluxo, e a FK passa porque o arquivamento e logico. A tarefa
        // nasce invisivel no board e sem destino alcancavel por RN-005, e nada
        // falha. E a mesma clausula que a rota de substituicao cumpre.
        bloquearProjeto(projetoId);

        Etapa primeira = exigirFluxoConfigurado(projetoId);
        UUID raiaId = exigirRaiaDoProjeto(projetoId, pedido.raiaId());

        var tarefa = new Tarefa();
        tarefa.setId(UUID.randomUUID());
        tarefa.setProjetoId(projetoId);
        tarefa.setTitulo(titulo);
        tarefa.setDescricao(descricao);
        tarefa.setEtapaId(primeira.getId());
        tarefa.setRaiaId(raiaId);
        tarefa.setCondicao(Condicao.AGUARDANDO_TOMADA);
        tarefa.setEpisodioAtual(1);
        tarefa.setCriadaEm(Instant.now());
        em.persist(tarefa);
        em.flush();

        var evento = registrador.registrar(new EventoTarefa.Novo(
                tarefa.getId(),
                projetoId,
                TipoDeEvento.TAREFA_CRIADA,
                atorId,
                1,
                null,
                // A etapa vai declarada no evento e nao deixada para o estado
                // corrente da tarefa: a reconstrucao reexecuta o log sobre uma
                // tarefa que ja se moveu, e sem esta coluna a permanencia inicial
                // seria reaberta na etapa de hoje, e nao na de origem (SDR-006).
                primeira.getId(),
                null,
                Condicao.AGUARDANDO_TOMADA,
                dadosDaCriacao(titulo)));

        var aplicados = aplicador.aplicar(evento, tarefa);
        // Sem impedimento e sem responsavel: a tarefa nasce no pool (RN-006).
        return CartaoResposta.de(tarefa, aplicados.abertos(), null, null, Instant.now());
    }

    /**
     * Toma bloqueio pessimista sobre a linha do projeto, cumprindo SDR-005 nesta
     * rota (ACH-03).
     *
     * <p><b>Nao introduz ponto de contencao novo</b>, e isso e o que torna a
     * correcao barata: toda escrita de tarefa ja serializa nesta mesma linha, no
     * {@code UPDATE projeto SET seq_atual} de {@link RegistradorDeEvento}
     * (SDR-004). O que muda e <i>quando</i> o bloqueio e tomado — antes da leitura
     * que decide a etapa, e nao depois dela. A ordem de aquisicao e a mesma da
     * rota de substituicao de fluxo — projeto primeiro —, de modo que nao ha ciclo
     * entre as duas.
     *
     * <p>Projeto ausente aqui nao e entrada de usuario: a borda resolveu o
     * {@code 404} antes. E defeito de estado, e o identificador nao entra na
     * mensagem, pela mesma razao registrada em {@code EtapaRepositorioImpl}.
     *
     * <p>O teto de espera e o {@code lock_timeout} de 5 s da sessao, e o teto da
     * transacao inteira e o {@code timeout = 10} desta rota — que ela nao tinha
     * (ACH-11) e passou a ter junto com o bloqueio, porque e o bloqueio que torna
     * a espera possivel.
     */
    private void bloquearProjeto(UUID projetoId) {
        var projeto = em.find(
                br.com.idsd.kanban.internal.projeto.Projeto.class,
                projetoId,
                jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (projeto == null) {
            throw new IllegalStateException("projeto inexistente na criacao de tarefa");
        }
    }

    /**
     * RN-023 — a raia e do projeto, e a chave estrangeira nao diz isso (ACH-01,
     * ACH-02).
     *
     * <p>{@code tarefa.raia_id} referencia {@code raia (id)} sem escopo de projeto,
     * e e tudo o que o banco sabe: raia de outro projeto passa e a tarefa nasce
     * apontando para ela, com o identificador alheio devolvido no cartao; raia
     * inexistente estoura na FK e sai como {@code 500}, porque o tradutor de
     * restricao e nominal e nao conhece esta. As duas entradas sao de cliente
     * autenticado e o contrato manda recusa-las na borda — aqui, <b>antes</b> de
     * qualquer escrita, como as demais recusas desta rota.
     *
     * <p>Raia arquivada e recusada junto: o arquivamento e logico, de modo que a FK
     * tambem a aceita, e nascer numa divisao que o board nao mostra tem o mesmo
     * efeito de nascer numa etapa arquivada.
     *
     * <p>Sem raia e o caso normal — o campo e opcional no contrato — e nao passa
     * por checagem alguma.
     */
    private UUID exigirRaiaDoProjeto(UUID projetoId, UUID raiaId) {
        if (raiaId == null) {
            return null;
        }
        if (!raias.existsByIdAndProjetoIdAndArquivadaEmIsNull(raiaId, projetoId)) {
            throw new RegraDeNegocioViolada(
                    "raia-fora-do-projeto",
                    "Raia inválida para este projeto",
                    "A raia informada não existe neste projeto ou já foi arquivada. "
                            + "Escolha uma raia do próprio projeto. Nada foi criado.")
                    .comErros(List.of(Map.of("campo", "raiaId")));
        }
        return raiaId;
    }

    /**
     * Teto de {@code descricao} na borda (ACH-05).
     *
     * <p>O unico limite era o filtro global de 256 KiB, de modo que uma escrita
     * autenticada gravava centenas de KB por requisicao numa coluna {@code text}
     * sem poda. Teto herdado de corpo nao e teto de campo: ele nao diz nada sobre o
     * que <i>este</i> campo aceita, e some no dia em que o corpo passar a carregar
     * outra coisa. {@code Etapa#TAMANHO_MAXIMO_DO_NOME} existe pela mesma razao.
     *
     * <p>O teto de {@code titulo} esta em {@link #exigirTitulo}, junto da recusa
     * que ja existia ali — e fechar o titulo fecha tambem ACH-06: acima de 8 KiB
     * ele era recusado la adiante pelo teto de {@code dados} do nucleo, como
     * {@code 400} generico e <b>depois</b> de a linha de {@code tarefa} ja ter sido
     * gravada.
     */
    private String exigirDescricaoNoTeto(String descricao) {
        if (descricao != null && descricao.length() > TAMANHO_MAXIMO_DA_DESCRICAO) {
            throw new RegraDeNegocioViolada(
                    "descricao-longa-demais",
                    "Descrição longa demais",
                    "A descrição não pode passar de " + TAMANHO_MAXIMO_DA_DESCRICAO
                            + " caracteres. Nada foi criado.")
                    .comErros(List.of(Map.of("campo", "descricao")));
        }
        return descricao;
    }

    /**
     * O documento de {@code dados}, montado aqui chave a chave — fechamento de
     * ACH-15.
     *
     * <p>O nucleo de escrita ganhou teto de tamanho e checagem de JSON, que e
     * validacao de <b>forma</b>, e nenhuma forma distingue um dado escrito pelo
     * servico de um colado do corpo da requisicao. O fechamento e estrutural: a
     * borda <b>constroi</b> o documento a partir das chaves que
     * {@code data-model.md} §4 declara para o tipo — para {@code TAREFA_CRIADA},
     * {@code titulo} e nada alem — em vez de repassar objeto recebido. Nao ha
     * caminho por onde uma chave nao prevista chegue ao log.
     *
     * <p>{@code titulo} <b>e</b> dado de cliente, e esta ali por decisao registrada
     * da spec: a regra de IDSD 4.10.1 proibe a passagem livre, e nao este campo.
     * {@code descricao} fica de fora pela mesma §4 — nao e reconstruivel, e dado de
     * cliente sem leitor num log que nao tem retificacao e exatamente o que ela
     * encarece.
     */
    private String dadosDaCriacao(String titulo) {
        ObjectNode documento = conversor.createObjectNode();
        documento.put("titulo", titulo);
        try {
            return conversor.writeValueAsString(documento);
        } catch (com.fasterxml.jackson.core.JsonProcessingException impossivel) {
            // Um ObjectNode montado aqui nao tem como falhar na serializacao; a
            // excecao e checada e precisa de destino.
            throw new IllegalStateException("documento de evento nao serializou", impossivel);
        }
    }

    /**
     * RF-004 — titulo obrigatorio, e branco conta como ausente (SCN-004.2).
     *
     * <p>Aparar antes de decidir e o ponto: {@code "   "} passaria em qualquer
     * checagem de nulo ou de comprimento, e gravaria um cartao sem nome legivel. O
     * titulo aparado e o que e gravado, para que o que se verificou seja o que se
     * guardou.
     *
     * <p>A recusa nomeia o campo em {@code errors} porque a tela precisa saber onde
     * por a mensagem (RNF-003), e no {@code detail} porque o teste unitario le a
     * razao sem passar por HTTP.
     */
    private String exigirTitulo(String titulo) {
        String aparado = titulo == null ? "" : titulo.strip();
        if (aparado.isEmpty()) {
            throw new RegraDeNegocioViolada(
                    "titulo-obrigatorio",
                    "Tarefa sem título",
                    "O campo titulo é obrigatório: a tarefa precisa de um título que "
                            + "não seja só espaços. Nada foi criado.")
                    .comErros(List.of(Map.of("campo", "titulo")));
        }
        if (aparado.length() > TAMANHO_MAXIMO_DO_TITULO) {
            // ACH-05 e ACH-06. O teto e conferido sobre o titulo **aparado**, que e
            // o que vai ser gravado — medir o bruto recusaria por espaco que nem
            // chega ao banco.
            throw new RegraDeNegocioViolada(
                    "titulo-longo-demais",
                    "Título longo demais",
                    "O título não pode passar de " + TAMANHO_MAXIMO_DO_TITULO
                            + " caracteres. Nada foi criado.")
                    .comErros(List.of(Map.of("campo", "titulo")));
        }
        return aparado;
    }

    /**
     * RN-038 — projeto sem fluxo nao recebe tarefa, e a recusa e de negocio.
     *
     * <p>{@code 422} e nao {@code 500}: o projeto existe, o pedido esta bem formado
     * e o estado e que nao comporta. A mensagem diz o que fazer porque o caminho de
     * instalacao passa exatamente aqui — quem cria o projeto e tenta criar a
     * primeira tarefa em seguida nao tem, no primeiro passo, nada que lembre o
     * segundo (SCN-022.3).
     *
     * <p>A primeira etapa e a de menor {@code ordem}, e vem de
     * {@link EtapaService#fluxoVigente} ja ordenada e ja sem as arquivadas.
     */
    private Etapa exigirFluxoConfigurado(UUID projetoId) {
        List<Etapa> fluxo = etapas.fluxoVigente(projetoId);
        if (fluxo.isEmpty()) {
            throw new RegraDeNegocioViolada(
                    "projeto-sem-fluxo",
                    "Projeto ainda sem fluxo de etapas",
                    "Este projeto ainda não tem etapas configuradas, e uma tarefa precisa "
                            + "nascer em alguma. Configure o fluxo de etapas do projeto e "
                            + "tente de novo.");
        }
        return fluxo.get(0);
    }
}
