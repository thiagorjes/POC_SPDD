package br.com.crudao.kanban.common;

import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Alteracao de campo estrutural em tarefa ja iniciada (RF-003). */
public class TarefaIniciadaException extends BusinessException {

  @Serial private static final long serialVersionUID = 1L;

  public static final String ERROR_CODE = "TAREFA_INICIADA_CAMPO_BLOQUEADO";

  public TarefaIniciadaException(String message) {
    super(ERROR_CODE, message, HttpStatus.UNPROCESSABLE_ENTITY);
  }
}
