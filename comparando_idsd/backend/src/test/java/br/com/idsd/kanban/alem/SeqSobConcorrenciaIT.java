package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * SDR-004 — a sequencia de eventos, sob concorrencia.
 *
 * <p>O {@code seq} e a rede de seguranca de RNF-002: o cliente detecta que
 * perdeu evento comparando a sequencia recebida com a esperada. Duplicata
 * destroi essa deteccao — duas mensagens com o mesmo numero fazem uma lacuna
 * parecer continuidade —, e por isso ADR-004, que atribuia o numero por pod,
 * foi superado.
 *
 * <p>Nenhum cenario congelado cobre isto porque nenhum cenario descreve
 * escrita simultanea no mesmo projeto. E exatamente o caso em que o defeito
 * aparece.
 */
class SeqSobConcorrenciaIT extends TesteDeIntegracao {

    private static final int ESCRITAS = 40;

    @Test
    @DisplayName("escritas simultaneas no mesmo projeto produzem sequencia sem duplicata nem buraco")
    void escritasSimultaneasProduzemSequenciaIntegra() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.fluxoPadrao(projeto, ana());

        List<Callable<Void>> escritas = new ArrayList<>();
        for (int i = 0; i < ESCRITAS; i++) {
            int indice = i;
            escritas.add(() -> {
                mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/projetos/{id}/tarefas", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Tarefa " + indice + "\"}"));
                return null;
            });
        }

        try (var executor = Executors.newFixedThreadPool(8)) {
            for (var futuro : executor.invokeAll(escritas)) {
                futuro.get();
            }
        }

        var sequencia = sequenciasDoProjeto(projeto);

        Assertions.assertThat(sequencia).hasSize(ESCRITAS);
        // Sem duplicata: e a propriedade que a deteccao de lacuna no cliente
        // pressupoe.
        Assertions.assertThat(sequencia).doesNotHaveDuplicates();
        // E sem buraco: contigua de 1 a N. Buraco faria o cliente concluir
        // perda de evento que nunca houve, e recarregar o board a toa.
        Assertions.assertThat(sequencia)
                .containsExactlyElementsOf(
                        java.util.stream.LongStream.rangeClosed(1, ESCRITAS).boxed().toList());
    }

    @Test
    @DisplayName("a escrita recusada nao consome numero da sequencia")
    void escritaRecusadaNaoConsomeNumero() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.fluxoPadrao(projeto, ana());
        cenario.criarTarefa(projeto, "Valida", ana());

        // A recusa acontece dentro da transacao que ja incrementou o contador,
        // se o incremento for feito cedo demais. O rollback tem de desfazer o
        // numero junto — e o buraco resultante seria indistinguivel de perda.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(MockMvcRequestBuilders
                    .post("/v1/projetos/{id}/tarefas", projeto)
                    .with(ana())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"titulo\": \"  \"}"));
        }

        cenario.criarTarefa(projeto, "Outra valida", ana());

        Assertions.assertThat(sequenciasDoProjeto(projeto)).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("cada projeto tem sua propria sequencia, e uma nao interfere na outra")
    void cadaProjetoTemSuaPropriaSequencia() throws Exception {
        var alfa = cenario.projeto("Alfa");
        var beta = cenario.projeto("Beta");
        cenario.participante(alfa, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.participante(beta, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.fluxoPadrao(alfa, ana());
        cenario.fluxoPadrao(beta, ana());

        cenario.criarTarefa(alfa, "No Alfa", ana());
        cenario.criarTarefa(beta, "No Beta", ana());
        cenario.criarTarefa(alfa, "Outra no Alfa", ana());

        // A inscricao no canal e por projeto; sequencia global obrigaria cada
        // cliente a tolerar buracos permanentes, o que anularia a deteccao.
        Assertions.assertThat(sequenciasDoProjeto(alfa)).containsExactly(1L, 2L);
        Assertions.assertThat(sequenciasDoProjeto(beta)).containsExactly(1L);
    }

    private List<Long> sequenciasDoProjeto(UUID projeto) throws Exception {
        var numeros = new ArrayList<Long>();
        try (Connection conexao = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var stmt = conexao.prepareStatement(
                        "SELECT seq FROM evento_tarefa WHERE projeto_id = ? ORDER BY seq")) {
            stmt.setObject(1, projeto);
            var resultado = stmt.executeQuery();
            while (resultado.next()) {
                numeros.add(resultado.getLong(1));
            }
        }
        return numeros;
    }
}
