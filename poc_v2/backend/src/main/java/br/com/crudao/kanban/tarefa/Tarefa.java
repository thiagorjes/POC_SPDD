package br.com.crudao.kanban.tarefa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;

/** Card do board. Posicao (etapa) e agrupamento visual (raia) sao dimensoes independentes. */
@Entity
@Table(name = "tarefa")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tarefa {

  @Id @GeneratedValue private UUID id;

  @Column(name = "projeto_id", nullable = false, updatable = false)
  private UUID projetoId;

  @Column(name = "workflow_id", nullable = false)
  private UUID workflowId;

  @Column(name = "etapa_id", nullable = false)
  private UUID etapaId;

  @Column(name = "raia_id", nullable = false)
  private UUID raiaId;

  @Column(name = "responsavel_id")
  private UUID responsavelId;

  @Column(name = "criador_id", nullable = false, updatable = false)
  private UUID criadorId;

  @Column(nullable = false)
  private String titulo;

  @Column private String descricao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TipoTarefa tipo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PrioridadeTarefa prioridade;

  /** Sticky: torna-se true na primeira saida da etapa de menor ordem e nunca reverte (A-4). */
  @Column(nullable = false)
  private boolean iniciada;

  /** Flag denormalizada derivada da existencia de um PeriodoImpedimento aberto. */
  @Column(nullable = false)
  private boolean impedida;

  @Generated
  @Column(name = "criada_em", nullable = false, insertable = false, updatable = false)
  private Instant criadaEm;

  @Version
  @Column(nullable = false)
  private long versao;
}
