package br.com.crudao.kanban.projeto;

/** Conjunto fechado de toggles por projeto (RF-016, BDR-001). Nenhum toggle novo sem BDR. */
public enum ChaveToggle {
  DEV_PODE_EXCLUIR_TAREFA(true),
  DEV_PODE_FINALIZAR_TAREFA(false),
  DEV_PODE_EDITAR_TAREFA_INICIADA(false),
  GESTOR_PODE_VER_BOARD(true);

  private final boolean padrao;

  ChaveToggle(boolean padrao) {
    this.padrao = padrao;
  }

  /** Valor default aplicado na criacao do projeto (bloco 2 das Operations). */
  public boolean padrao() {
    return padrao;
  }
}
