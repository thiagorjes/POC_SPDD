package br.com.crudao.kanban.tarefa;

import br.com.crudao.kanban.auditoria.AuditoriaTarefa;
import br.com.crudao.kanban.leadtime.dto.LeadTimeEtapaResponse;
import br.com.crudao.kanban.tarefa.dto.AuditoriaResponse;
import br.com.crudao.kanban.tarefa.dto.TarefaDetalheResponse;
import br.com.crudao.kanban.tarefa.dto.TarefaResumoResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Conversao entidade/DTO de tarefa. Os dados derivados (destinos permitidos, lead time, historico e
 * acoes permitidas) sao calculados na Service Layer e injetados como parametros.
 */
@Mapper(componentModel = "spring")
public interface TarefaMapper {

  @Mapping(target = "destinosPermitidos", source = "destinosPermitidos")
  TarefaResumoResponse paraResumo(Tarefa tarefa, List<UUID> destinosPermitidos);

  TarefaDetalheResponse paraDetalhe(
      Tarefa tarefa,
      List<LeadTimeEtapaResponse> leadTimePorEtapa,
      long impedimentoTotalSegundos,
      List<AuditoriaResponse> historico,
      Set<String> acoesPermitidas);

  AuditoriaResponse paraAuditoria(AuditoriaTarefa auditoria);

  List<AuditoriaResponse> paraAuditorias(List<AuditoriaTarefa> auditorias);
}
