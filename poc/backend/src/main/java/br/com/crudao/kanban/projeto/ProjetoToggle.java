package br.com.crudao.kanban.projeto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projeto_toggle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjetoToggle {

  @EmbeddedId private Id id;

  @Column(nullable = false)
  private boolean habilitado;

  public ProjetoToggle(UUID projetoId, ChaveToggle chave, boolean habilitado) {
    this.id = new Id(projetoId, chave);
    this.habilitado = habilitado;
  }

  public ChaveToggle getChave() {
    return id.getChave();
  }

  @Embeddable
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class Id implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @Column(name = "projeto_id", nullable = false)
    private UUID projetoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chave", nullable = false)
    private ChaveToggle chave;
  }
}
