package br.com.crudao.kanban.leadtime.dto;

import java.util.UUID;

/** Duracoes em segundos (contrato de API). */
public record LeadTimeEtapaResponse(
    UUID etapaId, String etapaNome, long permanenciaSegundos, long impedimentoSegundos) {}
