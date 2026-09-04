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
 * Resource server OIDC validando o JWT do Keycloak. Sem fallback de autenticacao local: Keycloak
 * indisponivel implica falha explicita de login (ADR-006).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${kanban.cors.origens-permitidas:http://localhost:3000}")
  private List<String> origensPermitidas;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.cors(Customizer.withDefaults())
        // API stateless com token no header: nao ha cookie de sessao a proteger contra CSRF.
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/api/**", "/ws/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
  }

  /**
   * Origens explicitas, nunca {@code *}. Sem isto o frontend em :3000 nao alcanca o backend em
   * :8081 no navegador. {@code allowCredentials=false}: o token viaja no header, nao em cookie.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuracao = new CorsConfiguration();
    configuracao.setAllowedOrigins(origensPermitidas);
    configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    configuracao.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuracao);
    source.registerCorsConfiguration("/ws/**", configuracao);
    return source;
  }
}
