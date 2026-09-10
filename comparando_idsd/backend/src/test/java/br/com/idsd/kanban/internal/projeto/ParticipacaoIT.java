package br.com.idsd.kanban.internal.projeto;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_CARLA;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static br.com.idsd.kanban.suporte.Sujeitos.carla;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * RF-019 — participacoes e papeis.
 *
 * <p>SCN-019.4 — a revogacao derrubando a inscricao no canal em tempo real — e
 * tipado {@code e2e} e vive em Playwright: o que ele afirma envolve duas
 * sessoes de navegador simultaneas.
 */
class ParticipacaoIT extends TesteDeIntegracao {

    private UUID projeto;
    private UUID brunoId;

    private void projetoComAnaAdministrando() {
        projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "project_admin");
        cenario.fluxoPadrao(projeto, ana());
    }

    @Test
    @DisplayName("SCN-019.1 — os papeis concedidos passam a valer imediatamente")
    void papeisConcedidosPassamAValer() throws Exception {
        projetoComAnaAdministrando();
        brunoId = cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example");

        // Sem papel algum a pessoa participa e nada pode fazer alem de ler.
        mockMvc.perform(post("/v1/projetos/{id}/tarefas", projeto)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Tentativa\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/v1/projetos/{p}/participacoes/{u}", projeto, brunoId)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papeis\": [\"dev\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis", Matchers.contains("dev")));

        // Sem novo login: a permissao e resolvida na aplicacao a cada
        // requisicao, e nao carregada no token (ADR-003).
        mockMvc.perform(post("/v1/projetos/{id}/tarefas", projeto)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Agora vai\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("SCN-019.1 — os papeis sao acumulaveis e a permissao e a uniao deles")
    void papeisSaoAcumulaveis() throws Exception {
        projetoComAnaAdministrando();
        brunoId = cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example");

        mockMvc.perform(put("/v1/projetos/{p}/participacoes/{u}", projeto, brunoId)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papeis\": [\"dev\", \"product_owner\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis", Matchers.hasSize(2)));

        mockMvc.perform(get("/v1/projetos/{id}", projeto).with(bruno()))
                .andExpect(jsonPath("$.permissoes", Matchers.hasItem("ESCREVER_TAREFA")))
                .andExpect(jsonPath("$.permissoes", Matchers.hasItem("REABRIR")))
                // BDR-001: acumular papeis soma permissao, e nunca a subtrai.
                .andExpect(jsonPath("$.permissoes", Matchers.not(Matchers.hasItem("CONFIGURAR"))));
    }

    @Test
    @DisplayName("SCN-019.2 — papel fora do catalogo fechado e recusado")
    void papelForaDoCatalogoERecusado() throws Exception {
        projetoComAnaAdministrando();
        brunoId = cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");

        mockMvc.perform(put("/v1/projetos/{p}/participacoes/{u}", projeto, brunoId)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papeis\": [\"superusuario\"]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        // O catalogo e fechado, entao a recusa nao pode deixar residuo: o
        // vinculo anterior segue exatamente como estava.
        mockMvc.perform(get("/v1/projetos/{id}/participacoes", projeto).with(ana()))
                .andExpect(jsonPath("$[?(@.usuario.email=='bruno@empresa.example')].papeis",
                        Matchers.hasItem(Matchers.contains("dev"))));
    }

    @Test
    @DisplayName("SCN-019.3 — removida a participacao, o acesso cessa e a tarefa assumida e devolvida")
    void removerParticipacaoDevolveTarefaEEncerraAcesso() throws Exception {
        projetoComAnaAdministrando();
        brunoId = cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        var tarefa = cenario.criarTarefa(projeto, "Tarefa do Bruno", ana());
        cenario.assumir(tarefa, bruno());

        mockMvc.perform(delete("/v1/projetos/{p}/participacoes/{u}", projeto, brunoId).with(ana()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/projetos/{id}", projeto).with(bruno()))
                .andExpect(status().isNotFound());

        // A tarefa nao fica presa a quem saiu. Volta a espera de tomada, e o
        // evento de devolucao e gravado sem ator: ninguem devolveu, o vinculo
        // acabou.
        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"))
                .andExpect(jsonPath("$.responsavel").doesNotExist())
                .andExpect(jsonPath("$.esperaTomada").isNotEmpty())
                .andExpect(jsonPath("$.log[-1].tipo").value("TAREFA_DEVOLVIDA"))
                .andExpect(jsonPath("$.log[-1].ator").doesNotExist());
    }

    @Test
    @DisplayName("SCN-019.3 — o historico de quem saiu permanece legivel no log da tarefa")
    void historicoDeQuemSaiuPermanece() throws Exception {
        projetoComAnaAdministrando();
        brunoId = cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        var tarefa = cenario.criarTarefa(projeto, "Tarefa do Bruno", ana());
        cenario.assumir(tarefa, bruno());

        mockMvc.perform(delete("/v1/projetos/{p}/participacoes/{u}", projeto, brunoId).with(ana()));

        // RNF-008: o log e imutavel. Remover a participacao nao pode apagar
        // que a tomada aconteceu — o tempo ja contado depende disso.
        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.log[?(@.tipo=='TAREFA_ASSUMIDA')]").exists())
                .andExpect(jsonPath("$.log[?(@.tipo=='TAREFA_ASSUMIDA')].ator.nome",
                        Matchers.hasItem("Bruno")));
    }

    @Test
    @DisplayName("SCN-019.2 — quem nao administra o projeto nao concede papel")
    void semPermissaoNaoConcedePapel() throws Exception {
        projetoComAnaAdministrando();
        brunoId = cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        cenario.participante(projeto, SUB_CARLA, "Carla", "carla@empresa.example", "dev");

        mockMvc.perform(put("/v1/projetos/{p}/participacoes/{u}", projeto, brunoId)
                        .with(carla())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papeis\": [\"project_admin\"]}"))
                .andExpect(status().isForbidden());
    }
}
