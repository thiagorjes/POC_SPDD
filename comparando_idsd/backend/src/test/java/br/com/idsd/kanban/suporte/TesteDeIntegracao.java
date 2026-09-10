package br.com.idsd.kanban.suporte;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base dos testes de integracao de contrato.
 *
 * <p>Escrito antes da implementacao, a partir dos contratos congelados. Nenhum
 * arquivo de producao foi lido para produzi-lo.
 *
 * <p>H2 nao serve aqui: nao implementa LISTEN/NOTIFY, que e o mecanismo de
 * RF-020, nem os indices unicos parciais e a semantica de intervalo do modelo de
 * dados. Um teste verde em H2 diria pouco, com aparencia de prova.
 *
 * <p>A imagem do PostgreSQL vem da mesma variavel que o compose consome. "A
 * mesma imagem" nao pode ser convencao em prosa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(SchemaMigradoExtension.class)
public abstract class TesteDeIntegracao {

    /** Consumida tambem pelo compose. Ver infra/docker. */
    private static final String IMAGEM_POSTGRES =
            System.getenv().getOrDefault("POSTGRES_IMAGE", "postgres:16-alpine");

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse(IMAGEM_POSTGRES)
                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("kanban")
                    .withUsername("kanban")
                    .withPassword("kanban")
                    .withReuse(true);

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    /**
     * Massa de teste montada pela propria API HTTP, e nunca por escrita direta
     * em tabela nem por classe de producao. E isso que mantem a suite acoplada
     * so ao contrato congelado: se o contrato mudar sem emenda, ela quebra; se a
     * implementacao interna mudar, ela nao percebe.
     */
    protected Cenario cenario;

    @org.junit.jupiter.api.BeforeEach
    void prepararCenario() {
        this.cenario = new Cenario(mockMvc);
    }

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // A aplicacao nunca migra: o schema e aplicado por servico dedicado
        // (ADR-011). Aqui a extensao faz o papel desse servico, e a aplicacao
        // sobe validando, exatamente como em producao.
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
