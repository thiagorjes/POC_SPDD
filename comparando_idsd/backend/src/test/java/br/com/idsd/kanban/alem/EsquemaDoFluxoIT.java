package br.com.idsd.kanban.alem;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O esquema de {@code etapa} e {@code raia} e afirmado contra o catalogo.
 *
 * <p>Nenhum cenario congelado verifica isto, e nenhum outro teste verificaria:
 * {@code ddl-auto=validate} so confere o que a entidade <b>declara</b>, e a
 * entidade nao declara indice, valor padrao nem chave estrangeira. Removendo o
 * indice unico parcial, o {@code NOT NULL} de {@code terminal}, o
 * {@code DEFAULT false} ou a FK para {@code projeto}, a suite inteira ficava
 * verde — zero cobertura automatizada do esquema (ACH-08 da revisao de
 * TASK-02.1).
 *
 * <p>As afirmacoes sao sobre a <b>definicao</b> e nao sobre o comportamento
 * observado por uma insercao, de proposito: insercao que falha prova que
 * <i>alguma</i> restricao recusou, e nao qual. A excecao e o indice parcial,
 * afirmado pelas duas metades — a definicao carrega o predicado, e arquivar
 * libera a ordem —, porque e a metade "parcial" que uma restricao total
 * quebraria sem mudar o nome do indice.
 */
class EsquemaDoFluxoIT extends TesteDeIntegracao {

    @Test
    @DisplayName("etapa tem o indice unico PARCIAL de ordem por projeto, e ele e parcial")
    void indiceParcialDeOrdemDaEtapa() throws Exception {
        var definicao = valor(
                "SELECT indexdef FROM pg_indexes "
                        + "WHERE tablename = 'etapa' AND indexname = 'etapa_projeto_ordem_unico'");

        Assertions.assertThat(definicao)
                .as("o indice unico parcial de ordem por projeto precisa existir")
                .isNotNull()
                .contains("UNIQUE")
                .contains("projeto_id")
                .contains("ordem")
                .as("sem o predicado a restricao vira total e impede reusar a ordem de uma "
                        + "etapa arquivada, que e operacao legitima")
                .contains("WHERE (arquivada_em IS NULL)");
    }

    @Test
    @DisplayName("raia NAO tem restricao de ordem unica: RN-023 nao lhe da semantica de ordem")
    void raiaNaoTemOrdemUnica() throws Exception {
        // ACH-06: o indice existia sem origem em data-model.md §6 e sem 422
        // correspondente no contrato de PUT .../raias, e foi removido na
        // migration de correcao. Afirmar a ausencia impede que ele volte por
        // simetria com `etapa`, que e como ele entrou.
        // A chave primaria tambem e indice unico, e por isso o filtro e por
        // `ordem`: afirmar "nenhum indice unico em raia" reprovaria por causa
        // do `raia_pkey`, que precisa existir.
        Assertions.assertThat(lista(
                        "SELECT indexdef FROM pg_indexes WHERE tablename = 'raia' "
                                + "AND indexdef LIKE '%UNIQUE%' AND indexdef LIKE '%ordem%'"))
                .as("raia e agrupamento livre: ordem empatada desempata a exibicao, nao uma "
                        + "transicao")
                .isEmpty();
    }

    @Test
    @DisplayName("terminal e NOT NULL com DEFAULT false")
    void colunaTerminal() throws Exception {
        Assertions.assertThat(valor(
                        "SELECT is_nullable FROM information_schema.columns "
                                + "WHERE table_name = 'etapa' AND column_name = 'terminal'"))
                .as("etapa sem resposta sobre ser terminal torna RN-005 indecidivel")
                .isEqualTo("NO");

        Assertions.assertThat(valor(
                        "SELECT column_default FROM information_schema.columns "
                                + "WHERE table_name = 'etapa' AND column_name = 'terminal'"))
                .as("o padrao seguro e nao-terminal: etapa criada sem declaracao nao pode "
                        + "concluir tarefa por omissao")
                .isEqualTo("false");
    }

    @Test
    @DisplayName("etapa e raia referenciam projeto por chave estrangeira")
    void chaveEstrangeiraParaProjeto() throws Exception {
        for (String tabela : List.of("etapa", "raia")) {
            Assertions.assertThat(lista(
                            "SELECT pg_get_constraintdef(oid) FROM pg_constraint "
                                    + "WHERE conrelid = '" + tabela + "'::regclass AND contype = 'f'"))
                    .as("%s sem FK deixa orfao alcancavel, e orfao de projeto e linha que "
                            + "nenhuma rota consegue mais ler nem apagar", tabela)
                    .anySatisfy(definicao -> Assertions.assertThat(definicao)
                            .contains("FOREIGN KEY (projeto_id)")
                            .contains("REFERENCES projeto(id)"));
        }
    }

    @Test
    @DisplayName("arquivar libera a ordem: duas etapas com a mesma ordem convivem se uma saiu")
    void arquivarLiberaAOrdem() throws Exception {
        // A metade de comportamento do indice parcial. Sem ela, trocar o
        // predicado por outro qualquer manteria o teste de definicao verde
        // enquanto a reconfiguracao do fluxo passaria a ser impossivel.
        var projeto = cenario.projeto("Esquema");
        try (Connection conexao = conectarComoAplicacao()) {
            executar(conexao, "INSERT INTO etapa (id, projeto_id, nome, ordem, terminal, "
                    + "arquivada_em) VALUES (gen_random_uuid(), '" + projeto
                    + "', 'Saiu', 3, false, now())");

            Assertions.assertThatCode(() -> executar(conexao,
                            "INSERT INTO etapa (id, projeto_id, nome, ordem, terminal) VALUES "
                                    + "(gen_random_uuid(), '" + projeto + "', 'Entrou', 3, false)"))
                    .doesNotThrowAnyException();

            Assertions.assertThatThrownBy(() -> executar(conexao,
                            "INSERT INTO etapa (id, projeto_id, nome, ordem, terminal) VALUES "
                                    + "(gen_random_uuid(), '" + projeto + "', 'Duplicada', 3, false)"))
                    .as("duas etapas ativas na mesma ordem tornam \"a etapa seguinte\" ambigua "
                            + "(RN-005)")
                    .isInstanceOf(java.sql.SQLException.class);
        }
    }

    // ------------------------------------------------------------ utilitarios

    private String valor(String sql) throws Exception {
        var linhas = lista(sql);
        return linhas.isEmpty() ? null : linhas.get(0);
    }

    private List<String> lista(String sql) throws Exception {
        try (Connection conexao = conectarComoAplicacao();
                var stmt = conexao.prepareStatement(sql)) {
            var resultado = stmt.executeQuery();
            var linhas = new ArrayList<String>();
            while (resultado.next()) {
                linhas.add(resultado.getString(1));
            }
            return linhas;
        }
    }

    private void executar(Connection conexao, String sql) throws Exception {
        try (var stmt = conexao.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    private Connection conectarComoAplicacao() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
