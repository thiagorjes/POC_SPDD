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

    private final ParticipacaoRepository participacoes =
            org.mockito.Mockito.mock(ParticipacaoRepository.class);
    private final br.com.idsd.kanban.internal.acesso.UsuarioRepository usuarios =
            org.mockito.Mockito.mock(br.com.idsd.kanban.internal.acesso.UsuarioRepository.class);
    private final ResolvedorDePermissao resolvedor =
            new ResolvedorDePermissao(participacoes, usuarios);

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

    // --- O caminho que le do banco. E o que importa para RNF-004: a resposta
    // sai do que esta gravado, e nunca de papel vindo do chamador.

    @Test
    @DisplayName("quem nao participa nao alcanca o projeto — vazio, e sem participacao")
    void naoParticipante() {
        var acesso = resolverPara(participacaoAusente(), usuarioComum());

        assertThat(acesso.permissoes()).isEmpty();
        assertThat(acesso.participa()).isFalse();
        assertThat(acesso.porAdministracaoGlobal()).isFalse();
        assertThat(acesso.semAlcance()).isTrue();
    }

    @Test
    @DisplayName("participar sem papel nao e o mesmo que nao participar")
    void participanteSemPapel() {
        var acesso = resolverPara(participacaoCom(), usuarioComum());

        // Os dois casos tem permissao vazia; o chamador precisa distingui-los
        // para escolher entre 403 e 404 (TechSpec §8, SCN-002.3).
        assertThat(acesso.permissoes()).isEmpty();
        assertThat(acesso.participa()).isTrue();
        assertThat(acesso.semAlcance()).isFalse();
    }

    @Test
    @DisplayName("a permissao sai da participacao gravada, nao de papel do chamador")
    void participanteComPapel() {
        var acesso = resolverPara(participacaoCom(Papel.DEV), usuarioComum());

        assertThat(acesso.permissoes())
                .containsExactlyInAnyOrder(Permissao.LER, Permissao.ESCREVER_TAREFA);
        assertThat(acesso.tem(Permissao.CONFIGURAR)).isFalse();
    }

    @Test
    @DisplayName("SCN-021.2 — a administracao global age em projeto de que nao participa")
    void adminGlobalSemParticipacao() {
        var acesso = resolverPara(participacaoAusente(), usuarioAdminGlobal());

        // RN-035: dispensa a participacao para ver e agir. E por construcao ela
        // nunca participa (RN-037), entao este e o caso normal e nao a excecao.
        assertThat(acesso.permissoes()).containsExactlyInAnyOrder(Permissao.values());
        assertThat(acesso.porAdministracaoGlobal()).isTrue();
        assertThat(acesso.participa()).isFalse();
        assertThat(acesso.semAlcance()).isFalse();
    }

    @Test
    @DisplayName("a marca de alcance global acompanha o acesso mesmo se a pessoa participa")
    void adminGlobalQueTambemParticipa() {
        var acesso = resolverPara(participacaoCom(Papel.GESTOR), usuarioAdminGlobal());

        // O alcance nao se soma ao papel: ele ja e o conjunto todo, e a marca
        // continua valendo porque e ela que dispensa a checagem de participacao.
        assertThat(acesso.permissoes()).containsExactlyInAnyOrder(Permissao.values());
        assertThat(acesso.porAdministracaoGlobal()).isTrue();
        assertThat(acesso.participa()).isTrue();
    }

    @Test
    @DisplayName("pode(...) decide pelos dois sujeitos, e nao so pelo participante")
    void atalhoDeDecisao() {
        assertThat(resolverEPerguntar(participacaoCom(Papel.GESTOR), usuarioComum(), Permissao.CONFIGURAR))
                .isFalse();
        assertThat(resolverEPerguntar(participacaoAusente(), usuarioAdminGlobal(), Permissao.CONFIGURAR))
                .isTrue();
    }

    private Set<Permissao> permissoesDe(Papel papel) {
        return resolvedor.permissoesDe(List.of(papel));
    }

    private static final java.util.UUID USUARIO = java.util.UUID.randomUUID();
    private static final java.util.UUID PROJETO = java.util.UUID.randomUUID();

    private ResolvedorDePermissao.Acesso resolverPara(
            java.util.Optional<Participacao> participacao,
            br.com.idsd.kanban.internal.acesso.Usuario usuario) {
        org.mockito.Mockito.when(participacoes.findByUsuarioIdAndProjetoId(USUARIO, PROJETO))
                .thenReturn(participacao);
        org.mockito.Mockito.when(usuarios.findById(USUARIO)).thenReturn(java.util.Optional.of(usuario));
        return resolvedor.acessoAoProjeto(USUARIO, PROJETO);
    }

    private boolean resolverEPerguntar(
            java.util.Optional<Participacao> participacao,
            br.com.idsd.kanban.internal.acesso.Usuario usuario,
            Permissao permissao) {
        org.mockito.Mockito.when(participacoes.findByUsuarioIdAndProjetoId(USUARIO, PROJETO))
                .thenReturn(participacao);
        org.mockito.Mockito.when(usuarios.findById(USUARIO)).thenReturn(java.util.Optional.of(usuario));
        return resolvedor.pode(USUARIO, PROJETO, permissao);
    }

    private java.util.Optional<Participacao> participacaoAusente() {
        return java.util.Optional.empty();
    }

    private java.util.Optional<Participacao> participacaoCom(Papel... papeis) {
        return java.util.Optional.of(new Participacao(
                usuarioComum(), new Projeto("Projeto", null), List.of(papeis)));
    }

    private br.com.idsd.kanban.internal.acesso.Usuario usuarioComum() {
        return new br.com.idsd.kanban.internal.acesso.Usuario(
                "sub-comum", "Comum", "comum@empresa.example");
    }

    private br.com.idsd.kanban.internal.acesso.Usuario usuarioAdminGlobal() {
        var usuario = org.mockito.Mockito.spy(new br.com.idsd.kanban.internal.acesso.Usuario(
                "sub-admin", "Admin", "admin@empresa.example"));
        // `adminGlobal` nao tem setter de proposito (ADR-010 item 5): a marca so
        // muda por promocao auditada ou por alteracao direta em banco.
        org.mockito.Mockito.when(usuario.isAdminGlobal()).thenReturn(true);
        return usuario;
    }
}
