package br.com.crudao.kanban.security;

import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.rbac.CodigoPapel;
import br.com.crudao.kanban.rbac.PapelRepository;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.rbac.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisionamento just-in-time do usuario a partir das claims do Keycloak (ADR-003) e bootstrap do
 * admin global por e-mail configurado (ADR-007).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioProvisioningService {

  private final UsuarioRepository usuarioRepository;
  private final PapelRepository papelRepository;

  @Value("${kanban.bootstrap.admin-email:}")
  private String adminEmailBootstrap;

  /**
   * Cria o usuario no primeiro acesso com o papel legado {@code user} (RN-014) e aplica o bootstrap
   * de admin global uma unica vez (ADR-007).
   */
  @Transactional
  public Usuario provisionar(Jwt jwt) {
    String sub = jwt.getSubject();
    if (sub == null || sub.isBlank()) {
      throw new PermissaoNegadaException("Token sem identificador de usuario (claim sub).");
    }
    Usuario usuario =
        usuarioRepository
            .findByKeycloakSub(sub)
            .orElseGet(
                () -> criar(sub, jwt.getClaimAsString("email"), jwt.getClaimAsString("name")));
    aplicarBootstrapAdmin(usuario);
    return usuarioRepository.save(usuario);
  }

  private Usuario criar(String sub, String email, String nome) {
    // O papel legado 'user' existe no catalogo e nao concede nenhuma permissao (RN-014).
    papelRepository
        .findByCodigo(CodigoPapel.USER)
        .orElseThrow(() -> new IllegalStateException("Catalogo de papeis nao inicializado."));
    log.info("Provisionando novo usuario a partir do token do provedor de identidade.");
    return Usuario.builder()
        .keycloakSub(sub)
        .email(email == null ? sub + "@sem-email.local" : email)
        .nome(nome == null ? "Usuario" : nome)
        .adminGlobal(false)
        .build();
  }

  /** ADR-007: aplicado uma unica vez, nunca reaplicado nem exposto em endpoint de escrita. */
  private void aplicarBootstrapAdmin(Usuario usuario) {
    if (adminEmailBootstrap == null || adminEmailBootstrap.isBlank() || usuario.isAdminGlobal()) {
      return;
    }
    if (adminEmailBootstrap.equalsIgnoreCase(usuario.getEmail())) {
      usuario.setAdminGlobal(true);
      log.info("Bootstrap de administrador global aplicado ao usuario {}", usuario.getId());
    }
  }
}
