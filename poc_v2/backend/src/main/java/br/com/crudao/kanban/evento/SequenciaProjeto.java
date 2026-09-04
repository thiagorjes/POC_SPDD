package br.com.crudao.kanban.evento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Contador monotonico de eventos por projeto, base do resync por gap de {@code seq} (ADR-004). */
@Entity
@Table(name = "sequencia_projeto")
@Getter
@Setter
@NoArgsConstructor
public class SequenciaProjeto {

  @Id
  @Column(name = "projeto_id")
  private UUID projetoId;

  @Column(name = "ultimo_seq", nullable = false)
  private long ultimoSeq;

  public SequenciaProjeto(UUID projetoId) {
    this.projetoId = projetoId;
    this.ultimoSeq = 0L;
  }
}
