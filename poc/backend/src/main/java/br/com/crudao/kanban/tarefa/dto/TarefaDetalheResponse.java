package br.com.crudao.kanban.tarefa.dto;

import br.com.crudao.kanban.leadtime.dto.LeadTimeEtapaResponse;
import br.com.crudao.kanban.tarefa.PrioridadeTarefa;
import br.com.crudao.kanban.tarefa.TipoTarefa;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record TarefaDetalheResponse(
    UUID id,
    UUID projetoId,
    UUID workflowId,
    UUID etapaId,
    UUID raiaId,
    String titulo,
    String descricao,
    TipoTarefa tipo,
    PrioridadeTarefa prioridade,
    UUID responsavelId,
    String responsavelNome,
    UUID criadorId,
    boolean iniciada,
    boolean impedida,
    String motivoImpedimento,
    Instant criadaEm,
    long versao,
    List<LeadTimeEtapaResponse> leadTimePorEtapa,
    long impedimentoTotalSegundos,
    List<AuditoriaResponse> historico,
    List<UUID> destinosPermitidos,
    Set<String> acoesPermitidas,
    boolean observando) {}
