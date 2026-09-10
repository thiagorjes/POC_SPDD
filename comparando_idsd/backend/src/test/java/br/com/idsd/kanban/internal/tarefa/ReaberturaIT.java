package br.com.idsd.kanban.internal.tarefa;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_DENIS;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.denis;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import java.time.Duration;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * RF-013 — reabertura da tarefa concluida.
 *
 * <p>E a unica acao do produto visivel para uma pessoa so, e por isso a
 * verificacao do episodio importa: sem ele o agregado somaria dois ciclos de
 * trabalho como se fossem um, e RN-019 exige distingui-los.
 */
class ReaberturaIT extends TesteDeIntegracao {

    private UUID projeto;
    private java.util.List<br.com.idsd.kanban.suporte.Cenario.Etapa> etapas;

    @BeforeEach
    void projetoPronto() {
        projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.participante(projeto, SUB_DENIS, "Denis", "denis@empresa.example", "product_owner");
        etapas = cenario.fluxoPadrao(projeto, ana());
    }

    private UUID tarefaConcluida() {
        var tarefa = cenario.criarTarefa(projeto, "Entregue", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());
        cenario.recuarInicioDoIntervalo(tarefa, "PERMANENCIA", Duration.ofMinutes(120));
        cenario.mover(tarefa, etapas.get(2).id(), ana());
        cenario.mover(tarefa, etapas.get(3).id(), ana());
        return tarefa;
    }

    @Test
    @DisplayName("SCN-013.1 — reabrir devolve a tarefa a primeira etapa, aguardando tomada")
    void reabrirDevolveAPrimeiraEtapa() throws Exception {
        var tarefa = tarefaConcluida();

        mockMvc.perform(post("/v1/tarefas/{id}/reabertura", tarefa)
                        .with(denis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, denis(),
                                "\"motivo\": \"o cliente apontou um defeito\"")))
                .andExpect(status().isOk())
                // A primeira etapa, e nao a terminal: devolver a terminal
                // reconcluiria a tarefa no instante seguinte.
                .andExpect(jsonPath("$.etapaId").value(etapas.get(0).id().toString()))
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"))
                .andExpect(jsonPath("$.responsavel").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.esperaTomada.desde").isNotEmpty());
    }

    @Test
    @DisplayName("SCN-013.2 — a reabertura inicia um episodio novo e preserva o anterior")
    void reaberturaIniciaEpisodioNovo() throws Exception {
        var tarefa = tarefaConcluida();

        mockMvc.perform(post("/v1/tarefas/{id}/reabertura", tarefa)
                .with(denis())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cenario.comOrigem(tarefa, denis(), "\"motivo\": \"defeito\"")));

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.episodioAtual").value(2))
                // O log e cronologico e continuo: o episodio 1 permanece
                // inteiro, e o 2 comeca depois dele.
                .andExpect(jsonPath("$.log[?(@.episodio==1)]",
                        Matchers.hasSize(Matchers.greaterThan(0))))
                .andExpect(jsonPath("$.log[?(@.episodio==2)]",
                        Matchers.hasSize(Matchers.greaterThan(0))))
                .andExpect(jsonPath("$.log[?(@.tipo=='TAREFA_REABERTA')].motivo",
                        Matchers.hasItem("defeito")));
    }

    @Test
    @DisplayName("SCN-013.2 — o tempo do episodio anterior nao e somado ao do novo")
    void tempoDoEpisodioAnteriorNaoESomado() throws Exception {
        var tarefa = tarefaConcluida();
        mockMvc.perform(post("/v1/tarefas/{id}/reabertura", tarefa)
                .with(denis())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cenario.comOrigem(tarefa, denis(), "\"motivo\": \"defeito\"")));

        // RN-019: o agregado distingue episodios. Colapsa-los faria uma tarefa
        // reaberta parecer uma tarefa que demorou o dobro, que e leitura
        // diferente e conclusao diferente.
        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.permanencia.decorrido", Matchers.lessThan(60)));

        mockMvc.perform(get("/v1/projetos/{id}/tempo-por-etapa", projeto).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[?(@.nome=='Desenvolvimento')].episodios",
                        Matchers.everyItem(Matchers.greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("SCN-013.3 — quem nao e product owner nao reabre")
    void quemNaoEProductOwnerNaoReabre() throws Exception {
        var tarefa = tarefaConcluida();

        // `project_admin` configura e encerra, mas nao reabre: reabrir e
        // decisao de produto, e nao de administracao do quadro (BDR-001).
        mockMvc.perform(post("/v1/tarefas/{id}/reabertura", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, ana(), "\"motivo\": \"quero mexer\"")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.condicao").value("CONCLUIDA"))
                .andExpect(jsonPath("$.episodioAtual").value(1));
    }

    @Test
    @DisplayName("SCN-013.3 — reabrir tarefa que nao esta concluida e recusado")
    void reabrirTarefaNaoConcluidaERecusado() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Ainda em curso", ana());

        mockMvc.perform(post("/v1/tarefas/{id}/reabertura", tarefa)
                        .with(denis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, denis(), "\"motivo\": \"qualquer\"")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("SCN-013.1 — a tarefa reaberta volta a aparecer no board, na primeira etapa")
    void tarefaReabertaVoltaAoBoard() throws Exception {
        var tarefa = tarefaConcluida();
        mockMvc.perform(post("/v1/tarefas/{id}/reabertura", tarefa)
                .with(denis())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cenario.comOrigem(tarefa, denis(), "\"motivo\": \"defeito\"")));

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.etapas[3].raias[0].tarefas", Matchers.hasSize(0)));
    }
}
