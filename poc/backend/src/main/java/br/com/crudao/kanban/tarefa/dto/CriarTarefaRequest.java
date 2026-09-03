package br.com.crudao.kanban.tarefa.dto;

import br.com.crudao.kanban.tarefa.PrioridadeTarefa;
import br.com.crudao.kanban.tarefa.TipoTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** {@code raiaId} e {@code responsavelId} opcionais: RN-CB-004/005 resolvem os defaults. */
public record CriarTarefaRequest(
    @NotBlank(message = "O titulo e obrigatorio.")
        @Size(min = 1, max = 200, message = "O titulo deve ter entre 1 e 200 caracteres.")
        String titulo,
    @Size(max = 4000, message = "A descricao deve ter no maximo 4000 caracteres.")
        String descricao,
    @NotNull(message = "O tipo da tarefa e obrigatorio.") TipoTarefa tipo,
    PrioridadeTarefa prioridade,
    UUID raiaId,
    UUID responsavelId) {

  public PrioridadeTarefa prioridadeOuPadrao() {
    return prioridade != null ? prioridade : PrioridadeTarefa.MEDIA;
  }
}
