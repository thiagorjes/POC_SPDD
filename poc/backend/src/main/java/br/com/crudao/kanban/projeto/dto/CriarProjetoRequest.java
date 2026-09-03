package br.com.crudao.kanban.projeto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarProjetoRequest(
    @NotBlank(message = "O nome do projeto e obrigatorio.")
        @Size(max = 200, message = "O nome deve ter no maximo 200 caracteres.")
        String nome,
    @Size(max = 4000, message = "A descricao deve ter no maximo 4000 caracteres.")
        String descricao) {}
