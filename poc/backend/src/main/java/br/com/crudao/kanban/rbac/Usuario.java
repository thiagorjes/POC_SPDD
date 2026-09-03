package br.com.crudao.kanban.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

  @Id
  @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
  private UUID id;

  @Column(name = "keycloak_sub", nullable = false, unique = true, updatable = false)
  private String keycloakSub;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String nome;

  /** Bypass de permissao em todos os projetos. Nunca exposto em endpoint de escrita (ADR-007). */
  @Column(name = "admin_global", nullable = false)
  private boolean adminGlobal;

  @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
  private Instant criadoEm;
}
