package br.com.crudao.kanban.board;

import br.com.crudao.kanban.raia.dto.RaiaResponse;
import br.com.crudao.kanban.tarefa.dto.TarefaResumoResponse;
import br.com.crudao.kanban.workflow.dto.EtapaResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Estado completo do board. O {@code seq} devolvido aqui e a ancora do resync (ADR-004): o cliente
 * descarta eventos com {@code seq} menor ou igual e pede novo snapshot ao detectar gap.
 */
public record BoardSnapshotResponse(
    UUID projetoId,
    UUID workflowId,
    long seq,
    boolean somenteLeitura,
    List<EtapaResponse> etapas,
    List<RaiaResponse> raias,
    List<TarefaResumoResponse> tarefas,
    Set<String> permissoes) {}
