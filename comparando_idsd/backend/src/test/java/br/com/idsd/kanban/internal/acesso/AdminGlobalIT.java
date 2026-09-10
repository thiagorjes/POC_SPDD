package br.com.idsd.kanban.internal.acesso;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ADMIN_GLOBAL;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.adminGlobal;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static br.com.idsd.kanban.suporte.Sujeitos.comoSub;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RF-021 — administracao global.
 *
 * <p>E o mecanismo de autorizacao mais poderoso do sistema, e o unico que
 * atravessa a fronteira entre visibilidade e execucao. ADR-010 o desenhou por
 * {@code sub} verificado, com promocao unica e auditada, porque a versao
 * anterior casava a claim {@code email} — mutavel e, em realm com autocadastro,
 * atribuivel por quem se registra — com autoprovisionamento na mesma chamada.
 *
 * <p>SCN-021.3 e o cenario que importa mais: o alcance e de escopo, nao de
 * imunidade.
 */
class AdminGlobalIT extends TesteDeIntegracao {

    @Test
    @DisplayName("SCN-021.1 — a promocao registra quem e quando, e a segunda e recusada")
    void promocaoEUnicaEAuditada() throws Exception {
        // Nao existe admin global ainda; a property de bootstrap aponta para o
        // `sub` designado. A entrada dessa pessoa promove.
        mockMvc.perform(get("/v1/sessao").with(adminGlobal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminGlobal").value(true));

        mockMvc.perform(get("/v1/administracao/promocoes").with(adminGlobal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].subjectId").value(SUB_ADMIN_GLOBAL))
                .andExpect(jsonPath("$[0].promovidoEm").isNotEmpty());

        // Segunda pessoa entrando pelo mesmo caminho: ja existe admin global,
        // logo ninguem mais e promovido. Sem esta recusa o bypass universal
        // ficaria a uma requisicao de distancia de qualquer um.
        mockMvc.perform(get("/v1/sessao")
                        .with(comoSub("55555555-5555-5555-5555-555555555555",
                                "Impostor", "admin@empresa.example")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminGlobal").value(false));

        mockMvc.perform(get("/v1/administracao/promocoes").with(adminGlobal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("SCN-021.1 — o e-mail coincidente nao promove ninguem; a chave e o sub")
    void emailCoincidenteNaoPromove() throws Exception {
        // A conta abaixo declara o mesmo e-mail da designada, mas outro `sub`.
        // Em realm com autocadastro isso e atribuivel por quem se registra.
        mockMvc.perform(get("/v1/sessao")
                        .with(comoSub("66666666-6666-6666-6666-666666666666",
                                "Outro", "admin@empresa.example")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminGlobal").value(false));
    }

    @Test
    @DisplayName("SCN-021.2 — o admin global ve e age em projeto do qual nao participa")
    void adminGlobalAtravessaOProjeto() throws Exception {
        var projeto = cenario.projeto("Projeto alheio");
        cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "project_admin");
        var etapas = cenario.fluxoPadrao(projeto, bruno());
        var tarefa = cenario.criarTarefa(projeto, "Tarefa qualquer", bruno());

        mockMvc.perform(get("/v1/sessao").with(adminGlobal()));

        mockMvc.perform(get("/v1/projetos").with(adminGlobal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[?(@.nome=='Projeto alheio')]").exists())
                // O alcance e visivel na resposta, e nao implicito.
                .andExpect(jsonPath("$.conteudo[?(@.nome=='Projeto alheio')].acessoPorAdministracaoGlobal")
                        .value(true));

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(adminGlobal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acessoPorAdministracaoGlobal").value(true))
                .andExpect(jsonPath("$.etapas", Matchers.hasSize(4)));

        // E age: mover e escrita, e a checagem de participacao cede ao alcance.
        mockMvc.perform(post("/v1/tarefas/{id}/movimentos", tarefa)
                        .with(adminGlobal())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, adminGlobal(),
                                "\"etapaDestinoId\": \"" + etapas.get(1).id() + "\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"));
    }

    @Test
    @DisplayName("SCN-021.3 — nenhuma visao oferece tempo agregado por pessoa, nem para o admin global")
    void alcanceGlobalNaoAbreRecortePorPessoa() throws Exception {
        var projeto = cenario.projeto("Projeto alheio");
        cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "project_admin");
        cenario.fluxoPadrao(projeto, bruno());
        mockMvc.perform(get("/v1/sessao").with(adminGlobal()));

        // A resposta nao tem campo de pessoa. Nao ha o que filtrar porque a
        // coluna nao existe no esquema — a garantia e estrutural, e se
        // dependesse de verificacao por papel o admin global seria justamente
        // o papel que a dispensaria.
        mockMvc.perform(get("/v1/projetos/{id}/tempo-por-etapa", projeto).with(adminGlobal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..responsavel").doesNotExist())
                .andExpect(jsonPath("$..usuario").doesNotExist())
                .andExpect(jsonPath("$..pessoa").doesNotExist());

        mockMvc.perform(get("/v1/projetos/{id}/tempo-por-etapa", projeto)
                        .param("usuarioId", SUB_BRUNO)
                        .with(adminGlobal()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("SCN-021.3 — a alteracao de tempo ja contado e recusada tambem para o admin global")
    void alcanceGlobalNaoAlteraTempoJaContado() throws Exception {
        var projeto = cenario.projeto("Projeto alheio");
        cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "project_admin");
        var etapas = cenario.fluxoPadrao(projeto, bruno());
        var tarefa = cenario.criarTarefa(projeto, "Tarefa qualquer", bruno());
        cenario.mover(tarefa, etapas.get(1).id(), bruno());
        mockMvc.perform(get("/v1/sessao").with(adminGlobal()));

        // Nenhuma rota de alteracao de tempo existe, para ele nem para
        // ninguem. A verificacao e por ausencia, e e desconfortavel de
        // proposito: e o que impede a erosao silenciosa.
        mockMvc.perform(patch("/v1/tarefas/{id}/intervalos", tarefa)
                        .with(adminGlobal())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"tipo\": \"PERMANENCIA\", \"inicio\": \"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(put("/v1/tarefas/{id}/eventos", tarefa)
                        .with(adminGlobal())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().is4xxClientError());
    }
}
