package br.com.crudao.kanban.integracao;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Container PostgreSQL unico para toda a suite (bloco 19.2). Um container por JVM evita repetir
 * as nove migrations Flyway a cada classe e permite compartilhar a instancia entre os testes de
 * integracao e os cenarios BDD.
 */
public final class PostgresDeTeste {

  private static final PostgreSQLContainer<?> INSTANCIA =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
          .withDatabaseName("kanban")
          .withUsername("kanban")
          .withPassword("kanban");

  static {
    INSTANCIA.start();
  }

  private PostgresDeTeste() {}

  public static void registrar(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", INSTANCIA::getJdbcUrl);
    registry.add("spring.datasource.username", INSTANCIA::getUsername);
    registry.add("spring.datasource.password", INSTANCIA::getPassword);
  }
}
