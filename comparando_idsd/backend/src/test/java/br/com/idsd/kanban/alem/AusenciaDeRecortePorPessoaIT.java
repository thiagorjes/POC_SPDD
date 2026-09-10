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
 * RN-014 — ausencia estrutural de recorte por pessoa.
 *
 * <p>C-02 e C-03 viraram restricao estrutural, e nao acordo de uso. Os cenarios
 * congelados verificam a restricao pela superficie — nenhuma resposta traz
 * campo de pessoa, nenhum filtro e aceito —, e superficie se conserta com um
 * commit. O que este teste verifica e a razao pela qual a regra resiste: a
 * projecao nao tem coluna de pessoa, entao nao ha o que agrupar.
 *
 * <p>E a regra mais exposta a erosao silenciosa do sistema. Nenhum teste de
 * cenario falharia se alguem acrescentasse a coluna "so para o log" e, seis
 * meses depois, uma consulta a usasse.
 */
class AusenciaDeRecortePorPessoaIT extends TesteDeIntegracao {

    private static final List<String> NOMES_DE_PESSOA = List.of(
            "usuario_id", "responsavel_id", "pessoa_id", "ator_id", "assumida_por");

    @Test
    @DisplayName("a projecao de intervalos nao tem coluna que identifique pessoa")
    void projecaoNaoTemColunaDePessoa() throws Exception {
        var colunas = colunasDe("intervalo_tarefa");

        Assertions.assertThat(colunas)
                .as("a projecao e o unico lugar de onde sai tempo agregado; "
                        + "coluna de pessoa aqui torna o recorte uma consulta de distancia")
                .doesNotContainAnyElementsOf(NOMES_DE_PESSOA);
    }

    @Test
    @DisplayName("o log guarda o ator, e e o unico lugar onde a pessoa aparece")
    void oLogGuardaOAtorEEOUnicoLugar() throws Exception {
        // A contraparte necessaria: o historico da tarefa precisa dizer quem
        // fez o que, e SCN-019.3 depende disso. A restricao nao e "nao guardar
        // pessoa", e sim "nao agregar tempo por pessoa" — sao coisas
        // diferentes, e confundi-las apagaria o log.
        Assertions.assertThat(colunasDe("evento_tarefa")).contains("ator_id");
    }

    @Test
    @DisplayName("nenhuma visao de banco cruza a projecao com a tabela de usuario")
    void nenhumaVisaoCruzaProjecaoComUsuario() throws Exception {
        var definicoes = new StringBuilder();
        try (Connection conexao = conectar();
                var stmt = conexao.prepareStatement("""
                        SELECT definition FROM pg_views WHERE schemaname = 'public'
                        """);
                var resultado = stmt.executeQuery()) {
            while (resultado.next()) {
                definicoes.append(resultado.getString(1).toLowerCase()).append('\n');
            }
        }

        // A visao e a porta dos fundos: ela reintroduziria o recorte sem
        // alterar tabela nenhuma, e o teste de coluna acima passaria em verde.
        for (String linha : definicoes.toString().split("\n")) {
            if (linha.contains("intervalo_tarefa")) {
                Assertions.assertThat(linha).doesNotContain("usuario");
            }
        }
    }

    private List<String> colunasDe(String tabela) throws Exception {
        var colunas = new ArrayList<String>();
        try (Connection conexao = conectar();
                var stmt = conexao.prepareStatement("""
                        SELECT column_name FROM information_schema.columns
                         WHERE table_schema = 'public' AND table_name = ?
                        """)) {
            stmt.setString(1, tabela);
            var resultado = stmt.executeQuery();
            while (resultado.next()) {
                colunas.add(resultado.getString(1));
            }
        }
        Assertions.assertThat(colunas)
                .as("tabela %s deve existir no esquema", tabela)
                .isNotEmpty();
        return colunas;
    }

    private Connection conectar() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
