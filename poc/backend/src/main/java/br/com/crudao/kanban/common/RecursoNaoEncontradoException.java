package br.com.crudao.kanban.common;

import java.io.Serial;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Recurso inexistente ou fora do escopo visivel do usuario. */
public class RecursoNaoEncontradoException extends BusinessException {

  @Serial private static final long serialVersionUID = 1L;

  public static final String ERROR_CODE = "RECURSO_NAO_ENCONTRADO";

  public RecursoNaoEncontradoException(String message) {
    super(ERROR_CODE, message, HttpStatus.NOT_FOUND);
  }

  public static RecursoNaoEncontradoException de(String recurso, UUID id) {
    return new RecursoNaoEncontradoException("%s nao encontrado(a): %s".formatted(recurso, id));
  }
}
