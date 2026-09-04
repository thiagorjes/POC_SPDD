package br.com.crudao.kanban.projeto.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** Atualizacao dos toggles do projeto; chaves fora do catalogo fechado sao rejeitadas (RF-016). */
public record AtualizarTogglesRequest(
    @NotNull(message = "O mapa de toggles e obrigatorio.") Map<String, Boolean> toggles) {}
