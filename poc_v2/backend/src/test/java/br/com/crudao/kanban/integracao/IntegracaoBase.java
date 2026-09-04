package br.com.crudao.kanban.integracao;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Base dos testes de integracao (bloco 19.2): contexto real sobre PostgreSQL do Testcontainers. */
@SpringBootTest
@ActiveProfiles("test")
abstract class IntegracaoBase {

  @Autowired protected CenarioDeTeste cenarioDeTeste;

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    PostgresDeTeste.registrar(registry);
  }

  @BeforeEach
  void limparBase() {
    cenarioDeTeste.limpar();
  }
}
