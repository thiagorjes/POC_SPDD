package br.com.idsd.kanban.internal.tempo;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_CARLA;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static br.com.idsd.kanban.suporte.Sujeitos.carla;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * RF-016 — tempo por etapa, e SCN-018.3 — a raia fora da agregacao.
 *
 * <p>SCN-018.3 esta aqui e nao em {@code RaiasIT} porque o que ele afirma e
 * sobre a consulta agregada: a raia organiza a leitura do board e nao e
 * dimensao de medicao.
 *
 * <p>SCN-016.2 recebe tambem verificacao de componente: ele afirma sobre
 * apresentacao, e aqui so se trava que o dado chega separado.
 */
class TempoPorEtapaIT extends TesteDeIntegracao {

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
    @DisplayName("SCN-016.1 — o tempo e agregado por etapa, com a quantidade que o compoe")
    void tempoAgregadoPorEtapa() throws Exception {
        var primeira = cenario.criarTarefa(projeto, "Uma", ana());
        cenario.recuarInicioDoIntervalo(primeira, "PERMANENCIA", Duration.ofMinutes(120));
        cenario.mover(primeira, etapas.get(1).id(), ana());

        var segunda = cenario.criarTarefa(projeto, "Outra", ana());
        cenario.recuarInicioDoIntervalo(segunda, "PERMANENCIA", Duration.ofMinutes(60));
        cenario.mover(segunda, etapas.get(1).id(), ana());

        mockMvc.perform(get("/v1/projetos/{id}/tempo-por-etapa", projeto).with(carla()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[?(@.nome=='Backlog')].quantidade",
                        Matchers.hasItem(2)))
                .andExpect(jsonPath("$.etapas[?(@.nome=='Backlog')].tempoTotal",
                        Matchers.everyItem(Matchers.greaterThan(0))))
                .andExpect(jsonPath("$.etapas[?(@.nome=='Backlog')].tempoMedio",
                        Matchers.everyItem(Matchers.greaterThan(0))))
                // Toda etapa aparece, inclusive a que nunca recebeu tarefa:
                // omiti-la faria a etapa vazia parecer inexistente na leitura.
                .andExpect(jsonPath("$.etapas", Matchers.hasSize(4)))
                .andExpect(jsonPath("$.etapas[?(@.nome=='Review')].quantidade",
                        Matchers.hasItem(0)));
    }

    @Test
    @DisplayName("SCN-016.2 — as tres series vem separadas, e nao existe campo que as some")
    void asTresSeriesVemSeparadas() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Medida", ana());
        cenario.recuarInicioDoIntervalo(tarefa, "ESPERA_TOMADA", Duration.ofMinutes(90));
        cenario.assumir(tarefa, bruno());
        cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", bruno());
        cenario.recuarInicioDoIntervalo(tarefa, "IMPEDIMENTO", Duration.ofMinutes(45));
        cenario.recuarInicioDoIntervalo(tarefa, "PERMANENCIA", Duration.ofMinutes(200));

        String resposta = mockMvc.perform(
                        get("/v1/projetos/{id}/tempo-por-etapa", projeto).with(carla()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[0].permanencia").isNumber())
                .andExpect(jsonPath("$.etapas[0].esperaTomada").isNumber())
                .andExpect(jsonPath("$.etapas[0].impedimento").isNumber())
                .andReturn().getResponse().getContentAsString();

        // RN-008: as series correm em paralelo e sobrepostas. Somar as tres
        // contaria o mesmo minuto tres vezes, e oferecer o campo do total e
        // convidar a leitura errada — por isso a verificacao e por ausencia.
        Assertions.assertThat(resposta)
                .doesNotContain("\"total\"")
                .doesNotContain("\"tempoSomado\"");
        Assertions.assertThat((Object) JsonPath.read(resposta, "$.etapas[0].impedimento"))
                .isNotNull();
    }

    @Test
    @DisplayName("SCN-016.2 — a serie de impedimento e atribuida a etapa onde o impedimento nasceu")
    void impedimentoEAtribuidoAEtapaOndeNasceu() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Travada no backlog", ana());
        cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", ana());
        cenario.recuarInicioDoIntervalo(tarefa, "IMPEDIMENTO", Duration.ofMinutes(300));
        cenario.mover(tarefa, etapas.get(1).id(), ana());

        // O intervalo guarda o instantaneo da etapa na abertura. Sem ele a
        // serie por etapa que RF-016 promete nao seria computavel — e mover
        // nao pode reatribuir tempo que correu em outro lugar (RN-009).
        mockMvc.perform(get("/v1/projetos/{id}/tempo-por-etapa", projeto).with(carla()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[?(@.nome=='Backlog')].impedimento",
                        Matchers.everyItem(Matchers.greaterThan(0))))
                .andExpect(jsonPath("$.etapas[?(@.nome=='Desenvolvimento')].impedimento",
                        Matchers.everyItem(Matchers.equalTo(0))));
    }

    @Test
    @DisplayName("SCN-016.3 — a exportacao traz as mesmas series, sem coluna de pessoa")
    void exportacaoTrazAsMesmasSeries() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Medida", ana());
        cenario.recuarInicioDoIntervalo(tarefa, "PERMANENCIA", Duration.ofMinutes(120));
        cenario.assumir(tarefa, bruno());

        String csv = mockMvc.perform(get("/v1/projetos/{id}/tempo-por-etapa", projeto)
                        .accept("text/csv")
                        .with(carla()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse().getContentAsString();

        Assertions.assertThat(csv.lines().findFirst().orElseThrow())
                .contains("etapa")
                .contains("permanencia")
                .contains("espera_tomada")
                .contains("impedimento")
                // A ausencia atravessa a exportacao: o CSV e a superficie mais
                // facil de virar planilha agregada por pessoa, que e
                // exatamente o que C-03 recusou.
                .doesNotContain("responsavel")
                .doesNotContain("usuario")
                .doesNotContain("pessoa");
        Assertions.assertThat(csv).doesNotContain("Bruno");
    }

    @Test
    @DisplayName("SCN-016.3 — a exportacao e recusada a quem nao participa do projeto")
    void exportacaoRecusadaAQuemNaoParticipa() throws Exception {
        cenario.usuario("88888888-8888-8888-8888-888888888888", "Fora", "fora@empresa.example");

        mockMvc.perform(get("/v1/projetos/{id}/tempo-por-etapa", projeto)
                        .accept("text/csv")
                        .with(br.com.idsd.kanban.suporte.Sujeitos.comoSub(
                                "88888888-8888-8888-8888-888888888888", "Fora",
                                "fora@empresa.example")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SCN-018.3 — a agregacao nao aceita recorte por raia")
    void agregacaoNaoAceitaRecortePorRaia() throws Exception {
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

        // A raia organiza a leitura do board e nao e dimensao de medicao:
        // aceitar o filtro faria dela um eixo de comparacao entre times, que e
        // a mesma leitura por pessoa com outro nome.
        mockMvc.perform(get("/v1/projetos/{id}/tempo-por-etapa", projeto)
                        .param("raiaId", suporte)
                        .with(carla()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/v1/projetos/{id}/tempo-por-etapa", projeto).with(carla()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..raia").doesNotExist())
                .andExpect(jsonPath("$..raiaId").doesNotExist());
    }
}
