package br.com.crudao.kanban.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;

/** Grafo de trabalho de um projeto. Exatamente um workflow fica ativo por projeto (A-9). */
@Entity
@Table(name = "workflow")
@Getter
@Setter
@NoArgsConstructor
public class Workflow {

  @Id @GeneratedValue private UUID id;

  @Column(name = "projeto_id", nullable = false, updatable = false)
  private UUID projetoId;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false)
  private boolean ativo;

  @Generated
  @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
  private Instant criadoEm;

  public Workflow(UUID projetoId, String nome, boolean ativo) {
    this.projetoId = projetoId;
    this.nome = nome;
    this.ativo = ativo;
  }
}
