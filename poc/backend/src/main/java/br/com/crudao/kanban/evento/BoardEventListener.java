package br.com.crudao.kanban.evento;

/** Consumo de eventos recebidos do canal do PostgreSQL. */
public interface BoardEventListener {

  void onEvento(EventoBoard evento);
}
