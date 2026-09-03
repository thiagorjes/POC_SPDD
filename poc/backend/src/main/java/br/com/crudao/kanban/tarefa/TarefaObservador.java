package br.com.crudao.kanban.tarefa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tarefa_observador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TarefaObservador {

  @EmbeddedId private Id id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrigemObservacao origem;

  public TarefaObservador(UUID tarefaId, UUID usuarioId, OrigemObservacao origem) {
    this.id = new Id(tarefaId, usuarioId);
    this.origem = origem;
  }

  @Embeddable
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class Id implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @Column(name = "tarefa_id", nullable = false)
    private UUID tarefaId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;
  }
}
