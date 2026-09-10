package br.com.idsd.kanban.internal.projeto;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.adminGlobal;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * RF-022 — criacao de projeto.
 *
 * <p>E a unica rota do sistema cuja autorizacao nao consulta participacao: nao
 * ha participacao a consultar antes de o projeto existir. Ate a emenda de
 * 2026-09-10 ela nao existia, e a propria suite semeava projeto e primeira
 * participacao por SQL — um sistema recem-instalado nao saia do zero por meios
 * proprios.
 *
 * <p>SCN-022.3 nao esta aqui: ele verifica a recusa da criacao de tarefa em
 * projeto sem fluxo, e mora em {@code CriacaoDeTarefaIT}, junto com SCN-004.3,
 * que e a mesma recusa por outro caminho.
 */
class CriacaoDeProjetoIT extends TesteDeIntegracao {

    @Test
    @DisplayName("SCN-022.1 — o admin global cria o projeto e nomeia a primeira project_admin")
    void criaEnomeiaAPrimeiraAdministradora() throws Exception {
        // Bruno precisa existir em `usuario`: ele passa a existir na primeira
        // entrada, pelo autoprovisionamento da sessao. Semear por SQL aqui
        // esconderia justamente a pre-condicao que a rota exige.
        mockMvc.perform(get("/v1/sessao").with(bruno())).andExpect(status().isOk());
        var brunoId = cenario.idDoUsuarioPorSub(SUB_BRUNO);

        var resposta = mockMvc.perform(post("/v1/projetos")
                        .with(adminGlobal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Alfa",
                                  "descricao": "Primeiro projeto",
                                  "primeiroAdministradorId": "%s" }
                                """.formatted(brunoId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Alfa"))
                // Nasce sem fluxo (RN-038). Lista vazia e a resposta correta,
                // nao um estado transitorio.
                .andExpect(jsonPath("$.etapas", Matchers.hasSize(0)))
                .andReturn().getResponse().getContentAsString();

        var projetoId = com.jayway.jsonpath.JsonPath.read(resposta, "$.id").toString();

        // Exatamente uma participacao, a de Bruno, com papel de project_admin.
        mockMvc.perform(get("/v1/projetos/{id}/participacoes", projetoId).with(bruno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].usuarioId").value(brunoId.toString()))
                .andExpect(jsonPath("$[0].papeis", Matchers.contains("project_admin")));

        // Quem cria nao vira participante (RN-037). Ele alcanca o projeto pelo
        // escopo global, e a marca do alcance e a prova de que continua sendo
        // escopo e nao participacao — se virasse participante, a marca sumiria
        // e SCN-021.2 deixaria de valer para este projeto.
        mockMvc.perform(get("/v1/projetos/{id}", projetoId).with(adminGlobal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acessoPorAdministracaoGlobal").value(true));
    }

    @Test
    @DisplayName("SCN-022.2 — quem nao e admin global nao cria projeto")
    void criacaoRecusadaSemAlcanceGlobal() throws Exception {
        mockMvc.perform(get("/v1/sessao").with(bruno())).andExpect(status().isOk());
        var brunoId = cenario.idDoUsuarioPorSub(SUB_BRUNO);

        // Bruno e project_admin de um projeto — o papel mais alto que existe
        // dentro de um projeto — e ainda assim nao cria projeto: a capacidade
        // nao pertence a papel nenhum de projeto (RN-036).
        var existente = cenario.projeto("Ja existente");
        cenario.participante(existente, SUB_BRUNO, "Bruno", "bruno@empresa.example", "project_admin");

        mockMvc.perform(post("/v1/projetos")
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Tentativa", "primeiroAdministradorId": "%s" }
                                """.formatted(brunoId)))
                // `403` e nao `404`: a colecao e conhecida do chamador e nao ha
                // existencia a ocultar. O `404` do detalhe protege outro caso.
                .andExpect(status().isForbidden());

        // Nada foi criado: a lista de Bruno continua com o projeto de antes.
        mockMvc.perform(get("/v1/projetos").with(bruno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].nome").value("Ja existente"));
    }

    @Test
    @DisplayName("RF-022 — nome em branco e pessoa inexistente sao recusados com razao explicita")
    void entradaInvalidaERecusadaComRazao() throws Exception {
        mockMvc.perform(get("/v1/sessao").with(bruno())).andExpect(status().isOk());
        var brunoId = cenario.idDoUsuarioPorSub(SUB_BRUNO);

        mockMvc.perform(post("/v1/projetos")
                        .with(adminGlobal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "   ", "primeiroAdministradorId": "%s" }
                                """.formatted(brunoId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").isNotEmpty());

        // Pessoa que nunca entrou no sistema nao existe em `usuario`, e a recusa
        // precisa dizer isso: e a diferenca entre "erro seu" e "essa pessoa
        // ainda nao entrou uma vez".
        mockMvc.perform(post("/v1/projetos")
                        .with(adminGlobal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Alfa",
                                  "primeiroAdministradorId": "00000000-0000-0000-0000-0000000000ff" }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
