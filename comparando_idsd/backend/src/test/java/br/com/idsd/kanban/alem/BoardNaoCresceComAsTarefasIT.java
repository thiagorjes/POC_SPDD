package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.ContagemDeConsultas;
import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Criterio 5 de TASK-02.6: o numero de consultas do board nao cresce com a
 * quantidade de tarefas.
 *
 * <p>Verificacao <b>alem dos cenarios</b> — nenhum cenario Gherkin descreve
 * custo de leitura. Ate aqui o criterio era medido por sonda descartavel fora
 * da suite congelada (revisao de TASK-02.6, ACH-04): trocar a montagem em
 * memoria por uma consulta por cartao deixava toda a suite verde, porque nada
 * a prendia. A forma da assercao segue {@link ContagemDeConsultas}: afirma-se
 * <b>invariancia a massa</b>, nunca um numero absoluto — numero absoluto
 * quebra por mudanca inocua e acaba desabilitado.
 *
 * <p>Todas as tarefas nascem na mesma etapa e raia (RN-004, RN-006, RN-023):
 * o teste nao precisa de movimento, que ainda nao existe nesta arvore.
 *
 * <p>Origem: ACH-04 da revisao de TASK-02.6.
 */
class BoardNaoCresceComAsTarefasIT extends TesteDeIntegracao {

    @Autowired
    private EntityManagerFactory fabrica;

    @Test
    @DisplayName("GET board custa o mesmo com 5 tarefas e com 50")
    void boardNaoCresceComOVolume() throws Exception {
        var contagem = new ContagemDeConsultas(fabrica);
        var projeto = cenario.projeto("Volume");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.fluxoPadrao(projeto, ana());

        semear(projeto, 5);
        long comPoucas = contagem.durante(() -> boardEsperando(projeto, 5));

        semear(projeto, 45);
        long comMuitas = contagem.durante(() -> boardEsperando(projeto, 50));

        assertThat(comMuitas)
                .as("5 tarefas custaram %d consultas e 50 custaram %d — "
                                + "a leitura do board cresce com o volume, que e N+1",
                        comPoucas, comMuitas)
                .isEqualTo(comPoucas);
    }

    private void semear(UUID projeto, int quantas) {
        for (int i = 0; i < quantas; i++) {
            cenario.criarTarefa(projeto, "Tarefa %d".formatted(i), ana());
        }
    }

    private void boardEsperando(UUID projeto, int quantas) throws Exception {
        mockMvc.perform(get("/v1/projetos/{id}/board", projeto).with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[0].raias[0].tarefas", Matchers.hasSize(quantas)));
    }
}
