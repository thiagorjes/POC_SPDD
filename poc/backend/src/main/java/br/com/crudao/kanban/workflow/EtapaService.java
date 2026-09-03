package br.com.crudao.kanban.workflow;

import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.common.RecursoEmUsoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import br.com.crudao.kanban.workflow.dto.CriarEtapaRequest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Etapas do workflow (RF-010) e o invariante estrutural RN-003. */
@Slf4j
@Service
@RequiredArgsConstructor
public class EtapaService {

  private final EtapaRepository etapaRepository;
  private final TransicaoRepository transicaoRepository;
  private final WorkflowRepository workflowRepository;
  private final TarefaRepository tarefaRepository;
  private final EventoBoardPublisher eventoBoardPublisher;

  /** Nova etapa entra ao final da ordem. No maximo uma etapa final por workflow (RN-004). */
  @Transactional
  public Etapa criar(UUID workflowId, CriarEtapaRequest request) {
    if (request.etapaFinal()
        && etapaRepository.findByWorkflowIdAndEtapaFinalTrue(workflowId).isPresent()) {
      throw new ConfiguracaoWorkflowInvalidaException(
          "O workflow ja possui uma etapa final. Remova a marcacao da etapa atual antes.");
    }
    Etapa etapa =
        etapaRepository.save(
            Etapa.builder()
                .workflowId(workflowId)
                .nome(request.nome())
                .ordem(etapaRepository.findMaiorOrdem(workflowId) + 1)
                .etapaFinal(request.etapaFinal())
                .build());
    publicarReconfiguracao(workflowId);
    return etapa;
  }

  @Transactional
  public Etapa renomear(UUID etapaId, String nome) {
    Etapa etapa = buscar(etapaId);
    etapa.setNome(nome);
    publicarReconfiguracao(etapa.getWorkflowId());
    return etapa;
  }

  /**
   * Reescreve apenas o campo {@code ordem} na sequencia informada. <b>Nao toca em transicoes</b>:
   * ordem e apresentacao, o grafo e independente.
   */
  @Transactional
  public void reordenar(UUID workflowId, List<UUID> ordemIds) {
    List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflowId);
    if (etapas.size() != ordemIds.size()
        || !etapas.stream().map(Etapa::getId).collect(java.util.stream.Collectors.toSet())
            .equals(new HashSet<>(ordemIds))) {
      throw new ConfiguracaoWorkflowInvalidaException(
          "A nova ordem deve conter exatamente todas as etapas do workflow.");
    }
    Map<UUID, Etapa> porId = new HashMap<>();
    etapas.forEach(etapa -> porId.put(etapa.getId(), etapa));

    // Desloca para uma faixa livre antes de reatribuir, evitando colisao com UNIQUE (workflow, ordem).
    int deslocamento = etapas.size() + 1;
    for (int i = 0; i < ordemIds.size(); i++) {
      porId.get(ordemIds.get(i)).setOrdem(deslocamento + i);
    }
    etapaRepository.saveAllAndFlush(etapas);
    for (int i = 0; i < ordemIds.size(); i++) {
      porId.get(ordemIds.get(i)).setOrdem(i);
    }
    etapaRepository.saveAllAndFlush(etapas);
    publicarReconfiguracao(workflowId);
  }

  /** RN-005 + limpeza de arestas incidentes, seguida de revalidacao do grafo. */
  @Transactional
  public void excluir(UUID etapaId) {
    Etapa etapa = buscar(etapaId);
    long ativas = tarefaRepository.contarAtivasNaEtapa(etapaId);
    if (ativas > 0) {
      throw new RecursoEmUsoException(
          "A etapa possui %d tarefa(s) ativa(s) e nao pode ser excluida.".formatted(ativas));
    }
    if (tarefaRepository.countByEtapaId(etapaId) > 0) {
      throw new RecursoEmUsoException(
          "A etapa possui tarefas historicas vinculadas e nao pode ser excluida.");
    }
    transicaoRepository.deleteByEtapaOrigemIdOrEtapaDestinoId(etapaId, etapaId);
    etapaRepository.delete(etapa);
    etapaRepository.flush();
    validarWorkflow(etapa.getWorkflowId());
    publicarReconfiguracao(etapa.getWorkflowId());
  }

  /**
   * Invariante RN-003/RN-004, revalidado apos <b>toda</b> mutacao de workflow, etapa ou transicao:
   *
   * <ul>
   *   <li>existe pelo menos uma etapa;
   *   <li>existe exatamente uma etapa final;
   *   <li>toda etapa nao-final possui ao menos uma transicao de saida;
   *   <li>nenhuma etapa e inalcancavel a partir da etapa de menor ordem.
   * </ul>
   */
  @Transactional(readOnly = true)
  public void validarWorkflow(UUID workflowId) {
    List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflowId);
    if (etapas.isEmpty()) {
      throw new ConfiguracaoWorkflowInvalidaException("O workflow deve possuir ao menos uma etapa.");
    }
    List<Etapa> finais = etapas.stream().filter(Etapa::isEtapaFinal).toList();
    if (finais.size() != 1) {
      throw new ConfiguracaoWorkflowInvalidaException(
          "O workflow deve possuir exatamente uma etapa final.");
    }

    List<Transicao> transicoes = transicaoRepository.findByWorkflowId(workflowId);
    Map<UUID, List<UUID>> saidas = new HashMap<>();
    for (Transicao transicao : transicoes) {
      saidas.computeIfAbsent(transicao.getEtapaOrigemId(), k -> new ArrayList<>())
          .add(transicao.getEtapaDestinoId());
    }

    for (Etapa etapa : etapas) {
      if (!etapa.isEtapaFinal() && saidas.getOrDefault(etapa.getId(), List.of()).isEmpty()) {
        throw new ConfiguracaoWorkflowInvalidaException(
            "A etapa \"%s\" nao possui nenhuma transicao de saida configurada."
                .formatted(etapa.getNome()));
      }
    }

    Set<UUID> alcancaveis = alcancaveisDe(etapas.get(0).getId(), saidas);
    for (Etapa etapa : etapas) {
      if (!alcancaveis.contains(etapa.getId())) {
        throw new ConfiguracaoWorkflowInvalidaException(
            "A etapa \"%s\" e inalcancavel a partir da primeira etapa do workflow."
                .formatted(etapa.getNome()));
      }
    }
  }

  @Transactional(readOnly = true)
  public List<Etapa> listar(UUID workflowId) {
    return etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflowId);
  }

  @Transactional(readOnly = true)
  public Etapa buscar(UUID etapaId) {
    return etapaRepository
        .findById(etapaId)
        .orElseThrow(() -> RecursoNaoEncontradoException.de("Etapa", etapaId));
  }

  private Set<UUID> alcancaveisDe(UUID raiz, Map<UUID, List<UUID>> saidas) {
    Set<UUID> visitados = new HashSet<>();
    Deque<UUID> fila = new ArrayDeque<>();
    fila.add(raiz);
    visitados.add(raiz);
    while (!fila.isEmpty()) {
      for (UUID destino : saidas.getOrDefault(fila.poll(), List.of())) {
        if (visitados.add(destino)) {
          fila.add(destino);
        }
      }
    }
    return visitados;
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
