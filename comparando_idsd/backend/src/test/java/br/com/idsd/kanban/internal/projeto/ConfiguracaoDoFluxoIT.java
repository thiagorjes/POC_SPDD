package br.com.idsd.kanban.internal.projeto;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** RF-017 — configuracao do fluxo de etapas do projeto. */
class ConfiguracaoDoFluxoIT extends TesteDeIntegracao {

    @Test
    @DisplayName("SCN-017.1 — o fluxo configurado passa a ser o do board, na ordem declarada")
    void fluxoConfiguradoViraOBoard() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "project_admin");

        mockMvc.perform(put("/v1/projetos/{id}/etapas", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "etapas": [
                                  { "nome": "Backlog",  "ordem": 1, "terminal": false },
                                  { "nome": "Fazendo",  "ordem": 2, "terminal": false },
                                  { "nome": "Feito",    "ordem": 3, "terminal": true  }
                                ] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.etapas[0].nome").value("Backlog"))
                .andExpect(jsonPath("$.etapas[2].terminal").value(true));

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.etapas[1].nome").value("Fazendo"))
                // Coluna sem tarefa e `tarefas: []` e nao ausencia de coluna: o
                // board precisa mostrar a etapa vazia para que se possa mover
                // alguma coisa para dentro dela.
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas", Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("SCN-017.3 — renomear a etapa preserva as tarefas e o tempo ja contado")
    void renomearEtapaPreservaTarefasETempo() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "project_admin");
        var etapas = cenario.fluxoPadrao(projeto, ana());
        var tarefa = cenario.criarTarefa(projeto, "Tarefa em curso", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());
        cenario.recuarInicioDoIntervalo(tarefa, "PERMANENCIA", java.time.Duration.ofMinutes(90));

        String antes = mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andReturn().getResponse().getContentAsString();
        Number decorridoAntes = JsonPath.read(antes, "$.permanencia.decorrido");

        // Renomear e alterar a etapa existente pelo `id`, e nao criar outra:
        // e por isso que a requisicao devolve o mesmo identificador.
        String corpo = """
                { "etapas": [
                  { "id": "%s", "nome": "Backlog",   "ordem": 1, "terminal": false },
                  { "id": "%s", "nome": "Em analise","ordem": 2, "terminal": false },
                  { "id": "%s", "nome": "Review",    "ordem": 3, "terminal": false },
                  { "id": "%s", "nome": "Concluido", "ordem": 4, "terminal": true  }
                ] }
                """.formatted(etapas.get(0).id(), etapas.get(1).id(),
                etapas.get(2).id(), etapas.get(3).id());

        mockMvc.perform(put("/v1/projetos/{id}/etapas", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[1].id").value(etapas.get(1).id().toString()))
                .andExpect(jsonPath("$.etapas[1].nome").value("Em analise"));

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapaId").value(etapas.get(1).id().toString()))
                .andExpect(jsonPath("$.etapaNome").value("Em analise"))
                // RN-009: renomear nao e movimentacao. O intervalo em curso
                // continua o mesmo, e o decorrido nao volta a zero.
                .andExpect(jsonPath("$.permanencia.decorrido",
                        Matchers.greaterThanOrEqualTo(decorridoAntes.intValue())));

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas[0].id").value(tarefa.toString()));
    }

    @Test
    @DisplayName("SCN-017.3 — arquivar etapa com tarefa ativa e recusado, e a etapa e nomeada")
    void arquivarEtapaComTarefaAtivaERecusado() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "project_admin");
        var etapas = cenario.fluxoPadrao(projeto, ana());
        var tarefa = cenario.criarTarefa(projeto, "Tarefa em curso", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());

        // Omitir a etapa da lista e o que a arquiva. Com tarefa ativa dentro,
        // aceitar deixaria a tarefa sem etapa — estado que o board nao sabe
        // desenhar e que o tempo por etapa nao sabe atribuir.
        String semADesenvolvimento = """
                { "etapas": [
                  { "id": "%s", "nome": "Backlog",   "ordem": 1, "terminal": false },
                  { "id": "%s", "nome": "Review",    "ordem": 2, "terminal": false },
                  { "id": "%s", "nome": "Concluido", "ordem": 3, "terminal": true  }
                ] }
                """.formatted(etapas.get(0).id(), etapas.get(2).id(), etapas.get(3).id());

        mockMvc.perform(put("/v1/projetos/{id}/etapas", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(semADesenvolvimento))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors").isNotEmpty())
                .andExpect(jsonPath("$.errors[*].etapaId",
                        Matchers.hasItem(etapas.get(1).id().toString())));

        mockMvc.perform(get("/v1/projetos/{id}/etapas", projeto).with(ana()))
                .andExpect(jsonPath("$.etapas", Matchers.hasSize(4)));
    }

    @Test
    @DisplayName("SCN-017.1 — quem nao pode configurar recebe 403")
    void semPermissaoDeConfigurarE403() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "project_admin");
        cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "product_owner");
        cenario.fluxoPadrao(projeto, ana());

        // `product_owner` reabre e encerra, mas nao configura o fluxo
        // (BDR-001). Papel poderoso nao e papel total.
        mockMvc.perform(put("/v1/projetos/{id}/etapas", projeto)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"etapas\": [ { \"nome\": \"X\", \"ordem\": 1, \"terminal\": true } ] }"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SCN-017.1 — projeto sem fluxo configurado recusa a criacao de tarefa")
    void semFluxoNaoSeCriaTarefa() throws Exception {
        var projeto = cenario.projeto("Sem fluxo");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "project_admin");

        // Nao ha primeira etapa onde a tarefa possa nascer. Recusar aqui e o
        // que impede tarefa orfa de etapa mais adiante.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/v1/projetos/{id}/tarefas", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Qualquer\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
