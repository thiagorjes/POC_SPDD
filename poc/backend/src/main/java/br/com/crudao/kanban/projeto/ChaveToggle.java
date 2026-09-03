package br.com.crudao.kanban.projeto;

import br.com.crudao.kanban.rbac.Papeis;

/**
 * Conjunto <b>fechado</b> de toggles por projeto (RF-016 / BDR-001). Nenhum toggle novo sem BDR.
 * Cada toggle libera uma capacidade para um papel condicionante especifico.
 */
public enum ChaveToggle {
  DEV_PODE_EXCLUIR_TAREFA(Papeis.DEV, true),
  DEV_PODE_FINALIZAR_TAREFA(Papeis.DEV, false),
  DEV_PODE_EDITAR_TAREFA_INICIADA(Papeis.DEV, false),
  GESTOR_PODE_VER_BOARD(Papeis.GESTOR, true);

  private final String papelCondicionante;
  private final boolean padrao;

  ChaveToggle(String papelCondicionante, boolean padrao) {
    this.papelCondicionante = papelCondicionante;
    this.padrao = padrao;
  }

  public String papelCondicionante() {
    return papelCondicionante;
  }

  public boolean padrao() {
    return padrao;
  }
}
