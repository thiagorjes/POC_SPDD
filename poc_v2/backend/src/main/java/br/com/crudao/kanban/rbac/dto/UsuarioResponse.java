package br.com.crudao.kanban.rbac.dto;

import java.util.UUID;

/** Identidade do usuario autenticado, provisionada JIT a partir do token (ADR-003/007). */
public record UsuarioResponse(UUID id, String nome, String email, boolean adminGlobal) {}
