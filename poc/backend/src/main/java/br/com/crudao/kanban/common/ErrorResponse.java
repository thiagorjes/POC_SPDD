package br.com.crudao.kanban.common;

import java.time.Instant;
import java.util.List;

/**
 * Formato unico de resposta de falha da API. O {@code errorCode} e parte do contrato: renomea-lo e
 * breaking change.
 */
public record ErrorResponse(
    String errorCode, String message, Instant timestamp, String path, List<CampoErro> campos) {

  public static ErrorResponse de(String errorCode, String message, String path) {
    return new ErrorResponse(errorCode, message, Instant.now(), path, List.of());
  }

  public static ErrorResponse de(
      String errorCode, String message, String path, List<CampoErro> campos) {
    return new ErrorResponse(errorCode, message, Instant.now(), path, campos);
  }
}
