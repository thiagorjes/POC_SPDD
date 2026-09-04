package br.com.crudao.kanban.common;

import org.springframework.http.HttpStatus;

/** Workflow que viola os invariantes de configuracao (RN-003, RN-004). */
public class ConfiguracaoWorkflowInvalidaException extends BusinessException {

  public ConfiguracaoWorkflowInvalidaException(String message) {
    super("WORKFLOW_INVALIDO", message, HttpStatus.UNPROCESSABLE_ENTITY);
  }
}
