package br.com.crudao.kanban.bdd;

import br.com.crudao.kanban.integracao.AbstractIntegracaoTest;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Os cenarios de aceite rodam sobre a aplicacao inteira via MockMvc: e a unica camada onde o
 * {@code @ExigePermissao}, o contrato de erro e os status HTTP sao exercitados juntos, que e o que
 * os criterios de RF-001..RF-019 descrevem.
 *
 * <p>Reutiliza o mesmo container da suite de integracao para nao subir um segundo PostgreSQL.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(MundoBdd.class)
public class ConfiguracaoBdd {

  @DynamicPropertySource
  static void propriedades(DynamicPropertyRegistry registro) {
    registro.add("spring.datasource.url", AbstractIntegracaoTest.POSTGRES::getJdbcUrl);
    registro.add("spring.datasource.username", AbstractIntegracaoTest.POSTGRES::getUsername);
    registro.add("spring.datasource.password", AbstractIntegracaoTest.POSTGRES::getPassword);
    registro.add(
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> "http://localhost:0/protocol/openid-connect/certs");
    registro.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "");
  }

  static {
    if (!AbstractIntegracaoTest.POSTGRES.isRunning()) {
      AbstractIntegracaoTest.POSTGRES.start();
    }
  }
}
