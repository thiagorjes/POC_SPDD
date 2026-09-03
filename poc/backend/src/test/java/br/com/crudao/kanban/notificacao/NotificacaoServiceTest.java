package br.com.crudao.kanban.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.tarefa.TarefaObservadorRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Notificacoes internas (RF-005): nada sai para canal externo e o autor nunca se auto-notifica. */
@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

  private static final UUID PROJETO = UUID.randomUUID();

  @Mock private NotificacaoRepository notificacaoRepository;
  @Mock private TarefaObservadorRepository tarefaObservadorRepository;
  @Mock private EventoBoardPublisher eventoBoardPublisher;
  @InjectMocks private NotificacaoService service;

  private final Usuario autor =
      Usuario.builder().id(UUID.randomUUID()).nome("Ana").email("ana@exemplo.test").build();

  private Tarefa tarefa() {
    return Tarefa.builder()
        .id(UUID.randomUUID())
        .projetoId(PROJETO)
        .titulo("Ajustar login")
        .build();
  }

  @Test
  @DisplayName("uma notificacao por observador, e o autor da acao e excluido da lista")
  void notificaObservadoresExcetoAutor() {
    Tarefa tarefa = tarefa();
    UUID observador = UUID.randomUUID();
    when(tarefaObservadorRepository.findUsuarioIds(tarefa.getId()))
        .thenReturn(List.of(observador, autor.getId()));

    service.notificarObservadores(tarefa, TipoNotificacao.ETAPA_ALTERADA, autor);

    ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
    verify(notificacaoRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().getDestinatarioId()).isEqualTo(observador);
    assertThat(captor.getValue().getTarefaId()).isEqualTo(tarefa.getId());
    assertThat(captor.getValue().getProjetoId()).isEqualTo(PROJETO);
    assertThat(captor.getValue().getMensagem()).isEqualTo("Ana moveu a tarefa \"Ajustar login\" de etapa.");
  }

  @Test
  @DisplayName("o evento e roteado apenas para os destinatarios da notificacao")
  void publicaEventoDirecionado() {
    Tarefa tarefa = tarefa();
    UUID observador = UUID.randomUUID();
    when(tarefaObservadorRepository.findUsuarioIds(tarefa.getId())).thenReturn(List.of(observador));

    service.notificarObservadores(tarefa, TipoNotificacao.IMPEDIMENTO_MARCADO, autor);

    ArgumentCaptor<Set<UUID>> destinatarios = ArgumentCaptor.captor();
    verify(eventoBoardPublisher).publicar(any(EventoBoard.class), destinatarios.capture());
    assertThat(destinatarios.getValue()).containsExactly(observador);
  }

  @Test
  @DisplayName("quando o unico observador e o proprio autor nada e persistido nem publicado")
  void semDestinatariosNaoNotifica() {
    Tarefa tarefa = tarefa();
    when(tarefaObservadorRepository.findUsuarioIds(tarefa.getId())).thenReturn(List.of(autor.getId()));

    service.notificarObservadores(tarefa, TipoNotificacao.ETAPA_ALTERADA, autor);

    verifyNoInteractions(notificacaoRepository);
    verify(eventoBoardPublisher, never()).publicar(any(), anySet());
  }

  @Test
  @DisplayName("a mensagem descreve a acao de impedimento em portugues do dominio")
  void mensagemDeImpedimento() {
    Tarefa tarefa = tarefa();
    when(tarefaObservadorRepository.findUsuarioIds(tarefa.getId()))
        .thenReturn(List.of(UUID.randomUUID()));

    service.notificarObservadores(tarefa, TipoNotificacao.IMPEDIMENTO_DESMARCADO, autor);

    ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
    verify(notificacaoRepository).save(captor.capture());
    assertThat(captor.getValue().getMensagem())
        .isEqualTo("Ana removeu o impedimento da tarefa \"Ajustar login\".");
  }

  @Test
  @DisplayName("listar respeita o filtro de nao lidas")
  void listarApenasNaoLidas() {
    Pageable pagina = PageRequest.of(0, 20);
    when(notificacaoRepository.findByDestinatarioIdAndLidaEmIsNullOrderByCriadaEmDesc(
            autor.getId(), pagina))
        .thenReturn(Page.empty());

    assertThat(service.listar(autor, true, pagina)).isEmpty();
    verify(notificacaoRepository, never())
        .findByDestinatarioIdOrderByCriadaEmDesc(any(), any());
  }

  @Test
  @DisplayName("listar sem filtro devolve todo o historico do destinatario")
  void listarTodas() {
    Pageable pagina = PageRequest.of(0, 20);
    when(notificacaoRepository.findByDestinatarioIdOrderByCriadaEmDesc(autor.getId(), pagina))
        .thenReturn(Page.empty());

    assertThat(service.listar(autor, false, pagina)).isEmpty();
  }

  @Test
  @DisplayName("marcarLida carimba o momento da leitura")
  void marcarLida() {
    Notificacao notificacao =
        Notificacao.builder().id(UUID.randomUUID()).destinatarioId(autor.getId()).build();
    when(notificacaoRepository.findById(notificacao.getId())).thenReturn(Optional.of(notificacao));

    service.marcarLida(notificacao.getId(), autor);

    assertThat(notificacao.getLidaEm()).isNotNull();
  }

  @Test
  @DisplayName("marcarLida e idempotente: nao reescreve o momento da primeira leitura")
  void marcarLidaIdempotente() {
    Instant primeira = Instant.parse("2026-01-01T10:00:00Z");
    Notificacao notificacao =
        Notificacao.builder()
            .id(UUID.randomUUID())
            .destinatarioId(autor.getId())
            .lidaEm(primeira)
            .build();
    when(notificacaoRepository.findById(notificacao.getId())).thenReturn(Optional.of(notificacao));

    service.marcarLida(notificacao.getId(), autor);

    assertThat(notificacao.getLidaEm()).isEqualTo(primeira);
  }

  @Test
  @DisplayName("quem nao e o destinatario nao marca a notificacao como lida")
  void marcarLidaDeOutroUsuario() {
    Notificacao notificacao =
        Notificacao.builder().id(UUID.randomUUID()).destinatarioId(UUID.randomUUID()).build();
    when(notificacaoRepository.findById(notificacao.getId())).thenReturn(Optional.of(notificacao));

    assertThatThrownBy(() -> service.marcarLida(notificacao.getId(), autor))
        .isInstanceOf(PermissaoNegadaException.class)
        .hasMessage("Voce nao e o destinatario desta notificacao.");
    assertThat(notificacao.getLidaEm()).isNull();
  }

  @Test
  @DisplayName("notificacao inexistente resulta em recurso nao encontrado")
  void marcarLidaInexistente() {
    UUID id = UUID.randomUUID();
    when(notificacaoRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.marcarLida(id, autor))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  @DisplayName("excluir a tarefa remove as notificacoes vinculadas")
  void removerDaTarefa() {
    UUID tarefaId = UUID.randomUUID();

    service.removerDaTarefa(tarefaId);

    verify(notificacaoRepository).deleteByTarefaId(tarefaId);
  }
}
