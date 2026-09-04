package br.com.crudao.kanban.common;

import org.springframework.http.HttpStatus;

/** Exclusao de recurso com tarefa ativa vinculada (RN-005). */
public class RecursoEmUsoException extends BusinessException {

  public RecursoEmUsoException(String message) {
    super("RECURSO_POSSUI_TAREFAS_ATIVAS", message, HttpStatus.CONFLICT);
  }
}
