package br.com.crudao.kanban.common;

import org.springframework.http.HttpStatus;

/** Permissao efetiva ausente no projeto, ou toggle que condiciona a acao desabilitado. */
public class PermissaoNegadaException extends BusinessException {

  public PermissaoNegadaException(String message) {
    super("PERMISSAO_NEGADA", message, HttpStatus.FORBIDDEN);
  }
}
