package br.com.idsd.kanban.internal.tarefa;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * RF-005 e RF-006 — movimentacao entre etapas e a contagem de permanencia.
 *
 * <p>SCN-006.3 esta aqui por conteudo e no EPIC-04 por dependencia: ele afirma
 * sobre a marca de impedimento sobrevivendo ao movimento, e nao ha o que
 * verificar antes de o impedimento existir.
 */
class MovimentacaoIT extends TesteDeIntegracao {

    private UUID projeto;
    private java.util.List<br.com.idsd.kanban.suporte.Cenario.Etapa> etapas;

    @BeforeEach
    void projetoPronto() {
        projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        etapas = cenario.fluxoPadrao(projeto, ana());
    }

    private String movimentoPara(UUID tarefa, UUID destino,
            org.springframework.test.web.servlet.request.RequestPostProcessor quem) {
        return cenario.comOrigem(tarefa, quem, "\"etapaDestinoId\": \"" + destino + "\"");
    }

    @Test
    @DisplayName("SCN-005.1 — mover para a etapa adjacente e aceito e a tarefa aparece la")
    void moverParaAdjacenteEAceito() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Corrigir o relatorio", ana());

        mockMvc.perform(post("/v1/tarefas/{id}/movimentos", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimentoPara(tarefa, etapas.get(1).id(), ana())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapaId").value(etapas.get(1).id().toString()))
                // Mover nao atribui responsavel: a tarefa chega esperando quem
                // a tome (DDR-006).
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"))
                .andExpect(jsonPath("$.esperaTomada").isNotEmpty());

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas", Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("SCN-005.2 — mover para etapa nao alcancavel e recusado")
    void moverParaEtapaNaoAlcancavelERecusado() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Corrigir o relatorio", ana());

        // Do Backlog o alcance e a etapa adjacente; saltar para Review pularia
        // a comparabilidade do tempo por etapa, que e a razao de o fluxo ser
        // ordenado.
        mockMvc.perform(post("/v1/tarefas/{id}/movimentos", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimentoPara(tarefa, etapas.get(2).id(), ana())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").isNotEmpty());

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.etapaId").value(etapas.get(0).id().toString()));
    }

    @Test
    @DisplayName("SCN-005.3 — voltar a primeira etapa e permitido de qualquer etapa")
    void voltarAPrimeiraEtapaEPermitidoDeQualquerLugar() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Corrigir o relatorio", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());
        cenario.mover(tarefa, etapas.get(2).id(), ana());

        // A excecao nomeada em RN-005. Sem ela a tarefa impedida em Review nao
        // teria como voltar ao backlog, que foi o caso concreto que motivou a
        // decisao de Q-01.
        mockMvc.perform(post("/v1/tarefas/{id}/movimentos", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimentoPara(tarefa, etapas.get(0).id(), ana())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapaId").value(etapas.get(0).id().toString()));
    }

    @Test
    @DisplayName("SCN-005.2 — mover a partir de estado divergente devolve 409 com o estado atual")
    void origemDivergenteDevolve409ComOEstadoAtual() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Disputada", ana());
        String origemVelha = movimentoPara(tarefa, etapas.get(1).id(), ana());

        // Alguem age antes. A requisicao seguinte carrega origem que ja nao
        // corresponde ao que esta no banco.
        cenario.assumir(tarefa, bruno());

        mockMvc.perform(post("/v1/tarefas/{id}/movimentos", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(origemVelha))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                // O corpo traz o estado atual completo para que a tela possa se
                // reconciliar sem uma segunda ida ao servidor (SDR-002).
                .andExpect(jsonPath("$.estadoAtual.condicao").value("EM_CURSO"))
                .andExpect(jsonPath("$.estadoAtual.responsavel.nome").value("Bruno"))
                .andExpect(jsonPath("$.estadoAtual.versao").isNumber());
    }

    @Test
    @DisplayName("SCN-006.1 — a permanencia reinicia a cada etapa, e a serie anterior fica fechada")
    void permanenciaReiniciaACadaEtapa() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Medida", ana());
        cenario.recuarInicioDoIntervalo(tarefa, "PERMANENCIA", Duration.ofMinutes(180));

        String antes = mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andReturn().getResponse().getContentAsString();
        Number decorridoNoBacklog = JsonPath.read(antes, "$.permanencia.decorrido");
        org.assertj.core.api.Assertions.assertThat(decorridoNoBacklog.longValue())
                .isGreaterThan(0);

        cenario.mover(tarefa, etapas.get(1).id(), ana());

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(status().isOk())
                // Nova etapa, nova contagem. O acumulado da etapa anterior nao
                // e transportado: ele permanece atribuido a etapa onde correu.
                .andExpect(jsonPath("$.permanencia.decorrido",
                        Matchers.lessThan(decorridoNoBacklog.intValue())));
    }

    @Test
    @DisplayName("SCN-006.2 — o tempo da etapa anterior permanece atribuido a ela")
    void tempoDaEtapaAnteriorPermaneceAtribuidoAEla() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Medida", ana());
        cenario.recuarInicioDoIntervalo(tarefa, "PERMANENCIA", Duration.ofMinutes(180));
        cenario.mover(tarefa, etapas.get(1).id(), ana());

        mockMvc.perform(get("/v1/projetos/{id}/tempo-por-etapa", projeto).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[?(@.nome=='Backlog')].tempoTotal",
                        Matchers.everyItem(Matchers.greaterThan(0))))
                // Nao existe coluna de total somando as series. RN-008 proibe a
                // soma, e oferecer o campo e convidar a ela.
                .andExpect(jsonPath("$.total").doesNotExist());
    }

    @Test
    @DisplayName("SCN-006.3 — a marca de impedimento sobrevive ao movimento e sua contagem nao reinicia")
    void marcaDeImpedimentoSobreviveAoMovimento() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada", ana());
        cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", ana());
        cenario.recuarInicioDoIntervalo(tarefa, "IMPEDIMENTO", Duration.ofMinutes(240));

        String antes = mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andReturn().getResponse().getContentAsString();
        Number impedimentoAntes = JsonPath.read(antes, "$.impedimento.decorrido");

        cenario.mover(tarefa, etapas.get(1).id(), ana());

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapaId").value(etapas.get(1).id().toString()))
                // RN-032: so o registro do desfecho apaga a marca. A marca e
                // derivada de `impedimento.desfecho IS NULL` e nao e coluna de
                // `tarefa` — nenhuma escrita de movimentacao a alcanca.
                .andExpect(jsonPath("$.impedimento.motivo").value("aguardando o fornecedor"))
                // RN-009: mover nao encerra nem reinicia a contagem de
                // impedimento. A permanencia reinicia; esta nao.
                .andExpect(jsonPath("$.impedimento.decorrido",
                        Matchers.greaterThanOrEqualTo(impedimentoAntes.intValue())));
    }

    @Test
    @DisplayName("SCN-006.3 — a serie de impedimento continua unica apos o movimento")
    void serieDeImpedimentoContinuaUnicaAposOMovimento() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada", ana());
        cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());

        // Um unico intervalo de impedimento, e nao dois. Dois apareceriam se o
        // movimento tivesse fechado o primeiro e aberto outro, que e o que
        // RN-009 proibe.
        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.log[?(@.tipo=='IMPEDIMENTO_SINALIZADO')]",
                        Matchers.hasSize(1)))
                .andExpect(jsonPath("$.log[?(@.tipo=='IMPEDIMENTO_RESOLVIDO')]",
                        Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("SCN-005.1 — mover para a etapa terminal conclui a tarefa")
    void moverParaTerminalConclui() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Quase pronta", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());
        cenario.mover(tarefa, etapas.get(2).id(), ana());

        mockMvc.perform(post("/v1/tarefas/{id}/movimentos", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimentoPara(tarefa, etapas.get(3).id(), ana())))
                .andExpect(status().isOk())
                // A conclusao e efeito de chegar a etapa terminal, e nao uma
                // segunda operacao paralela.
                .andExpect(jsonPath("$.condicao").value("CONCLUIDA"));
    }

    @Test
    @DisplayName("SCN-005.2 — mover tarefa em condicao terminal e recusado")
    void moverTarefaEmCondicaoTerminalERecusado() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Ja pronta", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());
        cenario.mover(tarefa, etapas.get(2).id(), ana());
        cenario.mover(tarefa, etapas.get(3).id(), ana());

        mockMvc.perform(post("/v1/tarefas/{id}/movimentos", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimentoPara(tarefa, etapas.get(0).id(), ana())))
                .andExpect(status().isUnprocessableEntity());
    }
}
