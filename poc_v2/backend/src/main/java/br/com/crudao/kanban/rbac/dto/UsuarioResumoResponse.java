package br.com.crudao.kanban.rbac.dto;

import java.util.UUID;

/** Usuario provisionado disponivel para associacao ao projeto (RF-015). */
public record UsuarioResumoResponse(UUID id, String nome, String email) {}
