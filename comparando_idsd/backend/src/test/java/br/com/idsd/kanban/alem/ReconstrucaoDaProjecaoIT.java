package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Reconstrucao da projecao a partir do log.
 *
 * <p>Nao ha cenario congelado que peca isto, e e por isso que o teste existe:
 * SDR-001 separa quem detem a verdade — o log de eventos, imutavel — de quem
 * detem a leitura, que e a projecao de intervalos. Essa separacao so vale
 * alguma coisa se a projecao for de fato descartavel. Se ela nao puder ser
 * reconstruida, ela e a verdade na pratica, e a decisao de arquitetura e
 * ficcao.
 *
 * <p>O modo de falha que este teste pega e silencioso: uma escrita que altera
 * a projecao sem gravar o evento correspondente passa em todos os cenarios,
 * porque todos leem a projecao.
 */
class ReconstrucaoDaProjecaoIT extends TesteDeIntegracao {

    @Test
    @DisplayName("a projecao reconstruida do log e identica a que estava em disco")
    void projecaoReconstruidaEIdentica() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.participante(projeto, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        var etapas = cenario.fluxoPadrao(projeto, ana());

        // Uma historia que passa por todas as transicoes que produzem
        // intervalo, incluindo reabertura, para que o episodio entre na conta.
        var comum = cenario.criarTarefa(projeto, "Percurso completo", ana());
        cenario.recuarInicioDoIntervalo(comum, "ESPERA_TOMADA", Duration.ofMinutes(30));
        cenario.assumir(comum, bruno());
        cenario.mover(comum, etapas.get(1).id(), bruno());
        cenario.recuarInicioDoIntervalo(comum, "PERMANENCIA", Duration.ofMinutes(120));

        var travada = cenario.criarTarefa(projeto, "Com impedimento", ana());
        var impedimento = cenario.abrirImpedimento(travada, "aguardando o fornecedor", ana());
        cenario.recuarInicioDoIntervalo(travada, "IMPEDIMENTO", Duration.ofMinutes(90));
        resolver(travada, impedimento);

        var devolvida = cenario.criarTarefa(projeto, "Devolvida", ana());
        cenario.assumir(devolvida, bruno());
        devolver(devolvida);

        String antes = leituraDeReferencia(projeto);
        String intervalosAntes = intervalosEmTexto();

        apagarAProjecao();
        Assertions.assertThat(intervalosEmTexto()).isEmpty();

        reconstruir();

        // Duas comparacoes, e as duas importam: a leitura de contrato prova
        // que a superficie voltou igual, e o dump dos intervalos prova que
        // voltou igual por dentro — leitura igual com intervalos diferentes
        // significa que a consulta esta escondendo a divergencia.
        Assertions.assertThat(intervalosEmTexto()).isEqualTo(intervalosAntes);
        Assertions.assertThat(leituraDeReferencia(projeto)).isEqualTo(antes);
    }

    // ------------------------------------------------------------ utilitarios

    private String leituraDeReferencia(UUID projeto) throws Exception {
        return mockMvc.perform(get("/v1/projetos/{id}/tempo-por-etapa", projeto).with(ana()))
                .andReturn().getResponse().getContentAsString();
    }

    private void resolver(UUID tarefa, UUID impedimento) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/v1/tarefas/{t}/impedimentos/{i}/resolucao", tarefa, impedimento)
                .with(ana())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(cenario.comOrigem(tarefa, ana(), "\"desfecho\": \"entregue\"")));
    }

    private void devolver(UUID tarefa) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/v1/tarefas/{id}/tomada", tarefa)
                .with(bruno())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(cenario.comOrigem(tarefa, bruno(), null)));
    }

    /**
     * Dump ordenado e determinista da projecao. O identificador da linha fica de
     * fora porque e sequencial e muda na reconstrucao — o que precisa coincidir
     * e o conteudo, e nao a chave artificial.
     */
    private String intervalosEmTexto() throws Exception {
        var texto = new StringBuilder();
        try (Connection conexao = conectar();
                var stmt = conexao.prepareStatement("""
                        SELECT tarefa_id, etapa_id, tipo, episodio, inicio, fim
                          FROM intervalo_tarefa
                         ORDER BY tarefa_id, tipo, episodio, inicio
                        """);
                var resultado = stmt.executeQuery()) {
            while (resultado.next()) {
                for (int coluna = 1; coluna <= 6; coluna++) {
                    texto.append(resultado.getString(coluna)).append('|');
                }
                texto.append('\n');
            }
        }
        return texto.toString();
    }

    private void apagarAProjecao() throws Exception {
        try (Connection conexao = conectar();
                var stmt = conexao.prepareStatement("TRUNCATE intervalo_tarefa")) {
            stmt.executeUpdate();
        }
    }

    private void reconstruir() {
        reconstrutor.reconstruirTudo();
    }

    @org.springframework.beans.factory.annotation.Autowired
    private br.com.idsd.kanban.internal.tempo.ReconstrutorDeProjecao reconstrutor;

    private Connection conectar() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
