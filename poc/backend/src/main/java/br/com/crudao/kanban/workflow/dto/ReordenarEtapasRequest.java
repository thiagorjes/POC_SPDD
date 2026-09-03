package br.com.crudao.kanban.workflow.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

/** Reordenar e apresentacao pura: nunca altera o grafo de transicoes. */
public record ReordenarEtapasRequest(
    @NotEmpty(message = "Informe a nova ordem das etapas.") List<UUID> ordemIds) {}
