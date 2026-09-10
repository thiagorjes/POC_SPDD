package br.com.idsd.kanban.internal.tarefa;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_CARLA;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static br.com.idsd.kanban.suporte.Sujeitos.carla;
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

/** RF-007 — tomada de tarefa. */
class TomadaIT extends TesteDeIntegracao {

    private UUID projeto;
    private java.util.List<br.com.idsd.kanban.suporte.Cenario.Etapa> etapas;

    @BeforeEach
    void projetoPronto() {
        projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        cenario.participante(projeto, SUB_CARLA, "Carla", "carla@empresa.example", "gestor");
        etapas = cenario.fluxoPadrao(projeto, ana());
    }

    @Test
    @DisplayName("SCN-007.1 — assumir encerra a espera de tomada e nomeia o responsavel")
    void assumirEncerraAEsperaENomeiaOResponsavel() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Corrigir o relatorio", ana());
        cenario.recuarInicioDoIntervalo(tarefa, "ESPERA_TOMADA", Duration.ofMinutes(60));

        mockMvc.perform(post("/v1/tarefas/{id}/tomada", tarefa)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, bruno(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condicao").value("EM_CURSO"))
                .andExpect(jsonPath("$.responsavel.nome").value("Bruno"))
                .andExpect(jsonPath("$.assumidaEm").isNotEmpty())
                // A espera fecha; a permanencia na etapa nao e tocada, porque
                // a tomada nao move a tarefa.
                .andExpect(jsonPath("$.esperaTomada").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.permanencia.desde").isNotEmpty());

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.log[-1].tipo").value("TAREFA_ASSUMIDA"))
                .andExpect(jsonPath("$.log[-1].ator.nome").value("Bruno"));
    }

    @Test
    @DisplayName("SCN-007.3 — assumir tarefa que ja tem outro responsavel devolve 409")
    void assumirTarefaJaAssumidaDevolve409() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Disputada", ana());
        String origemDeAna = cenario.comOrigem(tarefa, ana(), null);
        cenario.assumir(tarefa, bruno());

        mockMvc.perform(post("/v1/tarefas/{id}/tomada", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(origemDeAna))
                .andExpect(status().isConflict())
                // Quem perdeu a corrida precisa saber para quem perdeu, ou a
                // tela so pode dizer "tente de novo".
                .andExpect(jsonPath("$.estadoAtual.responsavel.nome").value("Bruno"))
                .andExpect(jsonPath("$.estadoAtual.condicao").value("EM_CURSO"));
    }

    @Test
    @DisplayName("SCN-007.2 — assumir de novo a propria tarefa nao altera nada")
    void assumirDeNovoAPropriaTarefaNaoAlteraNada() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Minha", ana());
        cenario.assumir(tarefa, bruno());
        String antes = mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(bruno()))
                .andReturn().getResponse().getContentAsString();
        String assumidaEm = JsonPath.read(antes, "$.assumidaEm");

        // Idempotencia: o mesmo clique repetido nao pode mover o instante da
        // tomada, ou o tempo de trabalho encolheria a cada repeticao.
        mockMvc.perform(post("/v1/tarefas/{id}/tomada", tarefa)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, bruno(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assumidaEm").value(assumidaEm))
                .andExpect(jsonPath("$.responsavel.nome").value("Bruno"));

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(bruno()))
                .andExpect(jsonPath("$.log[?(@.tipo=='TAREFA_ASSUMIDA')]", Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("SCN-007.4 — a tarefa impedida pode ser assumida, e a marca continua")
    void tarefaImpedidaPodeSerAssumida() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada no pool", ana());
        cenario.abrirImpedimento(tarefa, "falta a especificacao", ana());
        cenario.recuarInicioDoIntervalo(tarefa, "IMPEDIMENTO", Duration.ofMinutes(90));

        // RN-033: o impedimento nao e pre-condicao da tomada. Recusar aqui
        // deixaria a tarefa travada no pool sem ninguem que possa destrava-la
        // — quem assume assume tambem o trabalho de resolver.
        mockMvc.perform(post("/v1/tarefas/{id}/tomada", tarefa)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, bruno(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condicao").value("EM_CURSO"))
                .andExpect(jsonPath("$.responsavel.nome").value("Bruno"))
                // A tomada nao toca a marca: ela e derivada do impedimento sem
                // desfecho, e nenhuma escrita sobre `tarefa` a alcanca.
                .andExpect(jsonPath("$.impedimento.motivo").value("falta a especificacao"))
                .andExpect(jsonPath("$.impedimento.decorrido", Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("SCN-007.1 — quem so tem leitura nao assume tarefa")
    void somenteLeituraNaoAssume() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Do time", ana());

        mockMvc.perform(post("/v1/tarefas/{id}/tomada", tarefa)
                        .with(carla())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, carla(), null)))
                .andExpect(status().isForbidden());
    }
}
