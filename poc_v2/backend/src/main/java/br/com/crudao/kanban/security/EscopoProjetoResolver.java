package br.com.crudao.kanban.security;

import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.raia.RaiaRepository;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import br.com.crudao.kanban.workflow.EtapaRepository;
import br.com.crudao.kanban.workflow.TransicaoRepository;
import br.com.crudao.kanban.workflow.WorkflowRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deriva o {@code projetoId} de autorizacao a partir do recurso alvo. Aceitar o projeto de um
 * parametro do cliente e exatamente o vetor de escalonamento entre projetos que RNF-003 proibe.
 */
@Component
@RequiredArgsConstructor
public class EscopoProjetoResolver {

  private final TarefaRepository tarefaRepository;
  private final WorkflowRepository workflowRepository;
  private final EtapaRepository etapaRepository;
  private final TransicaoRepository transicaoRepository;
  private final RaiaRepository raiaRepository;

  /** Projeto que contem o recurso informado. */
  @Transactional(readOnly = true)
  public UUID resolver(OrigemEscopo origem, UUID recursoId) {
    if (recursoId == null) {
      throw new RecursoNaoEncontradoException("Recurso alvo da operacao nao informado.");
    }
    return switch (origem) {
      case PROJETO -> recursoId;
      case TAREFA ->
          tarefaRepository
              .findById(recursoId)
              .orElseThrow(() -> naoEncontrado("Tarefa"))
              .getProjetoId();
      case WORKFLOW -> projetoDoWorkflow(recursoId);
      case ETAPA ->
          projetoDoWorkflow(
              etapaRepository
                  .findById(recursoId)
                  .orElseThrow(() -> naoEncontrado("Etapa"))
                  .getWorkflowId());
      case TRANSICAO ->
          projetoDoWorkflow(
              transicaoRepository
                  .findById(recursoId)
                  .orElseThrow(() -> naoEncontrado("Transicao"))
                  .getWorkflowId());
      case RAIA ->
          raiaRepository
              .findById(recursoId)
              .orElseThrow(() -> naoEncontrado("Raia"))
              .getProjetoId();
    };
  }

  private UUID projetoDoWorkflow(UUID workflowId) {
    return workflowRepository
        .findById(workflowId)
        .orElseThrow(() -> naoEncontrado("Workflow"))
        .getProjetoId();
  }

  private RecursoNaoEncontradoException naoEncontrado(String recurso) {
    return new RecursoNaoEncontradoException(recurso + " nao encontrado(a).");
  }
}
