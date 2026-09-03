package br.com.crudao.kanban.workflow.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CriarTransicaoRequest(
    @NotNull(message = "A etapa de origem e obrigatoria.") UUID etapaOrigemId,
    @NotNull(message = "A etapa de destino e obrigatoria.") UUID etapaDestinoId) {}
