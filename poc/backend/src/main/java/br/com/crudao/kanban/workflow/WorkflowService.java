package br.com.crudao.kanban.workflow;

import br.com.crudao.kanban.common.RecursoEmUsoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import br.com.crudao.kanban.workflow.dto.CriarWorkflowRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestao dos workflows do projeto (RF-009). Exatamente um ativo por projeto (A-9). */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

  private final WorkflowRepository workflowRepository;
  private final EtapaService etapaService;
  private final TarefaRepository tarefaRepository;
  private final EventoBoardPublisher eventoBoardPublisher;

  /** O primeiro workflow do projeto nasce ativo; os demais nascem inativos. */
  @Transactional
  public Workflow criar(UUID projetoId, CriarWorkflowRequest request) {
    boolean primeiro = !workflowRepository.existsByProjetoId(projetoId);
    Workflow workflow =
        workflowRepository.save(
            Workflow.builder().projetoId(projetoId).nome(request.nome()).ativo(primeiro).build());
    if (primeiro) {
      eventoBoardPublisher.publicar(
          EventoBoard.de(projetoId, TipoEventoBoard.BOARD_RECONFIGURADO, null));
    }
    log.info("Workflow {} criado no projeto {} (ativo={})", workflow.getId(), projetoId, primeiro);
    return workflow;
  }

  /**
   * Troca o workflow ativo do projeto na mesma transacao. Bloqueia se o workflow corrente ainda
   * tiver tarefas ativas — trocar deixaria essas tarefas posicionadas em etapas orfas.
   */
  @Transactional
  public Workflow ativar(UUID workflowId) {
    Workflow novo = buscar(workflowId);
    if (novo.isAtivo()) {
      return novo;
    }
    etapaService.validarWorkflow(workflowId);

    Optional<Workflow> corrente = workflowRepository.findByProjetoIdAndAtivoTrue(novo.getProjetoId());
    if (corrente.isPresent()) {
      long ativas = tarefaRepository.contarAtivasNoWorkflow(corrente.get().getId());
      if (ativas > 0) {
        throw new RecursoEmUsoException(
            "O workflow atual possui %d tarefa(s) ativa(s). Conclua ou mova essas tarefas antes de trocar o workflow."
                .formatted(ativas));
      }
      corrente.get().setAtivo(false);
      workflowRepository.saveAndFlush(corrente.get());
    }
    novo.setAtivo(true);
    eventoBoardPublisher.publicar(
        EventoBoard.de(novo.getProjetoId(), TipoEventoBoard.BOARD_RECONFIGURADO, null));
    return novo;
  }

  /** RN-005: workflow com tarefa ativa vinculada nao pode ser excluido. */
  @Transactional
  public void excluir(UUID workflowId) {
    Workflow workflow = buscar(workflowId);
    long ativas = tarefaRepository.contarAtivasNoWorkflow(workflowId);
    if (ativas > 0) {
      throw new RecursoEmUsoException(
          "O workflow possui %d tarefa(s) ativa(s) e nao pode ser excluido.".formatted(ativas));
    }
    workflowRepository.delete(workflow);
    eventoBoardPublisher.publicar(
        EventoBoard.de(workflow.getProjetoId(), TipoEventoBoard.BOARD_RECONFIGURADO, null));
  }

  @Transactional(readOnly = true)
  public List<Workflow> listar(UUID projetoId) {
    return workflowRepository.findByProjetoIdOrderByNomeAsc(projetoId);
  }

  @Transactional(readOnly = true)
  public Workflow buscar(UUID workflowId) {
    return workflowRepository
        .findById(workflowId)
        .orElseThrow(() -> RecursoNaoEncontradoException.de("Workflow", workflowId));
  }

  @Transactional(readOnly = true)
  public Optional<Workflow> ativoDoProjeto(UUID projetoId) {
    return workflowRepository.findByProjetoIdAndAtivoTrue(projetoId);
  }
}
