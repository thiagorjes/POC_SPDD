package br.com.crudao.kanban.common;

import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Bloqueio otimista: o recurso foi alterado por outro usuario desde a leitura. */
public class ConflitoConcorrenciaException extends BusinessException {

  @Serial private static final long serialVersionUID = 1L;

  public static final String ERROR_CODE = "CONFLITO_CONCORRENCIA";

  public ConflitoConcorrenciaException(String message) {
    super(ERROR_CODE, message, HttpStatus.CONFLICT);
  }

  public static ConflitoConcorrenciaException padrao() {
    return new ConflitoConcorrenciaException(
        "A tarefa foi alterada por outro usuario. Recarregue o board e tente novamente.");
  }
}
