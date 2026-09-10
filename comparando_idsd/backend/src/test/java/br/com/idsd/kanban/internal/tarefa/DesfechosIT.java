package br.com.idsd.kanban.internal.tarefa;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_DENIS;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static br.com.idsd.kanban.suporte.Sujeitos.denis;
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
 * RF-011 — conclusao, e RF-012 — encerramento sem conclusao.
 *
 * <p>SCN-012.4 e o cenario que a emenda v1.2 do PRD criou: o encerramento com
 * impedimento aberto e <b>recusado</b>, e nao resolvido em cascata. Fechar em
 * cascata produziria desfecho que ninguem afirmou, que e a razao registrada em
 * B-04.
 */
class DesfechosIT extends TesteDeIntegracao {

    private UUID projeto;
    private java.util.List<br.com.idsd.kanban.suporte.Cenario.Etapa> etapas;

    @BeforeEach
    void projetoPronto() {
        projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        cenario.participante(projeto, SUB_DENIS, "Denis", "denis@empresa.example", "product_owner");
        etapas = cenario.fluxoPadrao(projeto, ana());
    }

    private UUID tarefaNaEtapaTerminal() {
        var tarefa = cenario.criarTarefa(projeto, "Quase pronta", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());
        cenario.mover(tarefa, etapas.get(2).id(), ana());
        cenario.mover(tarefa, etapas.get(3).id(), ana());
        return tarefa;
    }

    // ------------------------------------------------------------------ RF-011

    @Test
    @DisplayName("SCN-011.1 — a tarefa na etapa terminal fica concluida e todas as contagens cessam")
    void tarefaNaEtapaTerminalFicaConcluida() throws Exception {
        var tarefa = tarefaNaEtapaTerminal();

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condicao").value("CONCLUIDA"))
                .andExpect(jsonPath("$.etapaId").value(etapas.get(3).id().toString()))
                // Concluida e estado final do episodio: nada continua correndo.
                .andExpect(jsonPath("$.permanencia").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.esperaTomada").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.log[-1].tipo").value("TAREFA_CONCLUIDA"));
    }

    @Test
    @DisplayName("SCN-011.2 — concluir fora da etapa terminal e recusado")
    void concluirForaDaEtapaTerminalERecusado() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "No comeco", ana());

        // A rota existe porque o cenario congela uma recusa, e recusa sem
        // superficie nao tem o que exercitar. Ela nunca transiciona nada: a
        // conclusao continua sendo efeito de chegar a etapa terminal.
        mockMvc.perform(post("/v1/tarefas/{id}/conclusao", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, ana(), null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"));
    }

    @Test
    @DisplayName("SCN-011.2 — concluir tarefa ja concluida devolve 200 sem alterar nada")
    void concluirTarefaJaConcluidaEIdempotente() throws Exception {
        var tarefa = tarefaNaEtapaTerminal();
        String antes = mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andReturn().getResponse().getContentAsString();
        Number logAntes = ((java.util.List<?>) JsonPath.read(antes, "$.log")).size();

        mockMvc.perform(post("/v1/tarefas/{id}/conclusao", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, ana(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condicao").value("CONCLUIDA"))
                .andExpect(jsonPath("$.log", Matchers.hasSize(logAntes.intValue())));
    }

    @Test
    @DisplayName("SCN-011.3 — concluir com impedimento aberto e recusado")
    void concluirComImpedimentoAbertoERecusado() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());
        cenario.mover(tarefa, etapas.get(2).id(), ana());
        cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", ana());

        // RN-011: a marca impoe duas restricoes, e esta e a primeira. Concluir
        // com impedimento aberto deixaria o intervalo aberto para sempre numa
        // tarefa que ja saiu do fluxo.
        mockMvc.perform(post("/v1/tarefas/{id}/movimentos", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, ana(),
                                "\"etapaDestinoId\": \"" + etapas.get(3).id() + "\"")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").isNotEmpty());

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"))
                .andExpect(jsonPath("$.etapaId").value(etapas.get(2).id().toString()));
    }

    @Test
    @DisplayName("SCN-011.3 — resolvido o impedimento, a conclusao passa a ser aceita")
    void resolvidoOImpedimentoAConclusaoPassa() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());
        cenario.mover(tarefa, etapas.get(2).id(), ana());
        var impedimento = cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", ana());

        mockMvc.perform(post("/v1/tarefas/{t}/impedimentos/{i}/resolucao", tarefa, impedimento)
                .with(denis())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cenario.comOrigem(tarefa, denis(), "\"desfecho\": \"entregue\"")));

        cenario.mover(tarefa, etapas.get(3).id(), ana());

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.condicao").value("CONCLUIDA"));
    }

    // ------------------------------------------------------------------ RF-012

    @Test
    @DisplayName("SCN-012.1 — encerrar sem conclusao registra o motivo e cessa as duas contagens")
    void encerrarSemConclusaoRegistraOMotivo() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Nao vai acontecer", ana());
        cenario.recuarInicioDoIntervalo(tarefa, "PERMANENCIA", Duration.ofMinutes(60));

        mockMvc.perform(post("/v1/tarefas/{id}/encerramento", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, ana(),
                                "\"motivo\": \"o cliente desistiu\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condicao").value("ENCERRADA_SEM_CONCLUSAO"));

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                // As duas contagens que podiam estar correndo: permanencia e
                // espera de tomada. A de impedimento nao entra na lista porque
                // a requisicao com marca acesa e recusada antes (SCN-012.4).
                .andExpect(jsonPath("$.permanencia").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.esperaTomada").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.log[-1].tipo").value("TAREFA_ENCERRADA_SEM_CONCLUSAO"))
                .andExpect(jsonPath("$.log[-1].motivo").value("o cliente desistiu"));
    }

    @Test
    @DisplayName("SCN-012.2 — a tarefa encerrada permanece no historico e no tempo por etapa")
    void tarefaEncerradaPermaneceNoHistorico() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Nao vai acontecer", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());
        cenario.recuarInicioDoIntervalo(tarefa, "PERMANENCIA", Duration.ofMinutes(60));
        mockMvc.perform(post("/v1/tarefas/{id}/encerramento", tarefa)
                .with(ana())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cenario.comOrigem(tarefa, ana(), "\"motivo\": \"o cliente desistiu\"")));

        // Encerrar nao e excluir. Excluir apagaria o tempo por etapa que a
        // tarefa ja tinha produzido — foi por isso que a exclusao saiu do
        // escopo no `/design`.
        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.log[0].tipo").value("TAREFA_CRIADA"));

        mockMvc.perform(get("/v1/projetos/{id}/tempo-por-etapa", projeto).with(ana()))
                .andExpect(jsonPath("$.etapas[?(@.nome=='Backlog')].tempoTotal",
                        Matchers.everyItem(Matchers.greaterThan(0))));
    }

    @Test
    @DisplayName("SCN-012.3 — quem nao tem permissao de encerrar recebe 403")
    void semPermissaoNaoEncerra() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Nao vai acontecer", ana());

        mockMvc.perform(post("/v1/tarefas/{id}/encerramento", tarefa)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, bruno(), "\"motivo\": \"desisti\"")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"));
    }

    @Test
    @DisplayName("SCN-012.4 — encerrar com impedimento aberto e recusado")
    void encerrarComImpedimentoAbertoERecusado() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada e inviavel", ana());
        cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", ana());

        // A segunda restricao de RN-011. Nao e excecao a RN-032: o encerramento
        // e recusado enquanto a marca existir, e por isso o evento de
        // encerramento nunca chega a ser gravado com ela acesa.
        mockMvc.perform(post("/v1/tarefas/{id}/encerramento", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, ana(), "\"motivo\": \"inviavel\"")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").isNotEmpty());

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"))
                .andExpect(jsonPath("$.impedimento.motivo").value("aguardando o fornecedor"));
    }

    @Test
    @DisplayName("SCN-012.4 — registrado o desfecho, o encerramento passa a ser aceito")
    void registradoODesfechoOEncerramentoPassa() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada e inviavel", ana());
        var impedimento = cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", ana());

        // Sair do fluxo passa a exigir dois passos, e o primeiro e alguem dizer
        // como o impedimento terminou. O custo operacional foi assumido na
        // emenda v1.2 do PRD.
        mockMvc.perform(post("/v1/tarefas/{t}/impedimentos/{i}/resolucao", tarefa, impedimento)
                .with(denis())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cenario.comOrigem(tarefa, denis(),
                        "\"desfecho\": \"o fornecedor nunca respondeu\"")));

        mockMvc.perform(post("/v1/tarefas/{id}/encerramento", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, ana(), "\"motivo\": \"inviavel\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condicao").value("ENCERRADA_SEM_CONCLUSAO"));
    }
}
