package br.com.idsd.kanban.internal.tarefa;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import java.time.Duration;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
