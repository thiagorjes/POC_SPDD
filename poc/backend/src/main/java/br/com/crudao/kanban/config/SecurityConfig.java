package br.com.crudao.kanban.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Resource server OIDC validando o JWT do Keycloak. <b>Sem fallback de autenticacao local</b>
 * (ADR-006): Keycloak indisponivel implica falha explicita de login.
 *
 * <p>A autorizacao fina e responsabilidade da aplicacao ({@code PermissaoGuard}), nao do token
 * (ADR-003) — o filter chain apenas exige autenticacao.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final List<String> origensPermitidas;

  public SecurityConfig(
      @Value("${kanban.cors.origens-permitidas}") List<String> origensPermitidas) {
    this.origensPermitidas = origensPermitidas;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health/**", "/actuator/info")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/ws/**")
                    .authenticated()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
  }

  /**
   * Origens vem de configuracao e nunca sao {@code *}: o token viaja no header {@code
   * Authorization}, entao {@code allowCredentials} permanece falso e nao ha cookie de sessao a
   * proteger.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuracao = new CorsConfiguration();
    configuracao.setAllowedOrigins(origensPermitidas);
    configuracao.setAllowedMethods(
        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuracao.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
    fonte.registerCorsConfiguration("/api/**", configuracao);
    fonte.registerCorsConfiguration("/ws/**", configuracao);
    return fonte;
  }
}
