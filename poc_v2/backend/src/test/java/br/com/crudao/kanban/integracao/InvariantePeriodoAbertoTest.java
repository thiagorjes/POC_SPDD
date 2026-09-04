package br.com.crudao.kanban.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.tarefa.TarefaService;
import br.com.crudao.kanban.tarefa.TipoTarefa;
import br.com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Bloco 19.2: o indice unico parcial e a ultima linha de defesa do lead-time. Nenhum caminho de
 * codigo pode deixar duas janelas abertas para a mesma tarefa (RN-001, RN-002).
 */
class InvariantePeriodoAbertoTest extends IntegracaoBase {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TarefaService tarefaService;

  private CenarioDeTeste.Cenario cenario;
  private Tarefa tarefa;

  @BeforeEach
  void montar() {
    Usuario admin = cenarioDeTeste.autenticarComoAdmin();
    cenario = cenarioDeTeste.criarProjetoCompleto("Projeto Invariante", admin);
    tarefa =
        tarefaService.criar(
            cenario.projeto().getId(),
            new CriarTarefaRequest("Card", null, TipoTarefa.TAREFA, null, null, null),
            admin);
  }

  @Test
  @DisplayName("segundo periodo de etapa aberto e rejeitado pelo indice unico parcial")
  void segundoPeriodoDeEtapaAbertoERejeitado() {
    assertThat(periodosAbertos("periodo_etapa")).isEqualTo(1);

    assertThatThrownBy(() -> inserirPeriodoAberto("periodo_etapa", cenario.aFazer().getId()))
        .isInstanceOf(DuplicateKeyException.class);

    assertThat(periodosAbertos("periodo_etapa")).isEqualTo(1);
  }

  @Test
  @DisplayName("segundo periodo de impedimento aberto e rejeitado pelo indice unico parcial")
  void segundoPeriodoDeImpedimentoAbertoERejeitado() {
    Usuario admin = cenarioDeTeste.autenticarComoAdmin();
    tarefaService.marcarImpedimento(tarefa.getId(), "aguardando cliente", admin);
    assertThat(periodosAbertos("periodo_impedimento")).isEqualTo(1);

    assertThatThrownBy(() -> inserirPeriodoAberto("periodo_impedimento", cenario.aFazer().getId()))
        .isInstanceOf(DuplicateKeyException.class);

    assertThat(periodosAbertos("periodo_impedimento")).isEqualTo(1);
  }

  @Test
  @DisplayName("periodo encerrado nao conflita: o indice cobre apenas as janelas em aberto")
  void periodoEncerradoNaoConflita() {
    jdbcTemplate.update(
        "INSERT INTO periodo_etapa (id, tarefa_id, projeto_id, etapa_id, iniciado_em,"
            + " encerrado_em) VALUES (?, ?, ?, ?, now(), now())",
        UUID.randomUUID(),
        tarefa.getId(),
        cenario.projeto().getId(),
        cenario.aFazer().getId());

    assertThat(periodosAbertos("periodo_etapa")).isEqualTo(1);
  }

  private void inserirPeriodoAberto(String tabela, UUID etapaId) {
    jdbcTemplate.update(
        "INSERT INTO "
            + tabela
            + " (id, tarefa_id, projeto_id, etapa_id, iniciado_em) VALUES (?, ?, ?, ?, now())",
        UUID.randomUUID(),
        tarefa.getId(),
        cenario.projeto().getId(),
        etapaId);
  }

  private int periodosAbertos(String tabela) {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM " + tabela + " WHERE tarefa_id = ? AND encerrado_em IS NULL",
        Integer.class,
        tarefa.getId());
  }
}
