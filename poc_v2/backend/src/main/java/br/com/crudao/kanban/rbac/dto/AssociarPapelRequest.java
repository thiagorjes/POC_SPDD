package br.com.crudao.kanban.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Associacao de papel a um usuario dentro do projeto (RF-015). */
public record AssociarPapelRequest(
    @NotNull(message = "O usuario e obrigatorio.") UUID usuarioId,
    @NotBlank(message = "O codigo do papel e obrigatorio.") String codigoPapel) {}
