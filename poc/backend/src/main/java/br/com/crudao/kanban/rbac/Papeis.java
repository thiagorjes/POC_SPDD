package br.com.crudao.kanban.rbac;

import java.util.Set;

/** Catalogo fechado de papeis (BDR-001). */
public final class Papeis {

  public static final String ADMIN = "admin";
  public static final String PROJECT_ADMIN = "project_admin";
  public static final String PRODUCT_OWNER = "product_owner";
  public static final String DEV = "dev";
  public static final String GESTOR = "gestor";
  public static final String USER = "user";

  public static final Set<String> TODOS =
      Set.of(ADMIN, PROJECT_ADMIN, PRODUCT_OWNER, DEV, GESTOR, USER);

  /** Papeis que dispensam a checagem de toggle condicionante. */
  public static final Set<String> ADMINISTRATIVOS = Set.of(ADMIN, PROJECT_ADMIN, PRODUCT_OWNER);

  private Papeis() {}
}
