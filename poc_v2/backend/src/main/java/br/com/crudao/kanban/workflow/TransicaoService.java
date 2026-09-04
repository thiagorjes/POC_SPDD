package br.com.crudao.kanban.workflow;

import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.rbac.CodigoPermissao;
import br.com.crudao.kanban.security.ExigePermissao;
import br.com.crudao.kanban.security.OrigemEscopo;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Arestas do grafo de workflow (RF-002). */
@Service
@RequiredArgsConstructor
public class TransicaoService {

  private final TransicaoRepository transicaoRepository;
  private final EtapaRepository etapaRepository;
  private final EtapaService etapaService;
  private final WorkflowService workflowService;

  /**
   * Cria a aresta. Rejeita self-loop, duplicata e origem na etapa final: a etapa final nao tem
   * saida configuravel, o retorno e a operacao de desfinalizar (RN-004).
   */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.WORKFLOW_GERENCIAR,
      escopoProjeto = "#workflowId",
      origem = OrigemEscopo.WORKFLOW)
  public Transicao criar(UUID workflowId, UUID origemId, UUID destinoId) {
    if (origemId.equals(destinoId)) {
      throw new ConfiguracaoWorkflowInvalidaException(
          "A etapa de origem e a de destino nao podem ser a mesma.");
    }
    Etapa origem = etapaService.buscarEntidade(origemId);
    Etapa destino = etapaService.buscarEntidade(destinoId);
    if (!origem.getWorkflowId().equals(workflowId) || !destino.getWorkflowId().equals(workflowId)) {
      throw new ConfiguracaoWorkflowInvalidaException(
          "As etapas informadas nao pertencem a este workflow.");
    }
    if (origem.isEtapaFinal()) {
      throw new ConfiguracaoWorkflowInvalidaException(
          "A etapa final nao possui transicao de saida configuravel.");
    }
    if (transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(origemId, destinoId)) {
      throw new ConfiguracaoWorkflowInvalidaException("Esta transicao ja esta configurada.");
    }
    Transicao transicao = transicaoRepository.save(new Transicao(workflowId, origemId, destinoId));
    workflowService.publicarReconfiguracao(
        workflowService.buscarEntidade(workflowId).getProjetoId());
    return transicao;
  }

  /** Remove a aresta e revalida RN-003. */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.WORKFLOW_GERENCIAR,
      escopoProjeto = "#id",
      origem = OrigemEscopo.TRANSICAO)
  public void excluir(UUID id) {
    Transicao transicao =
        transicaoRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Transicao nao encontrada."));
    transicaoRepository.delete(transicao);
    transicaoRepository.flush();
    etapaService.validarWorkflow(transicao.getWorkflowId());
    workflowService.publicarReconfiguracao(
        workflowService.buscarEntidade(transicao.getWorkflowId()).getProjetoId());
  }

  /** Etapas alcancaveis a partir da origem informada, segundo o grafo configurado. */
  @Transactional(readOnly = true)
  public List<Etapa> destinosPermitidos(UUID etapaOrigemId) {
    List<UUID> ids =
        transicaoRepository.findByEtapaOrigemId(etapaOrigemId).stream()
            .map(Transicao::getEtapaDestinoId)
            .toList();
    return ids.isEmpty() ? List.of() : etapaRepository.findAllById(ids);
  }

  /** Transicoes do workflow. */
  @Transactional(readOnly = true)
  public List<Transicao> listar(UUID workflowId) {
    return transicaoRepository.findByWorkflowId(workflowId);
  }
}
