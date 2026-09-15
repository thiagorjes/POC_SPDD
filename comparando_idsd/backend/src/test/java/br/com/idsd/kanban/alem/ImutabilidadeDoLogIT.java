package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import java.sql.Connection;
import java.sql.DriverManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RNF-008 — o log de eventos e imutavel.
 *
 * <p>Nenhum cenario congelado pede isto, e o requisito nao seria verificado por
 * nenhum outro teste: todos escrevem pela API, e a API nao oferece rota de
 * alteracao de evento. Ausencia de rota nao e imutabilidade — e so ausencia de
 * rota. O que este teste exige e que a garantia esteja no banco, na role que a
 * aplicacao usa, de modo que ela sobreviva a um bug de servico, a uma migracao
 * mal escrita e a alguem com o console aberto usando a credencial da aplicacao.
 */
class ImutabilidadeDoLogIT extends TesteDeIntegracao {

    @Test
    @DisplayName("a role da aplicacao nao consegue alterar nem apagar evento gravado")
    void roleDaAplicacaoNaoAlteraNemApagaEvento() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        var etapas = cenario.fluxoPadrao(projeto, ana());
        var tarefa = cenario.criarTarefa(projeto, "Com historia", ana());
        cenario.mover(tarefa, etapas.get(1).id(), ana());

        // A conexao usa a mesma credencial da aplicacao de proposito: verificar
        // com superusuario nao provaria nada sobre o que a aplicacao pode
        // fazer.
        try (Connection conexao = conectarComoAplicacao()) {
            Assertions.assertThatThrownBy(() -> executar(conexao,
                            "UPDATE evento_tarefa SET ocorrido_em = now() WHERE tarefa_id = '"
                                    + tarefa + "'"))
                    .isInstanceOf(java.sql.SQLException.class);

            Assertions.assertThatThrownBy(() -> executar(conexao,
                            "DELETE FROM evento_tarefa WHERE tarefa_id = '" + tarefa + "'"))
                    .isInstanceOf(java.sql.SQLException.class);

            // Truncate contorna DELETE e apagaria a verdade inteira; e o
            // caminho que mais aparece em script de limpeza mal calibrado.
            Assertions.assertThatThrownBy(() -> executar(conexao, "TRUNCATE evento_tarefa"))
                    .isInstanceOf(java.sql.SQLException.class);
        }

        Assertions.assertThat(quantidadeDeEventos(tarefa.toString())).isGreaterThan(0);
    }

    @Test
    @DisplayName("a projecao continua sendo gravavel: a restricao e do log, e nao do banco")
    void aProjecaoContinuaGravavel() throws Exception {
        // Sem esta contraparte a verificacao anterior passaria com uma role
        // somente-leitura, que quebraria o sistema inteiro em vez de proteger
        // o log.
        try (Connection conexao = conectarComoAplicacao()) {
            Assertions.assertThatCode(() -> executar(conexao,
                            "DELETE FROM intervalo_tarefa WHERE tarefa_id = "
                                    + "'00000000-0000-0000-0000-000000000000'"))
                    .doesNotThrowAnyException();
        }
    }

    // ------------------------------------------------------------ utilitarios

    private void executar(Connection conexao, String sql) throws Exception {
        try (var stmt = conexao.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    private int quantidadeDeEventos(String tarefaId) throws Exception {
        try (Connection conexao = conectarComoAplicacao();
                var stmt = conexao.prepareStatement(
                        "SELECT count(*) FROM evento_tarefa WHERE tarefa_id = ?::uuid")) {
            stmt.setString(1, tarefaId);
            var resultado = stmt.executeQuery();
            resultado.next();
            return resultado.getInt(1);
        }
    }

    /**
     * Conecta com a credencial que a aplicacao de fato usa.
     *
     * <p>Antes de TASK-02.9 este metodo devolvia o usuario do contêiner, que e
     * dono do schema e superusuario — de modo que a verificacao dizia medir a
     * credencial da aplicacao e media o oposto dela. A correcao e de duas
     * linhas e e o que da sentido as tres asercoes acima.
     */
    private Connection conectarComoAplicacao() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), USUARIO_APLICACAO, SENHA_APLICACAO);
    }
}
