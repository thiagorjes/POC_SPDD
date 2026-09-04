package br.com.crudao.kanban.security;

import br.com.crudao.kanban.rbac.CodigoPapel;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Guard declarativo transversal. Interceptado por {@link PermissaoAspect}, aplica na ordem fixa:
 * permissao efetiva, toggle condicionante e projeto ativo.
 *
 * <p>Obrigatoria em todo handler de escrita; a ausencia falha o teste de arquitetura.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExigePermissao {

  /** Codigo da permissao exigida, do catalogo fechado ({@code CodigoPermissao}). */
  String valor();

  /**
   * Expressao SpEL avaliada sobre os argumentos do metodo que resolve o identificador do
   * <em>recurso</em> (nao do projeto). O projeto e derivado dele por {@link EscopoProjetoResolver},
   * jamais aceito de parametro do cliente (RNF-003).
   */
  String escopoProjeto();

  /** Tipo do recurso resolvido pela expressao acima. */
  OrigemEscopo origem() default OrigemEscopo.PROJETO;

  /**
   * Nome da chave de {@code ChaveToggle} que condiciona esta permissao; vazio quando a permissao
   * nao depende de toggle.
   */
  String toggle() default "";

  /**
   * Papel cujo acesso o toggle modula (RF-016). Relevante apenas quando {@link #toggle()} existe.
   */
  String papelCondicionante() default CodigoPapel.DEV;

  /** Quando verdadeiro, exige projeto ativo (RN-015). Leitura deve declarar {@code false}. */
  boolean escrita() default true;
}
