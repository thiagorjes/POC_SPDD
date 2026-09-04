package br.com.crudao.kanban.rbac;

/** Codigos do catalogo fechado de permissoes (RF-013, BDR-001). */
public final class CodigoPermissao {

  public static final String PROJETO_ADMINISTRAR = "projeto:administrar";
  public static final String PROJETO_VISUALIZAR = "projeto:visualizar";
  public static final String WORKFLOW_GERENCIAR = "workflow:gerenciar";
  public static final String RAIA_GERENCIAR = "raia:gerenciar";
  public static final String USUARIO_ASSOCIAR = "usuario:associar";
  public static final String TAREFA_GERENCIAR = "tarefa:gerenciar";
  public static final String TAREFA_MOVER = "tarefa:mover";
  public static final String TAREFA_FINALIZAR = "tarefa:finalizar";
  public static final String TAREFA_IMPEDIR = "tarefa:impedir";
  public static final String TAREFA_ATRIBUIR = "tarefa:atribuir";
  public static final String DASHBOARD_VISUALIZAR = "dashboard:visualizar";

  private CodigoPermissao() {}
}
