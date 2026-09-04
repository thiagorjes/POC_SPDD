package br.com.crudao.kanban.dashboard.dto;

import java.time.Instant;
import java.util.List;

/** Indicadores do projeto no periodo (RF-007). Etapas ordenadas por {@code Etapa.ordem}. */
public record DashboardResponse(
    Instant inicio,
    Instant fim,
    List<LeadTimeMedioEtapaResponse> etapas,
    long impedimentoMedioTotalSegundos,
    long totalTarefas) {}
