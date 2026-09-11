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

    @Autowired
    private javax.sql.DataSource fonteDeDados;

    /**
     * Massa de teste montada pela propria API HTTP, e nunca por escrita direta
     * em tabela nem por classe de producao. E isso que mantem a suite acoplada
     * so ao contrato congelado: se o contrato mudar sem emenda, ela quebra; se a
     * implementacao interna mudar, ela nao percebe.
     */
    protected Cenario cenario;

    @org.junit.jupiter.api.BeforeEach
    void prepararCenario() throws java.sql.SQLException {
        esvaziarBanco();
        ProvedorSimulado.instancia().disponivel();
        this.cenario = new Cenario(mockMvc);
    }

    /**
     * Devolve o banco ao estado vazio antes de cada teste.
     *
     * <p>O contêiner e reusado e o schema e aplicado uma vez so, entao sem isto o
     * estado de um teste vaza para o seguinte. Nao e higiene abstrata: SCN-021.1
     * afirma "ainda nao existe administrador global no sistema", e essa
     * pre-condicao e falsa a partir do primeiro teste que entrar como
     * administrador global — o cenario passaria ou falharia conforme a ordem em
     * que o JUnit resolvesse executar as classes, que e a forma mais cara de
     * teste intermitente.
     *
     * <p>A varredura e pelo catalogo do proprio banco, e nao por uma lista de
     * tabelas escrita a mao: lista a mao envelhece calada na proxima migration, e
     * o sintoma reaparece como contaminacao de estado meses depois.
     */
    protected void esvaziarBanco() throws java.sql.SQLException {
        try (var conexao = fonteDeDados.getConnection();
                var comando = conexao.createStatement()) {
            var tabelas = new java.util.ArrayList<String>();
            try (var linhas = comando.executeQuery("""
                    select tablename from pg_tables
                     where schemaname = 'public'
                       and tablename <> 'flyway_schema_history'
                    """)) {
                while (linhas.next()) {
                    tabelas.add(linhas.getString(1));
                }
            }
            if (tabelas.isEmpty()) {
                return;
            }
            comando.execute("truncate table "
                    + String.join(", ", tabelas)
                    + " restart identity cascade");
        }
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
        // A identidade e simulada, e o servidor de recurso busca a chave nela.
        // Sem isto o unico teste que exercita o decodificador — a
        // indisponibilidade de SCN-001.3 — nao teria como distinguir "provedor
        // fora do ar" de "credencial ruim": os dois cairiam no mesmo 401.
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> ProvedorSimulado.instancia().jwkSetUri());
        // O sujeito designado a administracao global (ADR-010). A promocao e por
        // `sub` verificado, nunca por e-mail, e SCN-021.1 verifica exatamente a
        // diferenca entre os dois.
        registry.add("idsd.admin-global.subject-id", () -> Sujeitos.SUB_ADMIN_GLOBAL);
    }
}
