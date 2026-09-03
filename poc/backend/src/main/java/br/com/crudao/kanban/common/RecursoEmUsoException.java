package br.com.crudao.kanban.common;

import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Exclusao de recurso com tarefa ativa vinculada (RN-005). */
public class RecursoEmUsoException extends BusinessException {

  @Serial private static final long serialVersionUID = 1L;

  public static final String ERROR_CODE = "RECURSO_POSSUI_TAREFAS_ATIVAS";

  public RecursoEmUsoException(String message) {
    super(ERROR_CODE, message, HttpStatus.CONFLICT);
  }
}
