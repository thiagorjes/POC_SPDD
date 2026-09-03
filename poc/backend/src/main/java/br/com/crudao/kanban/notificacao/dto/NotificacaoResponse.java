package br.com.crudao.kanban.notificacao.dto;

import br.com.crudao.kanban.notificacao.Notificacao;
import br.com.crudao.kanban.notificacao.TipoNotificacao;
import java.time.Instant;
import java.util.UUID;

public record NotificacaoResponse(
    UUID id,
    UUID projetoId,
    UUID tarefaId,
    TipoNotificacao tipo,
    String mensagem,
    Instant criadaEm,
    Instant lidaEm) {

  public static NotificacaoResponse de(Notificacao notificacao) {
    return new NotificacaoResponse(
        notificacao.getId(),
        notificacao.getProjetoId(),
        notificacao.getTarefaId(),
        notificacao.getTipo(),
        notificacao.getMensagem(),
        notificacao.getCriadaEm(),
        notificacao.getLidaEm());
  }
}
