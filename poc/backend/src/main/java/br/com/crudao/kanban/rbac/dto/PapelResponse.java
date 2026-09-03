package br.com.crudao.kanban.rbac.dto;

import java.util.Set;

/** Matriz papel x permissao exibida em leitura na TL-09. */
public record PapelResponse(String codigo, String nome, boolean protegido, Set<String> permissoes) {}
