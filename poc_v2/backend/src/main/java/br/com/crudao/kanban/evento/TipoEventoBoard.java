package br.com.crudao.kanban.evento;

/** Tipos de evento propagados no canal de board. */
public enum TipoEventoBoard {
  TAREFA_CRIADA,
  TAREFA_MOVIDA,
  TAREFA_ATUALIZADA,
  TAREFA_EXCLUIDA,
  TAREFA_IMPEDIDA,
  TAREFA_DESIMPEDIDA,
  BOARD_RECONFIGURADO,
  PROJETO_STATUS_ALTERADO
}
