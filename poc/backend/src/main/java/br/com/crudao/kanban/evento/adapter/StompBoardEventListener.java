package br.com.crudao.kanban.evento.adapter;

import br.com.crudao.kanban.evento.BoardEventListener;
import br.com.crudao.kanban.evento.EventoBoard;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

/**
 * Entrega o evento recebido do canal para as sessoes STOMP hospedadas <b>neste</b> pod. O broadcast
 * de board vai para todos; a notificacao pessoal e filtrada pelos destinatarios que este pod de
 * fato atende — resolve o roteamento multi-pod sem broker relay (proibido por ADR-002).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompBoardEventListener implements BoardEventListener {

  private static final String TOPICO_BOARD = "/topic/board/";
  private static final String TOPICO_NOTIFICACOES = "/topic/notificacoes/";

  private final SimpMessagingTemplate messagingTemplate;
  private final SimpUserRegistry userRegistry;

  @Override
  public void onEvento(EventoBoard evento) {
    messagingTemplate.convertAndSend(TOPICO_BOARD + evento.projetoId(), evento);

    if (evento.destinatarios().isEmpty()) {
      return;
    }
    Set<String> sessoesLocais =
        userRegistry.getUsers().stream()
            .map(usuario -> usuario.getName())
            .collect(Collectors.toSet());

    for (UUID destinatario : evento.destinatarios()) {
      if (sessoesLocais.contains(destinatario.toString())) {
        messagingTemplate.convertAndSend(TOPICO_NOTIFICACOES + destinatario, evento);
      }
    }
  }
}
