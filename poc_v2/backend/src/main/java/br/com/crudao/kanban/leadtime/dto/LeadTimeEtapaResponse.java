package br.com.crudao.kanban.leadtime.dto;

import java.util.UUID;

/** Lead-time de uma tarefa em uma etapa. Duracoes em segundos (Safeguards, secao 9). */
public record LeadTimeEtapaResponse(
    UUID etapaId, String etapaNome, int ordem, long duracaoSegundos, long impedimentoSegundos) {}
