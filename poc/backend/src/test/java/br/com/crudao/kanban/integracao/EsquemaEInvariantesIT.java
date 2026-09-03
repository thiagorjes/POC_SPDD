package br.com.crudao.kanban.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Flyway aplica limpo e os indices unicos parciais rejeitam o segundo intervalo aberto. */
class EsquemaEInvariantesIT extends AbstractIntegracaoTest {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private CenarioFixture fixture;

  @Test
  @DisplayName("as migrations Flyway aplicam limpo e todas ficam com success = true")
  void migrationsAplicamLimpo() {
    List<String> versoes =
        jdbc.queryForList(
            "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank",
            String.class);

    assertThat(versoes).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = false", Integer.class))
        .isZero();
  }

  @Test
  @DisplayName("ddl-auto=validate: as entidades batem com o esquema criado pelas migrations")
  void esquemaValidadoPeloHibernate() {
    // Se o mapeamento divergisse do DDL, o contexto nem teria subido para chegar ate aqui.
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tarefa", Integer.class)).isNotNull();
  }

  @Test
  @DisplayName("indice unico parcial rejeita um segundo periodo de etapa aberto")
  void segundoPeriodoEtapaAbertoRejeitado() {
    CenarioFixture.Cenario cenario = fixture.criar();
    UUID tarefaId = fixture.criarTarefa(cenario);

    jdbc.update(
        "INSERT INTO periodo_etapa (id, tarefa_id, projeto_id, etapa_id) VALUES (?, ?, ?, ?)",
        UUID.randomUUID(),
        tarefaId,
        cenario.projetoId(),
        cenario.aFazerId());

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO periodo_etapa (id, tarefa_id, projeto_id, etapa_id) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID(),
                    tarefaId,
                    cenario.projetoId(),
                    cenario.fazendoId()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("fechar o periodo anterior libera a abertura do proximo")
  void periodoFechadoLiberaProximo() {
    CenarioFixture.Cenario cenario = fixture.criar();
    UUID tarefaId = fixture.criarTarefa(cenario);

    jdbc.update(
        "INSERT INTO periodo_etapa (id, tarefa_id, projeto_id, etapa_id, encerrado_em)"
            + " VALUES (?, ?, ?, ?, now())",
        UUID.randomUUID(),
        tarefaId,
        cenario.projetoId(),
        cenario.aFazerId());
    jdbc.update(
        "INSERT INTO periodo_etapa (id, tarefa_id, projeto_id, etapa_id) VALUES (?, ?, ?, ?)",
        UUID.randomUUID(),
        tarefaId,
        cenario.projetoId(),
        cenario.fazendoId());

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM periodo_etapa WHERE tarefa_id = ?", Integer.class, tarefaId))
        .isEqualTo(2);
  }

  @Test
  @DisplayName("indice unico parcial rejeita um segundo impedimento aberto")
  void segundoImpedimentoAbertoRejeitado() {
    CenarioFixture.Cenario cenario = fixture.criar();
    UUID tarefaId = fixture.criarTarefa(cenario);

    jdbc.update(
        "INSERT INTO periodo_impedimento (id, tarefa_id, projeto_id, etapa_id, motivo)"
            + " VALUES (?, ?, ?, ?, 'Primeiro')",
        UUID.randomUUID(),
        tarefaId,
        cenario.projetoId(),
        cenario.aFazerId());

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO periodo_impedimento (id, tarefa_id, projeto_id, etapa_id, motivo)"
                        + " VALUES (?, ?, ?, ?, 'Segundo')",
                    UUID.randomUUID(),
                    tarefaId,
                    cenario.projetoId(),
                    cenario.aFazerId()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("check de ordem temporal rejeita encerramento anterior ao inicio")
  void ordemTemporalGarantida() {
    CenarioFixture.Cenario cenario = fixture.criar();
    UUID tarefaId = fixture.criarTarefa(cenario);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO periodo_etapa (id, tarefa_id, projeto_id, etapa_id, encerrado_em)"
                        + " VALUES (?, ?, ?, ?, now() - interval '1 hour')",
                    UUID.randomUUID(),
                    tarefaId,
                    cenario.projetoId(),
                    cenario.aFazerId()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
