package br.com.crudao.kanban.tarefa.dto;

import br.com.crudao.kanban.tarefa.PrioridadeTarefa;
import br.com.crudao.kanban.tarefa.TipoTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Criacao de card (RF-018). Raia e responsavel sao opcionais: sem raia usa-se a padrao do projeto e
 * sem responsavel a tarefa nasce nao atribuida (RN-CB-004/005).
 */
public record CriarTarefaRequest(
    @NotBlank(message = "O titulo e obrigatorio.")
        @Size(min = 1, max = 200, message = "O titulo deve ter entre 1 e 200 caracteres.")
        String titulo,
    @Size(max = 4000, message = "A descricao deve ter no maximo 4000 caracteres.") String descricao,
    @NotNull(message = "O tipo da tarefa e obrigatorio.") TipoTarefa tipo,
    PrioridadeTarefa prioridade,
    UUID raiaId,
    UUID responsavelId) {}
