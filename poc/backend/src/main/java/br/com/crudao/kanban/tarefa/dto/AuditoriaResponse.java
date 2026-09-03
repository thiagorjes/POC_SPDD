package br.com.crudao.kanban.tarefa.dto;

import br.com.crudao.kanban.auditoria.CampoAuditado;
import java.time.Instant;
import java.util.UUID;

public record AuditoriaResponse(
    UUID id,
    UUID autorId,
    String autorNome,
    CampoAuditado campo,
    String valorAnterior,
    String valorNovo,
    Instant ocorridoEm) {}
