package br.com.idsd.kanban.internal.projeto;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * RF-018 — raias.
 *
 * <p>A raia organiza a leitura do board e nao e dimensao de estado: ela nao
 * entra em nenhuma agregacao de tempo. SCN-018.3 verifica exatamente essa
 * ausencia e vive com as consultas, em {@code TempoPorEtapaIT}.
 */
class RaiasIT extends TesteDeIntegracao {

    @Test
    @DisplayName("SCN-018.1 — as raias configuradas aparecem no board, na ordem declarada")
    void raiasConfiguradasAparecemNoBoard() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "project_admin");
        cenario.fluxoPadrao(projeto, ana());

        mockMvc.perform(put("/v1/projetos/{id}/raias", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "raias": [
                                  { "nome": "Produto",  "ordem": 1 },
                                  { "nome": "Suporte",  "ordem": 2 }
                                ] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raias", Matchers.hasSize(2)));

        // Toda etapa carrega todas as raias, inclusive as vazias: a grade do
        // board e o produto cartesiano etapa x raia.
        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[0].raias", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.etapas[0].raias[0].nome").value("Produto"))
                .andExpect(jsonPath("$.etapas[3].raias[1].nome").value("Suporte"));
    }

    @Test
    @DisplayName("SCN-018.2 — a tarefa criada na raia aparece nela, e mover de etapa nao muda a raia")
    void tarefaPermaneceNaRaiaAoMudarDeEtapa() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "project_admin");
        var etapas = cenario.fluxoPadrao(projeto, ana());
        String raias = mockMvc.perform(put("/v1/projetos/{id}/raias", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "raias": [
                                  { "nome": "Produto", "ordem": 1 },
                                  { "nome": "Suporte", "ordem": 2 }
                                ] }
                                """))
                .andReturn().getResponse().getContentAsString();
        String suporte = JsonPath.read(raias, "$.raias[1].id");

        String criada = mockMvc.perform(post("/v1/projetos/{id}/tarefas", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Chamado do cliente\", \"raiaId\": \"" + suporte + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var tarefa = java.util.UUID.fromString(JsonPath.read(criada, "$.id"));

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(jsonPath("$.etapas[0].raias[1].tarefas", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas", Matchers.hasSize(0)));

        cenario.mover(tarefa, etapas.get(1).id(), ana());

        // Etapa e raia sao eixos independentes da grade. Mover na horizontal
        // nao arrasta na vertical.
        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(jsonPath("$.etapas[0].raias[1].tarefas", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.etapas[1].raias[1].tarefas", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.etapas[1].raias[1].tarefas[0].raiaId").value(suporte));
    }

    @Test
    @DisplayName("SCN-018.1 — sem raia configurada o board ainda desenha, com a raia unica")
    void semRaiaConfiguradaOBoardAindaDesenha() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "project_admin");
        cenario.fluxoPadrao(projeto, ana());

        // Raia e organizacao opcional. Se a estrutura da resposta dependesse
        // de haver raia, o board de um projeto novo nao renderizaria.
        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[0].raias", Matchers.hasSize(1)));
    }
}
