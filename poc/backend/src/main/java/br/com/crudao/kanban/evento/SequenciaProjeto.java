package br.com.crudao.kanban.evento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sequencia_projeto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SequenciaProjeto {

  @Id
  @Column(name = "projeto_id")
  private UUID projetoId;

  @Column(name = "ultimo_seq", nullable = false)
  private long ultimoSeq;
}
