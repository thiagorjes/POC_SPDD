package br.com.crudao.kanban.tarefa.dto;

import jakarta.validation.constraints.Size;

/** Marcacao de impedimento (RF-004). O motivo e opcional. */
public record MarcarImpedimentoRequest(
    @Size(max = 500, message = "O motivo deve ter no maximo 500 caracteres.") String motivo) {}
