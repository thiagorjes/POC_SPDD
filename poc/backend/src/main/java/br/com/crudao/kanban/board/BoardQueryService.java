package br.com.crudao.kanban.board;

import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.SequenciaProjeto;
import br.com.crudao.kanban.evento.SequenciaProjetoRepository;
import br.com.crudao.kanban.projeto.ChaveToggle;
import br.com.crudao.kanban.projeto.Projeto;
import br.com.crudao.kanban.projeto.ProjetoRepository;
import br.com.crudao.kanban.raia.RaiaMapper;
import br.com.crudao.kanban.raia.RaiaRepository;
import br.com.crudao.kanban.rbac.Papeis;
import br.com.crudao.kanban.rbac.Permissoes;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.rbac.UsuarioRepository;
import br.com.crudao.kanban.security.PermissaoGuard;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.tarefa.TarefaMapper;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import br.com.crudao.kanban.tarefa.TarefaService;
import br.com.crudao.kanban.tarefa.dto.TarefaResumoResponse;
import br.com.crudao.kanban.workflow.Etapa;
import br.com.crudao.kanban.workflow.EtapaRepository;
import br.com.crudao.kanban.workflow.Transicao;
import br.com.crudao.kanban.workflow.TransicaoRepository;
import br.com.crudao.kanban.workflow.Workflow;
import br.com.crudao.kanban.workflow.WorkflowMapper;
import br.com.crudao.kanban.workflow.WorkflowRepository;
import br.com.crudao.kanban.common.PermissaoNegadaException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Snapshot do board (RF-001, RF-011) e base do resync (ADR-004).
 *
 * <p>O grafo de transicoes e carregado uma unica vez e indexado em memoria: calcular {@code
 * destinosPermitidos} por card via repositorio seria N+1 no caminho mais quente do sistema.
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
  private final UsuarioRepository usuarioRepository;
  private final SequenciaProjetoRepository sequenciaProjetoRepository;
  private final PermissaoGuard permissaoGuard;
  private final WorkflowMapper workflowMapper;
  private final RaiaMapper raiaMapper;
  private final TarefaMapper tarefaMapper;

  @Transactional(readOnly = true)
  public BoardSnapshotResponse snapshot(UUID projetoId) {
    permissaoGuard.exigir(Permissoes.PROJETO_VISUALIZAR, projetoId);
    exigirToggleGestor(projetoId);

    Projeto projeto =
        projetoRepository
            .findById(projetoId)
            .orElseThrow(() -> RecursoNaoEncontradoException.de("Projeto", projetoId));

    Workflow workflow =
        workflowRepository
            .findByProjetoIdAndAtivoTrue(projetoId)
            .orElseThrow(
                () ->
                    new ConfiguracaoWorkflowInvalidaException(
                        "O projeto nao possui workflow ativo configurado."));

    List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflow.getId());
    Map<UUID, Etapa> etapasPorId =
        etapas.stream().collect(Collectors.toMap(Etapa::getId, Function.identity()));

    Map<UUID, List<UUID>> saidas = new HashMap<>();
    Map<UUID, List<UUID>> entradas = new HashMap<>();
    for (Transicao transicao : transicaoRepository.findByWorkflowId(workflow.getId())) {
      saidas
          .computeIfAbsent(transicao.getEtapaOrigemId(), k -> new ArrayList<>())
          .add(transicao.getEtapaDestinoId());
      entradas
          .computeIfAbsent(transicao.getEtapaDestinoId(), k -> new ArrayList<>())
          .add(transicao.getEtapaOrigemId());
    }

    Set<String> permissoes = permissaoGuard.permissoesEfetivas(projetoId);
    boolean podeMover = permissoes.contains(Permissoes.TAREFA_MOVER);
    boolean podeFinalizar = permissoes.contains(Permissoes.TAREFA_FINALIZAR);

    List<Tarefa> tarefas = tarefaRepository.findByProjetoId(projetoId);
    Map<UUID, String> nomes = nomesDeResponsaveis(tarefas);

    List<TarefaResumoResponse> cards = new ArrayList<>(tarefas.size());
    for (Tarefa tarefa : tarefas) {
      cards.add(
          tarefaMapper.paraResumo(
              tarefa,
              // Card sem responsavel e o estado normal apos a criacao (RF-018) e
              // Map.of() nao aceita chave nula.
              tarefa.getResponsavelId() == null ? null : nomes.get(tarefa.getResponsavelId()),
              destinos(tarefa, etapasPorId, saidas, entradas, podeMover, podeFinalizar)));
    }

    long seq =
        sequenciaProjetoRepository
            .findById(projetoId)
            .map(SequenciaProjeto::getUltimoSeq)
            .orElse(0L);

    return new BoardSnapshotResponse(
        projetoId,
        workflow.getId(),
        seq,
        !projeto.isAtivo(),
        workflowMapper.paraEtapaResponse(etapas),
        raiaMapper.paraResponse(raiaRepository.findByProjetoIdOrderByOrdemAsc(projetoId)),
        cards,
        permissoes);
  }

  /**
   * Destinos validos do card: arestas do grafo filtradas pela permissao. Na etapa final o unico
   * caminho e voltar para uma predecessora direta (desfinalizar), e so com {@code tarefa:finalizar}.
   *
   * @see TarefaService#destinosPermitidos
   */
  private List<UUID> destinos(
      Tarefa tarefa,
      Map<UUID, Etapa> etapasPorId,
      Map<UUID, List<UUID>> saidas,
      Map<UUID, List<UUID>> entradas,
      boolean podeMover,
      boolean podeFinalizar) {
    if (!podeMover) {
      return List.of();
    }
    Etapa atual = etapasPorId.get(tarefa.getEtapaId());
    if (atual == null) {
      return List.of();
    }
    if (atual.isEtapaFinal()) {
      return podeFinalizar ? List.copyOf(entradas.getOrDefault(atual.getId(), List.of())) : List.of();
    }
    List<UUID> candidatos = saidas.getOrDefault(atual.getId(), List.of());
    if (podeFinalizar) {
      return List.copyOf(candidatos);
    }
    return candidatos.stream()
        .filter(id -> etapasPorId.get(id) != null && !etapasPorId.get(id).isEtapaFinal())
        .toList();
  }

  /** Gestor so ve o board se o projeto liberar o toggle (RN-013). */
  private void exigirToggleGestor(UUID projetoId) {
    Usuario usuario = permissaoGuard.usuarioAtual();
    if (usuario.isAdminGlobal()) {
      return;
    }
    Set<String> papeis = permissaoGuard.papeisNoProjeto(projetoId);
    boolean somenteGestor = papeis.size() == 1 && papeis.contains(Papeis.GESTOR);
    if (somenteGestor
        && !permissaoGuard.toggleHabilitado(projetoId, ChaveToggle.GESTOR_PODE_VER_BOARD)) {
      throw new PermissaoNegadaException(
          "A visualizacao do board esta desabilitada para o seu papel na configuracao do projeto.");
    }
  }

  private Map<UUID, String> nomesDeResponsaveis(List<Tarefa> tarefas) {
    Set<UUID> ids =
        tarefas.stream()
            .map(Tarefa::getResponsavelId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    if (ids.isEmpty()) {
      return Map.of();
    }
    return usuarioRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(Usuario::getId, Usuario::getNome));
  }
}
