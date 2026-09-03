package br.com.crudao.kanban.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarEtapaRequest(
    @NotBlank(message = "O nome da etapa e obrigatorio.")
        @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres.")
        String nome,
    boolean etapaFinal) {}
