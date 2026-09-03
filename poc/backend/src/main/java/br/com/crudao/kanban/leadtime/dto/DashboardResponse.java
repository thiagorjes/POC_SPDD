package br.com.crudao.kanban.leadtime.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
    UUID projetoId,
    Instant inicio,
    Instant fim,
    List<LeadTimeMedioEtapaResponse> etapas,
    long impedimentoMedioTotalSegundos) {}
