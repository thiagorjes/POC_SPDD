package br.com.crudao.kanban.evento;

/** Consumo de eventos recebidos do canal LISTEN/NOTIFY. */
public interface BoardEventListener {

  void onEvento(EnvelopeEvento envelope);
}
