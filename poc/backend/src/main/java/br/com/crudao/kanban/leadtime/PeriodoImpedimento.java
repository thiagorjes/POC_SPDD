package br.com.crudao.kanban.leadtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Intervalo de impedimento carimbado com a etapa vigente — permite atribuir RN-002 por etapa. */
@Entity
@Table(name = "periodo_impedimento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodoImpedimento {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tarefa_id", nullable = false, updatable = false)
  private UUID tarefaId;

  @Column(name = "projeto_id", nullable = false, updatable = false)
  private UUID projetoId;

  @Column(name = "etapa_id", nullable = false, updatable = false)
  private UUID etapaId;

  @Column private String motivo;

  @Column(name = "iniciado_em", nullable = false, insertable = false, updatable = false)
  private Instant iniciadoEm;

  @Column(name = "encerrado_em")
  private Instant encerradoEm;

  public Duration duracao(Instant agora) {
    return Duration.between(iniciadoEm, encerradoEm != null ? encerradoEm : agora);
  }
}
