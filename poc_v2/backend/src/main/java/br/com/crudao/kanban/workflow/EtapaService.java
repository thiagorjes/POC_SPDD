package br.com.crudao.kanban.workflow;

import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.common.RecursoEmUsoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.rbac.CodigoPermissao;
import br.com.crudao.kanban.security.ExigePermissao;
import br.com.crudao.kanban.security.OrigemEscopo;
import br.com.crudao.kanban.tarefa.TarefaRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Colunas do board. A ordem e apresentacao; o grafo de transicoes e independente dela (RF-010). */
@Service
@RequiredArgsConstructor
public class EtapaService {

  private final EtapaRepository etapaRepository;
  private final TransicaoRepository transicaoRepository;
  private final TarefaRepository tarefaRepository;
  private final WorkflowService workflowService;

  /** Cria a etapa ao final da ordem corrente. No maximo uma etapa final por workflow (RN-004). */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.WORKFLOW_GERENCIAR,
      escopoProjeto = "#workflowId",
      origem = OrigemEscopo.WORKFLOW)
  public Etapa criar(UUID workflowId, String nome, boolean etapaFinal) {
    Workflow workflow = workflowService.buscarEntidade(workflowId);
    if (etapaFinal && etapaRepository.findByWorkflowIdAndEtapaFinalTrue(workflowId).isPresent()) {
      throw new ConfiguracaoWorkflowInvalidaException(
          "O workflow ja possui uma etapa final. Cada workflow admite exatamente uma.");
    }
    int ordem = etapaRepository.buscarMaiorOrdem(workflowId) + 1;
    Etapa etapa = etapaRepository.save(new Etapa(workflowId, nome, ordem, etapaFinal));
    workflowService.publicarReconfiguracao(workflow.getProjetoId());
    return etapa;
  }

  /** Renomeia a etapa. Nao altera ordem nem grafo. */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.WORKFLOW_GERENCIAR,
      escopoProjeto = "#etapaId",
      origem = OrigemEscopo.ETAPA)
  public Etapa atualizar(UUID etapaId, String nome) {
    Etapa etapa = buscarEntidade(etapaId);
    etapa.setNome(nome);
    workflowService.publicarReconfiguracao(projetoDe(etapa));
    return etapa;
  }

  /**
   * Reescreve a ordem das colunas na sequencia informada. Reordenar coluna nunca invalida
   * transicoes (Safeguards, secao 1).
   */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.WORKFLOW_GERENCIAR,
      escopoProjeto = "#workflowId",
      origem = OrigemEscopo.WORKFLOW)
  public void reordenar(UUID workflowId, List<UUID> ordemIds) {
    List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflowId);
    if (etapas.size() != ordemIds.size()
        || !etapas.stream().map(Etapa::getId).allMatch(ordemIds::contains)) {
      throw new ConfiguracaoWorkflowInvalidaException(
          "A ordem informada deve conter exatamente as etapas do workflow.");
    }
    Map<UUID, Etapa> porId = new HashMap<>();
    etapas.forEach(etapa -> porId.put(etapa.getId(), etapa));

    // Desloca para uma faixa livre antes de reatribuir: a unicidade (workflow, ordem) e imediata.
    int deslocamento = etapas.size() + 1;
    for (Etapa etapa : etapas) {
      etapa.setOrdem(etapa.getOrdem() + deslocamento);
    }
    etapaRepository.flush();
    for (int i = 0; i < ordemIds.size(); i++) {
      porId.get(ordemIds.get(i)).setOrdem(i);
    }
    etapaRepository.flush();
    workflowService.publicarReconfiguracao(
        workflowService.buscarEntidade(workflowId).getProjetoId());
  }

  /** RN-005: bloqueia enquanto houver tarefa ativa na etapa. Revalida RN-003 apos a remocao. */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.WORKFLOW_GERENCIAR,
      escopoProjeto = "#etapaId",
      origem = OrigemEscopo.ETAPA)
  public void excluir(UUID etapaId) {
    Etapa etapa = buscarEntidade(etapaId);
    if (tarefaRepository.contarAtivasPorEtapa(etapaId) > 0) {
      throw new RecursoEmUsoException("A etapa possui tarefas ativas e nao pode ser excluida.");
    }
    transicaoRepository.deleteByEtapaOrigemIdOrEtapaDestinoId(etapaId, etapaId);
    etapaRepository.delete(etapa);
    etapaRepository.flush();
    validarWorkflow(etapa.getWorkflowId());
    workflowService.publicarReconfiguracao(projetoDe(etapa));
  }

  /**
   * Invariante RN-003, revalidado apos toda mutacao de workflow, etapa ou transicao: existe uma
   * unica etapa final; toda etapa nao-final tem ao menos uma saida; nenhuma etapa e inalcancavel a
   * partir da etapa de menor ordem.
   */
  @Transactional(readOnly = true)
  public void validarWorkflow(UUID workflowId) {
    List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflowId);
    if (etapas.isEmpty()) {
      return;
    }
    List<Etapa> finais = etapas.stream().filter(Etapa::isEtapaFinal).toList();
    if (finais.size() != 1) {
      throw new ConfiguracaoWorkflowInvalidaException(
          "O workflow deve possuir exatamente uma etapa final.");
    }
    List<Transicao> transicoes = transicaoRepository.findByWorkflowId(workflowId);
    Map<UUID, List<UUID>> saidas = new HashMap<>();
    for (Transicao transicao : transicoes) {
      saidas
          .computeIfAbsent(transicao.getEtapaOrigemId(), chave -> new ArrayList<>())
          .add(transicao.getEtapaDestinoId());
    }
    for (Etapa etapa : etapas) {
      if (!etapa.isEtapaFinal() && saidas.getOrDefault(etapa.getId(), List.of()).isEmpty()) {
        throw new ConfiguracaoWorkflowInvalidaException(
            "A etapa \"" + etapa.getNome() + "\" nao possui nenhuma transicao de saida.");
      }
    }
    Set<UUID> alcancaveis = alcancaveis(etapas.get(0).getId(), saidas);
    for (Etapa etapa : etapas) {
      if (!alcancaveis.contains(etapa.getId())) {
        throw new ConfiguracaoWorkflowInvalidaException(
            "A etapa \"" + etapa.getNome() + "\" e inalcancavel a partir da primeira etapa.");
      }
    }
  }

  private Set<UUID> alcancaveis(UUID inicio, Map<UUID, List<UUID>> saidas) {
    Set<UUID> visitados = new HashSet<>();
    Deque<UUID> pilha = new ArrayDeque<>();
    pilha.push(inicio);
    while (!pilha.isEmpty()) {
      UUID atual = pilha.pop();
      if (!visitados.add(atual)) {
        continue;
      }
      saidas.getOrDefault(atual, List.of()).forEach(pilha::push);
    }
    return visitados;
  }

  /** Etapas ordenadas do workflow. */
  @Transactional(readOnly = true)
  public List<Etapa> listar(UUID workflowId) {
    return etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflowId);
  }

  Etapa buscarEntidade(UUID etapaId) {
    return etapaRepository
        .findById(etapaId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Etapa nao encontrada."));
  }

  private UUID projetoDe(Etapa etapa) {
    return workflowService.buscarEntidade(etapa.getWorkflowId()).getProjetoId();
  }
}
