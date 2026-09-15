package br.com.idsd.kanban.internal.tarefa;

import br.com.idsd.kanban.internal.projeto.Etapa;
import br.com.idsd.kanban.internal.projeto.EtapaService;
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
 * {@code EntityManager} e o conversor entram por campo para que essa assinatura
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

    private final EtapaService etapas;
    private final RegistradorDeEvento registrador;

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
    @Transactional
    public CartaoResposta criar(UUID projetoId, NovaTarefaRequisicao pedido, UUID atorId) {
        Objects.requireNonNull(pedido, "a requisicao e obrigatoria");
        String titulo = exigirTitulo(pedido.titulo());
        Etapa primeira = exigirFluxoConfigurado(projetoId);

        var tarefa = new Tarefa();
        tarefa.setId(UUID.randomUUID());
        tarefa.setProjetoId(projetoId);
        tarefa.setTitulo(titulo);
        tarefa.setDescricao(pedido.descricao());
        tarefa.setEtapaId(primeira.getId());
        tarefa.setRaiaId(pedido.raiaId());
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
        return CartaoResposta.de(tarefa, aplicados.abertos(), null);
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
