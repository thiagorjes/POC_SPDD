package br.com.crudao.kanban.workflow;

import br.com.crudao.kanban.workflow.dto.EtapaResponse;
import br.com.crudao.kanban.workflow.dto.TransicaoResponse;
import br.com.crudao.kanban.workflow.dto.WorkflowResponse;
import java.util.List;
import org.mapstruct.Mapper;

/** Conversao entidade/DTO do dominio de workflow. */
@Mapper(componentModel = "spring")
public interface WorkflowMapper {

  WorkflowResponse paraResponse(Workflow workflow);

  List<WorkflowResponse> paraWorkflowResponses(List<Workflow> workflows);

  EtapaResponse paraResponse(Etapa etapa);

  List<EtapaResponse> paraEtapaResponses(List<Etapa> etapas);

  TransicaoResponse paraResponse(Transicao transicao);

  List<TransicaoResponse> paraTransicaoResponses(List<Transicao> transicoes);
}
