package br.com.idsd.kanban.internal.projeto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Critério 6 da TASK-01.3 — o catalogo de permissoes por papel e por acumulo.
 *
 * <p>Este arquivo <b>nao pertence a suite congelada</b>: ele nao cobre cenario
 * Gherkin nenhum e nao foi escrito pelo {@code /tests}. Existe porque o criterio
 * de aceite da task pede verificacao unitaria de uma tabela que so vive em
 * codigo — sem ele, a unica prova do catalogo seria SCN-002.1, que depende de
 * rotas de tasks posteriores e nao exercita {@code project_admin} nem acumulo.
 *
 * <p>Nao toca em {@code .feature} nem em step definition, e nao altera nada da
 * suite existente.
 */
class ResolvedorDePermissaoTest {

    private final ResolvedorDePermissao resolvedor = new ResolvedorDePermissao(null);

    @Test
    @DisplayName("project_admin administra o projeto, mas nao reabre")
    void projectAdmin() {
        assertThat(permissoesDe(Papel.PROJECT_ADMIN))
                .containsExactlyInAnyOrder(
                        Permissao.LER,
                        Permissao.ESCREVER_TAREFA,
                        Permissao.DESBLOQUEAR,
                        Permissao.ENCERRAR,
                        Permissao.CONFIGURAR)
                // Papel poderoso nao e papel total: reabrir e privativo do
                // product_owner (RN-015).
                .doesNotContain(Permissao.REABRIR);
    }

    @Test
    @DisplayName("product_owner reabre e encerra sem administrar o projeto")
    void productOwner() {
        assertThat(permissoesDe(Papel.PRODUCT_OWNER))
                .containsExactlyInAnyOrder(
                        Permissao.LER,
                        Permissao.ESCREVER_TAREFA,
                        Permissao.DESBLOQUEAR,
                        Permissao.ENCERRAR,
                        Permissao.REABRIR)
                .doesNotContain(Permissao.CONFIGURAR);
    }

    @Test
    @DisplayName("dev escreve tarefa, mas nao desbloqueia nem encerra")
    void dev() {
        assertThat(permissoesDe(Papel.DEV))
                .containsExactlyInAnyOrder(Permissao.LER, Permissao.ESCREVER_TAREFA);
    }

    @Test
    @DisplayName("gestor e somente-leitura")
    void gestor() {
        assertThat(permissoesDe(Papel.GESTOR)).containsExactly(Permissao.LER);
    }

    @Test
    @DisplayName("user nao le o board — participar nao concede nada por si")
    void user() {
        assertThat(permissoesDe(Papel.USER)).isEmpty();
    }

    @Test
    @DisplayName("papeis acumulam: a uniao, e nada alem dela")
    void acumulo() {
        assertThat(resolvedor.permissoesDe(List.of(Papel.PROJECT_ADMIN, Papel.PRODUCT_OWNER)))
                .containsExactlyInAnyOrder(
                        Permissao.LER,
                        Permissao.ESCREVER_TAREFA,
                        Permissao.DESBLOQUEAR,
                        Permissao.ENCERRAR,
                        Permissao.CONFIGURAR,
                        Permissao.REABRIR);

        // gestor somado a dev nao subtrai: o somente-leitura do gestor e
        // ausencia de permissao, nao proibicao que viaje junto.
        assertThat(resolvedor.permissoesDe(List.of(Papel.GESTOR, Papel.DEV)))
                .containsExactlyInAnyOrder(Permissao.LER, Permissao.ESCREVER_TAREFA);
    }

    @Test
    @DisplayName("sem papel algum, conjunto vazio — nunca permissao por omissao")
    void semPapel() {
        assertThat(resolvedor.permissoesDe(EnumSet.noneOf(Papel.class))).isEmpty();
    }

    @Test
    @DisplayName("o codigo persistido e o do catalogo, e codigo desconhecido reprova")
    void codigos() {
        assertThat(Papel.PROJECT_ADMIN.getCodigo()).isEqualTo("project_admin");
        assertThat(Papel.porCodigo("product_owner")).isEqualTo(Papel.PRODUCT_OWNER);
        assertThat(Papel.porCodigo("gestor")).isEqualTo(Papel.GESTOR);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Papel.porCodigo("super_admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Set<Permissao> permissoesDe(Papel papel) {
        return resolvedor.permissoesDe(List.of(papel));
    }
}
