package br.com.crudao.kanban.leadtime.dto;

import java.util.List;

/** Agregado por tarefa: detalhe por etapa e total impedido (RF-006). */
public record DetalheLeadTime(List<LeadTimeEtapaResponse> etapas, long impedimentoTotalSegundos) {}
