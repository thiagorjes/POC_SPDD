package br.com.crudao.kanban.tarefa.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * {@code versaoEsperada} e obrigatorio: dois usuarios arrastando o mesmo card resultam em conflito
 * explicito para o segundo, e a UI devolve o card a posicao original.
 */
public record MoverTarefaRequest(
    @NotNull(message = "A etapa de destino e obrigatoria.") UUID etapaDestinoId,
    UUID raiaDestinoId,
    @NotNull(message = "A versao esperada e obrigatoria.") Long versaoEsperada) {}
