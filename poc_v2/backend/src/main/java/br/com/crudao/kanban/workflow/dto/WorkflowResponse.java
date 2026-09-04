package br.com.crudao.kanban.workflow.dto;

import java.util.UUID;

public record WorkflowResponse(UUID id, UUID projetoId, String nome, boolean ativo) {}
