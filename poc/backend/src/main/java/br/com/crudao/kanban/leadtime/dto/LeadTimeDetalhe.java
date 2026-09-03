package br.com.crudao.kanban.leadtime.dto;

import java.util.List;

/** Detalhe de lead-time de uma tarefa (RF-006). */
public record LeadTimeDetalhe(List<LeadTimeEtapaResponse> porEtapa, long impedimentoTotalSegundos) {}
