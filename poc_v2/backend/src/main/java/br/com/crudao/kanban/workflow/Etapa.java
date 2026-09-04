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

/**
 * Coluna do board. {@code ordem} e apenas apresentacao e resolucao do default de criacao: o grafo
 * de transicoes e independente dela.
 */
@Entity
@Table(name = "etapa")
@Getter
@Setter
@NoArgsConstructor
public class Etapa {

  @Id @GeneratedValue private UUID id;

  @Column(name = "workflow_id", nullable = false, updatable = false)
  private UUID workflowId;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false)
  private int ordem;

  /** Etapa final e unica por workflow e nao possui transicao de saida configuravel (RN-004). */
  @Column(name = "etapa_final", nullable = false)
  private boolean etapaFinal;

  public Etapa(UUID workflowId, String nome, int ordem, boolean etapaFinal) {
    this.workflowId = workflowId;
    this.nome = nome;
    this.ordem = ordem;
    this.etapaFinal = etapaFinal;
  }
}
