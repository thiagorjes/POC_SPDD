package br.com.crudao.kanban.integracao;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Monta o cenario minimo direto em SQL: projeto ativo, workflow ativo com tres etapas encadeadas,
 * raia padrao e um usuario. Escrever pelo servico exigiria contexto de seguranca, o que tiraria o
 * foco destes testes das garantias do banco.
 */
@TestComponent
@RequiredArgsConstructor
public class CenarioFixture {

  private final JdbcTemplate jdbc;

  public record Cenario(
      UUID projetoId,
      UUID workflowId,
      UUID aFazerId,
      UUID fazendoId,
      UUID concluidoId,
      UUID raiaId,
      UUID usuarioId) {}

  public Cenario criar() {
    String sufixo = UUID.randomUUID().toString().substring(0, 8);

    UUID usuarioId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO usuario (id, keycloak_sub, email, nome, admin_global)"
            + " VALUES (?, ?, ?, ?, false)",
        usuarioId,
        "sub-" + sufixo,
        "usuario-" + sufixo + "@exemplo.test",
        "Usuario " + sufixo);

    UUID projetoId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projeto (id, nome, status) VALUES (?, ?, 'ATIVO')",
        projetoId,
        "Projeto " + sufixo);
    jdbc.update(
        "INSERT INTO sequencia_projeto (projeto_id, ultimo_seq) VALUES (?, 0)", projetoId);

    UUID workflowId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO workflow (id, projeto_id, nome, ativo) VALUES (?, ?, 'Padrao', true)",
        workflowId,
        projetoId);

    UUID aFazer = etapa(workflowId, "A fazer", 1, false);
    UUID fazendo = etapa(workflowId, "Fazendo", 2, false);
    UUID concluido = etapa(workflowId, "Concluido", 3, true);
    transicao(workflowId, aFazer, fazendo);
    transicao(workflowId, fazendo, concluido);

    UUID raiaId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO raia (id, projeto_id, nome, ordem, padrao) VALUES (?, ?, 'Padrao', 1, true)",
        raiaId,
        projetoId);

    return new Cenario(projetoId, workflowId, aFazer, fazendo, concluido, raiaId, usuarioId);
  }

  public UUID criarTarefa(Cenario cenario) {
    UUID tarefaId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tarefa (id, projeto_id, workflow_id, etapa_id, raia_id, criador_id,"
            + " titulo, tipo, prioridade) VALUES (?, ?, ?, ?, ?, ?, 'Tarefa', 'FEATURE', 'MEDIA')",
        tarefaId,
        cenario.projetoId(),
        cenario.workflowId(),
        cenario.aFazerId(),
        cenario.raiaId(),
        cenario.usuarioId());
    return tarefaId;
  }

  private UUID etapa(UUID workflowId, String nome, int ordem, boolean etapaFinal) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO etapa (id, workflow_id, nome, ordem, etapa_final) VALUES (?, ?, ?, ?, ?)",
        id,
        workflowId,
        nome,
        ordem,
        etapaFinal);
    return id;
  }

  private void transicao(UUID workflowId, UUID origem, UUID destino) {
    jdbc.update(
        "INSERT INTO transicao (id, workflow_id, etapa_origem_id, etapa_destino_id)"
            + " VALUES (?, ?, ?, ?)",
        UUID.randomUUID(),
        workflowId,
        origem,
        destino);
  }
}
