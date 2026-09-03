package br.com.crudao.kanban.integracao;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de integracao contra PostgreSQL real: indice unico parcial, {@code now()} transacional e
 * {@code LISTEN/NOTIFY} nao existem em banco em memoria, e sao justamente as garantias que
 * sustentam lead-time e tempo real (ADR-002/ADR-004).
 *
 * <p>O container e {@code static}: uma instancia para toda a suite, com Flyway aplicando as
 * migrations no primeiro contexto.
 */
@Testcontainers
@SpringBootTest
@Import(CenarioFixture.class)
public abstract class AbstractIntegracaoTest {

  @SuppressWarnings("resource")
  public static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("kanban")
          .withUsername("kanban")
          .withPassword("kanban")
          .withReuse(true);

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void propriedades(DynamicPropertyRegistry registro) {
    registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registro.add("spring.datasource.username", POSTGRES::getUsername);
    registro.add("spring.datasource.password", POSTGRES::getPassword);
    // Evita descoberta OIDC na subida: o teste nao exercita o fluxo de token.
    registro.add(
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> "http://localhost:0/protocol/openid-connect/certs");
    registro.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "");
  }
}
