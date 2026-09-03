package br.com.crudao.kanban.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.common.RecursoEmUsoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import br.com.crudao.kanban.workflow.dto.CriarWorkflowRequest;
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

/** Exatamente um workflow ativo por projeto (A-9), com troca bloqueada por tarefa ativa (RN-005). */
@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

  private static final UUID PROJETO = UUID.randomUUID();

  @Mock private WorkflowRepository workflowRepository;
  @Mock private EtapaService etapaService;
  @Mock private TarefaRepository tarefaRepository;
  @Mock private EventoBoardPublisher eventoBoardPublisher;
  @InjectMocks private WorkflowService service;

  private Workflow workflow(boolean ativo) {
    return Workflow.builder()
        .id(UUID.randomUUID())
        .projetoId(PROJETO)
        .nome("Padrao")
        .ativo(ativo)
        .build();
  }

  @Test
  @DisplayName("o primeiro workflow do projeto nasce ativo e reconfigura o board")
  void primeiroWorkflowNasceAtivo() {
    when(workflowRepository.existsByProjetoId(PROJETO)).thenReturn(false);
    when(workflowRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Workflow criado = service.criar(PROJETO, new CriarWorkflowRequest("Padrao"));

    assertThat(criado.isAtivo()).isTrue();
    ArgumentCaptor<EventoBoard> captor = ArgumentCaptor.forClass(EventoBoard.class);
    verify(eventoBoardPublisher).publicar(captor.capture());
    assertThat(captor.getValue().tipo()).isEqualTo(TipoEventoBoard.BOARD_RECONFIGURADO);
  }

  @Test
  @DisplayName("workflow adicional nasce inativo e nao altera o board em uso")
  void workflowSeguinteNasceInativo() {
    when(workflowRepository.existsByProjetoId(PROJETO)).thenReturn(true);
    when(workflowRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    assertThat(service.criar(PROJETO, new CriarWorkflowRequest("Alternativo")).isAtivo()).isFalse();
    verifyNoInteractions(eventoBoardPublisher);
  }

  @Test
  @DisplayName("ativar o workflow que ja esta ativo e um no-op sem revalidacao")
  void ativarJaAtivo() {
    Workflow ativo = workflow(true);
    when(workflowRepository.findById(ativo.getId())).thenReturn(Optional.of(ativo));

    assertThat(service.ativar(ativo.getId())).isSameAs(ativo);
    verifyNoInteractions(etapaService);
    verifyNoInteractions(eventoBoardPublisher);
  }

  @Test
  @DisplayName("a troca de workflow exige que o grafo do novo workflow seja valido")
  void ativarValidaOGrafo() {
    Workflow novo = workflow(false);
    when(workflowRepository.findById(novo.getId())).thenReturn(Optional.of(novo));
    when(workflowRepository.findByProjetoIdAndAtivoTrue(PROJETO)).thenReturn(Optional.empty());

    service.ativar(novo.getId());

    verify(etapaService).validarWorkflow(novo.getId());
    assertThat(novo.isAtivo()).isTrue();
    verify(eventoBoardPublisher).publicar(any(EventoBoard.class));
  }

  @Test
  @DisplayName("trocar de workflow com tarefa ativa deixaria a tarefa em etapa orfa: bloqueado")
  void ativarComTarefaAtivaNoCorrente() {
    Workflow novo = workflow(false);
    Workflow corrente = workflow(true);
    when(workflowRepository.findById(novo.getId())).thenReturn(Optional.of(novo));
    when(workflowRepository.findByProjetoIdAndAtivoTrue(PROJETO)).thenReturn(Optional.of(corrente));
    when(tarefaRepository.contarAtivasNoWorkflow(corrente.getId())).thenReturn(4L);

    assertThatThrownBy(() -> service.ativar(novo.getId()))
        .isInstanceOf(RecursoEmUsoException.class)
        .hasMessage(
            "O workflow atual possui 4 tarefa(s) ativa(s). Conclua ou mova essas tarefas antes de"
                + " trocar o workflow.");
    assertThat(novo.isAtivo()).isFalse();
    assertThat(corrente.isAtivo()).isTrue();
  }

  @Test
  @DisplayName("a troca desativa o workflow corrente na mesma transacao")
  void ativarDesativaOCorrente() {
    Workflow novo = workflow(false);
    Workflow corrente = workflow(true);
    when(workflowRepository.findById(novo.getId())).thenReturn(Optional.of(novo));
    when(workflowRepository.findByProjetoIdAndAtivoTrue(PROJETO)).thenReturn(Optional.of(corrente));
    when(tarefaRepository.contarAtivasNoWorkflow(corrente.getId())).thenReturn(0L);

    service.ativar(novo.getId());

    assertThat(corrente.isAtivo()).isFalse();
    assertThat(novo.isAtivo()).isTrue();
    verify(workflowRepository).saveAndFlush(corrente);
  }

  @Test
  @DisplayName("workflow com tarefa ativa nao pode ser excluido")
  void excluirComTarefaAtiva() {
    Workflow workflow = workflow(true);
    when(workflowRepository.findById(workflow.getId())).thenReturn(Optional.of(workflow));
    when(tarefaRepository.contarAtivasNoWorkflow(workflow.getId())).thenReturn(2L);

    assertThatThrownBy(() -> service.excluir(workflow.getId()))
        .isInstanceOf(RecursoEmUsoException.class)
        .hasMessage("O workflow possui 2 tarefa(s) ativa(s) e nao pode ser excluido.");
    verify(workflowRepository, never()).delete(any());
  }

  @Test
  @DisplayName("workflow sem tarefa ativa e excluido e o board e reconfigurado")
  void excluirWorkflowLivre() {
    Workflow workflow = workflow(false);
    when(workflowRepository.findById(workflow.getId())).thenReturn(Optional.of(workflow));
    when(tarefaRepository.contarAtivasNoWorkflow(workflow.getId())).thenReturn(0L);

    service.excluir(workflow.getId());

    verify(workflowRepository).delete(workflow);
    verify(eventoBoardPublisher).publicar(any(EventoBoard.class));
  }

  @Test
  @DisplayName("buscar workflow inexistente resulta em recurso nao encontrado")
  void buscarInexistente() {
    UUID id = UUID.randomUUID();
    when(workflowRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(id)).isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  @DisplayName("listar e ativoDoProjeto delegam ao repositorio")
  void consultas() {
    Workflow workflow = workflow(true);
    when(workflowRepository.findByProjetoIdOrderByNomeAsc(PROJETO)).thenReturn(List.of(workflow));
    when(workflowRepository.findByProjetoIdAndAtivoTrue(PROJETO)).thenReturn(Optional.of(workflow));

    assertThat(service.listar(PROJETO)).containsExactly(workflow);
    assertThat(service.ativoDoProjeto(PROJETO)).contains(workflow);
  }
}
