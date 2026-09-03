package br.com.crudao.kanban.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Aresta dirigida do grafo de workflow, independente da ordem de apresentacao das etapas. */
@Entity
@Table(name = "transicao")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transicao {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "workflow_id", nullable = false, updatable = false)
  private UUID workflowId;

  @Column(name = "etapa_origem_id", nullable = false, updatable = false)
  private UUID etapaOrigemId;

  @Column(name = "etapa_destino_id", nullable = false, updatable = false)
  private UUID etapaDestinoId;
}
