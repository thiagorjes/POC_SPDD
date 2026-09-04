package br.com.crudao.kanban.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Bloco 19.2: as migrations Flyway aplicam limpo em base vazia e o schema resultante satisfaz o
 * {@code ddl-auto: validate} do Hibernate — se divergisse, o contexto nao subiria.
 */
class MigracoesFlywayTest extends IntegracaoBase {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("as nove migrations aplicam sem falha e o catalogo fechado e semeado")
  void migrationsAplicamLimpo() {
    List<Map<String, Object>> historico =
        jdbcTemplate.queryForList(
            "SELECT version, success FROM flyway_schema_history WHERE version IS NOT NULL"
                + " ORDER BY installed_rank");

    assertThat(historico).hasSize(9);
    assertThat(historico).allSatisfy(linha -> assertThat(linha.get("success")).isEqualTo(true));
    assertThat(historico.stream().map(linha -> linha.get("version")))
        .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");

    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM papel", Integer.class))
        .isEqualTo(6);
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM permissao", Integer.class))
        .isEqualTo(11);
  }

  @Test
  @DisplayName("indices parciais de periodo aberto existem no schema aplicado")
  void indicesParciaisExistem() {
    List<String> indices =
        jdbcTemplate.queryForList(
            "SELECT indexname FROM pg_indexes WHERE indexname IN"
                + " ('uk_periodo_etapa_aberto', 'uk_periodo_impedimento_aberto')",
            String.class);

    assertThat(indices)
        .containsExactlyInAnyOrder("uk_periodo_etapa_aberto", "uk_periodo_impedimento_aberto");
  }
}
