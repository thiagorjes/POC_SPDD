package br.com.idsd.kanban.internal.tarefa;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_CARLA;
import static br.com.idsd.kanban.suporte.Sujeitos.adminGlobal;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static br.com.idsd.kanban.suporte.Sujeitos.carla;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * RF-004 — criacao de tarefa.
 *
 * <p>SCN-004.2 (recusa sem titulo) e tipado {@code unitario} e vive tambem em
 * {@code CriacaoDeTarefaServiceTest}. Esta o cobre pelo contrato porque a
 * costura interna que o teste unitario fixa pode mudar sem que o cenario mude,
 * e o gate nao pode depender dela.
 */
class CriacaoDeTarefaIT extends TesteDeIntegracao {

    private UUID projeto;
    private java.util.List<br.com.idsd.kanban.suporte.Cenario.Etapa> etapas;

    @BeforeEach
    void projetoPronto() {
        projeto = cenario.projeto("Alfa");
        // Ana escreve e configura; os papeis sao acumulaveis por BDR-001.
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.participante(projeto, SUB_CARLA, "Carla", "carla@empresa.example", "gestor");
        etapas = cenario.fluxoPadrao(projeto, ana());
    }

    @Test
    @DisplayName("SCN-004.1 — a tarefa nasce na primeira etapa, aguardando tomada e sem responsavel")
    void tarefaNasceNaPrimeiraEtapaAguardandoTomada() throws Exception {
        var resposta = mockMvc.perform(post("/v1/projetos/{id}/tarefas", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Corrigir o relatorio\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.titulo").value("Corrigir o relatorio"))
                .andExpect(jsonPath("$.etapaId").value(etapas.get(0).id().toString()))
                // As tres dimensoes na criacao: primeira etapa, aguardando
                // tomada, sem marca de impedimento (RN-002).
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"))
                .andExpect(jsonPath("$.responsavel").doesNotExist())
                .andExpect(jsonPath("$.impedimento").value(Matchers.nullValue()))
                .andReturn().getResponse().getContentAsString();

        var tarefa = UUID.fromString(com.jayway.jsonpath.JsonPath.read(resposta, "$.id"));

        // Duas contagens comecam juntas e correm em paralelo: a permanencia na
        // etapa e a espera de tomada. Somar as duas e o erro que RN-008 proibe.
        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.permanencia.desde").isNotEmpty())
                .andExpect(jsonPath("$.esperaTomada.desde").isNotEmpty())
                .andExpect(jsonPath("$.log[0].tipo").value("TAREFA_CRIADA"))
                .andExpect(jsonPath("$.log[0].episodio").value(1));
    }

    @Test
    @DisplayName("SCN-004.2 — criacao sem titulo e recusada, e nada e criado")
    void criacaoSemTituloERecusada() throws Exception {
        mockMvc.perform(post("/v1/projetos/{id}/tarefas", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"   \"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                // O campo em falta e nomeado: a tela precisa saber onde por a
                // mensagem (RNF-003).
                .andExpect(jsonPath("$.errors[*].campo", Matchers.hasItem("titulo")));

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas", Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("SCN-004.2 — corpo malformado devolve 400 e nao 500")
    void corpoMalformadoE400() throws Exception {
        mockMvc.perform(post("/v1/projetos/{id}/tarefas", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("SCN-004.3 — quem so tem leitura nao cria tarefa")
    void somenteLeituraNaoCria() throws Exception {
        // `gestor` e somente-leitura por RN-015. A restricao e do servidor e
        // nao da tela: esconder o botao nao e a garantia.
        mockMvc.perform(post("/v1/projetos/{id}/tarefas", projeto)
                        .with(carla())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Nao deveria nascer\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas", Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("SCN-004.3 — o board continua legivel para quem so le")
    void somenteLeituraContinuaLendo() throws Exception {
        cenario.criarTarefa(projeto, "Visivel para o gestor", ana());

        // A recusa e de escrita, e nao de acesso: retirar a leitura junto
        // esvaziaria a razao de o gestor participar.
        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(carla()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas", Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("SCN-022.3 — projeto recem-criado recusa tarefa ate o fluxo ser configurado")
    void projetoNasceSemFluxoERecusaTarefa() throws Exception {
        // O percurso e o de instalacao de verdade, do zero: entrar, criar o
        // projeto pela rota e so entao descobrir que ele ainda nao serve para
        // nada. Semear o projeto por SQL aqui pularia justamente o passo cuja
        // consequencia o cenario existe para tornar visivel.
        mockMvc.perform(get("/v1/sessao").with(bruno())).andExpect(status().isOk());
        var brunoId = cenario.idDoUsuarioPorSub(SUB_BRUNO);

        var criado = mockMvc.perform(post("/v1/projetos")
                        .with(adminGlobal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Recem-criado", "primeiroAdministradorId": "%s" }
                                """.formatted(brunoId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var novo = UUID.fromString(com.jayway.jsonpath.JsonPath.read(criado, "$.id"));

        // Nem quem administra o projeto cria tarefa antes do fluxo: a recusa e
        // do estado do projeto, nao de permissao (RN-038).
        mockMvc.perform(post("/v1/projetos/{id}/tarefas", novo)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Cedo demais\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").isNotEmpty());

        // Custo declarado do desenho: o caminho de partida tem dois passos
        // obrigatorios, e nada no primeiro lembra o segundo. Depois do segundo,
        // a mesma requisicao passa.
        cenario.fluxoPadrao(novo, bruno());

        mockMvc.perform(post("/v1/projetos/{id}/tarefas", novo)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Agora sim\"}"))
                .andExpect(status().isCreated());
    }
}
