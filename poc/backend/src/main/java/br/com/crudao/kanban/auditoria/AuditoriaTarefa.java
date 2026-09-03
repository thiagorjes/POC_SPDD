package br.com.crudao.kanban.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Log append-only. Preservado mesmo apos a exclusao da tarefa — por isso sem FK para tarefa. */
@Entity
@Table(name = "auditoria_tarefa")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaTarefa {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tarefa_id", nullable = false, updatable = false)
  private UUID tarefaId;

  @Column(name = "projeto_id", nullable = false, updatable = false)
  private UUID projetoId;

  @Column(name = "autor_id", nullable = false, updatable = false)
  private UUID autorId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private CampoAuditado campo;

  @Column(name = "valor_anterior", updatable = false)
  private String valorAnterior;

  @Column(name = "valor_novo", updatable = false)
  private String valorNovo;

  @Column(name = "ocorrido_em", nullable = false, insertable = false, updatable = false)
  private Instant ocorridoEm;
}
