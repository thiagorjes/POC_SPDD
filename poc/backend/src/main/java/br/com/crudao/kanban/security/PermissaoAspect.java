package br.com.crudao.kanban.security;

import br.com.crudao.kanban.common.PermissaoNegadaException;
import java.lang.reflect.Method;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

/**
 * Intercepta {@link ExigePermissao} e aplica as tres checagens na ordem fixa: permissao efetiva,
 * toggle do projeto (quando declarado pela permissao) e projeto ativo.
 */
@Aspect
@Component
public class PermissaoAspect {

  private final PermissaoGuard permissaoGuard;
  private final EscopoProjetoResolver resolvedor;
  private final ExpressionParser parser = new SpelExpressionParser();
  private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  public PermissaoAspect(PermissaoGuard permissaoGuard, EscopoProjetoResolver resolvedor) {
    this.permissaoGuard = permissaoGuard;
    this.resolvedor = resolvedor;
  }

  @Around("@annotation(exigePermissao)")
  public Object verificar(ProceedingJoinPoint joinPoint, ExigePermissao exigePermissao)
      throws Throwable {
    UUID projetoId = resolverProjeto(joinPoint, exigePermissao.escopoProjeto());

    permissaoGuard.exigir(exigePermissao.valor(), projetoId);

    if (exigePermissao.escrita()) {
      permissaoGuard.exigirProjetoAtivo(projetoId);
    }
    return joinPoint.proceed();
  }

  private UUID resolverProjeto(ProceedingJoinPoint joinPoint, String expressao) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();

    MethodBasedEvaluationContext context =
        new MethodBasedEvaluationContext(
            joinPoint.getTarget(), method, joinPoint.getArgs(), parameterNameDiscoverer);
    context.setVariable("resolvedor", resolvedor);

    Object valor = parser.parseExpression(expressao).getValue(context);
    if (valor instanceof UUID uuid) {
      return uuid;
    }
    if (valor instanceof String texto) {
      return UUID.fromString(texto);
    }
    throw new PermissaoNegadaException("Nao foi possivel determinar o projeto do recurso.");
  }
}
