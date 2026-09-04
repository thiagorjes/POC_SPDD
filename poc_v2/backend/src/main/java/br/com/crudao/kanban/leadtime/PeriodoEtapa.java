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

/** Intervalo de permanencia de uma tarefa em uma etapa. Fonte de verdade do lead-time (RF-006). */
@Entity
@Table(name = "periodo_etapa")
@Getter
@Setter
@NoArgsConstructor
public class PeriodoEtapa {

  @Id @GeneratedValue private UUID id;

  @Column(name = "tarefa_id", nullable = false, updatable = false)
  private UUID tarefaId;

  @Column(name = "projeto_id", nullable = false, updatable = false)
  private UUID projetoId;

  @Column(name = "etapa_id", nullable = false, updatable = false)
  private UUID etapaId;

  @Column(name = "iniciado_em", nullable = false)
  private Instant iniciadoEm;

  @Column(name = "encerrado_em")
  private Instant encerradoEm;

  public PeriodoEtapa(UUID tarefaId, UUID projetoId, UUID etapaId, Instant iniciadoEm) {
    this.tarefaId = tarefaId;
    this.projetoId = projetoId;
    this.etapaId = etapaId;
    this.iniciadoEm = iniciadoEm;
  }

  /** Intervalo aberto e computado ate o instante informado (relogio do banco). */
  public Duration duracao(Instant agora) {
    return Duration.between(iniciadoEm, encerradoEm == null ? agora : encerradoEm);
  }

  public boolean aberto() {
    return encerradoEm == null;
  }
}
