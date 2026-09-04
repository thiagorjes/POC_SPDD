package br.com.crudao.kanban.notificacao.dto;

import br.com.crudao.kanban.notificacao.TipoNotificacao;
import java.time.Instant;
import java.util.UUID;

/** Notificacao interna do usuario (RF-005). Nenhum canal externo (Safeguards, secao 4). */
public record NotificacaoResponse(
    UUID id,
    UUID tarefaId,
    UUID projetoId,
    TipoNotificacao tipo,
    String mensagem,
    Instant criadaEm,
    Instant lidaEm) {}
