package br.com.crudao.kanban.common;

import org.springframework.http.HttpStatus;

/** Alteracao de campo estrutural de tarefa ja iniciada (RF-003, A-3). */
public class TarefaIniciadaException extends BusinessException {

  public TarefaIniciadaException(String message) {
    super("TAREFA_INICIADA_CAMPO_BLOQUEADO", message, HttpStatus.UNPROCESSABLE_ENTITY);
  }
}
