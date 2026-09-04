package br.com.crudao.kanban.projeto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Chave composta do toggle: (projeto, chave). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProjetoToggleId implements Serializable {

  @Column(name = "projeto_id", nullable = false)
  private UUID projetoId;

  @Enumerated(EnumType.STRING)
  @Column(name = "chave", nullable = false, length = 60)
  private ChaveToggle chave;
}
