package br.com.crudao.kanban.security;

import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.projeto.ChaveToggle;
import java.lang.reflect.Method;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Aplica {@link ExigePermissao} na ordem fixa: permissao efetiva, toggle do projeto e projeto
 * ativo. O {@code projetoId} e sempre derivado do recurso alvo.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class PermissaoAspect {

  private final PermissaoGuard permissaoGuard;
  private final EscopoProjetoResolver escopoProjetoResolver;
  private final ExpressionParser parser = new SpelExpressionParser();
  private final ParameterNameDiscoverer parameterNameDiscoverer =
      new DefaultParameterNameDiscoverer();

  @Around("@annotation(exigePermissao)")
  public Object aplicar(ProceedingJoinPoint joinPoint, ExigePermissao exigePermissao)
      throws Throwable {
    UUID recursoId = resolverRecurso(joinPoint, exigePermissao.escopoProjeto());
    UUID projetoId = escopoProjetoResolver.resolver(exigePermissao.origem(), recursoId);

    permissaoGuard.exigir(exigePermissao.valor(), projetoId);

    if (!exigePermissao.toggle().isBlank()) {
      permissaoGuard.exigirToggle(
          ChaveToggle.valueOf(exigePermissao.toggle()),
          projetoId,
          exigePermissao.papelCondicionante());
    }

    if (exigePermissao.escrita()) {
      permissaoGuard.exigirProjetoAtivo(projetoId);
    }

    return joinPoint.proceed();
  }

  private UUID resolverRecurso(ProceedingJoinPoint joinPoint, String expressao) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method metodo = signature.getMethod();
    EvaluationContext contexto = new StandardEvaluationContext(joinPoint.getTarget());
    String[] nomes = parameterNameDiscoverer.getParameterNames(metodo);
    Object[] argumentos = joinPoint.getArgs();
    if (nomes != null) {
      for (int i = 0; i < nomes.length; i++) {
        contexto.setVariable(nomes[i], argumentos[i]);
      }
    }
    Object valor = parser.parseExpression(expressao).getValue(contexto);
    if (valor instanceof UUID uuid) {
      return uuid;
    }
    if (valor instanceof String texto) {
      return UUID.fromString(texto);
    }
    throw new RecursoNaoEncontradoException("Recurso alvo da operacao nao informado.");
  }
}
