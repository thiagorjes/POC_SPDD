package br.com.crudao.kanban.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.common.RecursoEmUsoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import br.com.crudao.kanban.workflow.dto.CriarEtapaRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Etapas do workflow (RF-010) e o invariante estrutural RN-003/RN-004. */
@ExtendWith(MockitoExtension.class)
class EtapaServiceTest {

  private static final UUID PROJETO = UUID.randomUUID();
  private static final UUID WORKFLOW = UUID.randomUUID();

  @Mock private EtapaRepository etapaRepository;
  @Mock private TransicaoRepository transicaoRepository;
  @Mock private WorkflowRepository workflowRepository;
  @Mock private TarefaRepository tarefaRepository;
  @Mock private EventoBoardPublisher eventoBoardPublisher;
  @InjectMocks private EtapaService service;

  private Etapa etapa(String nome, int ordem, boolean etapaFinal) {
    return Etapa.builder()
        .id(UUID.randomUUID())
        .workflowId(WORKFLOW)
        .nome(nome)
        .ordem(ordem)
        .etapaFinal(etapaFinal)
        .build();
  }

  private Transicao aresta(Etapa origem, Etapa destino) {
    return Transicao.builder()
        .id(UUID.randomUUID())
        .workflowId(WORKFLOW)
        .etapaOrigemId(origem.getId())
        .etapaDestinoId(destino.getId())
        .build();
  }

  private void workflowResolvivel() {
    when(workflowRepository.findById(WORKFLOW))
        .thenReturn(Optional.of(Workflow.builder().id(WORKFLOW).projetoId(PROJETO).build()));
  }

  @Test
  @DisplayName("nova etapa entra ao final da ordem e reconfigura o board")
  void criarNoFimDaOrdem() {
    when(etapaRepository.findMaiorOrdem(WORKFLOW)).thenReturn(2);
    when(etapaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    workflowResolvivel();

    Etapa criada = service.criar(WORKFLOW, new CriarEtapaRequest("Revisao", false));

    assertThat(criada.getOrdem()).isEqualTo(3);
    assertThat(criada.isEtapaFinal()).isFalse();
    verify(eventoBoardPublisher).publicar(any(EventoBoard.class));
  }

  @Test
  @DisplayName("no maximo uma etapa final por workflow (RN-004)")
  void criarSegundaEtapaFinal() {
    CriarEtapaRequest request = new CriarEtapaRequest("Entregue", true);
    when(etapaRepository.findByWorkflowIdAndEtapaFinalTrue(WORKFLOW))
        .thenReturn(Optional.of(etapa("Concluido", 2, true)));

    assertThatThrownBy(() -> service.criar(WORKFLOW, request))
        .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
        .hasMessage("O workflow ja possui uma etapa final. Remova a marcacao da etapa atual antes.");
    verify(etapaRepository, never()).save(any());
  }

  @Test
  @DisplayName("renomear altera o nome e reconfigura o board")
  void renomear() {
    Etapa etapa = etapa("Fazendo", 1, false);
    when(etapaRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));
    workflowResolvivel();

    assertThat(service.renomear(etapa.getId(), "Em andamento").getNome()).isEqualTo("Em andamento");
    verify(eventoBoardPublisher).publicar(any(EventoBoard.class));
  }

  @Test
  @DisplayName("reordenar reescreve apenas a ordem, sem tocar no grafo de transicoes")
  void reordenar() {
    Etapa primeira = etapa("A fazer", 0, false);
    Etapa segunda = etapa("Fazendo", 1, false);
    when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(WORKFLOW))
        .thenReturn(List.of(primeira, segunda));
    workflowResolvivel();

    service.reordenar(WORKFLOW, List.of(segunda.getId(), primeira.getId()));

    assertThat(segunda.getOrdem()).isZero();
    assertThat(primeira.getOrdem()).isEqualTo(1);
    verify(etapaRepository, never()).delete(any());
    verify(transicaoRepository, never()).deleteByEtapaOrigemIdOrEtapaDestinoId(any(), any());
  }

  @Test
  @DisplayName("reordenar com conjunto de etapas divergente e rejeitado")
  void reordenarConjuntoDivergente() {
    Etapa primeira = etapa("A fazer", 0, false);
    Etapa segunda = etapa("Fazendo", 1, false);
    List<UUID> ordem = List.of(primeira.getId());
    when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(WORKFLOW))
        .thenReturn(List.of(primeira, segunda));

    assertThatThrownBy(() -> service.reordenar(WORKFLOW, ordem))
        .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
        .hasMessage("A nova ordem deve conter exatamente todas as etapas do workflow.");
  }

  @Test
  @DisplayName("etapa com tarefa ativa nao pode ser excluida")
  void excluirComTarefaAtiva() {
    Etapa etapa = etapa("Fazendo", 1, false);
    when(etapaRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));
    when(tarefaRepository.contarAtivasNaEtapa(etapa.getId())).thenReturn(2L);

    assertThatThrownBy(() -> service.excluir(etapa.getId()))
        .isInstanceOf(RecursoEmUsoException.class)
        .hasMessage("A etapa possui 2 tarefa(s) ativa(s) e nao pode ser excluida.");
  }

  @Test
  @DisplayName("etapa com tarefa historica preserva o vinculo e nao pode ser excluida")
  void excluirComTarefaHistorica() {
    Etapa etapa = etapa("Fazendo", 1, false);
    when(etapaRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));
    when(tarefaRepository.contarAtivasNaEtapa(etapa.getId())).thenReturn(0L);
    when(tarefaRepository.countByEtapaId(etapa.getId())).thenReturn(7L);

    assertThatThrownBy(() -> service.excluir(etapa.getId()))
        .isInstanceOf(RecursoEmUsoException.class)
        .hasMessage("A etapa possui tarefas historicas vinculadas e nao pode ser excluida.");
  }

  @Test
  @DisplayName("excluir remove as arestas incidentes e revalida o grafo restante")
  void excluirLimpaArestas() {
    Etapa alvo = etapa("Revisao", 1, false);
    Etapa inicial = etapa("A fazer", 0, false);
    Etapa fim = etapa("Concluido", 2, true);
    when(etapaRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
    when(tarefaRepository.contarAtivasNaEtapa(alvo.getId())).thenReturn(0L);
    when(tarefaRepository.countByEtapaId(alvo.getId())).thenReturn(0L);
    when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(WORKFLOW))
        .thenReturn(List.of(inicial, fim));
    when(transicaoRepository.findByWorkflowId(WORKFLOW)).thenReturn(List.of(aresta(inicial, fim)));
    workflowResolvivel();

    service.excluir(alvo.getId());

    verify(transicaoRepository).deleteByEtapaOrigemIdOrEtapaDestinoId(alvo.getId(), alvo.getId());
    verify(etapaRepository).delete(alvo);
    verify(eventoBoardPublisher).publicar(any(EventoBoard.class));
  }

  @Test
  @DisplayName("workflow sem nenhuma etapa e invalido")
  void validarSemEtapas() {
    when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(WORKFLOW)).thenReturn(List.of());

    assertThatThrownBy(() -> service.validarWorkflow(WORKFLOW))
        .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
        .hasMessage("O workflow deve possuir ao menos uma etapa.");
  }

  @Test
  @DisplayName("workflow sem etapa final e invalido")
  void validarSemEtapaFinal() {
    when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(WORKFLOW))
        .thenReturn(List.of(etapa("A fazer", 0, false)));

    assertThatThrownBy(() -> service.validarWorkflow(WORKFLOW))
        .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
        .hasMessage("O workflow deve possuir exatamente uma etapa final.");
  }

  @Test
  @DisplayName("etapa nao-final sem transicao de saida invalida o workflow (RN-003)")
  void validarEtapaSemSaida() {
    Etapa inicial = etapa("A fazer", 0, false);
    Etapa fim = etapa("Concluido", 1, true);
    when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(WORKFLOW)).thenReturn(List.of(inicial, fim));
    when(transicaoRepository.findByWorkflowId(WORKFLOW)).thenReturn(List.of());

    assertThatThrownBy(() -> service.validarWorkflow(WORKFLOW))
        .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
        .hasMessage("A etapa \"A fazer\" nao possui nenhuma transicao de saida configurada.");
  }

  @Test
  @DisplayName("etapa inalcancavel a partir da primeira invalida o workflow")
  void validarEtapaInalcancavel() {
    Etapa inicial = etapa("A fazer", 0, false);
    Etapa fim = etapa("Concluido", 1, true);
    Etapa orfa = etapa("Ilha", 2, false);
    when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(WORKFLOW))
        .thenReturn(List.of(inicial, fim, orfa));
    when(transicaoRepository.findByWorkflowId(WORKFLOW))
        .thenReturn(List.of(aresta(inicial, fim), aresta(orfa, fim)));

    assertThatThrownBy(() -> service.validarWorkflow(WORKFLOW))
        .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
        .hasMessage("A etapa \"Ilha\" e inalcancavel a partir da primeira etapa do workflow.");
  }

  @Test
  @DisplayName("grafo conexo com exatamente uma etapa final e valido")
  void validarGrafoValido() {
    Etapa inicial = etapa("A fazer", 0, false);
    Etapa meio = etapa("Fazendo", 1, false);
    Etapa fim = etapa("Concluido", 2, true);
    when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(WORKFLOW))
        .thenReturn(List.of(inicial, meio, fim));
    when(transicaoRepository.findByWorkflowId(WORKFLOW))
        .thenReturn(List.of(aresta(inicial, meio), aresta(meio, fim)));

    assertThatCode(() -> service.validarWorkflow(WORKFLOW)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("buscar etapa inexistente resulta em recurso nao encontrado")
  void buscarInexistente() {
    UUID id = UUID.randomUUID();
    when(etapaRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(id)).isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  @DisplayName("listar devolve as etapas na ordem configurada")
  void listar() {
    List<Etapa> etapas = List.of(etapa("A fazer", 0, false));
    when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(WORKFLOW)).thenReturn(etapas);

    assertThat(service.listar(WORKFLOW)).isEqualTo(etapas);
  }
}
