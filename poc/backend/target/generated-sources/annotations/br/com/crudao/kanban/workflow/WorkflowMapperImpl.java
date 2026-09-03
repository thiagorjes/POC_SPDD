package br.com.crudao.kanban.workflow;

import br.com.crudao.kanban.workflow.dto.EtapaResponse;
import br.com.crudao.kanban.workflow.dto.TransicaoResponse;
import br.com.crudao.kanban.workflow.dto.WorkflowResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-03T16:34:55-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21 (Oracle Corporation)"
)
@Component
public class WorkflowMapperImpl implements WorkflowMapper {

    @Override
    public WorkflowResponse paraResponse(Workflow workflow) {
        if ( workflow == null ) {
            return null;
        }

        UUID id = null;
        UUID projetoId = null;
        String nome = null;
        boolean ativo = false;

        id = workflow.getId();
        projetoId = workflow.getProjetoId();
        nome = workflow.getNome();
        ativo = workflow.isAtivo();

        WorkflowResponse workflowResponse = new WorkflowResponse( id, projetoId, nome, ativo );

        return workflowResponse;
    }

    @Override
    public List<WorkflowResponse> paraWorkflowResponse(List<Workflow> workflows) {
        if ( workflows == null ) {
            return null;
        }

        List<WorkflowResponse> list = new ArrayList<WorkflowResponse>( workflows.size() );
        for ( Workflow workflow : workflows ) {
            list.add( paraResponse( workflow ) );
        }

        return list;
    }

    @Override
    public EtapaResponse paraResponse(Etapa etapa) {
        if ( etapa == null ) {
            return null;
        }

        UUID id = null;
        String nome = null;
        int ordem = 0;
        boolean etapaFinal = false;

        id = etapa.getId();
        nome = etapa.getNome();
        ordem = etapa.getOrdem();
        etapaFinal = etapa.isEtapaFinal();

        EtapaResponse etapaResponse = new EtapaResponse( id, nome, ordem, etapaFinal );

        return etapaResponse;
    }

    @Override
    public List<EtapaResponse> paraEtapaResponse(List<Etapa> etapas) {
        if ( etapas == null ) {
            return null;
        }

        List<EtapaResponse> list = new ArrayList<EtapaResponse>( etapas.size() );
        for ( Etapa etapa : etapas ) {
            list.add( paraResponse( etapa ) );
        }

        return list;
    }

    @Override
    public TransicaoResponse paraResponse(Transicao transicao) {
        if ( transicao == null ) {
            return null;
        }

        UUID id = null;
        UUID etapaOrigemId = null;
        UUID etapaDestinoId = null;

        id = transicao.getId();
        etapaOrigemId = transicao.getEtapaOrigemId();
        etapaDestinoId = transicao.getEtapaDestinoId();

        TransicaoResponse transicaoResponse = new TransicaoResponse( id, etapaOrigemId, etapaDestinoId );

        return transicaoResponse;
    }

    @Override
    public List<TransicaoResponse> paraTransicaoResponse(List<Transicao> transicoes) {
        if ( transicoes == null ) {
            return null;
        }

        List<TransicaoResponse> list = new ArrayList<TransicaoResponse>( transicoes.size() );
        for ( Transicao transicao : transicoes ) {
            list.add( paraResponse( transicao ) );
        }

        return list;
    }
}
