package br.com.crudao.kanban.tarefa.dto;

import br.com.crudao.kanban.auditoria.CampoAuditado;
import java.time.Instant;
import java.util.UUID;

/** Entrada do historico da tarefa (RF-017). */
public record AuditoriaResponse(
    UUID id,
    UUID autorId,
    CampoAuditado campo,
    String valorAnterior,
    String valorNovo,
    Instant ocorridoEm) {}
