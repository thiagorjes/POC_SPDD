package br.com.crudao.kanban.tarefa;

import br.com.crudao.kanban.auditoria.AuditoriaService;
import br.com.crudao.kanban.auditoria.CampoAuditado;
import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.common.ConflitoConcorrenciaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.common.TarefaIniciadaException;
import br.com.crudao.kanban.common.TransicaoNaoPermitidaException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.leadtime.LeadTimeService;
import br.com.crudao.kanban.leadtime.dto.DetalheLeadTime;
import br.com.crudao.kanban.notificacao.NotificacaoService;
import br.com.crudao.kanban.notificacao.TipoNotificacao;
import br.com.crudao.kanban.projeto.ChaveToggle;
import br.com.crudao.kanban.projeto.ProjetoToggleService;
import br.com.crudao.kanban.raia.Raia;
import br.com.crudao.kanban.raia.RaiaRepository;
import br.com.crudao.kanban.rbac.CodigoPapel;
import br.com.crudao.kanban.rbac.CodigoPermissao;
import br.com.crudao.kanban.rbac.Usuario;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nucleo de regras do card: transicao pelo grafo, trava pos-inicio, atribuicao, impedimento e
 * exclusao. Escrita, auditoria, intervalos de lead time e incremento de {@code seq} ocorrem na
 * mesma transacao; apenas a entrega do evento acontece apos o commit (ADR-004).
 */
@Service
@RequiredArgsConstructor
public class TarefaService {

  private final TarefaRepository tarefaRepository;
  private final TarefaObservadorRepository tarefaObservadorRepository;
  private final EtapaRepository etapaRepository;
  private final TransicaoRepository transicaoRepository;
  private final WorkflowRepository workflowRepository;
  private final RaiaRepository raiaRepository;
  private final ProjetoToggleService projetoToggleService;
  private final LeadTimeService leadTimeService;
  private final AuditoriaService auditoriaService;
  private final NotificacaoService notificacaoService;
  private final EventoBoardPublisher eventoBoardPublisher;
  private final TarefaMapper tarefaMapper;
  private final PermissaoGuard permissaoGuard;

  /**
   * Cria o card na etapa de menor ordem do workflow ativo e na raia padrao quando nao informada
   * (RF-018, RN-CB-003/004/005). Nao gera auditoria: RN-016 cobre apenas alteracoes posteriores.
   */
  @Transactional
  public Tarefa criar(UUID projetoId, CriarTarefaRequest request, Usuario autor) {
    permissaoGuard.exigir(CodigoPermissao.TAREFA_GERENCIAR, projetoId);
    permissaoGuard.exigirProjetoAtivo(projetoId);

    Workflow workflow =
        workflowRepository
            .findByProjetoIdAndAtivoTrue(projetoId)
            .orElseThrow(
                () ->
                    new ConfiguracaoWorkflowInvalidaException(
                        "O projeto nao possui um workflow ativo configurado."));
    Etapa etapaInicial = primeiraEtapa(workflow.getId());
    Raia raia = resolverRaia(projetoId, request.raiaId());
    UUID responsavelId = resolverResponsavelNaCriacao(projetoId, request.responsavelId(), autor);

    Tarefa tarefa =
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
            .prioridade(
                request.prioridade() == null ? PrioridadeTarefa.MEDIA : request.prioridade())
            .iniciada(false)
            .impedida(false)
            .build();
    Tarefa salva = tarefaRepository.save(tarefa);

    leadTimeService.abrirPeriodoEtapa(salva, etapaInicial.getId());
    registrarObservador(salva.getId(), autor.getId(), OrigemObservacao.CRIADOR);
    if (responsavelId != null) {
      registrarObservador(salva.getId(), responsavelId, OrigemObservacao.RESPONSAVEL);
    }
    publicar(salva, TipoEventoBoard.TAREFA_CRIADA, Set.of());
    return salva;
  }

  /**
   * Movimenta o card validando o grafo contra a etapa relida na transacao (RF-002, RF-012,
   * RN-001/002/004/011). {@code versaoEsperada} divergente indica board desatualizado.
   */
  @Transactional
  public Tarefa mover(UUID tarefaId, MoverTarefaRequest request, Usuario autor) {
    Tarefa tarefa = buscarEntidade(tarefaId);
    permissaoGuard.exigir(CodigoPermissao.TAREFA_MOVER, tarefa.getProjetoId());
    permissaoGuard.exigirProjetoAtivo(tarefa.getProjetoId());

    if (tarefa.getVersao() != request.versaoEsperada().longValue()) {
      throw new ConflitoConcorrenciaException(
          "A tarefa foi alterada por outro usuario. Recarregue o board e tente novamente.");
    }

    Etapa etapaAtual = buscarEtapa(tarefa.getEtapaId());
    Etapa etapaDestino = buscarEtapa(request.etapaDestinoId());
    if (!etapaDestino.getWorkflowId().equals(tarefa.getWorkflowId())) {
      throw new TransicaoNaoPermitidaException(
          "A etapa de destino nao pertence ao workflow desta tarefa.");
    }

    if (etapaAtual.getId().equals(etapaDestino.getId())) {
      // Apenas troca de raia: nao ha transicao no grafo e nao ha novo intervalo de etapa.
      aplicarRaia(tarefa, request.raiaDestinoId());
      publicar(tarefa, TipoEventoBoard.TAREFA_ATUALIZADA, Set.of());
      return tarefa;
    }

    // Entrar na etapa final ou sair dela exige a permissao de finalizacao (RN-011).
    if (etapaDestino.isEtapaFinal() || etapaAtual.isEtapaFinal()) {
      permissaoGuard.exigir(CodigoPermissao.TAREFA_FINALIZAR, tarefa.getProjetoId());
      permissaoGuard.exigirToggle(
          ChaveToggle.DEV_PODE_FINALIZAR_TAREFA, tarefa.getProjetoId(), CodigoPapel.DEV);
    }

    validarGrafo(etapaAtual, etapaDestino);

    Etapa primeiraEtapa = primeiraEtapa(tarefa.getWorkflowId());

    leadTimeService.encerrarPeriodoEtapa(tarefa);
    tarefa.setEtapaId(etapaDestino.getId());
    leadTimeService.abrirPeriodoEtapa(tarefa, etapaDestino.getId());
    if (tarefa.isImpedida()) {
      // RN-002: o impedimento e reancorado na nova etapa sem perder o tempo ja acumulado.
      leadTimeService.reancorarImpedimento(tarefa, etapaDestino.getId());
    }
    // Sticky (A-4): a primeira saida da etapa de menor ordem inicia a tarefa e voltar nao desfaz.
    if (etapaAtual.getId().equals(primeiraEtapa.getId())) {
      tarefa.setIniciada(true);
    }
    aplicarRaia(tarefa, request.raiaDestinoId());

    auditoriaService.registrar(
        tarefa, CampoAuditado.ETAPA, etapaAtual.getNome(), etapaDestino.getNome(), autor);
    Set<UUID> destinatarios =
        notificacaoService.notificarObservadores(
            tarefa,
            TipoNotificacao.ETAPA_ALTERADA,
            "A tarefa \""
                + tarefa.getTitulo()
                + "\" foi movida para "
                + etapaDestino.getNome()
                + ".",
            autor);
    publicar(tarefa, TipoEventoBoard.TAREFA_MOVIDA, destinatarios);
    return tarefa;
  }

  /**
   * Edita o card (RF-003). Com {@code iniciada == true}, descricao e tipo so mudam com o toggle
   * {@code DEV_PODE_EDITAR_TAREFA_INICIADA} habilitado ou com {@code projeto:administrar} (A-3). O
   * titulo permanece editavel porque RN-016 exige auditoria de titulo.
   */
  @Transactional
  public Tarefa atualizar(UUID tarefaId, AtualizarTarefaRequest request, Usuario autor) {
    Tarefa tarefa = buscarEntidade(tarefaId);
    permissaoGuard.exigir(CodigoPermissao.TAREFA_GERENCIAR, tarefa.getProjetoId());
    permissaoGuard.exigirProjetoAtivo(tarefa.getProjetoId());

    boolean alterouEstrutural =
        !Objects.equals(tarefa.getDescricao(), request.descricao())
            || tarefa.getTipo() != request.tipo();
    if (tarefa.isIniciada() && alterouEstrutural) {
      permissaoGuard.exigirToggle(
          ChaveToggle.DEV_PODE_EDITAR_TAREFA_INICIADA, tarefa.getProjetoId(), CodigoPapel.DEV);
      boolean liberado =
          projetoToggleService.habilitado(
                  tarefa.getProjetoId(), ChaveToggle.DEV_PODE_EDITAR_TAREFA_INICIADA)
              || permissaoGuard.possui(CodigoPermissao.PROJETO_ADMINISTRAR, tarefa.getProjetoId());
      if (!liberado) {
        throw new TarefaIniciadaException(
            "A tarefa ja foi iniciada: descricao e tipo nao podem ser alterados.");
      }
    }

    String tituloAnterior = tarefa.getTitulo();
    tarefa.setTitulo(request.titulo());
    tarefa.setPrioridade(request.prioridade());
    tarefa.setDescricao(request.descricao());
    tarefa.setTipo(request.tipo());
    aplicarRaia(tarefa, request.raiaId());

    auditoriaService.registrar(
        tarefa, CampoAuditado.TITULO, tituloAnterior, tarefa.getTitulo(), autor);
    publicar(tarefa, TipoEventoBoard.TAREFA_ATUALIZADA, Set.of());
    return tarefa;
  }

  /**
   * Atribui responsavel (RN-012). Autoatribuicao e livre para quem possui {@code tarefa:mover},
   * inclusive sobre tarefa ja atribuida a outro; atribuir a terceiros exige {@code
   * tarefa:atribuir}.
   */
  @Transactional
  public Tarefa atribuir(UUID tarefaId, UUID novoResponsavelId, Usuario autor) {
    Tarefa tarefa = buscarEntidade(tarefaId);
    permissaoGuard.exigirProjetoAtivo(tarefa.getProjetoId());
    if (autor.getId().equals(novoResponsavelId)) {
      permissaoGuard.exigir(CodigoPermissao.TAREFA_MOVER, tarefa.getProjetoId());
    } else {
      permissaoGuard.exigir(CodigoPermissao.TAREFA_ATRIBUIR, tarefa.getProjetoId());
    }

    UUID anterior = tarefa.getResponsavelId();
    tarefa.setResponsavelId(novoResponsavelId);
    if (novoResponsavelId != null) {
      registrarObservador(tarefa.getId(), novoResponsavelId, OrigemObservacao.RESPONSAVEL);
    }
    auditoriaService.registrar(
        tarefa,
        CampoAuditado.RESPONSAVEL,
        anterior == null ? null : anterior.toString(),
        novoResponsavelId == null ? null : novoResponsavelId.toString(),
        autor);
    Set<UUID> destinatarios =
        notificacaoService.notificarObservadores(
            tarefa,
            TipoNotificacao.ETAPA_ALTERADA,
            "O responsavel da tarefa \"" + tarefa.getTitulo() + "\" foi alterado.",
            autor);
    publicar(tarefa, TipoEventoBoard.TAREFA_ATUALIZADA, destinatarios);
    return tarefa;
  }

  /**
   * Marca impedimento (RF-004, RN-013). Idempotente. O impedimento nao bloqueia nem libera a
   * movimentacao: e apenas sinalizacao mais medicao de tempo (DDR-002).
   */
  @Transactional
  public Tarefa marcarImpedimento(UUID tarefaId, String motivo, Usuario autor) {
    Tarefa tarefa = buscarEntidade(tarefaId);
    permissaoGuard.exigir(CodigoPermissao.TAREFA_IMPEDIR, tarefa.getProjetoId());
    permissaoGuard.exigirProjetoAtivo(tarefa.getProjetoId());

    if (leadTimeService.abrirImpedimento(tarefa, motivo).isEmpty()) {
      return tarefa;
    }
    tarefa.setImpedida(true);
    auditoriaService.registrar(tarefa, CampoAuditado.IMPEDIMENTO, "false", "true", autor);
    Set<UUID> destinatarios =
        notificacaoService.notificarObservadores(
            tarefa,
            TipoNotificacao.IMPEDIMENTO_MARCADO,
            "A tarefa \"" + tarefa.getTitulo() + "\" foi marcada como impedida.",
            autor);
    publicar(tarefa, TipoEventoBoard.TAREFA_IMPEDIDA, destinatarios);
    return tarefa;
  }

  /** Desmarca impedimento (RF-004). Idempotente: sem intervalo aberto, nada acontece. */
  @Transactional
  public Tarefa desmarcarImpedimento(UUID tarefaId, Usuario autor) {
    Tarefa tarefa = buscarEntidade(tarefaId);
    permissaoGuard.exigir(CodigoPermissao.TAREFA_IMPEDIR, tarefa.getProjetoId());
    permissaoGuard.exigirProjetoAtivo(tarefa.getProjetoId());

    if (!leadTimeService.encerrarImpedimento(tarefa)) {
      return tarefa;
    }
    tarefa.setImpedida(false);
    auditoriaService.registrar(tarefa, CampoAuditado.IMPEDIMENTO, "true", "false", autor);
    Set<UUID> destinatarios =
        notificacaoService.notificarObservadores(
            tarefa,
            TipoNotificacao.IMPEDIMENTO_DESMARCADO,
            "O impedimento da tarefa \"" + tarefa.getTitulo() + "\" foi removido.",
            autor);
    publicar(tarefa, TipoEventoBoard.TAREFA_DESIMPEDIDA, destinatarios);
    return tarefa;
  }

  /**
   * Exclui o card (RF-019). Fecha os intervalos abertos antes de remover, para nao deixar intervalo
   * orfao contaminando as medias; a auditoria e preservada por ser append-only e sem FK.
   */
  @Transactional
  public void excluir(UUID tarefaId, Usuario autor) {
    Tarefa tarefa = buscarEntidade(tarefaId);
    permissaoGuard.exigir(CodigoPermissao.TAREFA_GERENCIAR, tarefa.getProjetoId());
    permissaoGuard.exigirToggle(
        ChaveToggle.DEV_PODE_EXCLUIR_TAREFA, tarefa.getProjetoId(), CodigoPapel.DEV);
    permissaoGuard.exigirProjetoAtivo(tarefa.getProjetoId());

    leadTimeService.encerrarTodosOsPeriodos(tarefa);
    tarefaObservadorRepository.deleteByIdTarefaId(tarefa.getId());
    notificacaoService.removerDaTarefa(tarefa.getId());
    tarefaRepository.delete(tarefa);
    tarefaRepository.flush();
    publicar(tarefa, TipoEventoBoard.TAREFA_EXCLUIDA, Set.of());
  }

  /** Torna o usuario observador explicito do card (RF-005, A-5). Idempotente. */
  @Transactional
  public void observar(UUID tarefaId, Usuario usuario) {
    Tarefa tarefa = buscarEntidade(tarefaId);
    permissaoGuard.exigir(CodigoPermissao.PROJETO_VISUALIZAR, tarefa.getProjetoId());
    registrarObservador(tarefa.getId(), usuario.getId(), OrigemObservacao.EXPLICITO);
  }

  /** Remove apenas o vinculo explicito: CRIADOR e RESPONSAVEL sao implicitos e nao saem (A-5). */
  @Transactional
  public void desobservar(UUID tarefaId, Usuario usuario) {
    Tarefa tarefa = buscarEntidade(tarefaId);
    permissaoGuard.exigir(CodigoPermissao.PROJETO_VISUALIZAR, tarefa.getProjetoId());
    tarefaObservadorRepository
        .findById(new TarefaObservadorId(tarefa.getId(), usuario.getId()))
        .filter(observador -> observador.getOrigem() == OrigemObservacao.EXPLICITO)
        .ifPresent(tarefaObservadorRepository::delete);
  }

  /** Card por identificador, com a permissao de leitura do projeto validada. */
  @Transactional(readOnly = true)
  public Tarefa buscar(UUID tarefaId) {
    Tarefa tarefa = buscarEntidade(tarefaId);
    permissaoGuard.exigir(CodigoPermissao.PROJETO_VISUALIZAR, tarefa.getProjetoId());
    return tarefa;
  }

  /**
   * Detalhe do card com lead time por etapa, historico paginado e acoes permitidas (RF-006,
   * RF-017). {@code acoesPermitidas} espelha o RBAC efetivo para a UI condicional (RNF-003).
   */
  @Transactional(readOnly = true)
  public TarefaDetalheResponse detalhe(UUID tarefaId, Pageable paginacaoHistorico) {
    Tarefa tarefa = buscar(tarefaId);
    DetalheLeadTime leadTime =
        leadTimeService.calcularDetalhe(tarefa.getId(), tarefa.getWorkflowId());
    List<AuditoriaResponse> historico =
        tarefaMapper.paraAuditorias(
            auditoriaService.historico(tarefa.getId(), paginacaoHistorico).getContent());
    return tarefaMapper.paraDetalhe(
        tarefa,
        leadTime.etapas(),
        leadTime.impedimentoTotalSegundos(),
        historico,
        permissaoGuard.permissoesEfetivas(tarefa.getProjetoId()));
  }

  /** Historico de auditoria paginado do card (RF-017). */
  @Transactional(readOnly = true)
  public Page<AuditoriaResponse> historico(UUID tarefaId, Pageable pageable) {
    Tarefa tarefa = buscar(tarefaId);
    return auditoriaService.historico(tarefa.getId(), pageable).map(tarefaMapper::paraAuditoria);
  }

  private void validarGrafo(Etapa etapaAtual, Etapa etapaDestino) {
    if (etapaAtual.isEtapaFinal()) {
      // Desfinalizar (RF-012): destino valido e qualquer predecessora direta da etapa final.
      boolean predecessora =
          transicaoRepository.findByEtapaDestinoId(etapaAtual.getId()).stream()
              .map(Transicao::getEtapaOrigemId)
              .anyMatch(origemId -> origemId.equals(etapaDestino.getId()));
      if (!predecessora) {
        throw new TransicaoNaoPermitidaException(
            "Nao e permitido retornar de \""
                + etapaAtual.getNome()
                + "\" para \""
                + etapaDestino.getNome()
                + "\": apenas etapas que levam diretamente a etapa final sao destinos validos.");
      }
      return;
    }
    if (!transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(
        etapaAtual.getId(), etapaDestino.getId())) {
      throw new TransicaoNaoPermitidaException(
          "Nao existe transicao configurada de \""
              + etapaAtual.getNome()
              + "\" para \""
              + etapaDestino.getNome()
              + "\".");
    }
  }

  private Etapa primeiraEtapa(UUID workflowId) {
    return etapaRepository
        .findFirstByWorkflowIdOrderByOrdemAsc(workflowId)
        .orElseThrow(
            () ->
                new ConfiguracaoWorkflowInvalidaException(
                    "O workflow nao possui nenhuma etapa configurada."));
  }

  private void aplicarRaia(Tarefa tarefa, UUID raiaId) {
    if (raiaId == null || raiaId.equals(tarefa.getRaiaId())) {
      return;
    }
    // Raia nao possui grafo de transicao: o movimento entre raias e sempre livre.
    tarefa.setRaiaId(resolverRaia(tarefa.getProjetoId(), raiaId).getId());
  }

  private Raia resolverRaia(UUID projetoId, UUID raiaId) {
    if (raiaId == null) {
      return raiaRepository
          .findByProjetoIdAndPadraoTrue(projetoId)
          .orElseThrow(
              () ->
                  new ConfiguracaoWorkflowInvalidaException(
                      "O projeto nao possui uma raia padrao configurada."));
    }
    Raia raia =
        raiaRepository
            .findById(raiaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Raia nao encontrada."));
    if (!raia.getProjetoId().equals(projetoId)) {
      throw new RecursoNaoEncontradoException("Raia nao encontrada.");
    }
    return raia;
  }

  private UUID resolverResponsavelNaCriacao(UUID projetoId, UUID responsavelId, Usuario autor) {
    if (responsavelId == null) {
      return null;
    }
    if (!responsavelId.equals(autor.getId())) {
      permissaoGuard.exigir(CodigoPermissao.TAREFA_ATRIBUIR, projetoId);
    }
    return responsavelId;
  }

  private void registrarObservador(UUID tarefaId, UUID usuarioId, OrigemObservacao origem) {
    if (tarefaObservadorRepository.existsByIdTarefaIdAndIdUsuarioId(tarefaId, usuarioId)) {
      return;
    }
    tarefaObservadorRepository.save(new TarefaObservador(tarefaId, usuarioId, origem));
  }

  private void publicar(Tarefa tarefa, TipoEventoBoard tipo, Set<UUID> destinatarios) {
    eventoBoardPublisher.publicar(
        EventoBoard.deTarefa(
            tarefa.getProjetoId(), tipo, tarefa.getId(), tarefa.getEtapaId(), tarefa.getRaiaId()),
        destinatarios);
  }

  private Tarefa buscarEntidade(UUID tarefaId) {
    return tarefaRepository
        .findById(tarefaId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa nao encontrada."));
  }

  private Etapa buscarEtapa(UUID etapaId) {
    return etapaRepository
        .findById(etapaId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Etapa nao encontrada."));
  }
}
