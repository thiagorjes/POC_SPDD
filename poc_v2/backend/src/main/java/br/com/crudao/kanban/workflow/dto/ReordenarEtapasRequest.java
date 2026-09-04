package br.com.crudao.kanban.workflow.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

/** Reordenacao de colunas: nunca altera o grafo de transicoes (Safeguards, secao 1). */
public record ReordenarEtapasRequest(
    @NotEmpty(message = "A ordem das etapas e obrigatoria.") List<UUID> ordemIds) {}
