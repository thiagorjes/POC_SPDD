package br.com.crudao.kanban.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Papel acumulavel escopado ao par (usuario, projeto) — BDR-001. */
@Entity
@Table(name = "usuario_projeto_papel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioProjetoPapel {

  @EmbeddedId private Id id;

  @Column(name = "atribuido_em", nullable = false, insertable = false, updatable = false)
  private Instant atribuidoEm;

  @Column(name = "atribuido_por_id")
  private UUID atribuidoPorId;

  public UsuarioProjetoPapel(UUID usuarioId, UUID projetoId, UUID papelId, UUID atribuidoPorId) {
    this.id = new Id(usuarioId, projetoId, papelId);
    this.atribuidoPorId = atribuidoPorId;
  }

  @Embeddable
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class Id implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "projeto_id", nullable = false)
    private UUID projetoId;

    @Column(name = "papel_id", nullable = false)
    private UUID papelId;
  }
}
