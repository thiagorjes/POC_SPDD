package br.com.crudao.kanban.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Chave composta da associacao usuario x projeto x papel. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UsuarioProjetoPapelId implements Serializable {

  @Column(name = "usuario_id", nullable = false)
  private UUID usuarioId;

  @Column(name = "projeto_id", nullable = false)
  private UUID projetoId;

  @Column(name = "papel_id", nullable = false)
  private UUID papelId;
}
