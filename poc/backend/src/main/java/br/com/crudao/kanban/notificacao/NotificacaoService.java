package br.com.crudao.kanban.notificacao;

import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.tarefa.TarefaObservadorRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notificacoes <b>internas</b> (RF-005). Proibida integracao com email, Slack ou canal externo.
 * A entrega em tempo real e delegada ao publisher de eventos, que roteia por pod.
 */
@Service
@RequiredArgsConstructor
public class NotificacaoService {

  private final NotificacaoRepository notificacaoRepository;
  private final TarefaObservadorRepository tarefaObservadorRepository;
  private final EventoBoardPublisher eventoBoardPublisher;

  /**
   * Persiste uma notificacao por observador, <b>excluindo o proprio autor</b> da acao — quem agiu
   * nao precisa ser avisado do que acabou de fazer.
   */
  @Transactional
  public void notificarObservadores(Tarefa tarefa, TipoNotificacao tipo, Usuario autor) {
    List<UUID> observadores = tarefaObservadorRepository.findUsuarioIds(tarefa.getId());
    Set<UUID> destinatarios = new LinkedHashSet<>(observadores);
    destinatarios.remove(autor.getId());
    if (destinatarios.isEmpty()) {
      return;
    }

    String mensagem = mensagem(tipo, tarefa, autor);
    for (UUID destinatario : destinatarios) {
      notificacaoRepository.save(
          Notificacao.builder()
              .destinatarioId(destinatario)
              .tarefaId(tarefa.getId())
              .projetoId(tarefa.getProjetoId())
              .tipo(tipo)
              .mensagem(mensagem)
              .build());
    }

    eventoBoardPublisher.publicar(
        EventoBoard.de(
            tarefa.getProjetoId(), TipoEventoBoard.TAREFA_ATUALIZADA, tarefa.getId()),
        destinatarios);
  }

  @Transactional(readOnly = true)
  public Page<Notificacao> listar(Usuario usuario, boolean apenasNaoLidas, Pageable pageable) {
    return apenasNaoLidas
        ? notificacaoRepository.findByDestinatarioIdAndLidaEmIsNullOrderByCriadaEmDesc(
            usuario.getId(), pageable)
        : notificacaoRepository.findByDestinatarioIdOrderByCriadaEmDesc(usuario.getId(), pageable);
  }

  /** So o destinatario pode marcar a propria notificacao como lida. */
  @Transactional
  public void marcarLida(UUID id, Usuario usuario) {
    Notificacao notificacao =
        notificacaoRepository
            .findById(id)
            .orElseThrow(() -> RecursoNaoEncontradoException.de("Notificacao", id));
    if (!notificacao.getDestinatarioId().equals(usuario.getId())) {
      throw new PermissaoNegadaException("Voce nao e o destinatario desta notificacao.");
    }
    if (notificacao.getLidaEm() == null) {
      notificacao.setLidaEm(Instant.now());
    }
  }

  @Transactional
  public void removerDaTarefa(UUID tarefaId) {
    notificacaoRepository.deleteByTarefaId(tarefaId);
  }

  private String mensagem(TipoNotificacao tipo, Tarefa tarefa, Usuario autor) {
    return switch (tipo) {
      case ETAPA_ALTERADA ->
          "%s moveu a tarefa \"%s\" de etapa.".formatted(autor.getNome(), tarefa.getTitulo());
      case IMPEDIMENTO_MARCADO ->
          "%s marcou a tarefa \"%s\" como impedida.".formatted(autor.getNome(), tarefa.getTitulo());
      case IMPEDIMENTO_DESMARCADO ->
          "%s removeu o impedimento da tarefa \"%s\"."
              .formatted(autor.getNome(), tarefa.getTitulo());
    };
  }
}
