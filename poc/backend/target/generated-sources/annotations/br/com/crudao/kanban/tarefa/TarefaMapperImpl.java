package br.com.crudao.kanban.tarefa;

import br.com.crudao.kanban.auditoria.AuditoriaTarefa;
import br.com.crudao.kanban.auditoria.CampoAuditado;
import br.com.crudao.kanban.leadtime.dto.LeadTimeEtapaResponse;
import br.com.crudao.kanban.tarefa.dto.AuditoriaResponse;
import br.com.crudao.kanban.tarefa.dto.TarefaDetalheResponse;
import br.com.crudao.kanban.tarefa.dto.TarefaResumoResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-03T16:34:55-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21 (Oracle Corporation)"
)
@Component
public class TarefaMapperImpl implements TarefaMapper {

    @Override
    public TarefaResumoResponse paraResumo(Tarefa tarefa, String responsavelNome, List<UUID> destinosPermitidos) {
        if ( tarefa == null && responsavelNome == null && destinosPermitidos == null ) {
            return null;
        }

        UUID id = null;
        String titulo = null;
        TipoTarefa tipo = null;
        PrioridadeTarefa prioridade = null;
        UUID etapaId = null;
        UUID raiaId = null;
        UUID responsavelId = null;
        boolean iniciada = false;
        boolean impedida = false;
        long versao = 0L;
        if ( tarefa != null ) {
            id = tarefa.getId();
            titulo = tarefa.getTitulo();
            tipo = tarefa.getTipo();
            prioridade = tarefa.getPrioridade();
            etapaId = tarefa.getEtapaId();
            raiaId = tarefa.getRaiaId();
            responsavelId = tarefa.getResponsavelId();
            iniciada = tarefa.isIniciada();
            impedida = tarefa.isImpedida();
            versao = tarefa.getVersao();
        }
        String responsavelNome1 = null;
        responsavelNome1 = responsavelNome;
        List<UUID> destinosPermitidos1 = null;
        List<UUID> list = destinosPermitidos;
        if ( list != null ) {
            destinosPermitidos1 = new ArrayList<UUID>( list );
        }

        TarefaResumoResponse tarefaResumoResponse = new TarefaResumoResponse( id, titulo, tipo, prioridade, etapaId, raiaId, responsavelId, responsavelNome1, iniciada, impedida, versao, destinosPermitidos1 );

        return tarefaResumoResponse;
    }

    @Override
    public TarefaDetalheResponse paraDetalhe(Tarefa tarefa, String responsavelNome, String motivoImpedimento, List<LeadTimeEtapaResponse> leadTimePorEtapa, long impedimentoTotalSegundos, List<AuditoriaResponse> historico, List<UUID> destinosPermitidos, Set<String> acoesPermitidas, boolean observando) {
        if ( tarefa == null && responsavelNome == null && motivoImpedimento == null && leadTimePorEtapa == null && historico == null && destinosPermitidos == null && acoesPermitidas == null ) {
            return null;
        }

        UUID id = null;
        UUID projetoId = null;
        UUID workflowId = null;
        UUID etapaId = null;
        UUID raiaId = null;
        String titulo = null;
        String descricao = null;
        TipoTarefa tipo = null;
        PrioridadeTarefa prioridade = null;
        UUID responsavelId = null;
        UUID criadorId = null;
        boolean iniciada = false;
        boolean impedida = false;
        Instant criadaEm = null;
        long versao = 0L;
        if ( tarefa != null ) {
            id = tarefa.getId();
            projetoId = tarefa.getProjetoId();
            workflowId = tarefa.getWorkflowId();
            etapaId = tarefa.getEtapaId();
            raiaId = tarefa.getRaiaId();
            titulo = tarefa.getTitulo();
            descricao = tarefa.getDescricao();
            tipo = tarefa.getTipo();
            prioridade = tarefa.getPrioridade();
            responsavelId = tarefa.getResponsavelId();
            criadorId = tarefa.getCriadorId();
            iniciada = tarefa.isIniciada();
            impedida = tarefa.isImpedida();
            criadaEm = tarefa.getCriadaEm();
            versao = tarefa.getVersao();
        }
        String responsavelNome1 = null;
        responsavelNome1 = responsavelNome;
        String motivoImpedimento1 = null;
        motivoImpedimento1 = motivoImpedimento;
        List<LeadTimeEtapaResponse> leadTimePorEtapa1 = null;
        List<LeadTimeEtapaResponse> list = leadTimePorEtapa;
        if ( list != null ) {
            leadTimePorEtapa1 = new ArrayList<LeadTimeEtapaResponse>( list );
        }
        long impedimentoTotalSegundos1 = 0L;
        impedimentoTotalSegundos1 = impedimentoTotalSegundos;
        List<AuditoriaResponse> historico1 = null;
        List<AuditoriaResponse> list1 = historico;
        if ( list1 != null ) {
            historico1 = new ArrayList<AuditoriaResponse>( list1 );
        }
        List<UUID> destinosPermitidos1 = null;
        List<UUID> list2 = destinosPermitidos;
        if ( list2 != null ) {
            destinosPermitidos1 = new ArrayList<UUID>( list2 );
        }
        Set<String> acoesPermitidas1 = null;
        Set<String> set = acoesPermitidas;
        if ( set != null ) {
            acoesPermitidas1 = new LinkedHashSet<String>( set );
        }
        boolean observando1 = false;
        observando1 = observando;

        TarefaDetalheResponse tarefaDetalheResponse = new TarefaDetalheResponse( id, projetoId, workflowId, etapaId, raiaId, titulo, descricao, tipo, prioridade, responsavelId, responsavelNome1, criadorId, iniciada, impedida, motivoImpedimento1, criadaEm, versao, leadTimePorEtapa1, impedimentoTotalSegundos1, historico1, destinosPermitidos1, acoesPermitidas1, observando1 );

        return tarefaDetalheResponse;
    }

    @Override
    public AuditoriaResponse paraAuditoria(AuditoriaTarefa auditoria, String autorNome) {
        if ( auditoria == null && autorNome == null ) {
            return null;
        }

        UUID id = null;
        UUID autorId = null;
        CampoAuditado campo = null;
        String valorAnterior = null;
        String valorNovo = null;
        Instant ocorridoEm = null;
        if ( auditoria != null ) {
            id = auditoria.getId();
            autorId = auditoria.getAutorId();
            campo = auditoria.getCampo();
            valorAnterior = auditoria.getValorAnterior();
            valorNovo = auditoria.getValorNovo();
            ocorridoEm = auditoria.getOcorridoEm();
        }
        String autorNome1 = null;
        autorNome1 = autorNome;

        AuditoriaResponse auditoriaResponse = new AuditoriaResponse( id, autorId, autorNome1, campo, valorAnterior, valorNovo, ocorridoEm );

        return auditoriaResponse;
    }
}
