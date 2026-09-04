package br.com.crudao.kanban.raia;

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
 * Agrupamento visual puro: raia nao possui transicoes, regras nem permissoes proprias (design brief
 * secao 5).
 */
@Entity
@Table(name = "raia")
@Getter
@Setter
@NoArgsConstructor
public class Raia {

  @Id @GeneratedValue private UUID id;

  @Column(name = "projeto_id", nullable = false, updatable = false)
  private UUID projetoId;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false)
  private int ordem;

  /** Exatamente uma raia padrao por projeto (RN-CB-005, A-7). */
  @Column(nullable = false)
  private boolean padrao;

  public Raia(UUID projetoId, String nome, int ordem, boolean padrao) {
    this.projetoId = projetoId;
    this.nome = nome;
    this.ordem = ordem;
    this.padrao = padrao;
  }
}
