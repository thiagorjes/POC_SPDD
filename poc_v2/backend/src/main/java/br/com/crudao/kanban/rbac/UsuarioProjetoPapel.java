package br.com.crudao.kanban.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;

/** Papel acumulavel escopado ao par (usuario, projeto) (BDR-001). */
@Entity
@Table(name = "usuario_projeto_papel")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioProjetoPapel {

  @EmbeddedId private UsuarioProjetoPapelId id;

  @Generated
  @Column(name = "atribuido_em", nullable = false, insertable = false, updatable = false)
  private Instant atribuidoEm;

  @Column(name = "atribuido_por_id")
  private UUID atribuidoPorId;

  public UsuarioProjetoPapel(UUID usuarioId, UUID projetoId, UUID papelId, UUID atribuidoPorId) {
    this.id = new UsuarioProjetoPapelId(usuarioId, projetoId, papelId);
    this.atribuidoPorId = atribuidoPorId;
  }
}
