package br.com.crudao.kanban.workflow.dto;

import java.util.UUID;

public record TransicaoResponse(UUID id, UUID etapaOrigemId, UUID etapaDestinoId) {}
