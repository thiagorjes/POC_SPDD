package br.com.crudao.kanban.common;

import org.springframework.http.HttpStatus;

/** Movimentacao sem aresta correspondente no grafo do workflow (RN-001, RN-004). */
public class TransicaoNaoPermitidaException extends BusinessException {

  public TransicaoNaoPermitidaException(String message) {
    super("TRANSICAO_NAO_PERMITIDA", message, HttpStatus.UNPROCESSABLE_ENTITY);
  }
}
