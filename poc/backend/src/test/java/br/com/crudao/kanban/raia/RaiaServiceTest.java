package br.com.crudao.kanban.raia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.common.RecursoEmUsoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.raia.dto.CriarRaiaRequest;
import br.com.crudao.kanban.tarefa.TarefaRepository;
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

/** Raia e agrupamento visual (RF-011): sem regra de transicao, mas com invariante de raia padrao. */
@ExtendWith(MockitoExtension.class)
class RaiaServiceTest {

  private static final UUID PROJETO = UUID.randomUUID();

  @Mock private RaiaRepository raiaRepository;
  @Mock private TarefaRepository tarefaRepository;
  @Mock private EventoBoardPublisher eventoBoardPublisher;
  @InjectMocks private RaiaService service;

  private Raia raia(boolean padrao) {
    return Raia.builder()
        .id(UUID.randomUUID())
        .projetoId(PROJETO)
        .nome("Time A")
        .ordem(1)
        .padrao(padrao)
        .build();
  }

  @Test
  @DisplayName("a primeira raia do projeto nasce como padrao")
  void primeiraRaiaEPadrao() {
    when(raiaRepository.countByProjetoId(PROJETO)).thenReturn(0L);
    when(raiaRepository.findMaiorOrdem(PROJETO)).thenReturn(0);
    when(raiaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Raia criada = service.criar(PROJETO, new CriarRaiaRequest("Time A"));

    assertThat(criada.isPadrao()).isTrue();
    assertThat(criada.getOrdem()).isEqualTo(1);
  }

  @Test
  @DisplayName("raia seguinte nao e padrao e recebe a proxima ordem")
  void raiaSeguinteNaoEPadrao() {
    when(raiaRepository.countByProjetoId(PROJETO)).thenReturn(2L);
    when(raiaRepository.findMaiorOrdem(PROJETO)).thenReturn(2);
    when(raiaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Raia criada = service.criar(PROJETO, new CriarRaiaRequest("Time B"));

    assertThat(criada.isPadrao()).isFalse();
    assertThat(criada.getOrdem()).isEqualTo(3);
  }

  @Test
  @DisplayName("qualquer mutacao de raia reconfigura o board dos clientes conectados")
  void criarPublicaReconfiguracao() {
    when(raiaRepository.countByProjetoId(PROJETO)).thenReturn(0L);
    when(raiaRepository.findMaiorOrdem(PROJETO)).thenReturn(0);
    when(raiaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.criar(PROJETO, new CriarRaiaRequest("Time A"));

    ArgumentCaptor<EventoBoard> captor = ArgumentCaptor.forClass(EventoBoard.class);
    verify(eventoBoardPublisher).publicar(captor.capture());
    assertThat(captor.getValue().tipo()).isEqualTo(TipoEventoBoard.BOARD_RECONFIGURADO);
    assertThat(captor.getValue().projetoId()).isEqualTo(PROJETO);
  }

  @Test
  @DisplayName("renomear altera o nome e reconfigura o board")
  void renomear() {
    Raia raia = raia(true);
    when(raiaRepository.findById(raia.getId())).thenReturn(Optional.of(raia));

    assertThat(service.renomear(raia.getId(), "Suporte").getNome()).isEqualTo("Suporte");
    verify(eventoBoardPublisher).publicar(any(EventoBoard.class));
  }

  @Test
  @DisplayName("definirPadrao remove a flag da raia anterior: existe exatamente uma padrao")
  void definirPadraoTrocaAFlag() {
    Raia anterior = raia(true);
    Raia nova = raia(false);
    when(raiaRepository.findById(nova.getId())).thenReturn(Optional.of(nova));
    when(raiaRepository.findByProjetoIdAndPadraoTrue(PROJETO)).thenReturn(Optional.of(anterior));

    service.definirPadrao(nova.getId());

    assertThat(anterior.isPadrao()).isFalse();
    assertThat(nova.isPadrao()).isTrue();
    verify(raiaRepository).saveAndFlush(anterior);
  }

  @Test
  @DisplayName("definirPadrao na raia que ja e padrao e um no-op sem evento")
  void definirPadraoIdempotente() {
    Raia raia = raia(true);
    when(raiaRepository.findById(raia.getId())).thenReturn(Optional.of(raia));

    assertThat(service.definirPadrao(raia.getId())).isSameAs(raia);
    verify(eventoBoardPublisher, never()).publicar(any(EventoBoard.class));
  }

  @Test
  @DisplayName("raia com tarefa ativa nao pode ser excluida")
  void excluirComTarefaAtiva() {
    Raia raia = raia(false);
    when(raiaRepository.findById(raia.getId())).thenReturn(Optional.of(raia));
    when(tarefaRepository.contarAtivasNaRaia(raia.getId())).thenReturn(3L);

    assertThatThrownBy(() -> service.excluir(raia.getId()))
        .isInstanceOf(RecursoEmUsoException.class)
        .hasMessage("A raia possui 3 tarefa(s) ativa(s) e nao pode ser excluida.");
    verify(raiaRepository, never()).delete(any());
  }

  @Test
  @DisplayName("raia com tarefa historica preserva o vinculo e nao pode ser excluida")
  void excluirComTarefaHistorica() {
    Raia raia = raia(false);
    when(raiaRepository.findById(raia.getId())).thenReturn(Optional.of(raia));
    when(tarefaRepository.contarAtivasNaRaia(raia.getId())).thenReturn(0L);
    when(tarefaRepository.countByRaiaId(raia.getId())).thenReturn(5L);

    assertThatThrownBy(() -> service.excluir(raia.getId()))
        .isInstanceOf(RecursoEmUsoException.class)
        .hasMessage("A raia possui tarefas historicas vinculadas e nao pode ser excluida.");
  }

  @Test
  @DisplayName("a raia padrao nao e removivel enquanto houver outra raia no projeto")
  void excluirPadraoComOutrasRaias() {
    Raia raia = raia(true);
    when(raiaRepository.findById(raia.getId())).thenReturn(Optional.of(raia));
    when(tarefaRepository.contarAtivasNaRaia(raia.getId())).thenReturn(0L);
    when(tarefaRepository.countByRaiaId(raia.getId())).thenReturn(0L);
    when(raiaRepository.countByProjetoId(PROJETO)).thenReturn(2L);

    assertThatThrownBy(() -> service.excluir(raia.getId()))
        .isInstanceOf(RecursoEmUsoException.class)
        .hasMessage("A raia padrao nao pode ser excluida. Defina outra raia como padrao antes.");
  }

  @Test
  @DisplayName("raia sem tarefa alguma e removida e o board e reconfigurado")
  void excluirRaiaLivre() {
    Raia raia = raia(false);
    when(raiaRepository.findById(raia.getId())).thenReturn(Optional.of(raia));
    when(tarefaRepository.contarAtivasNaRaia(raia.getId())).thenReturn(0L);
    when(tarefaRepository.countByRaiaId(raia.getId())).thenReturn(0L);

    service.excluir(raia.getId());

    verify(raiaRepository).delete(raia);
    verify(eventoBoardPublisher).publicar(any(EventoBoard.class));
  }

  @Test
  @DisplayName("buscar raia inexistente resulta em recurso nao encontrado")
  void buscarInexistente() {
    UUID id = UUID.randomUUID();
    when(raiaRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(id)).isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  @DisplayName("listar devolve as raias na ordem configurada")
  void listarOrdenado() {
    List<Raia> raias = List.of(raia(true));
    when(raiaRepository.findByProjetoIdOrderByOrdemAsc(PROJETO)).thenReturn(raias);

    assertThat(service.listar(PROJETO)).isEqualTo(raias);
  }

  @Test
  @DisplayName("padraoDoProjeto expoe a raia default para criacao de tarefa")
  void padraoDoProjeto() {
    Raia raia = raia(true);
    when(raiaRepository.findByProjetoIdAndPadraoTrue(PROJETO)).thenReturn(Optional.of(raia));

    assertThat(service.padraoDoProjeto(PROJETO)).contains(raia);
  }
}
