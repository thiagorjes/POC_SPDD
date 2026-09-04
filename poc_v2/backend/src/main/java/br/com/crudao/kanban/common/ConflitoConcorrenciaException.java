package br.com.crudao.kanban.common;

import org.springframework.http.HttpStatus;

/** Bloqueio otimista: o recurso foi alterado por outro usuario desde a leitura. */
public class ConflitoConcorrenciaException extends BusinessException {

  public ConflitoConcorrenciaException(String message) {
    super("CONFLITO_CONCORRENCIA", message, HttpStatus.CONFLICT);
  }
}
