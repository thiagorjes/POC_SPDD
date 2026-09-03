package br.com.crudao.kanban.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * O {@code seq} e a unica coisa que permite ao cliente detectar gap; se ele repetir ou pular sob
 * concorrencia, o resync do board deixa de ser confiavel (ADR-004).
 */
class EventoBoardSeqIT extends AbstractIntegracaoTest {

  @Autowired private EventoBoardPublisher publisher;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private DataSource dataSource;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private ObjectMapper objectMapper;

  @Value("${kanban.eventos.canal}")
  private String canal;

  @Test
  @DisplayName("LISTEN/NOTIFY entrega o evento com o seq incrementado do projeto")
  void notifyEntregaSeqCorreto() throws Exception {
    CenarioFixture.Cenario cenario = fixtureCenario();
    UUID tarefaId = UUID.randomUUID();

    try (Connection ouvinte = dataSource.getConnection();
        Statement statement = ouvinte.createStatement()) {
      statement.execute("LISTEN " + canal);

      transactionTemplate.executeWithoutResult(
          status ->
              publisher.publicar(
                  EventoBoard.de(
                      cenario.projetoId(), TipoEventoBoard.TAREFA_CRIADA, tarefaId)));

      PGNotification[] notificacoes =
          ouvinte.unwrap(PGConnection.class).getNotifications(5_000);

      assertThat(notificacoes).isNotNull().isNotEmpty();
      JsonNode payload = payloadDoProjeto(notificacoes, cenario.projetoId());
      assertThat(payload).as("evento do projeto do cenario").isNotNull();
      assertThat(payload.get("seq").asLong()).isEqualTo(1L);
      assertThat(payload.get("tipo").asText()).isEqualTo("TAREFA_CRIADA");
      assertThat(payload.get("tarefaId").asText()).isEqualTo(tarefaId.toString());
    }
  }

  @Test
  @DisplayName("seq e monotonico e sem repeticao mesmo com publicacoes concorrentes")
  void seqMonotonicoSobConcorrencia() throws Exception {
    CenarioFixture.Cenario cenario = fixtureCenario();
    int publicacoes = 20;

    try (Connection ouvinte = dataSource.getConnection();
        Statement statement = ouvinte.createStatement()) {
      statement.execute("LISTEN " + canal);

      try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
        List<Callable<Void>> tarefas = new ArrayList<>();
        for (int i = 0; i < publicacoes; i++) {
          tarefas.add(
              () -> {
                transactionTemplate.executeWithoutResult(
                    status ->
                        publisher.publicar(
                            EventoBoard.de(
                                cenario.projetoId(),
                                TipoEventoBoard.TAREFA_ATUALIZADA,
                                UUID.randomUUID())));
                return null;
              });
        }
        for (Future<Void> resultado : executor.invokeAll(tarefas)) {
          resultado.get();
        }
      }

      Long ultimo =
          jdbc.queryForObject(
              "SELECT ultimo_seq FROM sequencia_projeto WHERE projeto_id = ?",
              Long.class,
              cenario.projetoId());

      assertThat(ultimo).isEqualTo(publicacoes);

      // Reservar o seq nao basta: o NOTIFY tem de sair para todas as publicacoes, sem repetir
      // nenhum seq — e o que garante que o cliente consiga detectar gap (ADR-004).
      List<Long> seqs = new ArrayList<>();
      long limite = System.currentTimeMillis() + 10_000;
      while (seqs.size() < publicacoes && System.currentTimeMillis() < limite) {
        PGNotification[] recebidas = ouvinte.unwrap(PGConnection.class).getNotifications(1_000);
        if (recebidas == null) {
          continue;
        }
        for (PGNotification notificacao : recebidas) {
          JsonNode no = objectMapper.readTree(notificacao.getParameter());
          if (cenario.projetoId().toString().equals(no.get("projetoId").asText())) {
            seqs.add(no.get("seq").asLong());
          }
        }
      }

      assertThat(seqs).hasSize(publicacoes).doesNotHaveDuplicates();
      assertThat(seqs).containsExactlyInAnyOrderElementsOf(
          java.util.stream.LongStream.rangeClosed(1, publicacoes).boxed().toList());
    }
  }

  @Test
  @DisplayName("rollback nao publica NOTIFY: o evento so sai em afterCommit")
  void rollbackNaoPublica() throws Exception {
    CenarioFixture.Cenario cenario = fixtureCenario();

    try (Connection ouvinte = dataSource.getConnection();
        Statement statement = ouvinte.createStatement()) {
      statement.execute("LISTEN " + canal);

      transactionTemplate.executeWithoutResult(
          status -> {
            publisher.publicar(
                EventoBoard.de(
                    cenario.projetoId(), TipoEventoBoard.TAREFA_CRIADA, UUID.randomUUID()));
            status.setRollbackOnly();
          });

      PGNotification[] notificacoes =
          ouvinte.unwrap(PGConnection.class).getNotifications(1_000);

      assertThat(payloadDoProjeto(notificacoes, cenario.projetoId())).isNull();
    }
  }

  private JsonNode payloadDoProjeto(PGNotification[] notificacoes, UUID projetoId)
      throws Exception {
    if (notificacoes == null) {
      return null;
    }
    for (PGNotification notificacao : notificacoes) {
      JsonNode no = objectMapper.readTree(notificacao.getParameter());
      if (projetoId.toString().equals(no.get("projetoId").asText())) {
        return no;
      }
    }
    return null;
  }

  @Autowired private CenarioFixture fixture;

  private CenarioFixture.Cenario fixtureCenario() {
    return fixture.criar();
  }
}
