package br.com.crudao.kanban.workflow;

import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
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
  private final WorkflowRepository workflowRepository;
  private final EventoBoardPublisher eventoBoardPublisher;

  /**
   * Rejeita self-loop, duplicata e saida a partir da etapa final: a etapa final nao tem transicao
   * de saida configuravel; o retorno e a operacao de desfinalizar (RN-004).
   */
  @Transactional
  public Transicao criar(UUID workflowId, UUID origemId, UUID destinoId) {
    if (origemId.equals(destinoId)) {
      throw new ConfiguracaoWorkflowInvalidaException(
          "A etapa de origem e a de destino nao podem ser a mesma.");
    }
    Etapa origem = etapaService.buscar(origemId);
    Etapa destino = etapaService.buscar(destinoId);
    if (!origem.getWorkflowId().equals(workflowId) || !destino.getWorkflowId().equals(workflowId)) {
      throw new ConfiguracaoWorkflowInvalidaException(
          "As etapas da transicao devem pertencer ao mesmo workflow.");
    }
    if (origem.isEtapaFinal()) {
      throw new ConfiguracaoWorkflowInvalidaException(
          "A etapa final nao possui transicao de saida configuravel. Use a operacao de desfinalizar.");
    }
    if (transicaoRepository.existsByWorkflowIdAndEtapaOrigemIdAndEtapaDestinoId(
        workflowId, origemId, destinoId)) {
      throw new ConfiguracaoWorkflowInvalidaException("Esta transicao ja esta configurada.");
    }

    Transicao transicao =
        transicaoRepository.save(
            Transicao.builder()
                .workflowId(workflowId)
                .etapaOrigemId(origemId)
                .etapaDestinoId(destinoId)
                .build());
    publicarReconfiguracao(workflowId);
    return transicao;
  }

  /** Revalida RN-003 apos a remocao — remover a ultima saida de uma etapa invalida o workflow. */
  @Transactional
  public void excluir(UUID id) {
    Transicao transicao =
        transicaoRepository
            .findById(id)
            .orElseThrow(() -> RecursoNaoEncontradoException.de("Transicao", id));
    UUID workflowId = transicao.getWorkflowId();
    transicaoRepository.delete(transicao);
    transicaoRepository.flush();
    etapaService.validarWorkflow(workflowId);
    publicarReconfiguracao(workflowId);
  }

  @Transactional(readOnly = true)
  public List<Transicao> listar(UUID workflowId) {
    return transicaoRepository.findByWorkflowId(workflowId);
  }

  @Transactional(readOnly = true)
  public List<Etapa> destinosPermitidos(UUID etapaOrigemId) {
    List<UUID> ids =
        transicaoRepository.findByEtapaOrigemId(etapaOrigemId).stream()
            .map(Transicao::getEtapaDestinoId)
            .toList();
    return ids.isEmpty() ? List.of() : etapaRepository.findAllById(ids);
  }

  private void publicarReconfiguracao(UUID workflowId) {
    workflowRepository
        .findById(workflowId)
        .ifPresent(
            workflow ->
                eventoBoardPublisher.publicar(
                    EventoBoard.de(
                        workflow.getProjetoId(), TipoEventoBoard.BOARD_RECONFIGURADO, null)));
  }
}
