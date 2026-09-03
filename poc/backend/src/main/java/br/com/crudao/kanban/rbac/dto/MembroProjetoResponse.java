package br.com.crudao.kanban.rbac.dto;

import java.util.List;
import java.util.UUID;

public record MembroProjetoResponse(
    UUID usuarioId, String nome, String email, List<String> papeis) {}
