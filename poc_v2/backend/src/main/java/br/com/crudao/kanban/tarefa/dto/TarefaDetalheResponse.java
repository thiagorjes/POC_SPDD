package br.com.crudao.kanban.tarefa.dto;

import br.com.crudao.kanban.leadtime.dto.LeadTimeEtapaResponse;
import br.com.crudao.kanban.tarefa.PrioridadeTarefa;
import br.com.crudao.kanban.tarefa.TipoTarefa;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Detalhe do card com lead-time e historico (RF-006, RF-017). */
public record TarefaDetalheResponse(
    UUID id,
    UUID projetoId,
    UUID workflowId,
    UUID etapaId,
    UUID raiaId,
    UUID responsavelId,
    UUID criadorId,
    String titulo,
    String descricao,
    TipoTarefa tipo,
    PrioridadeTarefa prioridade,
    boolean iniciada,
    boolean impedida,
    Instant criadaEm,
    long versao,
    List<LeadTimeEtapaResponse> leadTimePorEtapa,
    long impedimentoTotalSegundos,
    List<AuditoriaResponse> historico,
    Set<String> acoesPermitidas) {}
