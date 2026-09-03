package br.com.crudao.kanban.security;

import br.com.crudao.kanban.rbac.Papeis;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.rbac.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisionamento JIT do usuario a partir das claims do Keycloak (ADR-003) e bootstrap unico do
 * admin global por e-mail configurado (ADR-007).
 */
@Slf4j
@Service
public class UsuarioProvisioningService {

  private final UsuarioRepository usuarioRepository;
  private final String adminEmail;

  public UsuarioProvisioningService(
      UsuarioRepository usuarioRepository,
      @Value("${kanban.bootstrap.admin-email}") String adminEmail) {
    this.usuarioRepository = usuarioRepository;
    this.adminEmail = adminEmail;
  }

  /**
   * Resolve o usuario local do token, criando-o no primeiro acesso com o papel legado {@code user}
   * (RN-014). O bootstrap de admin global e aplicado uma unica vez e nunca reaplicado (ADR-007).
   */
  @Transactional
  public Usuario provisionar(Jwt jwt) {
    String sub = jwt.getSubject();
    String email = jwt.getClaimAsString("email");
    String nome = jwt.getClaimAsString("name");

    Usuario usuario =
        usuarioRepository
            .findByKeycloakSub(sub)
            .orElseGet(
                () -> {
                  log.info("Provisionando novo usuario a partir do token OIDC");
                  return usuarioRepository.save(
                      Usuario.builder()
                          .keycloakSub(sub)
                          .email(email)
                          .nome(nome != null ? nome : email)
                          .adminGlobal(false)
                          .build());
                });

    if (!usuario.isAdminGlobal() && adminEmail != null && adminEmail.equalsIgnoreCase(usuario.getEmail())) {
      usuario.setAdminGlobal(true);
      usuario = usuarioRepository.save(usuario);
      log.info("Bootstrap de admin global aplicado ao usuario {}", usuario.getId());
    }
    return usuario;
  }

  /** Papel atribuido ao usuario recem-provisionado; sem nenhuma permissao (RN-014). */
  public String papelInicial() {
    return Papeis.USER;
  }
}
