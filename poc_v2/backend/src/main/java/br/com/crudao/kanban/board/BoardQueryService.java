package br.com.crudao.kanban.board;

import br.com.crudao.kanban.board.dto.BoardSnapshotResponse;
import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.SequenciaProjetoRepository;
import br.com.crudao.kanban.projeto.ChaveToggle;
import br.com.crudao.kanban.projeto.Projeto;
import br.com.crudao.kanban.projeto.ProjetoRepository;
import br.com.crudao.kanban.raia.Raia;
import br.com.crudao.kanban.raia.RaiaMapper;
import br.com.crudao.kanban.raia.RaiaRepository;
import br.com.crudao.kanban.rbac.CodigoPapel;
import br.com.crudao.kanban.rbac.CodigoPermissao;
import br.com.crudao.kanban.security.PermissaoGuard;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.tarefa.TarefaMapper;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import br.com.crudao.kanban.tarefa.dto.TarefaResumoResponse;
import br.com.crudao.kanban.workflow.Etapa;
import br.com.crudao.kanban.workflow.EtapaRepository;
import br.com.crudao.kanban.workflow.Transicao;
import br.com.crudao.kanban.workflow.TransicaoRepository;
import br.com.crudao.kanban.workflow.Workflow;
import br.com.crudao.kanban.workflow.WorkflowMapper;
import br.com.crudao.kanban.workflow.WorkflowRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leitura do estado do board (RF-001, RF-011) e base do resync do stream (ADR-004). Carrega etapas,
 * raias, tarefas e transicoes em consultas por colecao (sem N+1) e devolve, por tarefa, os destinos
 * ja filtrados por grafo e por permissao — o drag destaca colunas sem round-trip extra (DDR-002).
 */
@Service
@RequiredArgsConstructor
public class BoardQueryService {

  private final ProjetoRepository projetoRepository;
  private final WorkflowRepository workflowRepository;
  private final EtapaRepository etapaRepository;
  private final TransicaoRepository transicaoRepository;
  private final RaiaRepository raiaRepository;
  private final TarefaRepository tarefaRepository;
  private final SequenciaProjetoRepository sequenciaProjetoRepository;
  private final WorkflowMapper workflowMapper;
  private final RaiaMapper raiaMapper;
  private final TarefaMapper tarefaMapper;
  private final PermissaoGuard permissaoGuard;

  /** Snapshot consistente do board na mesma transacao que le o {@code seq} do projeto. */
  @Transactional(readOnly = true)
  public BoardSnapshotResponse snapshot(UUID projetoId) {
    permissaoGuard.exigir(CodigoPermissao.PROJETO_VISUALIZAR, projetoId);
    // Gestor puro so ve o board com o toggle habilitado (RF-016).
    permissaoGuard.exigirToggle(ChaveToggle.GESTOR_PODE_VER_BOARD, projetoId, CodigoPapel.GESTOR);

    Projeto projeto =
        projetoRepository
            .findById(projetoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto nao encontrado."));
    Workflow workflow =
        workflowRepository
            .findByProjetoIdAndAtivoTrue(projetoId)
            .orElseThrow(
                () ->
                    new ConfiguracaoWorkflowInvalidaException(
                        "O projeto nao possui um workflow ativo configurado."));

    List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflow.getId());
    List<Raia> raias = raiaRepository.findByProjetoIdOrderByOrdemAsc(projetoId);
    List<Tarefa> tarefas = tarefaRepository.findByProjetoId(projetoId);
    List<Transicao> transicoes = transicaoRepository.findByWorkflowId(workflow.getId());

    Map<UUID, List<UUID>> destinosPorEtapa =
        projeto.ativo() ? destinosPorEtapa(projetoId, etapas, transicoes) : Map.of();
    List<TarefaResumoResponse> resumos = new ArrayList<>(tarefas.size());
    for (Tarefa tarefa : tarefas) {
      resumos.add(
          tarefaMapper.paraResumo(
              tarefa, destinosPorEtapa.getOrDefault(tarefa.getEtapaId(), List.of())));
    }

    long seq =
        sequenciaProjetoRepository
            .findById(projetoId)
            .map(sequencia -> sequencia.getUltimoSeq())
            .orElse(0L);

    return new BoardSnapshotResponse(
        projetoId,
        workflow.getId(),
        seq,
        !projeto.ativo(),
        workflowMapper.paraEtapaResponses(etapas),
        raiaMapper.paraResponses(raias),
        resumos);
  }

  /**
   * Destinos validos por etapa de origem, ja cruzados com a permissao do usuario. Projeto
   * finalizado nao oferece destino algum: RN-015 nao admite bypass.
   */
  private Map<UUID, List<UUID>> destinosPorEtapa(
      UUID projetoId, List<Etapa> etapas, List<Transicao> transicoes) {
    Map<UUID, List<UUID>> destinos = new HashMap<>();
    if (!permissaoGuard.possui(CodigoPermissao.TAREFA_MOVER, projetoId)) {
      return destinos;
    }
    boolean podeFinalizar = permissaoGuard.possui(CodigoPermissao.TAREFA_FINALIZAR, projetoId);
    Set<UUID> etapasFinais = new HashSet<>();
    for (Etapa etapa : etapas) {
      if (etapa.isEtapaFinal()) {
        etapasFinais.add(etapa.getId());
      }
    }

    for (Transicao transicao : transicoes) {
      boolean destinoFinal = etapasFinais.contains(transicao.getEtapaDestinoId());
      if (destinoFinal && !podeFinalizar) {
        continue;
      }
      destinos
          .computeIfAbsent(transicao.getEtapaOrigemId(), chave -> new ArrayList<>())
          .add(transicao.getEtapaDestinoId());
      // Desfinalizar (RF-012): a partir da etapa final, volta-se para qualquer predecessora direta.
      if (destinoFinal && podeFinalizar) {
        destinos
            .computeIfAbsent(transicao.getEtapaDestinoId(), chave -> new ArrayList<>())
            .add(transicao.getEtapaOrigemId());
      }
    }
    return destinos;
  }
}
