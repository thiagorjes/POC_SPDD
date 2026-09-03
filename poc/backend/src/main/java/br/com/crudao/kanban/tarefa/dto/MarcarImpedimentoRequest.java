package br.com.crudao.kanban.tarefa.dto;

import jakarta.validation.constraints.Size;

public record MarcarImpedimentoRequest(
    @Size(max = 500, message = "O motivo deve ter no maximo 500 caracteres.") String motivo) {}
