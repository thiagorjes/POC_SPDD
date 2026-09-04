package br.com.crudao.kanban.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Permissao do catalogo fechado (RF-013, BDR-001). */
@Entity
@Table(name = "permissao")
@Getter
@Setter
@NoArgsConstructor
public class Permissao {

  @Id @GeneratedValue private UUID id;

  @Column(nullable = false, unique = true, updatable = false)
  private String codigo;

  @Column(nullable = false)
  private String descricao;
}
