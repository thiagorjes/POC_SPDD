package br.com.crudao.kanban.projeto.dto;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Alimenta a UI condicional (RNF-003). A UI e conveniencia; o backend permanece como autoridade de
 * autorizacao.
 */
public record PermissoesEfetivasResponse(
    UUID projetoId,
    boolean adminGlobal,
    boolean projetoAtivo,
    Set<String> permissoes,
    Map<String, Boolean> toggles) {}
