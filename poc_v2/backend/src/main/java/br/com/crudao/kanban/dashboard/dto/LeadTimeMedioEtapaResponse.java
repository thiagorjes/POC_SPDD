package br.com.crudao.kanban.dashboard.dto;

import java.util.UUID;

/**
 * Media de permanencia por etapa (RF-007). {@code amostras == 0} identifica a etapa sem dados no
 * periodo e alimenta o estado vazio da TL-07.
 */
public record LeadTimeMedioEtapaResponse(
    UUID etapaId,
    String nome,
    int ordem,
    long mediaSegundos,
    long amostras,
    long impedimentoMedioSegundos) {}
