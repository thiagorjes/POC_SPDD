package br.com.idsd.kanban.internal.tarefa;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** RF-008 — devolucao da tarefa ao pool. */
class DevolucaoIT extends TesteDeIntegracao {

    private UUID projeto;
    private java.util.List<br.com.idsd.kanban.suporte.Cenario.Etapa> etapas;

    @BeforeEach
    void projetoPronto() {
        projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        etapas = cenario.fluxoPadrao(projeto, ana());
    }

    @Test
    @DisplayName("SCN-008.1 — devolver libera a tarefa e reabre a espera de tomada")
    void devolverLiberaEReabreAEspera() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Corrigir o relatorio", ana());
        cenario.assumir(tarefa, bruno());

        mockMvc.perform(delete("/v1/tarefas/{id}/tomada", tarefa)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, bruno(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"))
                .andExpect(jsonPath("$.responsavel").value(Matchers.nullValue()))
                // Nova espera, e nao a antiga retomada: o intervalo anterior
                // ficou fechado e atribuido ao periodo em que correu.
                .andExpect(jsonPath("$.esperaTomada.desde").isNotEmpty())
                .andExpect(jsonPath("$.esperaTomada.decorrido", Matchers.lessThan(60)));

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.log[-1].tipo").value("TAREFA_DEVOLVIDA"))
                .andExpect(jsonPath("$.log[-1].ator.nome").value("Bruno"));
    }

    @Test
    @DisplayName("SCN-008.1 — devolver nao move a tarefa nem reinicia a permanencia")
    void devolverNaoMoveNemReiniciaAPermanencia() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Corrigir o relatorio", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());
        cenario.assumir(tarefa, bruno());
        cenario.recuarInicioDoIntervalo(tarefa, "PERMANENCIA", Duration.ofMinutes(150));

        String antes = mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andReturn().getResponse().getContentAsString();
        Number permanenciaAntes = JsonPath.read(antes, "$.permanencia.decorrido");

        mockMvc.perform(delete("/v1/tarefas/{id}/tomada", tarefa)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, bruno(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapaId").value(etapas.get(1).id().toString()))
                // Devolucao e mudanca de condicao, e nao de etapa: a serie de
                // permanencia nao tem por que ser tocada.
                .andExpect(jsonPath("$.permanencia.decorrido",
                        Matchers.greaterThanOrEqualTo(permanenciaAntes.intValue())));
    }

    @Test
    @DisplayName("SCN-008.2 — quem nao e o responsavel nao devolve a tarefa")
    void quemNaoEResponsavelNaoDevolve() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Do Bruno", ana());
        cenario.assumir(tarefa, bruno());

        mockMvc.perform(delete("/v1/tarefas/{id}/tomada", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, ana(), null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.responsavel.nome").value("Bruno"));
    }

    @Test
    @DisplayName("SCN-008.3 — a marca de impedimento sobrevive a devolucao")
    void marcaSobreviveADevolucao() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada", ana());
        cenario.assumir(tarefa, bruno());
        cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", bruno());
        cenario.recuarInicioDoIntervalo(tarefa, "IMPEDIMENTO", Duration.ofMinutes(200));

        String antes = mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andReturn().getResponse().getContentAsString();
        Number impedimentoAntes = JsonPath.read(antes, "$.impedimento.decorrido");

        mockMvc.perform(delete("/v1/tarefas/{id}/tomada", tarefa)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, bruno(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"))
                // Devolver nao e resolver. Quem larga a tarefa nao sabe como o
                // impedimento terminou, e RN-032 exige que alguem diga.
                .andExpect(jsonPath("$.impedimento.motivo").value("aguardando o fornecedor"))
                .andExpect(jsonPath("$.impedimento.decorrido",
                        Matchers.greaterThanOrEqualTo(impedimentoAntes.intValue())));
    }

    @Test
    @DisplayName("SCN-008.3 — a tarefa devolvida com impedimento aparece impedida no pool")
    void tarefaDevolvidaComImpedimentoApareceImpedidaNoPool() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada", ana());
        cenario.assumir(tarefa, bruno());
        cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", bruno());
        mockMvc.perform(delete("/v1/tarefas/{id}/tomada", tarefa)
                .with(bruno())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cenario.comOrigem(tarefa, bruno(), null)));

        // As duas dimensoes coexistem no cartao: esperando quem a tome, e
        // impedida. Colapsa-las esconderia uma das duas de quem le o board.
        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas[0].condicao")
                        .value("AGUARDANDO_TOMADA"))
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas[0].esperaTomada").isNotEmpty())
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas[0].impedimento").isNotEmpty());
    }

    @Test
    @DisplayName("SCN-008.2 — devolver tarefa que ninguem assumiu e recusado")
    void devolverTarefaSemResponsavelERecusado() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Sem dono", ana());

        mockMvc.perform(delete("/v1/tarefas/{id}/tomada", tarefa)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, bruno(), null)))
                .andExpect(status().is4xxClientError());
    }
}
