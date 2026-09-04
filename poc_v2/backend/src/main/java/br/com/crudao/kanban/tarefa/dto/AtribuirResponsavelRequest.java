package br.com.crudao.kanban.tarefa.dto;

import java.util.UUID;

/** Atribuicao de responsavel (RN-012). {@code null} remove o responsavel. */
public record AtribuirResponsavelRequest(UUID responsavelId) {}
