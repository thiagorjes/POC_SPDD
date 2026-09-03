package br.com.crudao.kanban.security;

import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.rbac.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Resolve o usuario local correspondente ao token da requisicao corrente. */
@Component
@RequiredArgsConstructor
public class UsuarioAtualProvider {

  private final UsuarioProvisioningService provisioningService;

  public Usuario obrigatorio() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken token)) {
      throw new PermissaoNegadaException("Requisicao sem autenticacao valida.");
    }
    Jwt jwt = token.getToken();
    return provisioningService.provisionar(jwt);
  }
}
