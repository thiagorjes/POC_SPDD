package br.com.crudao.kanban.notificacao;

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

/** Notificacao interna entregue ao observador da tarefa (RF-005). */
@Entity
@Table(name = "notificacao")
@Getter
@Setter
@NoArgsConstructor
public class Notificacao {

  @Id @GeneratedValue private UUID id;

  @Column(name = "destinatario_id", nullable = false, updatable = false)
  private UUID destinatarioId;

  @Column(name = "tarefa_id", nullable = false, updatable = false)
  private UUID tarefaId;

  @Column(name = "projeto_id", nullable = false, updatable = false)
  private UUID projetoId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, updatable = false)
  private TipoNotificacao tipo;

  @Column(nullable = false, updatable = false)
  private String mensagem;

  @Column(name = "criada_em", nullable = false, updatable = false)
  private Instant criadaEm;

  @Column(name = "lida_em")
  private Instant lidaEm;

  public Notificacao(
      UUID destinatarioId,
      UUID tarefaId,
      UUID projetoId,
      TipoNotificacao tipo,
      String mensagem,
      Instant criadaEm) {
    this.destinatarioId = destinatarioId;
    this.tarefaId = tarefaId;
    this.projetoId = projetoId;
    this.tipo = tipo;
    this.mensagem = mensagem;
    this.criadaEm = criadaEm;
  }
}
