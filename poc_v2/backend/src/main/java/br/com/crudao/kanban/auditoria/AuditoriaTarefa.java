package br.com.crudao.kanban.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Log append-only chave-valor de alteracoes de tarefa (RN-016, RF-017). Nao e a fonte do lead-time:
 * os intervalos temporais vivem em PeriodoEtapa e PeriodoImpedimento.
 */
@Entity
@Table(name = "auditoria_tarefa")
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaTarefa {

  @Id @GeneratedValue private UUID id;

  @Column(name = "tarefa_id", nullable = false, updatable = false)
  private UUID tarefaId;

  @Column(name = "projeto_id", nullable = false, updatable = false)
  private UUID projetoId;

  @Column(name = "autor_id", nullable = false, updatable = false)
  private UUID autorId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private CampoAuditado campo;

  @Column(name = "valor_anterior", updatable = false)
  private String valorAnterior;

  @Column(name = "valor_novo", updatable = false)
  private String valorNovo;

  @Column(name = "ocorrido_em", nullable = false, updatable = false)
  private Instant ocorridoEm;

  public AuditoriaTarefa(
      UUID tarefaId,
      UUID projetoId,
      UUID autorId,
      CampoAuditado campo,
      String valorAnterior,
      String valorNovo,
      Instant ocorridoEm) {
    this.tarefaId = tarefaId;
    this.projetoId = projetoId;
    this.autorId = autorId;
    this.campo = campo;
    this.valorAnterior = valorAnterior;
    this.valorNovo = valorNovo;
    this.ocorridoEm = ocorridoEm;
  }
}
