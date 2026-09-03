package br.com.crudao.kanban.tarefa;

import br.com.crudao.kanban.auditoria.AuditoriaService;
import br.com.crudao.kanban.auditoria.AuditoriaTarefa;
import br.com.crudao.kanban.auditoria.CampoAuditado;
import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.common.ConflitoConcorrenciaException;
import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.common.TarefaIniciadaException;
import br.com.crudao.kanban.common.TransicaoNaoPermitidaException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.leadtime.LeadTimeService;
import br.com.crudao.kanban.leadtime.PeriodoEtapaRepository;
import br.com.crudao.kanban.leadtime.PeriodoImpedimento;
import br.com.crudao.kanban.leadtime.PeriodoImpedimentoRepository;
import br.com.crudao.kanban.leadtime.dto.LeadTimeDetalhe;
import br.com.crudao.kanban.notificacao.NotificacaoService;
import br.com.crudao.kanban.notificacao.TipoNotificacao;
import br.com.crudao.kanban.projeto.ChaveToggle;
import br.com.crudao.kanban.raia.Raia;
import br.com.crudao.kanban.raia.RaiaRepository;
import br.com.crudao.kanban.rbac.Papeis;
import br.com.crudao.kanban.rbac.Permissoes;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.rbac.UsuarioRepository;
import br.com.crudao.kanban.security.PermissaoGuard;
import br.com.crudao.kanban.tarefa.dto.AtualizarTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.AuditoriaResponse;
import br.com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.MoverTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.TarefaDetalheResponse;
import br.com.crudao.kanban.workflow.Etapa;
import br.com.crudao.kanban.workflow.EtapaRepository;
import br.com.crudao.kanban.workflow.Transicao;
import br.com.crudao.kanban.workflow.TransicaoRepository;
import br.com.crudao.kanban.workflow.Workflow;
import br.com.crudao.kanban.workflow.WorkflowRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nucleo de regras da tarefa. Escrita, auditoria, intervalos de lead-time e incremento de {@code
 * seq} acontecem na mesma transacao; apenas a publicacao do evento sai dela ({@code afterCommit}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TarefaService {

  private final TarefaRepository tarefaRepository;
  private final TarefaObservadorRepository tarefaObservadorRepository;
  private final EtapaRepository etapaRepository;
  private final TransicaoRepository transicaoRepository;
  private final WorkflowRepository workflowRepository;
  private final RaiaRepository raiaRepository;
  private final PeriodoEtapaRepository periodoEtapaRepository;
  private final PeriodoImpedimentoRepository periodoImpedimentoRepository;
  private final LeadTimeService leadTimeService;
  private final AuditoriaService auditoriaService;
  private final NotificacaoService notificacaoService;
  private final EventoBoardPublisher eventoBoardPublisher;
  private final PermissaoGuard permissaoGuard;
  private final UsuarioRepository usuarioRepository;
  private final TarefaMapper tarefaMapper;

  /**
   * Cria a tarefa na etapa de menor ordem do workflow ativo e na raia padrao do projeto quando nao
   * informadas (RN-CB-004/005). Nao gera auditoria: RN-016 cobre alteracoes, nao a criacao.
   *
   * @see CriarTarefaRequest
   */
  @Transactional
  public Tarefa criar(UUID projetoId, CriarTarefaRequest request, Usuario autor) {
    Workflow workflow =
        workflowRepository
            .findByProjetoIdAndAtivoTrue(projetoId)
            .orElseThrow(
                () ->
                    new ConfiguracaoWorkflowInvalidaException(
                        "O projeto nao possui workflow ativo configurado."));

    Etapa etapaInicial =
        etapaRepository
            .findFirstByWorkflowIdOrderByOrdemAsc(workflow.getId())
            .orElseThrow(
                () ->
                    new ConfiguracaoWorkflowInvalidaException(
                        "O workflow ativo nao possui nenhuma etapa configurada."));

    Raia raia = resolverRaia(projetoId, request.raiaId());
    UUID responsavelId = resolverResponsavelNaCriacao(projetoId, request.responsavelId(), autor);

    Tarefa tarefa =
        tarefaRepository.save(
            Tarefa.builder()
                .projetoId(projetoId)
                .workflowId(workflow.getId())
                .etapaId(etapaInicial.getId())
                .raiaId(raia.getId())
                .responsavelId(responsavelId)
                .criadorId(autor.getId())
                .titulo(request.titulo())
                .descricao(request.descricao())
                .tipo(request.tipo())
                .prioridade(request.prioridadeOuPadrao())
                .iniciada(false)
                .impedida(false)
                .build());

    leadTimeService.abrirPeriodoEtapa(tarefa, etapaInicial.getId());

    registrarObservador(tarefa.getId(), autor.getId(), OrigemObservacao.CRIADOR);
    if (responsavelId != null && !responsavelId.equals(autor.getId())) {
      registrarObservador(tarefa.getId(), responsavelId, OrigemObservacao.RESPONSAVEL);
    }

    eventoBoardPublisher.publicar(
        EventoBoard.de(
            projetoId,
            TipoEventoBoard.TAREFA_CRIADA,
            tarefa.getId(),
            etapaInicial.getId(),
            raia.getId()));
    log.info("Tarefa {} criada no projeto {}", tarefa.getId(), projetoId);
    return tarefa;
  }

  /**
   * Movimentacao validada contra a etapa de origem <b>relida na transacao</b>, nunca contra a etapa
   * que o cliente acredita ser a atual (RN-001/RN-004/RN-011).
   *
   * <p>Desfinalizar e o unico caminho que nao usa uma aresta direta: o destino valido e qualquer
   * predecessora direta da etapa final, o que impede que o retorno contorne o grafo.
   */
  @Transactional
  public Tarefa mover(UUID tarefaId, MoverTarefaRequest request, Usuario autor) {
    Tarefa tarefa = buscar(tarefaId);
    verificarVersao(tarefa, request.versaoEsperada());

    Etapa origem = etapa(tarefa.getEtapaId());
    Etapa destino = etapa(request.etapaDestinoId());

    if (!destino.getWorkflowId().equals(tarefa.getWorkflowId())) {
      throw new TransicaoNaoPermitidaException(
          "A etapa de destino nao pertence ao workflow da tarefa.");
    }
    if (origem.getId().equals(destino.getId()) && request.raiaDestinoId() == null) {
      return tarefa;
    }

    if (origem.isEtapaFinal() || destino.isEtapaFinal()) {
      exigirPermissaoFinalizar(tarefa.getProjetoId());
    }

    if (origem.isEtapaFinal()) {
      boolean predecessoraDireta =
          transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(destino.getId(), origem.getId());
      if (!predecessoraDireta) {
        throw new TransicaoNaoPermitidaException(
            "So e possivel desfinalizar retornando para uma etapa que leva diretamente a \"%s\"."
                .formatted(origem.getNome()));
      }
    } else if (!origem.getId().equals(destino.getId())
        && !transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(
            origem.getId(), destino.getId())) {
      throw TransicaoNaoPermitidaException.entre(origem.getNome(), destino.getNome());
    }

    boolean mudouEtapa = !origem.getId().equals(destino.getId());
    if (mudouEtapa) {
      leadTimeService.encerrarPeriodoEtapa(tarefa);
      tarefa.setEtapaId(destino.getId());
      tarefaRepository.saveAndFlush(tarefa);
      leadTimeService.abrirPeriodoEtapa(tarefa, destino.getId());

      if (tarefa.isImpedida()) {
        leadTimeService.reancorarImpedimento(tarefa, destino.getId());
      }

      // Sticky: sai da primeira etapa uma vez e nunca mais destrava (A-4).
      if (!tarefa.isIniciada() && ehPrimeiraEtapa(origem)) {
        tarefa.setIniciada(true);
      }
      auditoriaService.registrar(
          tarefa, CampoAuditado.ETAPA, origem.getNome(), destino.getNome(), autor);
    }

    if (request.raiaDestinoId() != null && !request.raiaDestinoId().equals(tarefa.getRaiaId())) {
      // Movimento entre raias e livre: raia nao participa do grafo de transicoes.
      tarefa.setRaiaId(resolverRaia(tarefa.getProjetoId(), request.raiaDestinoId()).getId());
    }

    if (mudouEtapa) {
      notificacaoService.notificarObservadores(tarefa, TipoNotificacao.ETAPA_ALTERADA, autor);
    }
    eventoBoardPublisher.publicar(
        EventoBoard.de(
            tarefa.getProjetoId(),
            TipoEventoBoard.TAREFA_MOVIDA,
            tarefa.getId(),
            tarefa.getEtapaId(),
            tarefa.getRaiaId()));
    return tarefa;
  }

  /**
   * Com {@code iniciada == true}, {@code descricao} e {@code tipo} sao imutaveis salvo o toggle
   * {@code DEV_PODE_EDITAR_TAREFA_INICIADA} ou papel administrativo (RF-003, A-3).
   *
   * <p>{@code titulo} permanece editavel: RN-016 exige audita-lo, o que so faz sentido se ele puder
   * mudar.
   */
  @Transactional
  public Tarefa atualizar(UUID tarefaId, AtualizarTarefaRequest request, Usuario autor) {
    Tarefa tarefa = buscar(tarefaId);
    verificarVersao(tarefa, request.versaoEsperada());

    boolean alterouEstrutural =
        !Objects.equals(tarefa.getDescricao(), request.descricao())
            || tarefa.getTipo() != request.tipo();

    if (tarefa.isIniciada() && alterouEstrutural && !podeEditarIniciada(tarefa.getProjetoId())) {
      throw new TarefaIniciadaException(
          "A tarefa ja foi iniciada: descricao e tipo nao podem mais ser alterados.");
    }

    String tituloAnterior = tarefa.getTitulo();
    tarefa.setTitulo(request.titulo());
    tarefa.setDescricao(request.descricao());
    tarefa.setTipo(request.tipo());
    tarefa.setPrioridade(request.prioridade());
    if (request.raiaId() != null) {
      tarefa.setRaiaId(resolverRaia(tarefa.getProjetoId(), request.raiaId()).getId());
    }

    auditoriaService.registrar(
        tarefa, CampoAuditado.TITULO, tituloAnterior, request.titulo(), autor);

    eventoBoardPublisher.publicar(
        EventoBoard.de(
            tarefa.getProjetoId(),
            TipoEventoBoard.TAREFA_ATUALIZADA,
            tarefa.getId(),
            tarefa.getEtapaId(),
            tarefa.getRaiaId()));
    return tarefa;
  }

  /**
   * Autoatribuicao e livre para quem tem {@code tarefa:mover}, <b>mesmo que a tarefa ja esteja
   * atribuida a outra pessoa</b>; atribuir a terceiros exige {@code tarefa:atribuir} (RN-012).
   */
  @Transactional
  public Tarefa atribuir(UUID tarefaId, UUID novoResponsavelId, Usuario autor) {
    Tarefa tarefa = buscar(tarefaId);
    boolean autoatribuicao = autor.getId().equals(novoResponsavelId);

    if (autoatribuicao) {
      permissaoGuard.exigir(Permissoes.TAREFA_MOVER, tarefa.getProjetoId());
    } else {
      permissaoGuard.exigir(Permissoes.TAREFA_ATRIBUIR, tarefa.getProjetoId());
    }
    permissaoGuard.exigirProjetoAtivo(tarefa.getProjetoId());

    UUID anterior = tarefa.getResponsavelId();
    if (Objects.equals(anterior, novoResponsavelId)) {
      return tarefa;
    }
    tarefa.setResponsavelId(novoResponsavelId);

    if (novoResponsavelId != null) {
      registrarObservador(tarefa.getId(), novoResponsavelId, OrigemObservacao.RESPONSAVEL);
    }
    auditoriaService.registrar(
        tarefa,
        CampoAuditado.RESPONSAVEL,
        anterior != null ? anterior.toString() : null,
        novoResponsavelId != null ? novoResponsavelId.toString() : null,
        autor);

    eventoBoardPublisher.publicar(
        EventoBoard.de(
            tarefa.getProjetoId(),
            TipoEventoBoard.TAREFA_ATUALIZADA,
            tarefa.getId(),
            tarefa.getEtapaId(),
            tarefa.getRaiaId()));
    return tarefa;
  }

  /**
   * Marcar e idempotente: marcar tarefa ja impedida nao abre um segundo periodo. Impedimento
   * <b>nao</b> bloqueia nem libera movimentacao (DDR-002).
   */
  @Transactional
  public Tarefa marcarImpedimento(UUID tarefaId, String motivo, Usuario autor) {
    Tarefa tarefa = buscar(tarefaId);
    if (tarefa.isImpedida()) {
      return tarefa;
    }
    leadTimeService.abrirImpedimento(tarefa, motivo);
    tarefa.setImpedida(true);

    auditoriaService.registrar(
        tarefa, CampoAuditado.IMPEDIMENTO, "false", "true", autor);
    notificacaoService.notificarObservadores(tarefa, TipoNotificacao.IMPEDIMENTO_MARCADO, autor);
    eventoBoardPublisher.publicar(
        EventoBoard.de(
            tarefa.getProjetoId(),
            TipoEventoBoard.TAREFA_IMPEDIDA,
            tarefa.getId(),
            tarefa.getEtapaId(),
            tarefa.getRaiaId()));
    return tarefa;
  }

  @Transactional
  public Tarefa desmarcarImpedimento(UUID tarefaId, Usuario autor) {
    Tarefa tarefa = buscar(tarefaId);
    if (!tarefa.isImpedida()) {
      return tarefa;
    }
    leadTimeService.encerrarImpedimento(tarefa);
    tarefa.setImpedida(false);

    auditoriaService.registrar(tarefa, CampoAuditado.IMPEDIMENTO, "true", "false", autor);
    notificacaoService.notificarObservadores(tarefa, TipoNotificacao.IMPEDIMENTO_DESMARCADO, autor);
    eventoBoardPublisher.publicar(
        EventoBoard.de(
            tarefa.getProjetoId(),
            TipoEventoBoard.TAREFA_DESIMPEDIDA,
            tarefa.getId(),
            tarefa.getEtapaId(),
            tarefa.getRaiaId()));
    return tarefa;
  }

  /**
   * Fecha os periodos abertos antes de excluir, para nao contaminar as medias com intervalo aberto
   * orfao. A auditoria e preservada (append-only) — por isso ela nao tem FK para a tarefa.
   */
  @Transactional
  public void excluir(UUID tarefaId, Usuario autor) {
    Tarefa tarefa = buscar(tarefaId);
    permissaoGuard.exigirToggle(
        ChaveToggle.DEV_PODE_EXCLUIR_TAREFA, tarefa.getProjetoId(), Papeis.DEV);

    leadTimeService.encerrarTudo(tarefa);
    tarefaRepository.saveAndFlush(tarefa);

    notificacaoService.removerDaTarefa(tarefaId);
    tarefaObservadorRepository.deleteByIdTarefaId(tarefaId);
    periodoEtapaRepository.deleteByTarefaId(tarefaId);
    periodoImpedimentoRepository.deleteByTarefaId(tarefaId);
    tarefaRepository.delete(tarefa);

    eventoBoardPublisher.publicar(
        EventoBoard.de(tarefa.getProjetoId(), TipoEventoBoard.TAREFA_EXCLUIDA, tarefaId));
    log.info("Tarefa {} excluida por {}", tarefaId, autor.getId());
  }

  /** Observacao explicita (RF-005, A-5). Observadores implicitos nao sao removiveis por aqui. */
  @Transactional
  public void observar(UUID tarefaId, Usuario usuario) {
    buscar(tarefaId);
    registrarObservador(tarefaId, usuario.getId(), OrigemObservacao.EXPLICITO);
  }

  @Transactional
  public void desobservar(UUID tarefaId, Usuario usuario) {
    buscar(tarefaId);
    tarefaObservadorRepository
        .findByIdTarefaIdAndIdUsuarioId(tarefaId, usuario.getId())
        .filter(observador -> observador.getOrigem() == OrigemObservacao.EXPLICITO)
        .ifPresent(tarefaObservadorRepository::delete);
  }

  /** Detalhe da TL-04: lead-time por etapa, historico de auditoria e acoes disponiveis (RF-006). */
  @Transactional(readOnly = true)
  public TarefaDetalheResponse detalhar(UUID tarefaId, Usuario usuario, Pageable paginaHistorico) {
    Tarefa tarefa = buscar(tarefaId);
    permissaoGuard.exigir(Permissoes.PROJETO_VISUALIZAR, tarefa.getProjetoId());

    Set<String> permissoes = permissaoGuard.permissoesEfetivas(tarefa.getProjetoId());
    LeadTimeDetalhe leadTime = leadTimeService.calcularDetalhe(tarefaId, tarefa.getWorkflowId());

    List<AuditoriaTarefa> registros =
        auditoriaService.historico(tarefaId, paginaHistorico).getContent();
    Map<UUID, String> nomes = nomes(registros, tarefa.getResponsavelId());
    List<AuditoriaResponse> historico =
        registros.stream()
            .map(registro -> tarefaMapper.paraAuditoria(registro, nomeDe(nomes, registro.getAutorId())))
            .toList();

    String motivo =
        periodoImpedimentoRepository
            .findByTarefaIdAndEncerradoEmIsNull(tarefaId)
            .map(PeriodoImpedimento::getMotivo)
            .orElse(null);

    boolean observando =
        tarefaObservadorRepository
            .findByIdTarefaIdAndIdUsuarioId(tarefaId, usuario.getId())
            .isPresent();

    return tarefaMapper.paraDetalhe(
        tarefa,
        nomeDe(nomes, tarefa.getResponsavelId()),
        motivo,
        leadTime.porEtapa(),
        leadTime.impedimentoTotalSegundos(),
        historico,
        destinosPermitidos(tarefa, permissoes),
        permissoes,
        observando);
  }

  @Transactional(readOnly = true)
  public Tarefa buscar(UUID tarefaId) {
    return tarefaRepository
        .findById(tarefaId)
        .orElseThrow(() -> RecursoNaoEncontradoException.de("Tarefa", tarefaId));
  }

  /**
   * Destinos validos para o card: arestas do grafo filtradas pela permissao do usuario. Alimenta o
   * destaque de colunas durante o drag sem round-trip extra (DDR-002).
   */
  @Transactional(readOnly = true)
  public List<UUID> destinosPermitidos(Tarefa tarefa, Set<String> permissoes) {
    if (!permissoes.contains(Permissoes.TAREFA_MOVER)) {
      return List.of();
    }
    Etapa atual = etapa(tarefa.getEtapaId());
    boolean podeFinalizar = permissoes.contains(Permissoes.TAREFA_FINALIZAR);

    if (atual.isEtapaFinal()) {
      if (!podeFinalizar) {
        return List.of();
      }
      return transicaoRepository.findByEtapaDestinoId(atual.getId()).stream()
          .map(Transicao::getEtapaOrigemId)
          .toList();
    }

    List<UUID> destinos =
        transicaoRepository.findByEtapaOrigemId(atual.getId()).stream()
            .map(Transicao::getEtapaDestinoId)
            .toList();
    if (podeFinalizar) {
      return destinos;
    }
    return destinos.stream().filter(id -> !etapa(id).isEtapaFinal()).toList();
  }

  private void exigirPermissaoFinalizar(UUID projetoId) {
    if (permissaoGuard.possui(Permissoes.TAREFA_FINALIZAR, projetoId)) {
      return;
    }
    // dev so finaliza se o projeto liberar o toggle (RN-011).
    Set<String> papeis = permissaoGuard.papeisNoProjeto(projetoId);
    if (papeis.contains(Papeis.DEV)
        && permissaoGuard.toggleHabilitado(projetoId, ChaveToggle.DEV_PODE_FINALIZAR_TAREFA)) {
      return;
    }
    throw new PermissaoNegadaException(
        "Voce nao tem permissao para finalizar ou desfinalizar tarefas neste projeto.");
  }

  private boolean podeEditarIniciada(UUID projetoId) {
    if (permissaoGuard.usuarioAtual().isAdminGlobal()) {
      return true;
    }
    Set<String> papeis = permissaoGuard.papeisNoProjeto(projetoId);
    if (papeis.stream().anyMatch(Papeis.ADMINISTRATIVOS::contains)) {
      return true;
    }
    return permissaoGuard.toggleHabilitado(projetoId, ChaveToggle.DEV_PODE_EDITAR_TAREFA_INICIADA);
  }

  private boolean ehPrimeiraEtapa(Etapa etapa) {
    return etapaRepository
        .findFirstByWorkflowIdOrderByOrdemAsc(etapa.getWorkflowId())
        .map(primeira -> primeira.getId().equals(etapa.getId()))
        .orElse(false);
  }

  private void verificarVersao(Tarefa tarefa, Long versaoEsperada) {
    if (versaoEsperada == null || tarefa.getVersao() != versaoEsperada) {
      throw ConflitoConcorrenciaException.padrao();
    }
  }

  private Raia resolverRaia(UUID projetoId, UUID raiaId) {
    if (raiaId == null) {
      return raiaRepository
          .findByProjetoIdAndPadraoTrue(projetoId)
          .orElseThrow(
              () ->
                  new ConfiguracaoWorkflowInvalidaException(
                      "O projeto nao possui raia padrao configurada."));
    }
    Raia raia =
        raiaRepository
            .findById(raiaId)
            .orElseThrow(() -> RecursoNaoEncontradoException.de("Raia", raiaId));
    if (!raia.getProjetoId().equals(projetoId)) {
      throw RecursoNaoEncontradoException.de("Raia", raiaId);
    }
    return raia;
  }

  private UUID resolverResponsavelNaCriacao(UUID projetoId, UUID responsavelId, Usuario autor) {
    if (responsavelId == null) {
      return null;
    }
    if (!responsavelId.equals(autor.getId())) {
      permissaoGuard.exigir(Permissoes.TAREFA_ATRIBUIR, projetoId);
    }
    return responsavelId;
  }

  /**
   * Tarefa sem responsavel e o estado normal logo apos a criacao (RF-018), e {@code Map.of()} nao
   * aceita chave nula — a consulta precisa tolerar id ausente.
   */
  private String nomeDe(Map<UUID, String> nomes, UUID id) {
    return id == null ? null : nomes.get(id);
  }

  private Map<UUID, String> nomes(List<AuditoriaTarefa> registros, UUID responsavelId) {
    Set<UUID> ids =
        registros.stream().map(AuditoriaTarefa::getAutorId).collect(Collectors.toCollection(HashSet::new));
    if (responsavelId != null) {
      ids.add(responsavelId);
    }
    if (ids.isEmpty()) {
      return Map.of();
    }
    return usuarioRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(Usuario::getId, Usuario::getNome));
  }

  private Etapa etapa(UUID etapaId) {
    return etapaRepository
        .findById(etapaId)
        .orElseThrow(() -> RecursoNaoEncontradoException.de("Etapa", etapaId));
  }

  private void registrarObservador(UUID tarefaId, UUID usuarioId, OrigemObservacao origem) {
    if (tarefaObservadorRepository.findByIdTarefaIdAndIdUsuarioId(tarefaId, usuarioId).isEmpty()) {
      tarefaObservadorRepository.save(new TarefaObservador(tarefaId, usuarioId, origem));
    }
  }
}
