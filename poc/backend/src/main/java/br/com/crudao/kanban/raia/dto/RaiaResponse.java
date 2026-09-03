package br.com.crudao.kanban.raia.dto;

import java.util.UUID;

public record RaiaResponse(UUID id, String nome, int ordem, boolean padrao) {}
