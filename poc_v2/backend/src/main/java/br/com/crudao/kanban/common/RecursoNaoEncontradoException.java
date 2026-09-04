package br.com.crudao.kanban.common;

import org.springframework.http.HttpStatus;

/** Recurso inexistente ou fora do escopo do usuario. */
public class RecursoNaoEncontradoException extends BusinessException {

  public RecursoNaoEncontradoException(String message) {
    super("RECURSO_NAO_ENCONTRADO", message, HttpStatus.NOT_FOUND);
  }
}
