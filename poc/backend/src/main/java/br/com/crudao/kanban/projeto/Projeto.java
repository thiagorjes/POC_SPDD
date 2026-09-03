package br.com.crudao.kanban.projeto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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

@Entity
@Table(name = "projeto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Projeto {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String nome;

  @Column private String descricao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StatusProjeto status;

  @Column(name = "finalizado_em")
  private Instant finalizadoEm;

  @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
  private Instant criadoEm;

  @Version
  @Column(nullable = false)
  private long versao;

  /** Projeto ativo aceita escrita; finalizado e somente leitura para todos (RN-015). */
  public boolean isAtivo() {
    return status == StatusProjeto.ATIVO;
  }
}
