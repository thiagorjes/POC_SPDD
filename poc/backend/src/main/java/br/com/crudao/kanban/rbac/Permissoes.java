package br.com.crudao.kanban.rbac;

import java.util.Set;

/** Catalogo fechado de permissoes (RF-013 / BDR-001). Nenhuma permissao fora desta lista existe. */
public final class Permissoes {

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

  public static final Set<String> TODAS =
      Set.of(
          PROJETO_ADMINISTRAR,
          PROJETO_VISUALIZAR,
          WORKFLOW_GERENCIAR,
          RAIA_GERENCIAR,
          USUARIO_ASSOCIAR,
          TAREFA_GERENCIAR,
          TAREFA_MOVER,
          TAREFA_FINALIZAR,
          TAREFA_IMPEDIR,
          TAREFA_ATRIBUIR,
          DASHBOARD_VISUALIZAR);

  private Permissoes() {}
}
