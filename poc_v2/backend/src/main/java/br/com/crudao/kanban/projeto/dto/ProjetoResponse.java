package br.com.crudao.kanban.projeto.dto;

import br.com.crudao.kanban.projeto.StatusProjeto;
import java.time.Instant;
import java.util.UUID;

/** Representacao de projeto na API. */
public record ProjetoResponse(
    UUID id,
    String nome,
    String descricao,
    StatusProjeto status,
    Instant finalizadoEm,
    Instant criadoEm,
    long versao) {}
