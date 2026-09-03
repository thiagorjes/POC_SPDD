package br.com.crudao.kanban.common;

import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Escrita em projeto finalizado. Sem bypass, inclusive para admin global (RN-015). */
public class ProjetoFinalizadoException extends BusinessException {

  @Serial private static final long serialVersionUID = 1L;

  public static final String ERROR_CODE = "PROJETO_FINALIZADO";

  public ProjetoFinalizadoException(String message) {
    super(ERROR_CODE, message, HttpStatus.CONFLICT);
  }
}
