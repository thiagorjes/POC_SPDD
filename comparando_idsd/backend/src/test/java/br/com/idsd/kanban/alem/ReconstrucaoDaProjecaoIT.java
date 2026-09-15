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
        cenario.assumir(comum, bruno());
        cenario.mover(comum, etapas.get(1).id(), bruno());

        var travada = cenario.criarTarefa(projeto, "Com impedimento", ana());
        var impedimento = cenario.abrirImpedimento(travada, "aguardando o fornecedor", ana());
        resolver(travada, impedimento);

        var devolvida = cenario.criarTarefa(projeto, "Devolvida", ana());
        cenario.assumir(devolvida, bruno());
        devolver(devolvida);

        // Translacao rigida, e nao `recuarInicioDoIntervalo`: aquele metodo
        // desloca a projecao sem deslocar o log, e a divergencia que ele
        // fabrica apareceria aqui como defeito da reconstrucao (ACH-03).
        cenario.envelhecerProjeto(projeto, Duration.ofHours(6));

        String antes = leituraDeReferencia(projeto);
        String intervalosAntes = intervalosEmTexto();

        corromperAProjecao();
        Assertions.assertThat(intervalosEmTexto()).isNotEqualTo(intervalosAntes);

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

    /**
     * Destroi a serie de tempo ate onde o esquema permite.
     *
     * <p>Nao e {@code TRUNCATE}: {@code impedimento.intervalo_id} e FK NOT NULL
     * para esta tabela, e o PostgreSQL recusa (ACH-04). Nao e contornavel com
     * {@code CASCADE}, e nao deve ser: por SDR-006 a rotina nao cria linha de
     * {@code impedimento}, de modo que apagar o impedimento junto destruiria
     * insumo e nao modelo de leitura — o teste passaria a exigir da rotina o
     * que ela declaradamente nao promete.
     *
     * <p>A destruicao possivel, entao, e em duas partes: apagar toda linha que
     * ninguem referencia e corromper as que sobram. Nas que sobram, os dois
     * campos corrompidos — {@code inicio} e {@code fim} — sao justamente os que
     * o log determina, de modo que uma rotina que nao os reescreva no lugar
     * deixa a corrupcao visivel na comparacao.
     */
    private void corromperAProjecao() throws Exception {
        try (Connection conexao = conectar()) {
            try (var stmt = conexao.prepareStatement("""
                    DELETE FROM intervalo_tarefa
                     WHERE id NOT IN (SELECT intervalo_id FROM impedimento)
                    """)) {
                stmt.executeUpdate();
            }
            try (var stmt = conexao.prepareStatement("""
                    UPDATE intervalo_tarefa
                       SET inicio = inicio - interval '999 minutes',
                           fim    = NULL
                    """)) {
                stmt.executeUpdate();
            }
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
