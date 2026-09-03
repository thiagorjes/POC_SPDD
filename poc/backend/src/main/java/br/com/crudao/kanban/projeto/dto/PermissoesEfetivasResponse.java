package br.com.crudao.kanban.projeto.dto;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Alimenta a UI condicional. A UI e conveniencia; o backend continua sendo a autoridade. */
public record PermissoesEfetivasResponse(
    UUID projetoId,
    boolean adminGlobal,
    boolean projetoAtivo,
    Set<String> permissoes,
    Set<String> papeis,
    Map<String, Boolean> toggles) {}
