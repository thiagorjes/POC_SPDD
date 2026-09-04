package br.com.crudao.kanban.common;

import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base de todas as excecoes de negocio. Carrega o {@code errorCode} estavel (parte do contrato de
 * API) e o HTTP status correspondente. Lancada exclusivamente pela Service Layer.
 */
@Getter
public class BusinessException extends RuntimeException {

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
