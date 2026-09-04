package br.com.crudao.kanban.bdd;

import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.integracao.PostgresDeTeste;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Contexto Spring dos cenarios BDD (bloco 19.3), sobre o mesmo PostgreSQL do Testcontainers usado
 * pelos testes de integracao. O publisher e espionado para permitir afirmar sobre o evento emitido
 * sem depender do tempo de entrega do canal LISTEN/NOTIFY.
 */
@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class ConfiguracaoCucumber {

  @MockitoSpyBean EventoBoardPublisher eventoBoardPublisher;

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    PostgresDeTeste.registrar(registry);
  }
}
