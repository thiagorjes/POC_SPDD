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

@Entity
@Table(name = "etapa")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Etapa {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "workflow_id", nullable = false, updatable = false)
  private UUID workflowId;

  @Column(nullable = false)
  private String nome;

  /** Apresentacao e resolucao da etapa inicial. Reordenar nunca altera o grafo de transicoes. */
  @Column(nullable = false)
  private int ordem;

  /** Nomeado {@code etapaFinal} — {@code eFinal} quebraria a introspeccao JavaBeans. */
  @Column(name = "etapa_final", nullable = false)
  private boolean etapaFinal;
}
