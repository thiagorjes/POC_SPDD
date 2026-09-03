package br.com.crudao.kanban.common;

import java.time.Instant;

/** Entidade cujo instante de criacao e carimbado pelo banco. */
public interface Auditavel {

  Instant getCriadoEm();

  void setCriadoEm(Instant criadoEm);
}
