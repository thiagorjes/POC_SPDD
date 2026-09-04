package br.com.crudao.kanban.common;

import org.springframework.http.HttpStatus;

/** Escrita em projeto finalizado. Sem bypass, inclusive para admin global (RN-015). */
public class ProjetoFinalizadoException extends BusinessException {

  public ProjetoFinalizadoException(String message) {
    super("PROJETO_FINALIZADO", message, HttpStatus.CONFLICT);
  }
}
