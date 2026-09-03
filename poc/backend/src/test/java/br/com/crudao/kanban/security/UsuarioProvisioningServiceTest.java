package br.com.crudao.kanban.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.rbac.Papeis;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.rbac.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

/** Provisionamento JIT (ADR-003) e bootstrap unico do admin global por e-mail (ADR-007). */
@ExtendWith(MockitoExtension.class)
class UsuarioProvisioningServiceTest {

  private static final String ADMIN_EMAIL = "admin@exemplo.test";

  @Mock private UsuarioRepository usuarioRepository;

  private UsuarioProvisioningService service(String adminEmail) {
    return new UsuarioProvisioningService(usuarioRepository, adminEmail);
  }

  private Jwt jwt(String sub, String email, String nome) {
    Jwt.Builder builder =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject(sub)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .claims(claims -> claims.put("email", email));
    if (nome != null) {
      builder.claim("name", nome);
    }
    return builder.build();
  }

  @Test
  @DisplayName("primeiro acesso cria o usuario local a partir das claims do token")
  void provisionaNoPrimeiroAcesso() {
    when(usuarioRepository.findByKeycloakSub("sub-1")).thenReturn(Optional.empty());
    when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Usuario usuario = service(ADMIN_EMAIL).provisionar(jwt("sub-1", "ana@exemplo.test", "Ana"));

    assertThat(usuario.getKeycloakSub()).isEqualTo("sub-1");
    assertThat(usuario.getEmail()).isEqualTo("ana@exemplo.test");
    assertThat(usuario.getNome()).isEqualTo("Ana");
    assertThat(usuario.isAdminGlobal()).isFalse();
  }

  @Test
  @DisplayName("token sem claim de nome usa o e-mail como nome de exibicao")
  void nomeAusenteUsaEmail() {
    when(usuarioRepository.findByKeycloakSub("sub-2")).thenReturn(Optional.empty());
    when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Usuario usuario = service(ADMIN_EMAIL).provisionar(jwt("sub-2", "bia@exemplo.test", null));

    assertThat(usuario.getNome()).isEqualTo("bia@exemplo.test");
  }

  @Test
  @DisplayName("acesso subsequente reutiliza o usuario existente sem reescrever nada")
  void reutilizaUsuarioExistente() {
    Usuario existente =
        Usuario.builder()
            .id(UUID.randomUUID())
            .keycloakSub("sub-3")
            .email("ana@exemplo.test")
            .nome("Ana")
            .adminGlobal(false)
            .build();
    when(usuarioRepository.findByKeycloakSub("sub-3")).thenReturn(Optional.of(existente));

    Usuario usuario = service(ADMIN_EMAIL).provisionar(jwt("sub-3", "ana@exemplo.test", "Ana"));

    assertThat(usuario).isSameAs(existente);
    verify(usuarioRepository, never()).save(any());
  }

  @Test
  @DisplayName("o e-mail configurado recebe admin global — a flag nunca vem do payload")
  void bootstrapDoAdminGlobal() {
    Usuario existente =
        Usuario.builder()
            .id(UUID.randomUUID())
            .keycloakSub("sub-4")
            .email("ADMIN@exemplo.test")
            .nome("Admin")
            .adminGlobal(false)
            .build();
    when(usuarioRepository.findByKeycloakSub("sub-4")).thenReturn(Optional.of(existente));
    when(usuarioRepository.save(existente)).thenReturn(existente);

    Usuario usuario = service(ADMIN_EMAIL).provisionar(jwt("sub-4", "ADMIN@exemplo.test", "Admin"));

    assertThat(usuario.isAdminGlobal()).isTrue();
  }

  @Test
  @DisplayName("sem e-mail de bootstrap configurado ninguem e promovido")
  void semEmailDeBootstrap() {
    Usuario existente =
        Usuario.builder()
            .id(UUID.randomUUID())
            .keycloakSub("sub-5")
            .email("ana@exemplo.test")
            .adminGlobal(false)
            .build();
    when(usuarioRepository.findByKeycloakSub("sub-5")).thenReturn(Optional.of(existente));

    assertThat(service(null).provisionar(jwt("sub-5", "ana@exemplo.test", "Ana")).isAdminGlobal())
        .isFalse();
    verify(usuarioRepository, never()).save(any());
  }

  @Test
  @DisplayName("o bootstrap nao e reaplicado a quem ja e admin global")
  void bootstrapNaoReaplicado() {
    Usuario existente =
        Usuario.builder()
            .id(UUID.randomUUID())
            .keycloakSub("sub-6")
            .email(ADMIN_EMAIL)
            .adminGlobal(true)
            .build();
    when(usuarioRepository.findByKeycloakSub("sub-6")).thenReturn(Optional.of(existente));

    service(ADMIN_EMAIL).provisionar(jwt("sub-6", ADMIN_EMAIL, "Admin"));

    verify(usuarioRepository, never()).save(any());
  }

  @Test
  @DisplayName("o usuario recem-provisionado recebe o papel legado sem permissao (RN-014)")
  void papelInicialSemPermissao() {
    assertThat(service(ADMIN_EMAIL).papelInicial()).isEqualTo(Papeis.USER);
  }
}
