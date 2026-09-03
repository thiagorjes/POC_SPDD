package br.com.crudao.kanban.common;

import java.io.Serial;
import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base de toda excecao de negocio. Carrega o {@code errorCode} estavel (contrato de API) e o status
 * HTTP correspondente. Lancada exclusivamente pela Service Layer.
 */
@Getter
public class BusinessException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final String errorCode;
  private final HttpStatus httpStatus;
  private final transient List<CampoErro> campos;

  public BusinessException(String errorCode, String message) {
    this(errorCode, message, HttpStatus.UNPROCESSABLE_ENTITY, List.of());
  }

  public BusinessException(String errorCode, String message, HttpStatus httpStatus) {
    this(errorCode, message, httpStatus, List.of());
  }

  public BusinessException(String errorCode, String message, List<CampoErro> campos) {
    this(errorCode, message, HttpStatus.UNPROCESSABLE_ENTITY, campos);
  }

  protected BusinessException(
      String errorCode, String message, HttpStatus httpStatus, List<CampoErro> campos) {
    super(message);
    this.errorCode = errorCode;
    this.httpStatus = httpStatus;
    this.campos = campos == null ? List.of() : List.copyOf(campos);
  }
}
