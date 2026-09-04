package br.com.crudao.kanban.tarefa;

/**
 * Origem do vinculo de observacao. CRIADOR e RESPONSAVEL sao implicitos e nao removiveis
 * manualmente; EXPLICITO e gerenciado pelo proprio usuario (A-5).
 */
public enum OrigemObservacao {
  CRIADOR,
  RESPONSAVEL,
  EXPLICITO
}
