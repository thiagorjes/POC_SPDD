package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RN-002 — as tres dimensoes sao ortogonais.
 *
 * <p>Foi o achado estrutural do {@code /solution} e a emenda mais cara do PRD.
 * Os cenarios congelados verificam a ortogonalidade um par por vez — a marca
 * sobrevivendo ao movimento, a devolucao, a tomada, a leitura. Este teste
 * verifica a propriedade em si, no esquema, porque e ali que ela se sustenta:
 * enquanto {@code IMPEDIDA} foi valor de {@code condicao}, toda movimentacao a
 * sobrescrevia em silencio, e preserva-la dependia de quem escrevesse o
 * servico lembrar de nao toca-la.
 */
class OrtogonalidadeDasDimensoesIT extends TesteDeIntegracao {

    @Test
    @DisplayName("IMPEDIDA nao pertence ao dominio de condicao, no banco")
    void impedidaNaoPertenceAoDominioDeCondicao() throws Exception {
        var valores = valoresAceitosPorCondicao();

        Assertions.assertThat(valores)
                .as("com IMPEDIDA no dominio, a marca volta a ser sobrescrivel "
                        + "por qualquer escrita de condicao")
                .doesNotContain("IMPEDIDA")
                .containsExactlyInAnyOrder(
                        "AGUARDANDO_TOMADA", "EM_CURSO", "CONCLUIDA", "ENCERRADA_SEM_CONCLUSAO");
    }

    @Test
    @DisplayName("a tabela de tarefa nao tem coluna de impedimento: a marca e derivada")
    void tarefaNaoTemColunaDeImpedimento() throws Exception {
        var colunas = colunasDe("tarefa");

        // RN-032 vira propriedade do esquema: nenhuma escrita sobre `tarefa`
        // alcanca a marca, porque ela nao mora ali. Basta existir a coluna
        // para que a regra volte a depender de disciplina.
        Assertions.assertThat(colunas)
                .doesNotContain("impedida", "impedido", "impedimento_id", "tem_impedimento");
    }

    @Test
    @DisplayName("as tres dimensoes variam de forma independente em uma unica tarefa")
    void asTresDimensoesVariamDeFormaIndependente() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        var etapas = cenario.fluxoPadrao(projeto, ana());
        var tarefa = cenario.criarTarefa(projeto, "As tres ao mesmo tempo", ana());

        cenario.abrirImpedimento(tarefa, "aguardando o fornecedor", ana());

        // Mexer numa dimensao de cada vez e conferir que as outras duas ficam
        // onde estavam. E a matriz que os cenarios cobrem por pares.
        cenario.assumir(tarefa, bruno());
        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.etapaId").value(etapas.get(0).id().toString()))
                .andExpect(jsonPath("$.condicao").value("EM_CURSO"))
                .andExpect(jsonPath("$.impedimento.motivo").value("aguardando o fornecedor"));

        cenario.mover(tarefa, etapas.get(1).id(), bruno());
        mockMvc.perform(get("/v1/tarefas/{id}", tarefa).with(ana()))
                .andExpect(jsonPath("$.etapaId").value(etapas.get(1).id().toString()))
                // Mover devolve a tarefa a espera de tomada — e isso e mudanca
                // de condicao, deliberada e nao acidental. O que nao pode
                // mudar e a marca.
                .andExpect(jsonPath("$.condicao").value("AGUARDANDO_TOMADA"))
                .andExpect(jsonPath("$.impedimento.motivo").value("aguardando o fornecedor"));
    }

    // ------------------------------------------------------------ utilitarios

    private List<String> valoresAceitosPorCondicao() throws Exception {
        var valores = new ArrayList<String>();
        try (Connection conexao = conectar();
                var stmt = conexao.prepareStatement("""
                        SELECT e.enumlabel
                          FROM pg_enum e
                          JOIN pg_type t ON t.oid = e.enumtypid
                         WHERE t.typname = 'condicao_tarefa'
                        """);
                var resultado = stmt.executeQuery()) {
            while (resultado.next()) {
                valores.add(resultado.getString(1));
            }
        }
        if (valores.isEmpty()) {
            // O dominio pode ter sido realizado por CHECK em vez de enum; as
            // duas formas satisfazem a decisao, e o que importa e o conjunto.
            valores.addAll(valoresDeCheck());
        }
        return valores;
    }

    private List<String> valoresDeCheck() throws Exception {
        var valores = new ArrayList<String>();
        try (Connection conexao = conectar();
                var stmt = conexao.prepareStatement("""
                        SELECT pg_get_constraintdef(oid) FROM pg_constraint
                         WHERE conrelid = 'tarefa'::regclass AND contype = 'c'
                        """);
                var resultado = stmt.executeQuery()) {
            while (resultado.next()) {
                var definicao = resultado.getString(1);
                if (!definicao.contains("condicao")) {
                    continue;
                }
                var matcher = java.util.regex.Pattern.compile("'([A-Z_]+)'").matcher(definicao);
                while (matcher.find()) {
                    valores.add(matcher.group(1));
                }
            }
        }
        return valores;
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
        Assertions.assertThat(colunas).isNotEmpty();
        return colunas;
    }

    private Connection conectar() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
