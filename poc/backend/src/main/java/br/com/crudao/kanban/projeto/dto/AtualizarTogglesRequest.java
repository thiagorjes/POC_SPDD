package br.com.crudao.kanban.projeto.dto;

import br.com.crudao.kanban.projeto.ChaveToggle;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/** Conjunto fechado: chave fora do enum e rejeitada na desserializacao (BDR-001). */
public record AtualizarTogglesRequest(
    @NotEmpty(message = "Informe ao menos um toggle.") Map<ChaveToggle, Boolean> valores) {}
