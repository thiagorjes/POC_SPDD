package br.com.crudao.kanban.raia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarRaiaRequest(
    @NotBlank(message = "O nome da raia e obrigatorio.")
        @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres.")
        String nome) {}
