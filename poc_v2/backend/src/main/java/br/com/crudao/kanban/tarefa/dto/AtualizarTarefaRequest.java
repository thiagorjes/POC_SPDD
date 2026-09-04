package br.com.crudao.kanban.tarefa.dto;

import br.com.crudao.kanban.tarefa.PrioridadeTarefa;
import br.com.crudao.kanban.tarefa.TipoTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Edicao de card (RF-003). Campos estruturais travam apos o inicio (A-3). */
public record AtualizarTarefaRequest(
    @NotBlank(message = "O titulo e obrigatorio.")
        @Size(min = 1, max = 200, message = "O titulo deve ter entre 1 e 200 caracteres.")
        String titulo,
    @Size(max = 4000, message = "A descricao deve ter no maximo 4000 caracteres.") String descricao,
    @NotNull(message = "O tipo da tarefa e obrigatorio.") TipoTarefa tipo,
    @NotNull(message = "A prioridade e obrigatoria.") PrioridadeTarefa prioridade,
    UUID raiaId) {}
