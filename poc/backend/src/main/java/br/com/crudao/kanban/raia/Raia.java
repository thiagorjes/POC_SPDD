package br.com.crudao.kanban.raia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Agrupamento visual puro: sem transicoes, regras ou permissoes proprias (design brief 5). */
@Entity
@Table(name = "raia")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Raia {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "projeto_id", nullable = false, updatable = false)
  private UUID projetoId;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false)
  private int ordem;

  /** Exatamente uma raia padrao por projeto (indice unico parcial) — RN-CB-005. */
  @Column(nullable = false)
  private boolean padrao;
}
