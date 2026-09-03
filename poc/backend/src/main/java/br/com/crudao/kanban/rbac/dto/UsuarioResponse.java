package br.com.crudao.kanban.rbac.dto;

import java.util.UUID;

/** {@code adminGlobal} e somente leitura: nenhum endpoint de escrita o aceita (ADR-007). */
public record UsuarioResponse(UUID id, String nome, String email, boolean adminGlobal) {}
