package br.com.crudao.kanban.tarefa;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Interessado na tarefa; destinatario das notificacoes internas (RF-005). */
@Entity
@Table(name = "tarefa_observador")
@Getter
@Setter
@NoArgsConstructor
public class TarefaObservador {

  @EmbeddedId private TarefaObservadorId id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrigemObservacao origem;

  public TarefaObservador(UUID tarefaId, UUID usuarioId, OrigemObservacao origem) {
    this.id = new TarefaObservadorId(tarefaId, usuarioId);
    this.origem = origem;
  }
}
