package br.com.crudao.kanban.common;

/** Erro associado a um campo especifico do payload de entrada. */
public record CampoErro(String campo, String mensagem) {}
