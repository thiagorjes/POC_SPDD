package br.com.crudao.kanban.security;

import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.rbac.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Resolve o usuario autenticado da requisicao corrente a partir do JWT do Keycloak. */
@Component
@RequiredArgsConstructor
public class UsuarioAtualProvider {

  private final UsuarioRepository usuarioRepository;
  private final UsuarioProvisioningService usuarioProvisioningService;

  /** Usuario da requisicao corrente, provisionado just-in-time no primeiro acesso (ADR-003). */
  @Transactional
  public Usuario atual() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new PermissaoNegadaException("Requisicao sem usuario autenticado.");
    }
    return usuarioRepository
        .findByKeycloakSub(jwt.getSubject())
        .orElseGet(() -> usuarioProvisioningService.provisionar(jwt));
  }
}
