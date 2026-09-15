package br.com.idsd.kanban.suporte;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Aplica o schema antes de qualquer contexto Spring subir.
 *
 * <p>A aplicacao nao migra: por ADR-011 o Flyway roda em servico dedicado, fora
 * do boot, e o backend sobe com {@code ddl-auto=validate} e sem permissao de
 * migrar. Esta extensao ocupa o lugar daquele servico dentro da suite, para que
 * o teste exercite a mesma configuracao que vai para producao — e nao uma
 * variante que migra sozinha e esconderia justamente a falha que o desenho
 * quer provocar (aplicacao contra banco desatualizado deve falhar).
 */
public class SchemaMigradoExtension implements BeforeAllCallback {

    private static boolean aplicado = false;

    @Override
    public synchronized void beforeAll(ExtensionContext contexto) {
        if (aplicado) {
            return;
        }
        Flyway.configure()
                .dataSource(
                        TesteDeIntegracao.POSTGRES.getJdbcUrl(),
                        TesteDeIntegracao.POSTGRES.getUsername(),
                        TesteDeIntegracao.POSTGRES.getPassword())
                .locations("classpath:db/migration")
                // O callback `afterMigrate` atribui a senha da role de login da
                // aplicacao, e o valor nunca esta no arquivo. Em producao ele
                // vem do segredo montado; aqui, da constante do arnes. Sem o
                // placeholder o Flyway falha, e falhar e o certo: a alternativa
                // seria a role existir sem senha e ninguem descobrir.
                .placeholders(java.util.Map.of(
                        "senha_aplicacao", TesteDeIntegracao.SENHA_APLICACAO))
                .load()
                .migrate();
        aplicado = true;
    }
}
