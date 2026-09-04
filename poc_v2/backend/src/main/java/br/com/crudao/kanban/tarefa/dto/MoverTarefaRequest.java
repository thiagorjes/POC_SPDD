package br.com.crudao.kanban.tarefa.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

/**
 * Movimentacao de card (RF-002, RF-012). {@code versaoEsperada} e obrigatoria: dois usuarios
 * arrastando o mesmo card resultam em conflito para o segundo.
 */
public record MoverTarefaRequest(
    @NotNull(message = "A etapa de destino e obrigatoria.") UUID etapaDestinoId,
    UUID raiaDestinoId,
    @NotNull(message = "A versao esperada da tarefa e obrigatoria.")
        @PositiveOrZero(message = "A versao esperada deve ser maior ou igual a zero.")
        Long versaoEsperada) {}
