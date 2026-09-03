package br.com.crudao.kanban.common;

import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Usuario autenticado sem permissao efetiva para a operacao no projeto (RNF-003). */
public class PermissaoNegadaException extends BusinessException {

  @Serial private static final long serialVersionUID = 1L;

  public static final String ERROR_CODE = "PERMISSAO_NEGADA";

  public PermissaoNegadaException(String message) {
    super(ERROR_CODE, message, HttpStatus.FORBIDDEN);
  }
}
