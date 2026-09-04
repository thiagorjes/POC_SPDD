package br.com.crudao.kanban.workflow.dto;

import java.util.UUID;

public record TransicaoResponse(
    UUID id, UUID workflowId, UUID etapaOrigemId, UUID etapaDestinoId) {}
