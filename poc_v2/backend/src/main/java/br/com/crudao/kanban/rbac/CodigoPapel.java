package br.com.crudao.kanban.rbac;

/** Codigos do catalogo fechado de papeis (BDR-001), referenciados por regra de negocio. */
public final class CodigoPapel {

  public static final String ADMIN = "admin";
  public static final String PROJECT_ADMIN = "project_admin";
  public static final String PRODUCT_OWNER = "product_owner";
  public static final String DEV = "dev";
  public static final String GESTOR = "gestor";
  public static final String USER = "user";

  private CodigoPapel() {}
}
