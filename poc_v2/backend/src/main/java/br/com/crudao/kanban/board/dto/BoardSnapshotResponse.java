package br.com.crudao.kanban.board.dto;

import br.com.crudao.kanban.raia.dto.RaiaResponse;
import br.com.crudao.kanban.tarefa.dto.TarefaResumoResponse;
import br.com.crudao.kanban.workflow.dto.EtapaResponse;
import java.util.List;
import java.util.UUID;

/**
 * Estado completo do board (RF-001, RF-011) e base do resync do stream (ADR-004): {@code seq} e o
 * ultimo valor observado do projeto no momento da leitura.
 */
public record BoardSnapshotResponse(
    UUID projetoId,
    UUID workflowId,
    long seq,
    boolean somenteLeitura,
    List<EtapaResponse> etapas,
    List<RaiaResponse> raias,
    List<TarefaResumoResponse> tarefas) {}
