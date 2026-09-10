package br.com.idsd.kanban.internal.tarefa;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_CARLA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_DENIS;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static br.com.idsd.kanban.suporte.Sujeitos.carla;
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
 * RF-009 e RF-010 — sinalizacao e resolucao de impedimento.
 *
 * <p>SCN-009.2 e SCN-010.3 sao tipados {@code unitario} e vivem tambem em
 * {@code ImpedimentoServiceTest}. Aqui recebem cobertura pelo contrato, para
 * que o gate nao dependa da costura interna que aqueles testes fixam.
 */
class ImpedimentoIT extends TesteDeIntegracao {

    private UUID projeto;
    private java.util.List<br.com.idsd.kanban.suporte.Cenario.Etapa> etapas;

    @BeforeEach
    void projetoPronto() {
        projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        cenario.participante(projeto, SUB_CARLA, "Carla", "carla@empresa.example", "gestor");
        cenario.participante(projeto, SUB_DENIS, "Denis", "denis@empresa.example", "product_owner");
        etapas = cenario.fluxoPadrao(projeto, ana());
    }

    @Test
    @DisplayName("SCN-009.1 — sinalizar acende a marca e inicia a terceira contagem")
    void sinalizarAcendeAMarcaEIniciaAContagem() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Corrigir o relatorio", ana());
        cenario.assumir(tarefa, bruno());

        mockMvc.perform(post("/v1/tarefas/{id}/impedimentos", tarefa)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, bruno(),
                                "\"motivo\": \"aguardando o fornecedor\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.motivo").value("aguardando o fornecedor"))
                .andExpect(jsonPath("$.desfecho").value(Matchers.nullValue()));

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                // A condicao nao muda: o impedimento e ortogonal a ela. Se
                // aparecesse `IMPEDIDA` aqui, a modelagem teria colapsado duas
                // dimensoes numa.
                .andExpect(jsonPath("$.condicao").value("EM_CURSO"))
                .andExpect(jsonPath("$.responsavel.nome").value("Bruno"))
                .andExpect(jsonPath("$.impedimento.desde").isNotEmpty())
                .andExpect(jsonPath("$.log[-1].tipo").value("IMPEDIMENTO_SINALIZADO"));
    }

    @Test
    @DisplayName("SCN-009.2 — sinalizar sem motivo e recusado e nenhuma marca acende")
    void sinalizarSemMotivoERecusado() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Corrigir o relatorio", ana());

        mockMvc.perform(post("/v1/tarefas/{id}/impedimentos", tarefa)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, ana(), "\"motivo\": \"   \"")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[*].campo", Matchers.hasItem("motivo")));

        // Marca sem motivo seria marca que ninguem sabe explicar, e o painel
        // de impedimentos existe justamente para dizer por que as coisas param.
        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.impedimento").value(Matchers.nullValue()));
    }

    @Test
    @DisplayName("SCN-009.3 — sinalizar de novo anota no impedimento aberto, sem abrir outro")
    void sinalizarDeNovoAnotaSemAbrirOutro() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada", ana());
        var impedimento = cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", ana());
        cenario.recuarInicioDoIntervalo(tarefa, "IMPEDIMENTO", Duration.ofMinutes(100));

        String antes = mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andReturn().getResponse().getContentAsString();
        Number decorridoAntes = JsonPath.read(antes, "$.impedimento.decorrido");

        mockMvc.perform(post("/v1/tarefas/{id}/impedimentos", tarefa)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, bruno(),
                                "\"motivo\": \"o fornecedor respondeu, falta o aceite\"")))
                // `200` e nao `201`: nada foi criado, a nova razao foi anotada
                // no impedimento que ja estava aberto.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(impedimento.toString()))
                .andExpect(jsonPath("$.anotacoes", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.anotacoes[0].texto")
                        .value("o fornecedor respondeu, falta o aceite"));

        // Abrir um segundo intervalo reiniciaria a contagem e apagaria o tempo
        // em que a tarefa esteve travada — que e a metrica que motivou o RF.
        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.impedimento.decorrido",
                        Matchers.greaterThanOrEqualTo(decorridoAntes.intValue())))
                .andExpect(jsonPath("$.log[?(@.tipo=='IMPEDIMENTO_SINALIZADO')]",
                        Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("SCN-010.1 — registrar o desfecho apaga a marca e fecha a contagem")
    void registrarODesfechoApagaAMarca() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada", ana());
        var impedimento = cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", ana());
        cenario.recuarInicioDoIntervalo(tarefa, "IMPEDIMENTO", Duration.ofMinutes(120));

        mockMvc.perform(post("/v1/tarefas/{t}/impedimentos/{i}/resolucao", tarefa, impedimento)
                        .with(denis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, denis(),
                                "\"desfecho\": \"o fornecedor entregou\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desfecho").value("o fornecedor entregou"))
                .andExpect(jsonPath("$.resolvidoPor.nome").value("Denis"));

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                // RN-032: e a unica operacao que apaga a marca.
                .andExpect(jsonPath("$.impedimento").value(Matchers.nullValue()))
                // A condicao nao e "devolvida" a nada, porque a abertura nunca
                // a alterou.
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"))
                .andExpect(jsonPath("$.log[-1].tipo").value("IMPEDIMENTO_RESOLVIDO"));

        // O tempo contado permanece no historico: o intervalo fechou, nao
        // sumiu.
        mockMvc.perform(get("/v1/projetos/{id}/tempo-por-etapa", projeto).with(ana()))
                .andExpect(jsonPath("$.etapas[?(@.nome=='Backlog')].impedimento",
                        Matchers.everyItem(Matchers.greaterThan(0))));
    }

    @Test
    @DisplayName("SCN-010.2 — quem nao pode desbloquear nem abriu o impedimento recebe 403")
    void semPermissaoDeDesbloquearE403() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada", ana());
        var impedimento = cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", ana());

        // Carla so le (RN-015) e nao abriu este impedimento.
        mockMvc.perform(post("/v1/tarefas/{t}/impedimentos/{i}/resolucao", tarefa, impedimento)
                        .with(carla())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, carla(), "\"desfecho\": \"resolvido\"")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.impedimento").isNotEmpty());
    }

    @Test
    @DisplayName("SCN-010.2 — quem abriu o impedimento pode resolve-lo sem papel de desbloqueio")
    void quemAbriuPodeResolver() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada", ana());
        cenario.assumir(tarefa, bruno());
        var impedimento = cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", bruno());

        // BDR-002 poe o desbloqueio em `project_admin` e `product_owner`, e
        // SCN-019.3 depende de este fallback existir: quem sinalizou sabe
        // quando deixou de estar travado, e obrigar um terceiro a confirmar
        // travaria o fluxo pelo processo.
        mockMvc.perform(post("/v1/tarefas/{t}/impedimentos/{i}/resolucao", tarefa, impedimento)
                        .with(bruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, bruno(),
                                "\"desfecho\": \"resolvi por conta propria\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desfecho").value("resolvi por conta propria"));
    }

    @Test
    @DisplayName("SCN-010.3 — resolver de novo devolve 200 sem alterar o desfecho nem o tempo")
    void resolverDeNovoNaoAlteraNada() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada", ana());
        var impedimento = cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", ana());
        cenario.recuarInicioDoIntervalo(tarefa, "IMPEDIMENTO", Duration.ofMinutes(120));

        String primeira = mockMvc.perform(
                        post("/v1/tarefas/{t}/impedimentos/{i}/resolucao", tarefa, impedimento)
                                .with(denis())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cenario.comOrigem(tarefa, denis(),
                                        "\"desfecho\": \"o fornecedor entregou\"")))
                .andReturn().getResponse().getContentAsString();
        String resolvidoEm = JsonPath.read(primeira, "$.resolvidoEm");

        mockMvc.perform(post("/v1/tarefas/{t}/impedimentos/{i}/resolucao", tarefa, impedimento)
                        .with(denis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, denis(),
                                "\"desfecho\": \"outro desfecho qualquer\"")))
                .andExpect(status().isOk())
                // O desfecho e o instante da resolucao sao os da primeira vez:
                // mover o `fim` reescreveria tempo ja contado, que RNF-008
                // protege.
                .andExpect(jsonPath("$.desfecho").value("o fornecedor entregou"))
                .andExpect(jsonPath("$.resolvidoEm").value(resolvidoEm));

        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.log[?(@.tipo=='IMPEDIMENTO_RESOLVIDO')]",
                        Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("SCN-010.1 — resolver um impedimento nao apaga a marca de outra tarefa")
    void resolucaoNaoVazaEntreTarefas() throws Exception {
        var travada = cenario.criarTarefa(projeto, "Travada", ana());
        var tambemTravada = cenario.criarTarefa(projeto, "Tambem travada", ana());
        var impedimento = cenario.abrirImpedimento(travada, "motivo A", ana());
        cenario.abrirImpedimento(tambemTravada, "motivo B", ana());

        mockMvc.perform(post("/v1/tarefas/{t}/impedimentos/{i}/resolucao", travada, impedimento)
                .with(denis())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cenario.comOrigem(travada, denis(), "\"desfecho\": \"pronto\"")));

        mockMvc.perform(get("/v1/tarefas/{id}", tambemTravada).with(ana()))
                .andExpect(jsonPath("$.impedimento.motivo").value("motivo B"));
    }
}
