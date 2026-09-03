package br.com.crudao.kanban.common;

import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Movimentacao sem aresta correspondente no grafo de workflow (RN-001/RN-004). */
public class TransicaoNaoPermitidaException extends BusinessException {

  @Serial private static final long serialVersionUID = 1L;

  public static final String ERROR_CODE = "TRANSICAO_NAO_PERMITIDA";

  public TransicaoNaoPermitidaException(String message) {
    super(ERROR_CODE, message, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  public static TransicaoNaoPermitidaException entre(String origem, String destino) {
    return new TransicaoNaoPermitidaException(
        "Nao existe transicao configurada de \"%s\" para \"%s\".".formatted(origem, destino));
  }
}
