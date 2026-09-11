package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.adminGlobal;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * O efeito em disco de {@code POST /v1/projetos} (RF-022, RN-036, RN-037).
 *
 * <p>Verificacao <b>alem dos cenarios</b>, e ela existe por uma razao mecanica:
 * SCN-022.1 afirma a primeira participacao lendo
 * {@code GET /v1/projetos/{id}/participacoes}, que e rota de TASK-06.2 e nao
 * existe ainda — o cenario congelado nao pode ficar verde nesta task, e sem esta
 * classe os criterios de aceite 1, 2 e 5 seriam cumpridos por inspecao e nao por
 * execucao. Quando a rota nascer, SCN-022.1 volta a ser a prova de contrato e
 * esta classe continua sendo a prova do efeito, que e outra coisa.
 *
 * <p>A leitura e por SQL de proposito: o que se afirma aqui e o estado gravado, e
 * ir por rota faria a prova depender de uma rota que ainda nao ha.
 */
class PrimeiraParticipacaoIT extends TesteDeIntegracao {

    @Autowired
    private DataSource fonte;

    @Test
    @DisplayName("uma participacao so, a da pessoa nomeada, com project_admin (criterio 1)")
    void gravaExatamenteUmaParticipacao() throws Exception {
        var projetoId = criarProjetoNomeando(entrarComoBruno());

        var jdbc = new JdbcTemplate(fonte);
        List<Map<String, Object>> participacoes = jdbc.queryForList(
                "select usuario_id from participacao where projeto_id = ?::uuid", projetoId);
        assertThat(participacoes).hasSize(1);

        List<String> papeis = jdbc.queryForList("""
                select pp.papel
                  from participacao_papel pp
                  join participacao p on p.id = pp.participacao_id
                 where p.projeto_id = ?::uuid
                """, String.class, projetoId);
        assertThat(papeis).containsExactly("project_admin");
    }

    @Test
    @DisplayName("quem cria nao vira participante, e alcanca por escopo (criterio 2, RN-037)")
    void criadorNaoViraParticipante() throws Exception {
        var brunoId = entrarComoBruno();
        var projetoId = criarProjetoNomeando(brunoId);

        // Se o criador virasse participante, a marca de alcance global sumiria
        // para ele neste projeto e SCN-021.2 deixaria de valer justamente no
        // projeto recem-criado — alcance e participacao voltariam a se confundir.
        var jdbc = new JdbcTemplate(fonte);
        List<String> subs = jdbc.queryForList("""
                select u.subject_id
                  from participacao p
                  join usuario u on u.id = p.usuario_id
                 where p.projeto_id = ?::uuid
                """, String.class, projetoId);
        assertThat(subs).containsExactly(SUB_BRUNO);
    }

    @Test
    @DisplayName("pessoa inexistente nao deixa projeto orfao (criterio 5)")
    void recusaNaoDeixaProjetoOrfao() throws Exception {
        entrarComoBruno();

        // A pessoa nomeada nao existe: a participacao e impossivel, e o projeto
        // nao pode sobreviver a ela. Projeto sem participacao nenhuma nao e um
        // erro visivel — ele aparece so para a administracao global, e a pessoa
        // nomeada nunca saberia que foi nomeada.
        mockMvc.perform(post("/v1/projetos")
                        .with(adminGlobal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Orfao",
                                  "primeiroAdministradorId":
                                      "00000000-0000-0000-0000-0000000000ff" }
                                """))
                .andExpect(status().isUnprocessableEntity());

        var jdbc = new JdbcTemplate(fonte);
        assertThat(jdbc.queryForObject("select count(*) from projeto", Integer.class)).isZero();
    }

    private String entrarComoBruno() throws Exception {
        mockMvc.perform(get("/v1/sessao").with(bruno())).andExpect(status().isOk());
        return cenario.idDoUsuarioPorSub(SUB_BRUNO).toString();
    }

    private String criarProjetoNomeando(String usuarioId) throws Exception {
        var corpo = mockMvc.perform(post("/v1/projetos")
                        .with(adminGlobal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Alfa", "primeiroAdministradorId": "%s" }
                                """.formatted(usuarioId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(corpo, "$.id").toString();
    }
}
