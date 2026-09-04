package br.com.crudao.kanban.tarefa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Chave composta (tarefa, usuario). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TarefaObservadorId implements Serializable {

  @Column(name = "tarefa_id", nullable = false)
  private UUID tarefaId;

  @Column(name = "usuario_id", nullable = false)
  private UUID usuarioId;
}
