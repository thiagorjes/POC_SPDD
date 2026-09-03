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

@Mapper(componentModel = "spring")
public interface TarefaMapper {

  @Mapping(target = "responsavelNome", source = "responsavelNome")
  @Mapping(target = "destinosPermitidos", source = "destinosPermitidos")
  TarefaResumoResponse paraResumo(
      Tarefa tarefa, String responsavelNome, List<UUID> destinosPermitidos);

  @Mapping(target = "responsavelNome", source = "responsavelNome")
  @Mapping(target = "motivoImpedimento", source = "motivoImpedimento")
  @Mapping(target = "leadTimePorEtapa", source = "leadTimePorEtapa")
  @Mapping(target = "impedimentoTotalSegundos", source = "impedimentoTotalSegundos")
  @Mapping(target = "historico", source = "historico")
  @Mapping(target = "destinosPermitidos", source = "destinosPermitidos")
  @Mapping(target = "acoesPermitidas", source = "acoesPermitidas")
  @Mapping(target = "observando", source = "observando")
  TarefaDetalheResponse paraDetalhe(
      Tarefa tarefa,
      String responsavelNome,
      String motivoImpedimento,
      List<LeadTimeEtapaResponse> leadTimePorEtapa,
      long impedimentoTotalSegundos,
      List<AuditoriaResponse> historico,
      List<UUID> destinosPermitidos,
      Set<String> acoesPermitidas,
      boolean observando);

  @Mapping(target = "autorNome", source = "autorNome")
  AuditoriaResponse paraAuditoria(AuditoriaTarefa auditoria, String autorNome);
}
