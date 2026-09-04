package br.com.crudao.kanban.security;

/**
 * Tipo do recurso a partir do qual o {@code projetoId} de autorizacao e derivado. O projeto nunca e
 * aceito de parametro enviado pelo cliente (Safeguards, secao 3).
 */
public enum OrigemEscopo {
  PROJETO,
  TAREFA,
  WORKFLOW,
  ETAPA,
  TRANSICAO,
  RAIA
}
