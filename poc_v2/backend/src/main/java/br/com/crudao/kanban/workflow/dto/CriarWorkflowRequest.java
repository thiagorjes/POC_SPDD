package br.com.crudao.kanban.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload de criacao de workflow (RF-009). */
public record CriarWorkflowRequest(
    @NotBlank(message = "O nome do workflow e obrigatorio.")
        @Size(max = 200, message = "O nome deve ter no maximo 200 caracteres.")
        String nome) {}
