package br.com.idsd.kanban.internal.tempo;

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
 * RF-014 — a fila pessoal.
 *
 * <p>E o suporte mais direto de H-01: se a pessoa nao tem onde ver o que
 * chegou para ela, a direcao inteira depende de disciplina de aviso. SCN-014.2
 * e tipado {@code e2e} e vive em Playwright.
 */
class FilaPessoalIT extends TesteDeIntegracao {

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
    @DisplayName("SCN-014.1 — a fila traz o que assumi e o que aguarda tomada, separados")
    void filaSeparaOQueAssumiDoQueAguarda() throws Exception {
        var minha = cenario.criarTarefa(projeto, "Minha em curso", ana());
        cenario.assumir(minha, bruno());
        var doPool = cenario.criarTarefa(projeto, "Esperando alguem", ana());
        var deOutro = cenario.criarTarefa(projeto, "Da Ana", ana());
        cenario.assumir(deOutro, ana());

        mockMvc.perform(get("/v1/fila").with(bruno()))
                .andExpect(status().isOk())
                // A separacao e o ponto: DDR-006 fez da espera um estado de
                // primeira classe, com fila propria, porque misturar as duas
                // esconde o que ainda nao tem dono.
                .andExpect(jsonPath("$.emCurso", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.emCurso[0].id").value(minha.toString()))
                .andExpect(jsonPath("$.aguardandoTomada", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.aguardandoTomada[0].id").value(doPool.toString()))
                // A tarefa de outra pessoa nao aparece em nenhuma das duas.
                .andExpect(jsonPath("$..id", Matchers.not(Matchers.hasItem(deOutro.toString()))));
    }

    @Test
    @DisplayName("SCN-014.1 — a fila atravessa os projetos de que participo")
    void filaAtravessaOsProjetos() throws Exception {
        var beta = cenario.projeto("Beta");
        cenario.participante(beta, SUB_ANA, "Ana", "ana@empresa.example", "project_admin");
        cenario.participante(beta, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        cenario.fluxoPadrao(beta, ana());

        var noAlfa = cenario.criarTarefa(projeto, "No Alfa", ana());
        var noBeta = cenario.criarTarefa(beta, "No Beta", ana());
        cenario.assumir(noAlfa, bruno());
        cenario.assumir(noBeta, bruno());

        // A fila e da pessoa, e nao do quadro: quem trabalha em tres projetos
        // nao tem tres filas, tem uma.
        mockMvc.perform(get("/v1/fila").with(bruno()))
                .andExpect(jsonPath("$.emCurso", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.emCurso[*].projeto.nome",
                        Matchers.containsInAnyOrder("Alfa", "Beta")));
    }

    @Test
    @DisplayName("SCN-014.3 — a fila mostra a espera correndo e a marca de impedimento")
    void filaMostraEsperaEImpedimento() throws Exception {
        var travada = cenario.criarTarefa(projeto, "Travada no pool", ana());
        cenario.abrirImpedimento(travada, "falta a especificacao", ana());
        cenario.recuarInicioDoIntervalo(travada, "ESPERA_TOMADA", Duration.ofMinutes(300));
        cenario.recuarInicioDoIntervalo(travada, "IMPEDIMENTO", Duration.ofMinutes(180));

        mockMvc.perform(get("/v1/fila").with(bruno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aguardandoTomada[0].esperaTomada.decorrido",
                        Matchers.greaterThan(0)))
                // A tarefa impedida continua na fila e continua oferecivel:
                // esconde-la deixaria a tarefa parada sem ninguem a reclamar.
                .andExpect(jsonPath("$.aguardandoTomada[0].impedimento.motivo")
                        .value("falta a especificacao"))
                .andExpect(jsonPath("$.aguardandoTomada[0].condicao").value("AGUARDANDO_TOMADA"));
    }

    @Test
    @DisplayName("SCN-014.3 — a tarefa concluida sai da fila")
    void tarefaConcluidaSaiDaFila() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Entregue", ana());
        cenario.assumir(tarefa, bruno());
        cenario.mover(tarefa, etapas.get(1).id(), bruno());
        cenario.mover(tarefa, etapas.get(2).id(), bruno());
        cenario.mover(tarefa, etapas.get(3).id(), bruno());

        mockMvc.perform(get("/v1/fila").with(bruno()))
                .andExpect(jsonPath("$.emCurso", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.aguardandoTomada", Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("SCN-014.1 — a fila de quem nao participa de projeto algum vem vazia")
    void filaDeQuemNaoParticipaVemVazia() throws Exception {
        cenario.usuario("77777777-7777-7777-7777-777777777777", "Fora", "fora@empresa.example");
        cenario.criarTarefa(projeto, "Nao e dele", ana());

        mockMvc.perform(get("/v1/fila").with(br.com.idsd.kanban.suporte.Sujeitos.comoSub(
                        "77777777-7777-7777-7777-777777777777", "Fora", "fora@empresa.example")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emCurso", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.aguardandoTomada", Matchers.hasSize(0)));
    }
}
