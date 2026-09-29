package br.com.idsd.kanban.internal.tarefa;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * RF-003 — leitura do board.
 *
 * <p>SCN-003.1 e tipado {@code e2e} e vive em Playwright. SCN-003.2 e SCN-003.3
 * sao {@code integracao} e recebem tambem verificacao de componente, porque
 * afirmam sobre apresentacao: o que este arquivo trava e que o dado chega, e o
 * teste de componente trava que ele e mostrado.
 */
class BoardIT extends TesteDeIntegracao {

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
    @DisplayName("SCN-003.2 — o cartao traz as tres dimensoes e as contagens em curso")
    void cartaoTrazAsTresDimensoes() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Corrigir o relatorio", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());
        cenario.assumir(tarefa, bruno());
        cenario.recuarInicioDoIntervalo(tarefa, "PERMANENCIA", Duration.ofMinutes(120));

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seq").isNumber())
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas[0].titulo")
                        .value("Corrigir o relatorio"))
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas[0].condicao").value("EM_CURSO"))
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas[0].responsavel.nome").value("Bruno"))
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas[0].versao").isNumber())
                // A permanencia corre; a espera de tomada foi encerrada pela
                // tomada e por isso vem nula. As duas nunca sao somadas.
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas[0].permanencia.decorrido",
                        Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas[0].esperaTomada")
                        .value(Matchers.nullValue()))
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas[0].impedimento")
                        .value(Matchers.nullValue()));
    }

    @Test
    @DisplayName("SCN-003.2 — a tarefa aguardando tomada traz a espera correndo e nenhum responsavel")
    void tarefaAguardandoTomadaTrazAEspera() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Sem dono ainda", ana());
        cenario.recuarInicioDoIntervalo(tarefa, "ESPERA_TOMADA", Duration.ofMinutes(45));

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas[0].condicao")
                        .value("AGUARDANDO_TOMADA"))
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas[0].responsavel")
                        .value(Matchers.nullValue()))
                // DDR-006: a espera e estado de primeira classe, e nao ausencia
                // de estado. Ela precisa vir contada no cartao.
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas[0].esperaTomada.decorrido",
                        Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("SCN-003.3 — o cartao impedido traz a marca com motivo, sem perder a condicao")
    void cartaoImpedidoTrazAMarcaSemPerderACondicao() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());
        cenario.assumir(tarefa, bruno());
        cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", bruno());
        cenario.recuarInicioDoIntervalo(tarefa, "IMPEDIMENTO", Duration.ofMinutes(30));

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(status().isOk())
                // O impedimento e dimensao ortogonal (DDR-007): a condicao
                // continua EM_CURSO. `IMPEDIDA` nao pertence ao dominio de
                // `condicao`, e ve-la aqui significa que a modelagem colapsou.
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas[0].condicao").value("EM_CURSO"))
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas[0].impedimento.motivo")
                        .value("aguardando o fornecedor"))
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas[0].impedimento.decorrido",
                        Matchers.greaterThan(0)))
                // A terceira serie corre em paralelo as outras duas.
                .andExpect(jsonPath("$.etapas[1].raias[0].tarefas[0].permanencia.decorrido",
                        Matchers.greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("SCN-003.3 — a condicao devolvida nunca e IMPEDIDA, em cartao algum")
    void condicaoNuncaEImpedida() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada no pool", ana());
        cenario.abrirImpedimento(tarefa, "falta a especificacao", ana());

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(jsonPath("$..condicao", Matchers.everyItem(Matchers.not("IMPEDIDA"))))
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas[0].condicao")
                        .value("AGUARDANDO_TOMADA"))
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas[0].impedimento.motivo")
                        .value("falta a especificacao"));

        // A mesma leitura pela ficha da tarefa: as duas superficies precisam
        // concordar, porque a tela alterna entre elas.
        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"))
                .andExpect(jsonPath("$.impedimento.motivo").value("falta a especificacao"));
    }

    @Test
    @DisplayName("SCN-003.2 — o board de projeto sem tarefa devolve a grade completa e vazia")
    void boardVazioDevolveAGradeCompleta() throws Exception {
        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas", Matchers.hasSize(4)))
                .andExpect(jsonPath("$.etapas[*].raias[*].tarefas",
                        Matchers.everyItem(Matchers.hasSize(0))));
    }

    /**
     * Criterio 6 de TASK-02.6, a metade que faltava. O caso de nao-participante
     * ja e coberto por {@code SessaoEProjetosIT.boardDeProjetoSemParticipacaoTambemE404}
     * — este cobre o outro, que depende de {@code BoardController.java:88}
     * (ACH-06 da revisao): identificador que nao corresponde a projeto algum.
     */
    @Test
    @DisplayName("Criterio 6 — projeto inexistente devolve 404, como o sem participacao")
    void projetoInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/v1/projetos/{id}/board", UUID.randomUUID()).with(ana()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.etapas").doesNotExist());
    }

    /**
     * Criterio 7 de TASK-02.6. O {@code seq} do board vem de
     * {@code projeto.seq_atual} (SDR-004); a referencia independente e o
     * proprio log, e as duas leituras precisam concordar porque e essa
     * igualdade que o cliente usa para detectar lacuna no WebSocket
     * (ADR-004).
     */
    @Test
    @DisplayName("Criterio 7 — o seq do board e o mesmo do ultimo evento do log")
    void seqDoBoardEOUltimoDoLog() throws Exception {
        cenario.criarTarefa(projeto, "Uma", ana());
        cenario.criarTarefa(projeto, "Duas", ana());

        String resposta = mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Number seqDoBoard = JsonPath.read(resposta, "$.seq");

        assertThat(seqDoBoard.longValue())
                .as("o seq do board diverge do ultimo numero de sequencia do log")
                .isEqualTo(cenario.ultimoSeqDoLog(projeto));
    }

    /**
     * SCN-003.4 e RN-039 (emenda de 2026-09-16, ACH-03 da revisao do board).
     * O recorte depende de duas pecas que ainda nao existem nesta arvore:
     * {@code POST .../movimentos} conclui a tarefa ao alcancar etapa terminal
     * (RF-011, TASK-02.7), e o registro de {@code tarefa.tornou_se_terminal_em}
     * no desfecho e obrigacao nomeada de EPIC-03/04 (ACH-06 da revisao de
     * TASK-02.6) — sem ele a coluna fica {@code null} e a consulta trata
     * {@code null} como "mostrar", de proposito. Este teste fica vermelho por
     * essa dupla dependencia ate as duas pecas existirem, e passa a medir
     * RN-039 de verdade a partir dai — {@link #envelhecerConclusao} so desloca
     * um valor que a escrita real precisa ter produzido primeiro.
     */
    @Test
    @DisplayName("SCN-003.4 — conclusao antiga sai do board sem sair do registro")
    void conclusaoAntigaSaiDoBoardSemSairDoRegistro() throws Exception {
        var antiga = cenario.criarTarefa(projeto, "Entregue faz tempo", ana());
        cenario.mover(antiga, etapas.get(3).id(), ana());
        cenario.envelhecerConclusao(antiga, Duration.ofDays(31));

        var recente = cenario.criarTarefa(projeto, "Entregue essa semana", ana());
        cenario.mover(recente, etapas.get(3).id(), ana());

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[3].raias[0].tarefas[*].titulo",
                        Matchers.hasItem("Entregue essa semana")))
                .andExpect(jsonPath("$.etapas[3].raias[0].tarefas[*].titulo",
                        Matchers.not(Matchers.hasItem("Entregue faz tempo"))));

        // O recorte e da tela, nunca do registro (RN-022, RNF-008): a ficha
        // continua acessivel e com o historico inteiro.
        mockMvc.perform(get("/v1/tarefas/{id}", antiga).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.log").isNotEmpty());
    }

    /**
     * ACH-02 e ACH-04 da revisao do board: a grade tem duas faixas sinteticas,
     * uma por eixo, sob a mesma regra — a sintetica existe quando, e so
     * quando, ha cartao que precise dela. {@code RaiasIT} cobre o eixo da
     * raia; este cobre o da etapa. Depende de {@code POST .../movimentos}
     * (TASK-02.7, ainda nao implementada nesta arvore) para colocar a tarefa
     * na etapa terminal antes de arquiva-la.
     */
    @Test
    @DisplayName("Cartao em etapa arquivada vai para a faixa sintetica 'Fora do fluxo'")
    void cartaoEmEtapaArquivadaVaiParaAFaixaSintetica() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Historico antigo", ana());
        cenario.mover(tarefa, etapas.get(3).id(), ana());

        // Arquivar a etapa terminal e permitido: a contagem que barra o
        // arquivamento exclui CONCLUIDA e ENCERRADA_SEM_CONCLUSAO por decisao
        // declarada (ContagemDeTarefasAtivas) — e e exatamente essa operacao
        // permitida que a faixa sintetica de etapa existe para cobrir.
        mockMvc.perform(put("/v1/projetos/{id}/etapas", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "etapas": [
                                  { "id": "%s", "nome": "Backlog",         "ordem": 1, "terminal": false },
                                  { "id": "%s", "nome": "Desenvolvimento", "ordem": 2, "terminal": false },
                                  { "id": "%s", "nome": "Review",          "ordem": 3, "terminal": false }
                                ] }
                                """.formatted(etapas.get(0).id(), etapas.get(1).id(), etapas.get(2).id()))
                )
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas", Matchers.hasSize(4)))
                .andExpect(jsonPath("$.etapas[3].id").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.etapas[3].nome").value("Fora do fluxo"))
                .andExpect(jsonPath("$.etapas[3].terminal").value(false))
                .andExpect(jsonPath("$.etapas[3].raias[0].tarefas[0].titulo")
                        .value("Historico antigo"))
                // O cartao continua declarando a etapa arquivada real, nao a
                // faixa sintetica: e dali que a origem de SDR-002 vem.
                .andExpect(jsonPath("$.etapas[3].raias[0].tarefas[0].etapaId")
                        .value(etapas.get(3).id().toString()));
    }
}
