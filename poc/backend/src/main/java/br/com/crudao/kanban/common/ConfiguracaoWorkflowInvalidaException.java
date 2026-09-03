package br.com.crudao.kanban.common;

import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Workflow que viola os invariantes de RN-003/RN-004. */
public class ConfiguracaoWorkflowInvalidaException extends BusinessException {

  @Serial private static final long serialVersionUID = 1L;

  public static final String ERROR_CODE = "WORKFLOW_INVALIDO";

  public ConfiguracaoWorkflowInvalidaException(String message) {
    super(ERROR_CODE, message, HttpStatus.UNPROCESSABLE_ENTITY);
  }
}
