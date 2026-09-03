package br.com.crudao.kanban.workflow.dto;

import java.util.UUID;

public record EtapaResponse(UUID id, String nome, int ordem, boolean etapaFinal) {}
