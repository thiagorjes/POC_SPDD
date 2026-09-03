package br.com.crudao.kanban.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.auditoria.AuditoriaService;
import br.com.crudao.kanban.auditoria.CampoAuditado;
import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.common.ConflitoConcorrenciaException;
import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.TarefaIniciadaException;
import br.com.crudao.kanban.common.TransicaoNaoPermitidaException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.leadtime.LeadTimeService;
import br.com.crudao.kanban.leadtime.PeriodoEtapaRepository;
import br.com.crudao.kanban.leadtime.PeriodoImpedimentoRepository;
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
import br.com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.MoverTarefaRequest;
import br.com.crudao.kanban.workflow.Etapa;
import br.com.crudao.kanban.workflow.EtapaRepository;
import br.com.crudao.kanban.workflow.Transicao;
import br.com.crudao.kanban.workflow.TransicaoRepository;
import br.com.crudao.kanban.workflow.Workflow;
import br.com.crudao.kanban.workflow.WorkflowRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Regras de tarefa isoladas de Spring e de banco. Cobre a matriz de transicoes, desfinalizar, trava
 * pos-inicio, autoatribuicao e idempotencia de impedimento.
 */
@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

  private static final UUID PROJETO = UUID.randomUUID();
  private static final UUID WORKFLOW = UUID.randomUUID();

  @Mock private TarefaRepository tarefaRepository;
  @Mock private TarefaObservadorRepository tarefaObservadorRepository;
  @Mock private EtapaRepository etapaRepository;
  @Mock private TransicaoRepository transicaoRepository;
  @Mock private WorkflowRepository workflowRepository;
  @Mock private RaiaRepository raiaRepository;
  @Mock private PeriodoEtapaRepository periodoEtapaRepository;
  @Mock private PeriodoImpedimentoRepository periodoImpedimentoRepository;
  @Mock private LeadTimeService leadTimeService;
  @Mock private AuditoriaService auditoriaService;
  @Mock private NotificacaoService notificacaoService;
  @Mock private EventoBoardPublisher eventoBoardPublisher;
  @Mock private PermissaoGuard permissaoGuard;
  @Mock private UsuarioRepository usuarioRepository;
  @Mock private TarefaMapper tarefaMapper;

  @InjectMocks private TarefaService servico;

  private Usuario autor;
  private Etapa aFazer;
  private Etapa fazendo;
  private Etapa concluido;
  private Raia padrao;

  @BeforeEach
  void preparar() {
    autor = Usuario.builder().id(UUID.randomUUID()).nome("Ana").adminGlobal(false).build();
    aFazer = etapa("A fazer", 1, false);
    fazendo = etapa("Fazendo", 2, false);
    concluido = etapa("Concluido", 3, true);
    padrao =
        Raia.builder().id(UUID.randomUUID()).projetoId(PROJETO).nome("Padrao").padrao(true).build();
  }

  private Etapa etapa(String nome, int ordem, boolean etapaFinal) {
    return Etapa.builder()
        .id(UUID.randomUUID())
        .workflowId(WORKFLOW)
        .nome(nome)
        .ordem(ordem)
        .etapaFinal(etapaFinal)
        .build();
  }

  private Tarefa tarefa(Etapa etapaAtual) {
    return Tarefa.builder()
        .id(UUID.randomUUID())
        .projetoId(PROJETO)
        .workflowId(WORKFLOW)
        .etapaId(etapaAtual.getId())
        .raiaId(padrao.getId())
        .criadorId(autor.getId())
        .titulo("Titulo")
        .tipo(TipoTarefa.FEATURE)
        .prioridade(PrioridadeTarefa.MEDIA)
        .versao(3L)
        .build();
  }

  private void comEtapas(Etapa... etapas) {
    for (Etapa etapa : etapas) {
      lenient().when(etapaRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));
    }
  }

  private void comTarefa(Tarefa tarefa) {
    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
  }

  @Nested
  @DisplayName("criar")
  class Criar {

    @Test
    @DisplayName("nasce na etapa de menor ordem e na raia padrao quando nada e informado")
    void nasceNaPrimeiraEtapaERaiaPadrao() {
      when(workflowRepository.findByProjetoIdAndAtivoTrue(PROJETO))
          .thenReturn(Optional.of(Workflow.builder().id(WORKFLOW).projetoId(PROJETO).ativo(true).build()));
      when(etapaRepository.findFirstByWorkflowIdOrderByOrdemAsc(WORKFLOW))
          .thenReturn(Optional.of(aFazer));
      when(raiaRepository.findByProjetoIdAndPadraoTrue(PROJETO)).thenReturn(Optional.of(padrao));
      when(tarefaRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
      when(tarefaObservadorRepository.findByIdTarefaIdAndIdUsuarioId(any(), any()))
          .thenReturn(Optional.empty());

      Tarefa criada =
          servico.criar(
              PROJETO,
              new CriarTarefaRequest("Nova", null, TipoTarefa.BUG, null, null, null),
              autor);

      assertThat(criada.getEtapaId()).isEqualTo(aFazer.getId());
      assertThat(criada.getRaiaId()).isEqualTo(padrao.getId());
      assertThat(criada.getResponsavelId()).isNull();
      assertThat(criada.getPrioridade()).isEqualTo(PrioridadeTarefa.MEDIA);
      assertThat(criada.isIniciada()).isFalse();
      verify(leadTimeService).abrirPeriodoEtapa(criada, aFazer.getId());
      // RN-016 audita alteracoes, nao a criacao.
      verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("atribuir a terceiro na criacao exige tarefa:atribuir")
    void atribuirTerceiroExigePermissao() {
      when(workflowRepository.findByProjetoIdAndAtivoTrue(PROJETO))
          .thenReturn(Optional.of(Workflow.builder().id(WORKFLOW).projetoId(PROJETO).ativo(true).build()));
      when(etapaRepository.findFirstByWorkflowIdOrderByOrdemAsc(WORKFLOW))
          .thenReturn(Optional.of(aFazer));
      when(raiaRepository.findByProjetoIdAndPadraoTrue(PROJETO)).thenReturn(Optional.of(padrao));
      when(tarefaRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

      servico.criar(
          PROJETO,
          new CriarTarefaRequest("Nova", null, TipoTarefa.BUG, null, null, UUID.randomUUID()),
          autor);

      verify(permissaoGuard).exigir(Permissoes.TAREFA_ATRIBUIR, PROJETO);
    }

    @Test
    @DisplayName("sem workflow ativo a criacao falha com mensagem de configuracao")
    void semWorkflowAtivo() {
      when(workflowRepository.findByProjetoIdAndAtivoTrue(PROJETO)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  servico.criar(
                      PROJETO,
                      new CriarTarefaRequest("Nova", null, TipoTarefa.BUG, null, null, null),
                      autor))
          .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
          .hasMessage("O projeto nao possui workflow ativo configurado.");
    }
  }

  @Nested
  @DisplayName("mover")
  class Mover {

    @Test
    @DisplayName("transicao existente move, audita a etapa e notifica observadores")
    void transicaoValida() {
      Tarefa tarefa = tarefa(aFazer);
      comTarefa(tarefa);
      comEtapas(aFazer, fazendo);
      when(transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(aFazer.getId(), fazendo.getId()))
          .thenReturn(true);
      when(etapaRepository.findFirstByWorkflowIdOrderByOrdemAsc(WORKFLOW))
          .thenReturn(Optional.of(aFazer));

      servico.mover(tarefa.getId(), new MoverTarefaRequest(fazendo.getId(), null, 3L), autor);

      assertThat(tarefa.getEtapaId()).isEqualTo(fazendo.getId());
      // Saiu da etapa de menor ordem: iniciada vira sticky (A-4).
      assertThat(tarefa.isIniciada()).isTrue();
      verify(auditoriaService)
          .registrar(tarefa, CampoAuditado.ETAPA, "A fazer", "Fazendo", autor);
      verify(notificacaoService)
          .notificarObservadores(tarefa, TipoNotificacao.ETAPA_ALTERADA, autor);
      verify(leadTimeService).encerrarPeriodoEtapa(tarefa);
      verify(leadTimeService).abrirPeriodoEtapa(tarefa, fazendo.getId());
    }

    @Test
    @DisplayName("sem aresta no grafo a movimentacao e recusada")
    void semAresta() {
      Tarefa tarefa = tarefa(aFazer);
      comTarefa(tarefa);
      comEtapas(aFazer, fazendo);
      when(transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(aFazer.getId(), fazendo.getId()))
          .thenReturn(false);

      assertThatThrownBy(
              () ->
                  servico.mover(
                      tarefa.getId(), new MoverTarefaRequest(fazendo.getId(), null, 3L), autor))
          .isInstanceOf(TransicaoNaoPermitidaException.class)
          .hasMessage("Nao existe transicao configurada de \"A fazer\" para \"Fazendo\".");
      assertThat(tarefa.getEtapaId()).isEqualTo(aFazer.getId());
    }

    @Test
    @DisplayName("destino de outro workflow e recusado antes de qualquer checagem de grafo")
    void destinoDeOutroWorkflow() {
      Tarefa tarefa = tarefa(aFazer);
      Etapa estrangeira =
          Etapa.builder()
              .id(UUID.randomUUID())
              .workflowId(UUID.randomUUID())
              .nome("Alheia")
              .ordem(1)
              .build();
      comTarefa(tarefa);
      comEtapas(aFazer, estrangeira);

      assertThatThrownBy(
              () ->
                  servico.mover(
                      tarefa.getId(), new MoverTarefaRequest(estrangeira.getId(), null, 3L), autor))
          .isInstanceOf(TransicaoNaoPermitidaException.class)
          .hasMessage("A etapa de destino nao pertence ao workflow da tarefa.");
      verify(transicaoRepository, never())
          .existsByEtapaOrigemIdAndEtapaDestinoId(any(), any());
    }

    @Test
    @DisplayName("versao divergente gera conflito de concorrencia")
    void versaoDivergente() {
      Tarefa tarefa = tarefa(aFazer);
      comTarefa(tarefa);

      assertThatThrownBy(
              () ->
                  servico.mover(
                      tarefa.getId(), new MoverTarefaRequest(fazendo.getId(), null, 2L), autor))
          .isInstanceOf(ConflitoConcorrenciaException.class)
          .hasMessage("A tarefa foi alterada por outro usuario. Recarregue o board e tente novamente.");
    }

    @Test
    @DisplayName("finalizar sem permissao e sem toggle e negado")
    void finalizarSemPermissao() {
      Tarefa tarefa = tarefa(fazendo);
      comTarefa(tarefa);
      comEtapas(fazendo, concluido);
      when(permissaoGuard.possui(Permissoes.TAREFA_FINALIZAR, PROJETO)).thenReturn(false);
      when(permissaoGuard.papeisNoProjeto(PROJETO)).thenReturn(Set.of(Papeis.DEV));
      when(permissaoGuard.toggleHabilitado(PROJETO, ChaveToggle.DEV_PODE_FINALIZAR_TAREFA))
          .thenReturn(false);

      assertThatThrownBy(
              () ->
                  servico.mover(
                      tarefa.getId(), new MoverTarefaRequest(concluido.getId(), null, 3L), autor))
          .isInstanceOf(PermissaoNegadaException.class)
          .hasMessage("Voce nao tem permissao para finalizar ou desfinalizar tarefas neste projeto.");
    }

    @Test
    @DisplayName("dev finaliza quando o projeto libera o toggle")
    void devFinalizaComToggle() {
      Tarefa tarefa = tarefa(fazendo);
      comTarefa(tarefa);
      comEtapas(fazendo, concluido);
      when(permissaoGuard.possui(Permissoes.TAREFA_FINALIZAR, PROJETO)).thenReturn(false);
      when(permissaoGuard.papeisNoProjeto(PROJETO)).thenReturn(Set.of(Papeis.DEV));
      when(permissaoGuard.toggleHabilitado(PROJETO, ChaveToggle.DEV_PODE_FINALIZAR_TAREFA))
          .thenReturn(true);
      when(transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(
              fazendo.getId(), concluido.getId()))
          .thenReturn(true);
      when(etapaRepository.findFirstByWorkflowIdOrderByOrdemAsc(WORKFLOW))
          .thenReturn(Optional.of(aFazer));

      servico.mover(tarefa.getId(), new MoverTarefaRequest(concluido.getId(), null, 3L), autor);

      assertThat(tarefa.getEtapaId()).isEqualTo(concluido.getId());
    }

    @Test
    @DisplayName("desfinalizar so aceita predecessora direta da etapa final")
    void desfinalizarSoPredecessoraDireta() {
      Tarefa tarefa = tarefa(concluido);
      comTarefa(tarefa);
      comEtapas(concluido, fazendo, aFazer);
      when(permissaoGuard.possui(Permissoes.TAREFA_FINALIZAR, PROJETO)).thenReturn(true);
      // "A fazer" nao leva diretamente a "Concluido".
      when(transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(
              aFazer.getId(), concluido.getId()))
          .thenReturn(false);

      assertThatThrownBy(
              () ->
                  servico.mover(
                      tarefa.getId(), new MoverTarefaRequest(aFazer.getId(), null, 3L), autor))
          .isInstanceOf(TransicaoNaoPermitidaException.class)
          .hasMessage(
              "So e possivel desfinalizar retornando para uma etapa que leva diretamente a \"Concluido\".");

      when(transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(
              fazendo.getId(), concluido.getId()))
          .thenReturn(true);
      when(etapaRepository.findFirstByWorkflowIdOrderByOrdemAsc(WORKFLOW))
          .thenReturn(Optional.of(aFazer));

      servico.mover(tarefa.getId(), new MoverTarefaRequest(fazendo.getId(), null, 3L), autor);
      assertThat(tarefa.getEtapaId()).isEqualTo(fazendo.getId());
    }

    @Test
    @DisplayName("tarefa impedida reancora o impedimento na etapa de destino")
    void reancoraImpedimento() {
      Tarefa tarefa = tarefa(aFazer);
      tarefa.setImpedida(true);
      comTarefa(tarefa);
      comEtapas(aFazer, fazendo);
      when(transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(aFazer.getId(), fazendo.getId()))
          .thenReturn(true);
      when(etapaRepository.findFirstByWorkflowIdOrderByOrdemAsc(WORKFLOW))
          .thenReturn(Optional.of(aFazer));

      servico.mover(tarefa.getId(), new MoverTarefaRequest(fazendo.getId(), null, 3L), autor);

      verify(leadTimeService).reancorarImpedimento(tarefa, fazendo.getId());
    }

    @Test
    @DisplayName("mesma etapa sem troca de raia e no-op")
    void mesmaEtapaSemRaia() {
      Tarefa tarefa = tarefa(fazendo);
      comTarefa(tarefa);
      comEtapas(fazendo);

      servico.mover(tarefa.getId(), new MoverTarefaRequest(fazendo.getId(), null, 3L), autor);

      verify(leadTimeService, never()).encerrarPeriodoEtapa(any());
      verify(eventoBoardPublisher, never()).publicar(any());
    }

    @Test
    @DisplayName("troca de raia na mesma etapa nao consulta o grafo de transicoes")
    void trocaDeRaiaNaoUsaGrafo() {
      Tarefa tarefa = tarefa(fazendo);
      Raia outra =
          Raia.builder().id(UUID.randomUUID()).projetoId(PROJETO).nome("Suporte").build();
      comTarefa(tarefa);
      comEtapas(fazendo);
      when(raiaRepository.findById(outra.getId())).thenReturn(Optional.of(outra));

      servico.mover(
          tarefa.getId(), new MoverTarefaRequest(fazendo.getId(), outra.getId(), 3L), autor);

      assertThat(tarefa.getRaiaId()).isEqualTo(outra.getId());
      verify(transicaoRepository, never()).existsByEtapaOrigemIdAndEtapaDestinoId(any(), any());
      verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }
  }

  @Nested
  @DisplayName("atualizar")
  class Atualizar {

    @Test
    @DisplayName("tarefa iniciada bloqueia descricao e tipo")
    void bloqueiaPosInicio() {
      Tarefa tarefa = tarefa(fazendo);
      tarefa.setIniciada(true);
      tarefa.setDescricao("original");
      comTarefa(tarefa);
      when(permissaoGuard.usuarioAtual()).thenReturn(autor);
      when(permissaoGuard.papeisNoProjeto(PROJETO)).thenReturn(Set.of(Papeis.DEV));
      when(permissaoGuard.toggleHabilitado(PROJETO, ChaveToggle.DEV_PODE_EDITAR_TAREFA_INICIADA))
          .thenReturn(false);

      assertThatThrownBy(
              () ->
                  servico.atualizar(
                      tarefa.getId(),
                      new AtualizarTarefaRequest(
                          "Titulo",
                          "alterada",
                          TipoTarefa.FEATURE,
                          PrioridadeTarefa.MEDIA,
                          null,
                          3L),
                      autor))
          .isInstanceOf(TarefaIniciadaException.class)
          .hasMessage("A tarefa ja foi iniciada: descricao e tipo nao podem mais ser alterados.");
    }

    @Test
    @DisplayName("titulo continua editavel apos o inicio e e auditado")
    void tituloSempreEditavel() {
      Tarefa tarefa = tarefa(fazendo);
      tarefa.setIniciada(true);
      tarefa.setDescricao("original");
      comTarefa(tarefa);

      servico.atualizar(
          tarefa.getId(),
          new AtualizarTarefaRequest(
              "Novo titulo", "original", TipoTarefa.FEATURE, PrioridadeTarefa.ALTA, null, 3L),
          autor);

      assertThat(tarefa.getTitulo()).isEqualTo("Novo titulo");
      verify(auditoriaService)
          .registrar(tarefa, CampoAuditado.TITULO, "Titulo", "Novo titulo", autor);
    }

    @Test
    @DisplayName("toggle liberado permite editar descricao apos o inicio")
    void toggleLiberaEdicao() {
      Tarefa tarefa = tarefa(fazendo);
      tarefa.setIniciada(true);
      tarefa.setDescricao("original");
      comTarefa(tarefa);
      when(permissaoGuard.usuarioAtual()).thenReturn(autor);
      when(permissaoGuard.papeisNoProjeto(PROJETO)).thenReturn(Set.of(Papeis.DEV));
      when(permissaoGuard.toggleHabilitado(PROJETO, ChaveToggle.DEV_PODE_EDITAR_TAREFA_INICIADA))
          .thenReturn(true);

      assertThatCode(
              () ->
                  servico.atualizar(
                      tarefa.getId(),
                      new AtualizarTarefaRequest(
                          "Titulo",
                          "alterada",
                          TipoTarefa.FEATURE,
                          PrioridadeTarefa.MEDIA,
                          null,
                          3L),
                      autor))
          .doesNotThrowAnyException();
      assertThat(tarefa.getDescricao()).isEqualTo("alterada");
    }
  }

  @Nested
  @DisplayName("atribuir")
  class Atribuir {

    @Test
    @DisplayName("autoatribuicao exige apenas tarefa:mover")
    void autoatribuicao() {
      Tarefa tarefa = tarefa(fazendo);
      tarefa.setResponsavelId(UUID.randomUUID());
      comTarefa(tarefa);
      when(tarefaObservadorRepository.findByIdTarefaIdAndIdUsuarioId(tarefa.getId(), autor.getId()))
          .thenReturn(Optional.empty());

      servico.atribuir(tarefa.getId(), autor.getId(), autor);

      verify(permissaoGuard).exigir(Permissoes.TAREFA_MOVER, PROJETO);
      verify(permissaoGuard, never()).exigir(eq(Permissoes.TAREFA_ATRIBUIR), any());
      verify(permissaoGuard).exigirProjetoAtivo(PROJETO);
      assertThat(tarefa.getResponsavelId()).isEqualTo(autor.getId());
    }

    @Test
    @DisplayName("atribuir a terceiro exige tarefa:atribuir")
    void terceiro() {
      Tarefa tarefa = tarefa(fazendo);
      UUID outro = UUID.randomUUID();
      comTarefa(tarefa);
      when(tarefaObservadorRepository.findByIdTarefaIdAndIdUsuarioId(tarefa.getId(), outro))
          .thenReturn(Optional.empty());

      servico.atribuir(tarefa.getId(), outro, autor);

      verify(permissaoGuard).exigir(Permissoes.TAREFA_ATRIBUIR, PROJETO);
    }

    @Test
    @DisplayName("reatribuir ao mesmo responsavel nao gera auditoria")
    void mesmoResponsavel() {
      Tarefa tarefa = tarefa(fazendo);
      tarefa.setResponsavelId(autor.getId());
      comTarefa(tarefa);

      servico.atribuir(tarefa.getId(), autor.getId(), autor);

      verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }
  }

  @Nested
  @DisplayName("impedimento")
  class Impedimento {

    @Test
    @DisplayName("marcar impedimento audita false -> true e publica evento")
    void marcar() {
      Tarefa tarefa = tarefa(fazendo);
      comTarefa(tarefa);

      servico.marcarImpedimento(tarefa.getId(), "Aguardando cliente", autor);

      assertThat(tarefa.isImpedida()).isTrue();
      verify(leadTimeService).abrirImpedimento(tarefa, "Aguardando cliente");
      verify(auditoriaService)
          .registrar(tarefa, CampoAuditado.IMPEDIMENTO, "false", "true", autor);

      ArgumentCaptor<EventoBoard> captor = ArgumentCaptor.forClass(EventoBoard.class);
      verify(eventoBoardPublisher).publicar(captor.capture());
      assertThat(captor.getValue().tipo()).isEqualTo(TipoEventoBoard.TAREFA_IMPEDIDA);
    }

    @Test
    @DisplayName("marcar tarefa ja impedida e no-op: nao abre segundo periodo")
    void marcarIdempotente() {
      Tarefa tarefa = tarefa(fazendo);
      tarefa.setImpedida(true);
      comTarefa(tarefa);

      servico.marcarImpedimento(tarefa.getId(), "Outro motivo", autor);

      verify(leadTimeService, never()).abrirImpedimento(any(), anyString());
      verify(eventoBoardPublisher, never()).publicar(any());
    }

    @Test
    @DisplayName("desmarcar tarefa nao impedida e no-op")
    void desmarcarIdempotente() {
      Tarefa tarefa = tarefa(fazendo);
      comTarefa(tarefa);

      servico.desmarcarImpedimento(tarefa.getId(), autor);

      verify(leadTimeService, never()).encerrarImpedimento(any());
      verify(eventoBoardPublisher, never()).publicar(any());
    }

    @Test
    @DisplayName("desmarcar encerra o periodo e audita true -> false")
    void desmarcar() {
      Tarefa tarefa = tarefa(fazendo);
      tarefa.setImpedida(true);
      comTarefa(tarefa);

      servico.desmarcarImpedimento(tarefa.getId(), autor);

      assertThat(tarefa.isImpedida()).isFalse();
      verify(leadTimeService).encerrarImpedimento(tarefa);
      verify(auditoriaService)
          .registrar(tarefa, CampoAuditado.IMPEDIMENTO, "true", "false", autor);
    }
  }

  @Nested
  @DisplayName("excluir")
  class Excluir {

    @Test
    @DisplayName("exige o toggle de exclusao, fecha intervalos e preserva a auditoria")
    void excluiPreservandoAuditoria() {
      Tarefa tarefa = tarefa(fazendo);
      comTarefa(tarefa);

      servico.excluir(tarefa.getId(), autor);

      verify(permissaoGuard)
          .exigirToggle(ChaveToggle.DEV_PODE_EXCLUIR_TAREFA, PROJETO, Papeis.DEV);
      verify(leadTimeService).encerrarTudo(tarefa);
      verify(periodoEtapaRepository).deleteByTarefaId(tarefa.getId());
      verify(periodoImpedimentoRepository).deleteByTarefaId(tarefa.getId());
      verify(tarefaRepository).delete(tarefa);
      verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }
  }

  @Nested
  @DisplayName("destinosPermitidos")
  class DestinosPermitidos {

    @Test
    @DisplayName("sem tarefa:mover nao ha destino algum")
    void semPermissaoDeMover() {
      assertThat(servico.destinosPermitidos(tarefa(aFazer), Set.of())).isEmpty();
    }

    @Test
    @DisplayName("sem tarefa:finalizar a etapa final e removida dos destinos")
    void filtraEtapaFinal() {
      comEtapas(fazendo, concluido, aFazer);
      when(transicaoRepository.findByEtapaOrigemId(fazendo.getId()))
          .thenReturn(
              List.of(
                  Transicao.builder()
                      .etapaOrigemId(fazendo.getId())
                      .etapaDestinoId(concluido.getId())
                      .build(),
                  Transicao.builder()
                      .etapaOrigemId(fazendo.getId())
                      .etapaDestinoId(aFazer.getId())
                      .build()));

      List<UUID> destinos =
          servico.destinosPermitidos(tarefa(fazendo), Set.of(Permissoes.TAREFA_MOVER));

      assertThat(destinos).containsExactly(aFazer.getId());
    }

    @Test
    @DisplayName("na etapa final os destinos sao as predecessoras diretas")
    void naEtapaFinal() {
      comEtapas(concluido);
      when(transicaoRepository.findByEtapaDestinoId(concluido.getId()))
          .thenReturn(
              List.of(
                  Transicao.builder()
                      .etapaOrigemId(fazendo.getId())
                      .etapaDestinoId(concluido.getId())
                      .build()));

      List<UUID> destinos =
          servico.destinosPermitidos(
              tarefa(concluido), Set.of(Permissoes.TAREFA_MOVER, Permissoes.TAREFA_FINALIZAR));

      assertThat(destinos).containsExactly(fazendo.getId());
    }

    @Test
    @DisplayName("na etapa final sem tarefa:finalizar nao ha retorno possivel")
    void naEtapaFinalSemPermissao() {
      comEtapas(concluido);

      assertThat(servico.destinosPermitidos(tarefa(concluido), Set.of(Permissoes.TAREFA_MOVER)))
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("observadores")
  class Observadores {

    @Test
    @DisplayName("desobservar so remove observacao explicita")
    void desobservarSoExplicito() {
      Tarefa tarefa = tarefa(fazendo);
      comTarefa(tarefa);
      TarefaObservador implicito =
          new TarefaObservador(tarefa.getId(), autor.getId(), OrigemObservacao.RESPONSAVEL);
      when(tarefaObservadorRepository.findByIdTarefaIdAndIdUsuarioId(tarefa.getId(), autor.getId()))
          .thenReturn(Optional.of(implicito));

      servico.desobservar(tarefa.getId(), autor);

      verify(tarefaObservadorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("observar registra uma unica vez")
    void observarIdempotente() {
      Tarefa tarefa = tarefa(fazendo);
      comTarefa(tarefa);
      when(tarefaObservadorRepository.findByIdTarefaIdAndIdUsuarioId(tarefa.getId(), autor.getId()))
          .thenReturn(Optional.empty())
          .thenReturn(
              Optional.of(
                  new TarefaObservador(tarefa.getId(), autor.getId(), OrigemObservacao.EXPLICITO)));

      servico.observar(tarefa.getId(), autor);
      servico.observar(tarefa.getId(), autor);

      verify(tarefaObservadorRepository, times(1)).save(any());
    }
  }
}
