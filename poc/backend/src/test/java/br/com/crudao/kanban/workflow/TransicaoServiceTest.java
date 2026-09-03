package br.com.crudao.kanban.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Arestas do grafo de workflow (RF-002/RN-004). */
@ExtendWith(MockitoExtension.class)
class TransicaoServiceTest {

  private static final UUID PROJETO = UUID.randomUUID();
  private static final UUID WORKFLOW = UUID.randomUUID();

  @Mock private TransicaoRepository transicaoRepository;
  @Mock private EtapaRepository etapaRepository;
  @Mock private EtapaService etapaService;
  @Mock private WorkflowRepository workflowRepository;
  @Mock private EventoBoardPublisher eventoBoardPublisher;
  @InjectMocks private TransicaoService service;

  private Etapa etapa(String nome, boolean etapaFinal) {
    return etapa(nome, etapaFinal, WORKFLOW);
  }

  private Etapa etapa(String nome, boolean etapaFinal, UUID workflowId) {
    return Etapa.builder()
        .id(UUID.randomUUID())
        .workflowId(workflowId)
        .nome(nome)
        .ordem(0)
        .etapaFinal(etapaFinal)
        .build();
  }

  private void workflowResolvivel() {
    when(workflowRepository.findById(WORKFLOW))
        .thenReturn(Optional.of(Workflow.builder().id(WORKFLOW).projetoId(PROJETO).build()));
  }

  @Test
  @DisplayName("transicao valida e persistida e reconfigura o board")
  void criarTransicaoValida() {
    Etapa origem = etapa("A fazer", false);
    Etapa destino = etapa("Fazendo", false);
    when(etapaService.buscar(origem.getId())).thenReturn(origem);
    when(etapaService.buscar(destino.getId())).thenReturn(destino);
    when(transicaoRepository.existsByWorkflowIdAndEtapaOrigemIdAndEtapaDestinoId(
            WORKFLOW, origem.getId(), destino.getId()))
        .thenReturn(false);
    when(transicaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    workflowResolvivel();

    Transicao criada = service.criar(WORKFLOW, origem.getId(), destino.getId());

    assertThat(criada.getEtapaOrigemId()).isEqualTo(origem.getId());
    assertThat(criada.getEtapaDestinoId()).isEqualTo(destino.getId());
    ArgumentCaptor<EventoBoard> captor = ArgumentCaptor.forClass(EventoBoard.class);
    verify(eventoBoardPublisher).publicar(captor.capture());
    assertThat(captor.getValue().tipo()).isEqualTo(TipoEventoBoard.BOARD_RECONFIGURADO);
    assertThat(captor.getValue().projetoId()).isEqualTo(PROJETO);
  }

  @Test
  @DisplayName("self-loop e rejeitado antes de qualquer consulta")
  void criarSelfLoop() {
    UUID mesma = UUID.randomUUID();

    assertThatThrownBy(() -> service.criar(WORKFLOW, mesma, mesma))
        .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
        .hasMessage("A etapa de origem e a de destino nao podem ser a mesma.");
    verifyNoInteractions(etapaService);
  }

  @Test
  @DisplayName("etapas de workflows diferentes nao formam transicao")
  void criarEntreWorkflowsDiferentes() {
    Etapa origem = etapa("A fazer", false);
    Etapa destino = etapa("Fazendo", false, UUID.randomUUID());
    when(etapaService.buscar(origem.getId())).thenReturn(origem);
    when(etapaService.buscar(destino.getId())).thenReturn(destino);

    assertThatThrownBy(() -> service.criar(WORKFLOW, origem.getId(), destino.getId()))
        .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
        .hasMessage("As etapas da transicao devem pertencer ao mesmo workflow.");
  }

  @Test
  @DisplayName("a etapa final nao tem transicao de saida configuravel (RN-004)")
  void criarSaindoDaEtapaFinal() {
    Etapa origem = etapa("Concluido", true);
    Etapa destino = etapa("Fazendo", false);
    when(etapaService.buscar(origem.getId())).thenReturn(origem);
    when(etapaService.buscar(destino.getId())).thenReturn(destino);

    assertThatThrownBy(() -> service.criar(WORKFLOW, origem.getId(), destino.getId()))
        .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
        .hasMessage(
            "A etapa final nao possui transicao de saida configuravel. Use a operacao de"
                + " desfinalizar.");
  }

  @Test
  @DisplayName("transicao duplicada e rejeitada")
  void criarDuplicada() {
    Etapa origem = etapa("A fazer", false);
    Etapa destino = etapa("Fazendo", false);
    when(etapaService.buscar(origem.getId())).thenReturn(origem);
    when(etapaService.buscar(destino.getId())).thenReturn(destino);
    when(transicaoRepository.existsByWorkflowIdAndEtapaOrigemIdAndEtapaDestinoId(
            WORKFLOW, origem.getId(), destino.getId()))
        .thenReturn(true);

    assertThatThrownBy(() -> service.criar(WORKFLOW, origem.getId(), destino.getId()))
        .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
        .hasMessage("Esta transicao ja esta configurada.");
    verify(transicaoRepository, never()).save(any());
  }

  @Test
  @DisplayName("remover uma aresta revalida o grafo antes de reconfigurar o board")
  void excluirRevalidaOGrafo() {
    Transicao transicao =
        Transicao.builder()
            .id(UUID.randomUUID())
            .workflowId(WORKFLOW)
            .etapaOrigemId(UUID.randomUUID())
            .etapaDestinoId(UUID.randomUUID())
            .build();
    when(transicaoRepository.findById(transicao.getId())).thenReturn(Optional.of(transicao));
    workflowResolvivel();

    service.excluir(transicao.getId());

    verify(transicaoRepository).delete(transicao);
    verify(etapaService).validarWorkflow(WORKFLOW);
    verify(eventoBoardPublisher).publicar(any(EventoBoard.class));
  }

  @Test
  @DisplayName("excluir transicao inexistente resulta em recurso nao encontrado")
  void excluirInexistente() {
    UUID id = UUID.randomUUID();
    when(transicaoRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.excluir(id)).isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  @DisplayName("listar delega ao repositorio")
  void listar() {
    List<Transicao> transicoes = List.of(Transicao.builder().workflowId(WORKFLOW).build());
    when(transicaoRepository.findByWorkflowId(WORKFLOW)).thenReturn(transicoes);

    assertThat(service.listar(WORKFLOW)).isEqualTo(transicoes);
  }

  @Test
  @DisplayName("destinosPermitidos resolve as etapas alvo das arestas de saida")
  void destinosPermitidos() {
    Etapa destino = etapa("Fazendo", false);
    UUID origemId = UUID.randomUUID();
    when(transicaoRepository.findByEtapaOrigemId(origemId))
        .thenReturn(
            List.of(
                Transicao.builder()
                    .workflowId(WORKFLOW)
                    .etapaOrigemId(origemId)
                    .etapaDestinoId(destino.getId())
                    .build()));
    when(etapaRepository.findAllById(List.of(destino.getId()))).thenReturn(List.of(destino));

    assertThat(service.destinosPermitidos(origemId)).containsExactly(destino);
  }

  @Test
  @DisplayName("etapa sem aresta de saida nao consulta o repositorio de etapas")
  void destinosPermitidosVazio() {
    UUID origemId = UUID.randomUUID();
    when(transicaoRepository.findByEtapaOrigemId(origemId)).thenReturn(List.of());

    assertThat(service.destinosPermitidos(origemId)).isEmpty();
    verifyNoInteractions(etapaRepository);
  }
}
