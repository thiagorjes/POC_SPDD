package br.com.crudao.kanban.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.crudao.kanban.evento.EnvelopeEvento;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.tarefa.TarefaService;
import br.com.crudao.kanban.tarefa.TipoTarefa;
import br.com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.MoverTarefaRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Bloco 19.2: o evento chega pelo canal LISTEN/NOTIFY com o {@code seq} atribuido na transacao de
 * escrita, e apenas depois do commit (ADR-004). O teste escuta com uma conexao propria, no mesmo
 * papel que o {@code BoardEventLoop} cumpre em producao.
 */
class EventoListenNotifyTest extends IntegracaoBase {

  private static final String CANAL = "board_events";
  private static final int TIMEOUT_MS = 10_000;

  @Autowired private DataSource dataSource;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TarefaService tarefaService;

  @Test
  @DisplayName("criar e mover entregam eventos com seq monotonico por projeto")
  void entregaEventosComSeqMonotonico() throws Exception {
    Usuario admin = cenarioDeTeste.autenticarComoAdmin();
    CenarioDeTeste.Cenario cenario = cenarioDeTeste.criarProjetoCompleto("Projeto Evento", admin);

    try (Connection escuta = dataSource.getConnection()) {
      try (Statement statement = escuta.createStatement()) {
        statement.execute("LISTEN " + CANAL);
      }
      PGConnection pgConnection = escuta.unwrap(PGConnection.class);
      // Descarta os eventos de reconfiguracao emitidos ao montar workflow, etapas e transicoes.
      pgConnection.getNotifications(500);

      Tarefa tarefa =
          tarefaService.criar(
              cenario.projeto().getId(),
              new CriarTarefaRequest("Card observado", null, TipoTarefa.TAREFA, null, null, null),
              admin);
      tarefaService.mover(
          tarefa.getId(), new MoverTarefaRequest(cenario.fazendo().getId(), null, 0L), admin);

      List<EnvelopeEvento> recebidos = aguardar(pgConnection, 2);

      assertThat(recebidos).hasSize(2);
      assertThat(recebidos.get(0).evento().tipo()).isEqualTo(TipoEventoBoard.TAREFA_CRIADA);
      assertThat(recebidos.get(1).evento().tipo()).isEqualTo(TipoEventoBoard.TAREFA_MOVIDA);
      assertThat(recebidos)
          .allSatisfy(
              envelope -> {
                assertThat(envelope.evento().projetoId()).isEqualTo(cenario.projeto().getId());
                assertThat(envelope.evento().tarefaId()).isEqualTo(tarefa.getId());
                assertThat(envelope.evento().ocorridoEm()).isNotNull();
              });
      assertThat(recebidos.get(1).evento().seq()).isEqualTo(recebidos.get(0).evento().seq() + 1);
      assertThat(recebidos.get(1).evento().etapaId()).isEqualTo(cenario.fazendo().getId());

      Long ultimoSeq =
          jdbcTemplate.queryForObject(
              "SELECT ultimo_seq FROM sequencia_projeto WHERE projeto_id = ?",
              Long.class,
              cenario.projeto().getId());
      assertThat(ultimoSeq).isEqualTo(recebidos.get(1).evento().seq());
    }
  }

  private List<EnvelopeEvento> aguardar(PGConnection pgConnection, int quantidade)
      throws Exception {
    List<EnvelopeEvento> recebidos = new ArrayList<>();
    long limite = System.currentTimeMillis() + TIMEOUT_MS;
    while (recebidos.size() < quantidade && System.currentTimeMillis() < limite) {
      PGNotification[] notificacoes = pgConnection.getNotifications(500);
      if (notificacoes == null) {
        continue;
      }
      for (PGNotification notificacao : notificacoes) {
        recebidos.add(objectMapper.readValue(notificacao.getParameter(), EnvelopeEvento.class));
      }
    }
    return recebidos;
  }
}
