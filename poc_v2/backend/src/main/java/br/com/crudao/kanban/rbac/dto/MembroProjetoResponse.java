package br.com.crudao.kanban.rbac.dto;

import java.util.List;
import java.util.UUID;

/** Membro do projeto e os papeis acumulados que ele possui nele (RF-015). */
public record MembroProjetoResponse(
    UUID usuarioId, String nome, String email, List<String> papeis) {}
