package br.com.crudao.kanban.evento;

import java.util.Set;
import java.util.UUID;

/** Envelope serializado no payload do NOTIFY: evento + destinatarios de notificacao pessoal. */
public record EnvelopeEvento(EventoBoard evento, Set<UUID> destinatarios) {}
