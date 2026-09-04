package br.com.crudao.kanban.notificacao;

import br.com.crudao.kanban.common.ClockProvider;
import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.tarefa.TarefaObservador;
import br.com.crudao.kanban.tarefa.TarefaObservadorRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Notificacoes internas aos observadores da tarefa (RF-005). Nenhum canal externo. */
@Service
@RequiredArgsConstructor
public class NotificacaoService {

  private final NotificacaoRepository notificacaoRepository;
  private final TarefaObservadorRepository tarefaObservadorRepository;
  private final ClockProvider clockProvider;

  /**
   * Persiste uma notificacao por observador, excluindo o autor da acao, e devolve o conjunto de
   * destinatarios para que o publicador de eventos faca a entrega em tempo real.
   */
  @Transactional
  public Set<UUID> notificarObservadores(
      Tarefa tarefa, TipoNotificacao tipo, String mensagem, Usuario autor) {
    Set<UUID> destinatarios = new LinkedHashSet<>();
    List<TarefaObservador> observadores =
        tarefaObservadorRepository.findByIdTarefaId(tarefa.getId());
    for (TarefaObservador observador : observadores) {
      UUID usuarioId = observador.getId().getUsuarioId();
      if (usuarioId.equals(autor.getId())) {
        continue;
      }
      destinatarios.add(usuarioId);
    }
    for (UUID destinatario : destinatarios) {
      notificacaoRepository.save(
          new Notificacao(
              destinatario,
              tarefa.getId(),
              tarefa.getProjetoId(),
              tipo,
              mensagem,
              clockProvider.agora()));
    }
    return destinatarios;
  }

  /** Caixa de notificacoes do usuario autenticado (RF-005). */
  @Transactional(readOnly = true)
  public Page<Notificacao> listar(Usuario usuario, boolean apenasNaoLidas, Pageable pageable) {
    return apenasNaoLidas
        ? notificacaoRepository.findByDestinatarioIdAndLidaEmIsNullOrderByCriadaEmDesc(
            usuario.getId(), pageable)
        : notificacaoRepository.findByDestinatarioIdOrderByCriadaEmDesc(usuario.getId(), pageable);
  }

  /** Apenas o destinatario pode marcar a propria notificacao como lida. */
  @Transactional
  public void marcarLida(UUID id, Usuario usuario) {
    Notificacao notificacao =
        notificacaoRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Notificacao nao encontrada."));
    if (!notificacao.getDestinatarioId().equals(usuario.getId())) {
      throw new PermissaoNegadaException("A notificacao pertence a outro usuario.");
    }
    if (notificacao.getLidaEm() == null) {
      notificacao.setLidaEm(clockProvider.agora());
    }
  }

  /** Remove as notificacoes vinculadas a uma tarefa excluida (RF-019). */
  @Transactional
  public void removerDaTarefa(UUID tarefaId) {
    notificacaoRepository.deleteByTarefaId(tarefaId);
  }
}
