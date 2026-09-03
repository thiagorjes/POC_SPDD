package br.com.crudao.kanban.workflow;

import br.com.crudao.kanban.workflow.dto.EtapaResponse;
import br.com.crudao.kanban.workflow.dto.TransicaoResponse;
import br.com.crudao.kanban.workflow.dto.WorkflowResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkflowMapper {

  WorkflowResponse paraResponse(Workflow workflow);

  List<WorkflowResponse> paraWorkflowResponse(List<Workflow> workflows);

  EtapaResponse paraResponse(Etapa etapa);

  List<EtapaResponse> paraEtapaResponse(List<Etapa> etapas);

  TransicaoResponse paraResponse(Transicao transicao);

  List<TransicaoResponse> paraTransicaoResponse(List<Transicao> transicoes);
}
