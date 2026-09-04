package br.com.crudao.kanban.tarefa.dto;

import br.com.crudao.kanban.tarefa.PrioridadeTarefa;
import br.com.crudao.kanban.tarefa.TipoTarefa;
import java.util.List;
import java.util.UUID;

/** Card no snapshot do board (RF-001). */
public record TarefaResumoResponse(
    UUID id,
    String titulo,
    TipoTarefa tipo,
    PrioridadeTarefa prioridade,
    UUID etapaId,
    UUID raiaId,
    UUID responsavelId,
    boolean iniciada,
    boolean impedida,
    long versao,
    List<UUID> destinosPermitidos) {}
