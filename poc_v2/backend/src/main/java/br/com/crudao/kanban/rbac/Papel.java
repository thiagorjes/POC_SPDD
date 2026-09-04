package br.com.crudao.kanban.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Papel do catalogo fechado (BDR-001). Nao ha criacao livre de papeis. */
@Entity
@Table(name = "papel")
@Getter
@Setter
@NoArgsConstructor
public class Papel {

  @Id @GeneratedValue private UUID id;

  @Column(nullable = false, unique = true, updatable = false)
  private String codigo;

  @Column(nullable = false)
  private String nome;

  /** Papel protegido nao pode ser criado, editado nem excluido por papel delegado (RN-006). */
  @Column(nullable = false)
  private boolean protegido;

  @Column(name = "global", nullable = false)
  private boolean global;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "papel_permissao",
      joinColumns = @JoinColumn(name = "papel_id"),
      inverseJoinColumns = @JoinColumn(name = "permissao_id"))
  private Set<Permissao> permissoes = new LinkedHashSet<>();
}
