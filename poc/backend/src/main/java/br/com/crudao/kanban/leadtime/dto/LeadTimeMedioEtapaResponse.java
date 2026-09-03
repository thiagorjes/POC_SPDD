package br.com.crudao.kanban.leadtime.dto;

import java.util.UUID;

/**
 * Linha do dashboard. {@code amostras == 0} identifica a etapa sem dados na janela — o front usa
 * isso para o estado vazio da TL-07 em vez de exibir media zero como se fosse medicao real.
 */
public record LeadTimeMedioEtapaResponse(
    UUID etapaId,
    String etapaNome,
    int ordem,
    long amostras,
    long mediaPermanenciaSegundos,
    long mediaImpedimentoSegundos) {}
