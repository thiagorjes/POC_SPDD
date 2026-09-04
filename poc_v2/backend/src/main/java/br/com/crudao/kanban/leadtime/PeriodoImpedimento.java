package br.com.crudao.kanban.leadtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Intervalo de impedimento carimbado com a etapa vigente. Mover tarefa impedida fecha e reabre o
 * periodo na nova etapa, permitindo atribuir o tempo impedido por etapa (RN-002).
 */
@Entity
@Table(name = "periodo_impedimento")
@Getter
@Setter
@NoArgsConstructor
public class PeriodoImpedimento {

  @Id @GeneratedValue private UUID id;

  @Column(name = "tarefa_id", nullable = false, updatable = false)
  private UUID tarefaId;

  @Column(name = "projeto_id", nullable = false, updatable = false)
  private UUID projetoId;

  @Column(name = "etapa_id", nullable = false, updatable = false)
  private UUID etapaId;

  @Column private String motivo;

  @Column(name = "iniciado_em", nullable = false)
  private Instant iniciadoEm;

  @Column(name = "encerrado_em")
  private Instant encerradoEm;

  public PeriodoImpedimento(
      UUID tarefaId, UUID projetoId, UUID etapaId, String motivo, Instant iniciadoEm) {
    this.tarefaId = tarefaId;
    this.projetoId = projetoId;
    this.etapaId = etapaId;
    this.motivo = motivo;
    this.iniciadoEm = iniciadoEm;
  }

  public Duration duracao(Instant agora) {
    return Duration.between(iniciadoEm, encerradoEm == null ? agora : encerradoEm);
  }

  public boolean aberto() {
    return encerradoEm == null;
  }
}
