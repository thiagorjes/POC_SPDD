package br.com.crudao.kanban.projeto;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Toggle de projeto que modula permissoes de papeis (RF-016). */
@Entity
@Table(name = "projeto_toggle")
@Getter
@Setter
@NoArgsConstructor
public class ProjetoToggle {

  @EmbeddedId private ProjetoToggleId id;

  @Column(nullable = false)
  private boolean habilitado;

  public ProjetoToggle(UUID projetoId, ChaveToggle chave, boolean habilitado) {
    this.id = new ProjetoToggleId(projetoId, chave);
    this.habilitado = habilitado;
  }
}
