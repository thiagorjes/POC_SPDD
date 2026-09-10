package br.com.idsd.kanban.internal.tempo;

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
import java.time.Duration;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * RF-015 — painel de andamento.
 *
 * <p>SCN-015.2 — o gestor externo somente-leitura — e tipado {@code e2e}: o que
 * ele afirma e que nenhuma acao aparece na tela, e isso nao se verifica por
 * codigo de resposta. Aqui fica a contraparte de servidor, que e a que impede a
 * acao mesmo quando alguem chama a rota direto.
 */
class AndamentoIT extends TesteDeIntegracao {

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
    @DisplayName("SCN-015.1 — o painel resume as tarefas por etapa e o que esta impedido")
    void painelResumePorEtapaEImpedimento() throws Exception {
        cenario.criarTarefa(projeto, "No backlog", ana());
        var emDesenvolvimento = cenario.criarTarefa(projeto, "Em desenvolvimento", ana());
        cenario.mover(emDesenvolvimento, etapas.get(1).id(), ana());
        var travada = cenario.criarTarefa(projeto, "Travada", ana());
        cenario.abrirImpedimento(travada, "aguardando o fornecedor", ana());
        cenario.recuarInicioDoIntervalo(travada, "IMPEDIMENTO", Duration.ofMinutes(240));

        mockMvc.perform(get("/v1/projetos/{id}/andamento", projeto).with(carla()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[?(@.nome=='Backlog')].quantidade",
                        Matchers.hasItem(2)))
                .andExpect(jsonPath("$.etapas[?(@.nome=='Desenvolvimento')].quantidade",
                        Matchers.hasItem(1)))
                // O impedimento e dimensao propria tambem no painel: contar as
                // travadas dentro da etapa esconderia justamente o que o
                // gestor abre o painel para ver.
                .andExpect(jsonPath("$.impedidas.quantidade").value(1))
                .andExpect(jsonPath("$.impedidas.tarefas[0].motivo")
                        .value("aguardando o fornecedor"))
                .andExpect(jsonPath("$.impedidas.tarefas[0].decorrido", Matchers.greaterThan(0)));

        // A tarefa impedida continua contada na etapa onde esta: as duas
        // leituras sao ortogonais e nao se subtraem.
        mockMvc.perform(get("/v1/projetos/{id}/andamento", projeto).with(carla()))
                .andExpect(jsonPath("$.etapas[?(@.nome=='Backlog')].quantidade",
                        Matchers.hasItem(2)));
    }

    @Test
    @DisplayName("SCN-015.1 — o painel nao oferece recorte por pessoa")
    void painelNaoOfereceRecortePorPessoa() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Do Bruno", ana());
        cenario.assumir(tarefa, bruno());

        // RN-014 e propriedade do esquema, e nao disciplina de quem escreve a
        // consulta: a projecao nao tem coluna de pessoa, entao nao ha o que
        // agrupar. Era a regra mais exposta a erosao silenciosa, porque nenhum
        // teste falharia se alguem acrescentasse o filtro depois — este falha.
        mockMvc.perform(get("/v1/projetos/{id}/andamento", projeto).with(carla()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porPessoa").doesNotExist())
                .andExpect(jsonPath("$.porResponsavel").doesNotExist());

        mockMvc.perform(get("/v1/projetos/{id}/andamento", projeto)
                        .param("usuarioId", SUB_BRUNO)
                        .with(carla()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SCN-015.3 — o gestor le o painel e nao consegue escrever nada")
    void gestorLeENaoEscreve() throws Exception {
        var tarefa = cenario.criarTarefa(projeto, "Do time", ana());

        mockMvc.perform(get("/v1/projetos/{id}/andamento", projeto).with(carla()))
                .andExpect(status().isOk());

        // C-02 virou restricao estrutural, e nao acordo de uso: a visibilidade
        // ao gestor nao pode se converter em interferencia. Verificar so a tela
        // deixaria a rota aberta.
        mockMvc.perform(post("/v1/tarefas/{id}/tomada", tarefa)
                        .with(carla())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, carla(), null)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/v1/tarefas/{id}/movimentos", tarefa)
                        .with(carla())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, carla(),
                                "\"etapaDestinoId\": \"" + etapas.get(1).id() + "\"")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/v1/tarefas/{id}/impedimentos", tarefa)
                        .with(carla())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cenario.comOrigem(tarefa, carla(), "\"motivo\": \"travou\"")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SCN-015.3 — as permissoes devolvidas ao gestor nao incluem escrita")
    void permissoesDoGestorNaoIncluemEscrita() throws Exception {
        // RNF-004: a tela nao apresenta acao que a pessoa nao pode executar, e
        // decide isso pela lista que o servidor devolve. As duas pontas
        // precisam concordar, ou a tela oferece o que a rota recusa.
        mockMvc.perform(get("/v1/projetos/{id}", projeto).with(carla()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissoes", Matchers.hasItem("LER")))
                .andExpect(jsonPath("$.permissoes", Matchers.not(Matchers.hasItem("ESCREVER_TAREFA"))))
                .andExpect(jsonPath("$.permissoes", Matchers.not(Matchers.hasItem("DESBLOQUEAR"))))
                .andExpect(jsonPath("$.permissoes", Matchers.not(Matchers.hasItem("ENCERRAR"))))
                .andExpect(jsonPath("$.permissoes", Matchers.not(Matchers.hasItem("REABRIR"))))
                .andExpect(jsonPath("$.permissoes", Matchers.not(Matchers.hasItem("CONFIGURAR"))));
    }
}
