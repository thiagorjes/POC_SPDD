package br.com.crudao.kanban.workflow;

import br.com.crudao.kanban.common.RecursoEmUsoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.rbac.CodigoPermissao;
import br.com.crudao.kanban.security.ExigePermissao;
import br.com.crudao.kanban.security.OrigemEscopo;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestao dos workflows do projeto. Exatamente um permanece ativo (A-9, RF-009). */
@Service
@RequiredArgsConstructor
public class WorkflowService {

  private final WorkflowRepository workflowRepository;
  private final TarefaRepository tarefaRepository;
  private final EventoBoardPublisher eventoBoardPublisher;

  /** O primeiro workflow do projeto nasce ativo; os demais nascem inativos (RF-009). */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.WORKFLOW_GERENCIAR,
      escopoProjeto = "#projetoId",
      origem = OrigemEscopo.PROJETO)
  public Workflow criar(UUID projetoId, String nome) {
    boolean primeiro = workflowRepository.countByProjetoId(projetoId) == 0;
    Workflow workflow = workflowRepository.save(new Workflow(projetoId, nome, primeiro));
    publicarReconfiguracao(projetoId);
    return workflow;
  }

  /**
   * Ativa o workflow informado e desativa o anterior na mesma transacao. Bloqueia quando existem
   * tarefas ativas no workflow corrente, para nao deixar cards orfaos de etapa.
   */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.WORKFLOW_GERENCIAR,
      escopoProjeto = "#workflowId",
      origem = OrigemEscopo.WORKFLOW)
  public Workflow ativar(UUID workflowId) {
    Workflow novo = buscarEntidade(workflowId);
    Optional<Workflow> atual = workflowRepository.findByProjetoIdAndAtivoTrue(novo.getProjetoId());
    if (atual.isPresent() && atual.get().getId().equals(workflowId)) {
      return novo;
    }
    if (atual.isPresent() && tarefaRepository.contarAtivasPorWorkflow(atual.get().getId()) > 0) {
      throw new RecursoEmUsoException(
          "O workflow atual possui tarefas ativas e nao pode ser substituido.");
    }
    atual.ifPresent(workflow -> workflow.setAtivo(false));
    workflowRepository.flush();
    novo.setAtivo(true);
    publicarReconfiguracao(novo.getProjetoId());
    return novo;
  }

  /** RN-005: bloqueia a exclusao enquanto houver tarefa ativa vinculada. */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.WORKFLOW_GERENCIAR,
      escopoProjeto = "#workflowId",
      origem = OrigemEscopo.WORKFLOW)
  public void excluir(UUID workflowId) {
    Workflow workflow = buscarEntidade(workflowId);
    if (tarefaRepository.contarAtivasPorWorkflow(workflowId) > 0) {
      throw new RecursoEmUsoException("O workflow possui tarefas ativas e nao pode ser excluido.");
    }
    workflowRepository.delete(workflow);
    publicarReconfiguracao(workflow.getProjetoId());
  }

  /** Workflows do projeto (RF-009). */
  @Transactional(readOnly = true)
  @ExigePermissao(
      valor = CodigoPermissao.PROJETO_VISUALIZAR,
      escopoProjeto = "#projetoId",
      origem = OrigemEscopo.PROJETO,
      escrita = false)
  public List<Workflow> listar(UUID projetoId) {
    return workflowRepository.findByProjetoIdOrderByNomeAsc(projetoId);
  }

  /** Workflow ativo do projeto; base do board renderizado (A-9). */
  @Transactional(readOnly = true)
  public Optional<Workflow> ativoDoProjeto(UUID projetoId) {
    return workflowRepository.findByProjetoIdAndAtivoTrue(projetoId);
  }

  /** Operacoes de reconfiguracao emitem um unico evento em lote (Safeguards, secao 2). */
  void publicarReconfiguracao(UUID projetoId) {
    eventoBoardPublisher.publicar(
        EventoBoard.deProjeto(projetoId, TipoEventoBoard.BOARD_RECONFIGURADO), Set.of());
  }

  Workflow buscarEntidade(UUID workflowId) {
    return workflowRepository
        .findById(workflowId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Workflow nao encontrado."));
  }
}
