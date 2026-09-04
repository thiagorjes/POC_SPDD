package br.com.crudao.kanban.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Aresta dirigida do grafo de workflow. */
@Entity
@Table(name = "transicao")
@Getter
@Setter
@NoArgsConstructor
public class Transicao {

  @Id @GeneratedValue private UUID id;

  @Column(name = "workflow_id", nullable = false, updatable = false)
  private UUID workflowId;

  @Column(name = "etapa_origem_id", nullable = false, updatable = false)
  private UUID etapaOrigemId;

  @Column(name = "etapa_destino_id", nullable = false, updatable = false)
  private UUID etapaDestinoId;

  public Transicao(UUID workflowId, UUID etapaOrigemId, UUID etapaDestinoId) {
    this.workflowId = workflowId;
    this.etapaOrigemId = etapaOrigemId;
    this.etapaDestinoId = etapaDestinoId;
  }
}
