package br.com.crudao.kanban.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Guard declarativo de autorizacao. Todo handler de escrita deve declara-lo — a ausencia falha o
 * build por teste de arquitetura (ArchUnit).
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExigePermissao {

  /** Codigo da permissao exigida ({@code br.com.crudao.kanban.rbac.Permissoes}). */
  String valor();

  /**
   * SpEL que extrai o projeto <b>a partir do recurso</b>, nunca de um parametro de autorizacao
   * enviado pelo cliente. O contexto expoe os parametros do metodo por nome e o bean
   * {@code resolvedor} ({@link EscopoProjetoResolver}).
   *
   * <p>Exemplo: {@code "#resolvedor.deTarefa(#id)"}.
   */
  String escopoProjeto();

  /** Operacao de escrita aciona adicionalmente {@code exigirProjetoAtivo} (RN-015). */
  boolean escrita() default true;
}
