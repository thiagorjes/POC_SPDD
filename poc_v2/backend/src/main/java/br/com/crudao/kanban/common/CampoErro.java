package br.com.crudao.kanban.common;

/** Detalhe de erro por campo, usado nas respostas de validacao de entrada. */
public record CampoErro(String campo, String mensagem) {}
