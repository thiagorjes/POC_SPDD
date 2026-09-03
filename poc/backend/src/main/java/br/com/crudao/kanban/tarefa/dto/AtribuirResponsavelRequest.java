package br.com.crudao.kanban.tarefa.dto;

import java.util.UUID;

/** {@code responsavelId} nulo remove o responsavel (RN-CB-004). */
public record AtribuirResponsavelRequest(UUID responsavelId) {}
