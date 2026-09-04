package br.com.crudao.kanban.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.auditoria.AuditoriaService;
import br.com.crudao.kanban.auditoria.CampoAuditado;
import br.com.crudao.kanban.common.ConflitoConcorrenciaException;
import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.common.TarefaIniciadaException;
import br.com.crudao.kanban.common.TransicaoNaoPermitidaException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.leadtime.LeadTimeService;
import br.com.crudao.kanban.leadtime.PeriodoImpedimento;
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
 * Regras de card sem contexto Spring (bloco 19.1): matriz de transicoes, desfinalizacao, trava
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
  @Mock private ProjetoToggleService projetoToggleService;
  @Mock private LeadTimeService leadTimeService;
  @Mock private AuditoriaService auditoriaService;
  @Mock private NotificacaoService notificacaoService;
  @Mock private EventoBoardPublisher eventoBoardPublisher;
  @Mock private TarefaMapper tarefaMapper;
  @Mock private PermissaoGuard permissaoGuard;

  @InjectMocks private TarefaService tarefaService;

  private Usuario autor;
  private Etapa aFazer;
  private Etapa fazendo;
  private Etapa concluido;
  private Raia raiaPadrao;

  @BeforeEach
  void preparar() {
    autor = Usuario.builder().id(UUID.randomUUID()).nome("Autor").email("a@x").build();
    aFazer = etapa("A Fazer", 0, false);
    fazendo = etapa("Fazendo", 1, false);
    concluido = etapa("Concluido", 2, true);
    raiaPadrao = new Raia(PROJETO, "Padrao", 0, true);
    definirId(raiaPadrao);
  }

  private Etapa etapa(String nome, int ordem, boolean etapaFinal) {
    Etapa etapa = new Etapa(WORKFLOW, nome, ordem, etapaFinal);
    etapa.setId(UUID.randomUUID());
    return etapa;
  }

  private void definirId(Raia raia) {
    raia.setId(UUID.randomUUID());
  }

  private Tarefa tarefaEm(Etapa etapa) {
    return Tarefa.builder()
        .id(UUID.randomUUID())
        .projetoId(PROJETO)
        .workflowId(WORKFLOW)
        .etapaId(etapa.getId())
        .raiaId(raiaPadrao.getId())
        .criadorId(autor.getId())
        .titulo("Card")
        .tipo(TipoTarefa.FEATURE)
        .prioridade(PrioridadeTarefa.MEDIA)
        .build();
  }

  private void tarefaExiste(Tarefa tarefa) {
    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
  }

  private void etapasExistem(Etapa... etapas) {
    for (Etapa etapa : etapas) {
      lenient().when(etapaRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));
    }
  }

  private void primeiraEtapaEh(Etapa etapa) {
    lenient()
        .when(etapaRepository.findFirstByWorkflowIdOrderByOrdemAsc(WORKFLOW))
        .thenReturn(Optional.of(etapa));
  }

  private MoverTarefaRequest paraEtapa(Etapa destino, long versao) {
    return new MoverTarefaRequest(destino.getId(), null, versao);
  }

  @Nested
  @DisplayName("criacao (RN-CB-004/005)")
  class Criacao {

    @Test
    @DisplayName("posiciona na etapa de menor ordem e na raia padrao, sem responsavel")
    void criaComDefaults() {
      Workflow workflow = new Workflow(PROJETO, "Principal", true);
      workflow.setId(WORKFLOW);
      when(workflowRepository.findByProjetoIdAndAtivoTrue(PROJETO)).thenReturn(Optional.of(workflow));
      primeiraEtapaEh(aFazer);
      when(raiaRepository.findByProjetoIdAndPadraoTrue(PROJETO)).thenReturn(Optional.of(raiaPadrao));
      when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(chamada -> chamada.getArgument(0));

      Tarefa criada =
          tarefaService.criar(
              PROJETO,
              new CriarTarefaRequest("Novo card", null, TipoTarefa.BUG, null, null, null),
              autor);

      assertThat(criada.getEtapaId()).isEqualTo(aFazer.getId());
      assertThat(criada.getRaiaId()).isEqualTo(raiaPadrao.getId());
      assertThat(criada.getResponsavelId()).isNull();
      assertThat(criada.getPrioridade()).isEqualTo(PrioridadeTarefa.MEDIA);
      assertThat(criada.isIniciada()).isFalse();
      verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("atribuir a terceiro na criacao exige tarefa:atribuir")
    void exigeAtribuirParaTerceiro() {
      Workflow workflow = new Workflow(PROJETO, "Principal", true);
      workflow.setId(WORKFLOW);
      when(workflowRepository.findByProjetoIdAndAtivoTrue(PROJETO)).thenReturn(Optional.of(workflow));
      primeiraEtapaEh(aFazer);
      when(raiaRepository.findByProjetoIdAndPadraoTrue(PROJETO)).thenReturn(Optional.of(raiaPadrao));

      UUID terceiro = UUID.randomUUID();
      permissaoNegada(CodigoPermissao.TAREFA_ATRIBUIR);

      assertThatThrownBy(
              () ->
                  tarefaService.criar(
                      PROJETO,
                      new CriarTarefaRequest("Card", null, TipoTarefa.TAREFA, null, null, terceiro),
                      autor))
          .isInstanceOf(PermissaoNegadaException.class);
    }
  }

  @Nested
  @DisplayName("matriz de transicoes (RN-001, RN-004, RF-012)")
  class Transicoes {

    @Test
    @DisplayName("transicao configurada e aceita e inicia a tarefa na saida da primeira etapa")
    void moveComTransicaoValida() {
      Tarefa tarefa = tarefaEm(aFazer);
      tarefaExiste(tarefa);
      etapasExistem(aFazer, fazendo);
      primeiraEtapaEh(aFazer);
      when(transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(
              aFazer.getId(), fazendo.getId()))
          .thenReturn(true);
      when(notificacaoService.notificarObservadores(any(), any(), anyString(), any()))
          .thenReturn(Set.of());

      tarefaService.mover(tarefa.getId(), paraEtapa(fazendo, 0L), autor);

      assertThat(tarefa.getEtapaId()).isEqualTo(fazendo.getId());
      assertThat(tarefa.isIniciada()).isTrue();
      verify(leadTimeService).encerrarPeriodoEtapa(tarefa);
      verify(leadTimeService).abrirPeriodoEtapa(tarefa, fazendo.getId());
      verify(auditoriaService)
          .registrar(tarefa, CampoAuditado.ETAPA, "A Fazer", "Fazendo", autor);
      verificarEventoPublicado(TipoEventoBoard.TAREFA_MOVIDA);
    }

    @Test
    @DisplayName("transicao ausente no grafo e recusada e o card nao se move")
    void recusaTransicaoInexistente() {
      Tarefa tarefa = tarefaEm(aFazer);
      tarefaExiste(tarefa);
      etapasExistem(aFazer, concluido);
      when(transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(
              aFazer.getId(), concluido.getId()))
          .thenReturn(false);

      assertThatThrownBy(() -> tarefaService.mover(tarefa.getId(), paraEtapa(concluido, 0L), autor))
          .isInstanceOf(TransicaoNaoPermitidaException.class)
          .hasMessageContaining("Nao existe transicao configurada");
      assertThat(tarefa.getEtapaId()).isEqualTo(aFazer.getId());
    }

    @Test
    @DisplayName("etapa de outro workflow e recusada antes de consultar o grafo")
    void recusaEtapaDeOutroWorkflow() {
      Tarefa tarefa = tarefaEm(aFazer);
      tarefaExiste(tarefa);
      Etapa estrangeira = new Etapa(UUID.randomUUID(), "Externa", 0, false);
      estrangeira.setId(UUID.randomUUID());
      etapasExistem(aFazer, estrangeira);

      assertThatThrownBy(
              () -> tarefaService.mover(tarefa.getId(), paraEtapa(estrangeira, 0L), autor))
          .isInstanceOf(TransicaoNaoPermitidaException.class)
          .hasMessageContaining("nao pertence ao workflow");
      verify(transicaoRepository, never())
          .existsByEtapaOrigemIdAndEtapaDestinoId(any(), any());
    }

    @Test
    @DisplayName("desfinalizar so aceita predecessora direta da etapa final")
    void desfinalizaParaPredecessoraDireta() {
      Tarefa tarefa = tarefaEm(concluido);
      tarefa.setIniciada(true);
      tarefaExiste(tarefa);
      etapasExistem(concluido, fazendo);
      primeiraEtapaEh(aFazer);
      Transicao entrada = new Transicao(WORKFLOW, fazendo.getId(), concluido.getId());
      when(transicaoRepository.findByEtapaDestinoId(concluido.getId())).thenReturn(List.of(entrada));
      when(notificacaoService.notificarObservadores(any(), any(), anyString(), any()))
          .thenReturn(Set.of());

      tarefaService.mover(tarefa.getId(), paraEtapa(fazendo, 0L), autor);

      assertThat(tarefa.getEtapaId()).isEqualTo(fazendo.getId());
      verify(permissaoGuard).exigir(CodigoPermissao.TAREFA_FINALIZAR, PROJETO);
      verify(permissaoGuard)
          .exigirToggle(ChaveToggle.DEV_PODE_FINALIZAR_TAREFA, PROJETO, CodigoPapel.DEV);
    }

    @Test
    @DisplayName("desfinalizar para etapa que nao leva a final e recusado")
    void recusaDesfinalizarParaEtapaNaoPredecessora() {
      Tarefa tarefa = tarefaEm(concluido);
      tarefaExiste(tarefa);
      etapasExistem(concluido, aFazer);
      primeiraEtapaEh(aFazer);
      Transicao entrada = new Transicao(WORKFLOW, fazendo.getId(), concluido.getId());
      when(transicaoRepository.findByEtapaDestinoId(concluido.getId())).thenReturn(List.of(entrada));

      assertThatThrownBy(() -> tarefaService.mover(tarefa.getId(), paraEtapa(aFazer, 0L), autor))
          .isInstanceOf(TransicaoNaoPermitidaException.class)
          .hasMessageContaining("apenas etapas que levam diretamente a etapa final");
    }

    @Test
    @DisplayName("entrar na etapa final exige tarefa:finalizar e o toggle do papel dev (RN-011)")
    void finalizarExigePermissaoEToggle() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefaExiste(tarefa);
      etapasExistem(fazendo, concluido);
      permissaoNegada(CodigoPermissao.TAREFA_FINALIZAR);

      assertThatThrownBy(() -> tarefaService.mover(tarefa.getId(), paraEtapa(concluido, 0L), autor))
          .isInstanceOf(PermissaoNegadaException.class);
      verify(transicaoRepository, never())
          .existsByEtapaOrigemIdAndEtapaDestinoId(any(), any());
    }

    @Test
    @DisplayName("troca apenas de raia nao consulta o grafo nem abre novo periodo de etapa")
    void trocaDeRaiaNaoPassaPeloGrafo() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefaExiste(tarefa);
      etapasExistem(fazendo);
      Raia outra = new Raia(PROJETO, "Suporte", 1, false);
      definirId(outra);
      when(raiaRepository.findById(outra.getId())).thenReturn(Optional.of(outra));

      tarefaService.mover(
          tarefa.getId(), new MoverTarefaRequest(fazendo.getId(), outra.getId(), 0L), autor);

      assertThat(tarefa.getRaiaId()).isEqualTo(outra.getId());
      verify(leadTimeService, never()).abrirPeriodoEtapa(any(), any());
      verify(transicaoRepository, never())
          .existsByEtapaOrigemIdAndEtapaDestinoId(any(), any());
      verificarEventoPublicado(TipoEventoBoard.TAREFA_ATUALIZADA);
    }

    @Test
    @DisplayName("versao divergente indica board desatualizado e aborta a movimentacao")
    void recusaVersaoDivergente() {
      Tarefa tarefa = tarefaEm(aFazer);
      tarefa.setVersao(7L);
      tarefaExiste(tarefa);

      assertThatThrownBy(() -> tarefaService.mover(tarefa.getId(), paraEtapa(fazendo, 3L), autor))
          .isInstanceOf(ConflitoConcorrenciaException.class);
      assertThat(tarefa.getEtapaId()).isEqualTo(aFazer.getId());
    }

    @Test
    @DisplayName("card impedido tem o impedimento reancorado na etapa de destino (RN-002)")
    void reancoraImpedimentoAoMover() {
      Tarefa tarefa = tarefaEm(aFazer);
      tarefa.setImpedida(true);
      tarefaExiste(tarefa);
      etapasExistem(aFazer, fazendo);
      primeiraEtapaEh(aFazer);
      when(transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(
              aFazer.getId(), fazendo.getId()))
          .thenReturn(true);
      when(notificacaoService.notificarObservadores(any(), any(), anyString(), any()))
          .thenReturn(Set.of());

      tarefaService.mover(tarefa.getId(), paraEtapa(fazendo, 0L), autor);

      verify(leadTimeService).reancorarImpedimento(tarefa, fazendo.getId());
    }

    @Test
    @DisplayName("iniciada e sticky: retroceder para a primeira etapa nao reverte a flag (A-4)")
    void iniciadaNaoReverte() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefa.setIniciada(true);
      tarefaExiste(tarefa);
      etapasExistem(fazendo, aFazer);
      primeiraEtapaEh(aFazer);
      when(transicaoRepository.existsByEtapaOrigemIdAndEtapaDestinoId(
              fazendo.getId(), aFazer.getId()))
          .thenReturn(true);
      when(notificacaoService.notificarObservadores(any(), any(), anyString(), any()))
          .thenReturn(Set.of());

      tarefaService.mover(tarefa.getId(), paraEtapa(aFazer, 0L), autor);

      assertThat(tarefa.isIniciada()).isTrue();
    }

    @Test
    @DisplayName("tarefa inexistente resulta em recurso nao encontrado")
    void tarefaInexistente() {
      UUID inexistente = UUID.randomUUID();
      when(tarefaRepository.findById(inexistente)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> tarefaService.mover(inexistente, paraEtapa(fazendo, 0L), autor))
          .isInstanceOf(RecursoNaoEncontradoException.class);
    }
  }

  @Nested
  @DisplayName("trava pos-inicio (RF-003, A-3)")
  class TravaPosInicio {

    @Test
    @DisplayName("alterar descricao de tarefa iniciada sem toggle nem admin e recusado")
    void bloqueiaEstruturalAposInicio() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefa.setIniciada(true);
      tarefa.setDescricao("original");
      tarefaExiste(tarefa);
      when(projetoToggleService.habilitado(PROJETO, ChaveToggle.DEV_PODE_EDITAR_TAREFA_INICIADA))
          .thenReturn(false);
      when(permissaoGuard.possui(CodigoPermissao.PROJETO_ADMINISTRAR, PROJETO)).thenReturn(false);

      AtualizarTarefaRequest request =
          new AtualizarTarefaRequest(
              "Card", "outra", TipoTarefa.FEATURE, PrioridadeTarefa.ALTA, null);

      assertThatThrownBy(() -> tarefaService.atualizar(tarefa.getId(), request, autor))
          .isInstanceOf(TarefaIniciadaException.class);
      assertThat(tarefa.getDescricao()).isEqualTo("original");
    }

    @Test
    @DisplayName("titulo continua editavel apos o inicio porque RN-016 exige auditoria de titulo")
    void permiteEditarTituloAposInicio() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefa.setIniciada(true);
      tarefa.setDescricao("original");
      tarefaExiste(tarefa);

      tarefaService.atualizar(
          tarefa.getId(),
          new AtualizarTarefaRequest(
              "Novo titulo", "original", TipoTarefa.FEATURE, PrioridadeTarefa.ALTA, null),
          autor);

      assertThat(tarefa.getTitulo()).isEqualTo("Novo titulo");
      verify(auditoriaService)
          .registrar(tarefa, CampoAuditado.TITULO, "Card", "Novo titulo", autor);
    }

    @Test
    @DisplayName("toggle habilitado libera a edicao estrutural apos o inicio")
    void toggleLiberaEdicaoEstrutural() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefa.setIniciada(true);
      tarefaExiste(tarefa);
      when(projetoToggleService.habilitado(PROJETO, ChaveToggle.DEV_PODE_EDITAR_TAREFA_INICIADA))
          .thenReturn(true);

      tarefaService.atualizar(
          tarefa.getId(),
          new AtualizarTarefaRequest(
              "Card", "nova descricao", TipoTarefa.BUG, PrioridadeTarefa.MEDIA, null),
          autor);

      assertThat(tarefa.getDescricao()).isEqualTo("nova descricao");
      assertThat(tarefa.getTipo()).isEqualTo(TipoTarefa.BUG);
    }

    @Test
    @DisplayName("tarefa nao iniciada aceita alteracao estrutural sem consultar toggle")
    void semTravaAntesDoInicio() {
      Tarefa tarefa = tarefaEm(aFazer);
      tarefaExiste(tarefa);

      tarefaService.atualizar(
          tarefa.getId(),
          new AtualizarTarefaRequest(
              "Card", "descricao", TipoTarefa.MELHORIA, PrioridadeTarefa.BAIXA, null),
          autor);

      assertThat(tarefa.getTipo()).isEqualTo(TipoTarefa.MELHORIA);
      verify(projetoToggleService, never()).habilitado(any(), any());
    }
  }

  @Nested
  @DisplayName("atribuicao (RN-012)")
  class Atribuicao {

    @Test
    @DisplayName("autoatribuicao exige apenas tarefa:mover")
    void autoatribuicaoUsaTarefaMover() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefa.setResponsavelId(UUID.randomUUID());
      tarefaExiste(tarefa);
      when(notificacaoService.notificarObservadores(any(), any(), anyString(), any()))
          .thenReturn(Set.of());

      tarefaService.atribuir(tarefa.getId(), autor.getId(), autor);

      assertThat(tarefa.getResponsavelId()).isEqualTo(autor.getId());
      verify(permissaoGuard).exigir(CodigoPermissao.TAREFA_MOVER, PROJETO);
      verify(permissaoGuard, never()).exigir(eq(CodigoPermissao.TAREFA_ATRIBUIR), any());
    }

    @Test
    @DisplayName("atribuir a terceiro exige tarefa:atribuir")
    void atribuirTerceiroUsaTarefaAtribuir() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefaExiste(tarefa);
      permissaoNegada(CodigoPermissao.TAREFA_ATRIBUIR);

      UUID terceiro = UUID.randomUUID();
      assertThatThrownBy(() -> tarefaService.atribuir(tarefa.getId(), terceiro, autor))
          .isInstanceOf(PermissaoNegadaException.class);
      assertThat(tarefa.getResponsavelId()).isNull();
    }
  }

  @Nested
  @DisplayName("impedimento (RF-004)")
  class Impedimento {

    @Test
    @DisplayName("marcar impedimento abre periodo, audita e notifica")
    void marcaImpedimento() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefaExiste(tarefa);
      when(leadTimeService.abrirImpedimento(tarefa, "aguardando cliente"))
          .thenReturn(Optional.of(new PeriodoImpedimento()));
      when(notificacaoService.notificarObservadores(
              any(), eq(TipoNotificacao.IMPEDIMENTO_MARCADO), anyString(), any()))
          .thenReturn(Set.of());

      tarefaService.marcarImpedimento(tarefa.getId(), "aguardando cliente", autor);

      assertThat(tarefa.isImpedida()).isTrue();
      verify(auditoriaService)
          .registrar(tarefa, CampoAuditado.IMPEDIMENTO, "false", "true", autor);
      verificarEventoPublicado(TipoEventoBoard.TAREFA_IMPEDIDA);
    }

    @Test
    @DisplayName("marcar duas vezes e idempotente: nao audita nem publica de novo")
    void marcarImpedimentoEhIdempotente() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefa.setImpedida(true);
      tarefaExiste(tarefa);
      when(leadTimeService.abrirImpedimento(tarefa, null)).thenReturn(Optional.empty());

      tarefaService.marcarImpedimento(tarefa.getId(), null, autor);

      assertThat(tarefa.isImpedida()).isTrue();
      verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
      verify(eventoBoardPublisher, never()).publicar(any(), any());
    }

    @Test
    @DisplayName("desmarcar sem impedimento aberto e idempotente")
    void desmarcarImpedimentoEhIdempotente() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefaExiste(tarefa);
      when(leadTimeService.encerrarImpedimento(tarefa)).thenReturn(false);

      tarefaService.desmarcarImpedimento(tarefa.getId(), autor);

      assertThat(tarefa.isImpedida()).isFalse();
      verify(eventoBoardPublisher, never()).publicar(any(), any());
    }

    @Test
    @DisplayName("desmarcar com impedimento aberto encerra o periodo e audita")
    void desmarcaImpedimento() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefa.setImpedida(true);
      tarefaExiste(tarefa);
      when(leadTimeService.encerrarImpedimento(tarefa)).thenReturn(true);
      when(notificacaoService.notificarObservadores(
              any(), eq(TipoNotificacao.IMPEDIMENTO_DESMARCADO), anyString(), any()))
          .thenReturn(Set.of());

      tarefaService.desmarcarImpedimento(tarefa.getId(), autor);

      assertThat(tarefa.isImpedida()).isFalse();
      verify(auditoriaService)
          .registrar(tarefa, CampoAuditado.IMPEDIMENTO, "true", "false", autor);
      verificarEventoPublicado(TipoEventoBoard.TAREFA_DESIMPEDIDA);
    }
  }

  @Nested
  @DisplayName("exclusao (RF-019, RN-005)")
  class Exclusao {

    @Test
    @DisplayName("fecha os intervalos abertos antes de remover para nao contaminar as medias")
    void excluiFechandoPeriodos() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefaExiste(tarefa);

      tarefaService.excluir(tarefa.getId(), autor);

      verify(leadTimeService).encerrarTodosOsPeriodos(tarefa);
      verify(tarefaObservadorRepository).deleteByIdTarefaId(tarefa.getId());
      verify(notificacaoService).removerDaTarefa(tarefa.getId());
      verify(tarefaRepository).delete(tarefa);
      verificarEventoPublicado(TipoEventoBoard.TAREFA_EXCLUIDA);
    }

    @Test
    @DisplayName("toggle dev_pode_excluir_tarefa desabilitado bloqueia a exclusao")
    void toggleBloqueiaExclusao() {
      Tarefa tarefa = tarefaEm(fazendo);
      tarefaExiste(tarefa);
      org.mockito.Mockito.doThrow(new PermissaoNegadaException("negado"))
          .when(permissaoGuard)
          .exigirToggle(ChaveToggle.DEV_PODE_EXCLUIR_TAREFA, PROJETO, CodigoPapel.DEV);

      assertThatThrownBy(() -> tarefaService.excluir(tarefa.getId(), autor))
          .isInstanceOf(PermissaoNegadaException.class);
      verify(tarefaRepository, never()).delete(any());
    }
  }

  private void permissaoNegada(String permissao) {
    lenient()
        .doThrow(
            new PermissaoNegadaException(
                "Voce nao possui a permissao necessaria para esta acao neste projeto."))
        .when(permissaoGuard)
        .exigir(eq(permissao), any());
  }

  private void verificarEventoPublicado(TipoEventoBoard esperado) {
    ArgumentCaptor<EventoBoard> captor = ArgumentCaptor.forClass(EventoBoard.class);
    verify(eventoBoardPublisher).publicar(captor.capture(), any());
    assertThat(captor.getValue().tipo()).isEqualTo(esperado);
  }
}
