package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.adminGlobal;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.internal.projeto.ParticipacaoRepository;
import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * A invariante que da nome a TASK-01.8: <b>uma transacao, nao duas</b>.
 *
 * <p>Verificacao alem dos cenarios, e ela existe porque a que havia nao tinha
 * poder de falha. Medir a atomicidade pela recusa da pessoa inexistente nao mede
 * nada: essa verificacao acontece <b>antes</b> do primeiro {@code save}, entao a
 * assercao de "nenhum projeto gravado" ficaria verde igualmente com
 * {@code @Transactional} removido do metodo. Criterio de aceite cumprido por
 * teste que nao pode falhar e criterio nao verificado.
 *
 * <p>A falha aqui e forcada <b>depois</b> da gravacao do projeto, que e a unica
 * ordem em que a pergunta faz sentido: sem a transacao, {@code projetos.save}
 * comita por conta propria — o repositorio do Spring Data e transacional — e o
 * projeto orfao fica em disco, que e exatamente o estado que esta rota existe
 * para eliminar.
 */
class TransacaoUnicaDeCriacaoIT extends TesteDeIntegracao {

    @Autowired
    private DataSource fonte;

    @MockitoBean
    private ParticipacaoRepository participacoes;

    @Test
    @DisplayName("falha ao gravar a participacao desfaz o projeto (criterio 5)")
    void falhaNaParticipacaoDesfazOProjeto() throws Exception {
        willThrow(new DataIntegrityViolationException("participacao recusada pelo banco"))
                .given(participacoes)
                .save(any());

        mockMvc.perform(get("/v1/sessao").with(bruno())).andExpect(status().isOk());
        var brunoId = cenario.idDoUsuarioPorSub(SUB_BRUNO).toString();

        mockMvc.perform(post("/v1/projetos")
                        .with(adminGlobal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Alfa", "primeiroAdministradorId": "%s" }
                                """.formatted(brunoId)))
                .andExpect(status().is5xxServerError());

        var jdbc = new JdbcTemplate(fonte);
        assertThat(jdbc.queryForObject("select count(*) from projeto", Integer.class)).isZero();
    }
}
