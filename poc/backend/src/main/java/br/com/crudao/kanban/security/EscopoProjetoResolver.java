package br.com.crudao.kanban.security;

import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.notificacao.NotificacaoRepository;
import br.com.crudao.kanban.raia.RaiaRepository;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import br.com.crudao.kanban.workflow.EtapaRepository;
import br.com.crudao.kanban.workflow.WorkflowRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deriva o projeto de autorizacao a partir do recurso alvo. Nenhum endpoint aceita {@code projetoId}
 * como parametro de autorizacao (restricao de seguranca 3).
 */
@Component("resolvedor")
@RequiredArgsConstructor
public class EscopoProjetoResolver {

  private final TarefaRepository tarefaRepository;
  private final WorkflowRepository workflowRepository;
  private final EtapaRepository etapaRepository;
  private final RaiaRepository raiaRepository;
  private final NotificacaoRepository notificacaoRepository;

  /** O proprio recurso ja e o projeto: usado apenas em rotas cujo path e {@code /projetos/{id}}. */
  public UUID deProjeto(UUID projetoId) {
    return projetoId;
  }

  @Transactional(readOnly = true)
  public UUID deTarefa(UUID tarefaId) {
    return tarefaRepository
        .findById(tarefaId)
        .orElseThrow(() -> RecursoNaoEncontradoException.de("Tarefa", tarefaId))
        .getProjetoId();
  }

  @Transactional(readOnly = true)
  public UUID deWorkflow(UUID workflowId) {
    return workflowRepository
        .findById(workflowId)
        .orElseThrow(() -> RecursoNaoEncontradoException.de("Workflow", workflowId))
        .getProjetoId();
  }

  @Transactional(readOnly = true)
  public UUID deEtapa(UUID etapaId) {
    UUID workflowId =
        etapaRepository
            .findById(etapaId)
            .orElseThrow(() -> RecursoNaoEncontradoException.de("Etapa", etapaId))
            .getWorkflowId();
    return deWorkflow(workflowId);
  }

  @Transactional(readOnly = true)
  public UUID deRaia(UUID raiaId) {
    return raiaRepository
        .findById(raiaId)
        .orElseThrow(() -> RecursoNaoEncontradoException.de("Raia", raiaId))
        .getProjetoId();
  }

  @Transactional(readOnly = true)
  public UUID deNotificacao(UUID notificacaoId) {
    return notificacaoRepository
        .findById(notificacaoId)
        .orElseThrow(() -> RecursoNaoEncontradoException.de("Notificacao", notificacaoId))
        .getProjetoId();
  }
}
