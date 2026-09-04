package br.com.crudao.kanban.security;

import br.com.crudao.kanban.rbac.CodigoPermissao;
import java.util.Set;

/** Catalogo fechado usado como conjunto efetivo do admin global (RF-013, BDR-001). */
final class CatalogoPermissoes {

  static final Set<String> TODAS =
      Set.of(
          CodigoPermissao.PROJETO_ADMINISTRAR,
          CodigoPermissao.PROJETO_VISUALIZAR,
          CodigoPermissao.WORKFLOW_GERENCIAR,
          CodigoPermissao.RAIA_GERENCIAR,
          CodigoPermissao.USUARIO_ASSOCIAR,
          CodigoPermissao.TAREFA_GERENCIAR,
          CodigoPermissao.TAREFA_MOVER,
          CodigoPermissao.TAREFA_FINALIZAR,
          CodigoPermissao.TAREFA_IMPEDIR,
          CodigoPermissao.TAREFA_ATRIBUIR,
          CodigoPermissao.DASHBOARD_VISUALIZAR);

  private CatalogoPermissoes() {}
}
