package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;

import br.com.idsd.kanban.internal.projeto.Etapa;
import br.com.idsd.kanban.internal.projeto.EtapaRepositorio;
import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import com.jayway.jsonpath.JsonPath;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Os tetos e as recusas de conjunto de {@code PUT /v1/projetos/{id}/etapas}.
 *
 * <p>Esta classe existe porque o conserto dos bloqueantes da <b>primeira</b>
 * revisao de TASK-02.2 entrou sem verificacao alguma (ACH-12, ACH-14, ACH-15,
 * ACH-22 e ACH-23 da reexecucao). Os tetos de {@link Etapa}, as tres recusas de
 * forma do conjunto, o tradutor nominal de {@code 409}, as duas guardas de estado
 * do repositorio e o passo intermediario de liberacao de ordens estavam todos
 * escritos e nenhum deles aparecia na suite: apagar qualquer um deixava a suite
 * verde, que e a definicao de nao estar verificado.
 *
 * <p>Nenhum cenario congelado alcanca isto, e a razao e a mesma em todos os casos:
 * os cenarios descrevem o fluxo que o produto quer, e estes sao os limites que a
 * escrita autenticada nao pode ultrapassar — contenção de abuso e defesa de
 * invariante, nao comportamento pedido.
 */
class LimitesDaConfiguracaoDoFluxoIT extends TesteDeIntegracao {

    @Autowired
    private EtapaRepositorio repositorio;

    // ------------------------------------------------------- tetos (ACH-15)

    @Test
    @DisplayName("fluxo com mais etapas que o teto e recusado com 422, sem escrever nada")
    void fluxoAcimaDoTetoDeEtapasERecusado() throws Exception {
        UUID projeto = projetoConfiguravel();
        cenario.fluxoPadrao(projeto, ana());
        List<String> antes = nomesDoFluxo(projeto);

        StringBuilder etapas = new StringBuilder();
        for (int i = 0; i < Etapa.MAXIMO_DE_ETAPAS + 1; i++) {
            etapas.append(i == 0 ? "" : ",")
                    .append(etapa(null, "E" + i, i + 1, i == Etapa.MAXIMO_DE_ETAPAS));
        }

        Assertions.assertThat(configurar(projeto, etapas.toString()).status()).isEqualTo(422);
        // O teto e de seguranca: ele so vale se a recusa acontecer antes da
        // escrita. Fluxo alterado com `422` na resposta seria o pior desfecho.
        Assertions.assertThat(nomesDoFluxo(projeto)).isEqualTo(antes);
    }

    @Test
    @DisplayName("nome de etapa acima do teto e recusado com 422")
    void nomeAcimaDoTetoERecusado() throws Exception {
        UUID projeto = projetoConfiguravel();

        String longo = "n".repeat(Etapa.TAMANHO_MAXIMO_DO_NOME + 1);
        Assertions.assertThat(configurar(projeto, etapa(null, longo, 1, true)).status())
                .isEqualTo(422);

        // E o teto e teto, nao aproximacao: o tamanho exato passa.
        String noLimite = "n".repeat(Etapa.TAMANHO_MAXIMO_DO_NOME);
        Assertions.assertThat(configurar(projeto, etapa(null, noLimite, 1, true)).status())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("ordem acima do teto e recusada, e a faixa de trabalho fica inalcancavel")
    void ordemAcimaDoTetoERecusada() throws Exception {
        UUID projeto = projetoConfiguravel();

        Assertions.assertThat(
                        configurar(projeto, etapa(null, "Alem", Etapa.ORDEM_MAXIMA + 1, true)).status())
                .isEqualTo(422);

        // Este e o ponto de ACH-04 da primeira revisao, e a razao de `ORDEM_MAXIMA`
        // existir: sem ele, uma requisicao podia gravar dentro da faixa em que o
        // passo intermediario estaciona as ordens vigentes, e o deslocamento
        // colidiria com dado real.
        Assertions.assertThat(
                        configurar(projeto, etapa(null, "Na faixa", Etapa.FAIXA_DE_TRABALHO, true))
                                .status())
                .isEqualTo(422);
        Assertions.assertThat(Etapa.ORDEM_MAXIMA).isLessThan(Etapa.FAIXA_DE_TRABALHO);
    }

    // --------------------------------------- forma do conjunto (ACH-15)

    @Test
    @DisplayName("o mesmo id duas vezes sai 422 etapa-repetida, e nao 500")
    void idRepetidoERecusado() throws Exception {
        UUID projeto = projetoConfiguravel();
        UUID id = cenario.fluxoPadrao(projeto, ana()).getFirst().id();

        var resposta = configurar(projeto,
                etapa(id, "Um", 1, false) + "," + etapa(id, "Outro", 2, true));

        Assertions.assertThat(resposta.status()).isEqualTo(422);
        Assertions.assertThat(resposta.corpo()).contains("etapa-repetida");
    }

    @Test
    @DisplayName("duas etapas na mesma posicao saem 422 ordem-repetida, antes de qualquer escrita")
    void ordemRepetidaERecusada() throws Exception {
        UUID projeto = projetoConfiguravel();

        var resposta = configurar(projeto,
                etapa(null, "Um", 1, false) + "," + etapa(null, "Outro", 1, true));

        Assertions.assertThat(resposta.status()).isEqualTo(422);
        Assertions.assertThat(resposta.corpo()).contains("ordem-repetida");
    }

    @Test
    @DisplayName("item vazio na lista sai 422, e nao derruba a rota")
    void itemNuloNaListaERecusado() throws Exception {
        UUID projeto = projetoConfiguravel();

        // O slug nao e fixado aqui de proposito: a recusa pode vir da anotacao
        // `@NotNull` no elemento ou da guarda de conjunto no servico, e as duas
        // sao corretas. O que o contrato promete, e o que a rota antes quebrava,
        // e o status.
        Assertions.assertThat(configurar(projeto, "null," + etapa(null, "Fim", 1, true)).status())
                .isEqualTo(422);
    }

    // ------------------------------ disputa de ordem e passo intermediario (ACH-23)

    @Test
    @DisplayName("permutar as posicoes das mesmas etapas e aceito, sem colidir no indice unico")
    void permutacaoDasMesmasEtapasEAceita() throws Exception {
        UUID projeto = projetoConfiguravel();
        List<br.com.idsd.kanban.suporte.Cenario.Etapa> fluxo = cenario.fluxoPadrao(projeto, ana());

        // Inverter as posicoes e o caso que `haDisputaDeOrdem` detecta e que o
        // passo intermediario existe para resolver: a primeira etapa quer a
        // posicao que a ultima ainda ocupa. Sem a liberacao de ordens, esta
        // requisicao viola `etapa_projeto_ordem_unico` no flush.
        StringBuilder corpo = new StringBuilder();
        for (int i = 0; i < fluxo.size(); i++) {
            var etapa = fluxo.get(i);
            corpo.append(i == 0 ? "" : ",")
                    .append(etapa(etapa.id(), etapa.nome(), fluxo.size() - i, i == 0));
        }

        Assertions.assertThat(configurar(projeto, corpo.toString()).status()).isEqualTo(200);
        Assertions.assertThat(nomesDoFluxo(projeto))
                .containsExactlyElementsOf(fluxo.reversed().stream().map(
                        br.com.idsd.kanban.suporte.Cenario.Etapa::nome).toList());
    }

    @Test
    @DisplayName("renomear sem mexer nas posicoes e aceito pelo caminho sem disputa")
    void renomeioSemDisputaDeOrdemEAceito() throws Exception {
        UUID projeto = projetoConfiguravel();
        List<br.com.idsd.kanban.suporte.Cenario.Etapa> fluxo = cenario.fluxoPadrao(projeto, ana());

        // O outro lado de `haDisputaDeOrdem`: mesmas etapas, mesmas posicoes,
        // nomes novos. Aqui o passo intermediario e puro custo, e o metodo existe
        // para evita-lo — o desfecho, porem, tem de ser identico.
        StringBuilder corpo = new StringBuilder();
        for (int i = 0; i < fluxo.size(); i++) {
            var etapa = fluxo.get(i);
            corpo.append(i == 0 ? "" : ",")
                    .append(etapa(etapa.id(), "R" + i, etapa.ordem(), i == fluxo.size() - 1));
        }

        Assertions.assertThat(configurar(projeto, corpo.toString()).status()).isEqualTo(200);
        Assertions.assertThat(nomesDoFluxo(projeto))
                .containsExactly(java.util.stream.IntStream.range(0, fluxo.size())
                        .mapToObj(i -> "R" + i).toArray(String[]::new));
    }

    // -------------------------------------- fluxoConfigurado e o filtro (ACH-14)

    @Test
    @DisplayName("projeto com todas as etapas arquivadas volta a contar como nao configurado")
    void projetoComTudoArquivadoNaoEConfigurado() throws Exception {
        UUID projeto = projetoConfiguravel();
        cenario.fluxoPadrao(projeto, ana());
        Assertions.assertThat(fluxoConfigurado(projeto)).isTrue();

        // O arquivamento e feito por SQL porque a rota nao permite chegar aqui:
        // RN-001 exige etapa terminal, de modo que nenhum corpo valido esvazia o
        // fluxo. Ainda assim o estado e alcancavel — e a clausula
        // `and e.arquivadaEm is null` da subconsulta so tem poder de falha contra
        // ele. Sem a clausula, `fluxoConfigurado` ficaria verdadeiro para sempre
        // depois da primeira configuracao, e a tela mandaria configurar um fluxo
        // que ela mesma diz existir.
        arquivarTodasAsEtapas(projeto);

        Assertions.assertThat(fluxoConfigurado(projeto)).isFalse();
    }

    // ------------------------------ guardas de estado do repositorio (ACH-22)

    @Test
    @Transactional
    @DisplayName("bloquear projeto inexistente recusa como defeito de estado")
    void bloquearProjetoInexistenteRecusa() {
        // Nao e entrada de usuario: a rota resolve o `404` antes do servico. Sem a
        // guarda, `find` devolveria `null`, o bloqueio nao aconteceria e a
        // substituicao seguiria <b>sem serializacao alguma</b> — a falha de SDR-005
        // mais silenciosa possivel.
        //
        // A excecao chega envolta: o proxy de repositorio do Spring Data traduz
        // `IllegalStateException` em `InvalidDataAccessApiUsageException`. A raiz e
        // o que a classe lanca, e e nela que a asercao se ancora — fixar o
        // envoltorio amarraria o teste ao tradutor e nao a guarda.
        Assertions.assertThatThrownBy(() -> repositorio.bloquearProjeto(UUID.randomUUID()))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("projeto inexistente");
    }

    @Test
    @Transactional
    @DisplayName("substituir fluxo com etapa de outro projeto recusa como defeito de estado")
    void substituirFluxoComEtapaDeOutroProjetoRecusa() throws Exception {
        UUID alfa = projetoConfiguravel();
        cenario.fluxoPadrao(alfa, ana());
        UUID beta = projetoConfiguravel();

        List<Etapa> doAlfa = repositorio.findByProjetoIdAndArquivadaEmIsNullOrderByOrdemAsc(alfa);

        // O `projetoId` do parametro delimita o que a chamada pode escrever. Sem a
        // guarda ele seria decorativo, e etapa de outro projeto entraria no fluxo
        // deste por um caminho que nenhuma validacao de borda cobre.
        Assertions.assertThatThrownBy(() -> repositorio.substituirFluxo(beta, doAlfa))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("etapa de outro projeto");
    }

    // ------------------------------------------------------------- utilitarios

    private record Resposta(int status, String corpo) {}

    private UUID projetoConfiguravel() {
        UUID projeto = cenario.projeto("Alfa " + UUID.randomUUID());
        cenario.participante(
                projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        return projeto;
    }

    private static String etapa(UUID id, String nome, int ordem, boolean terminal) {
        return "{"
                + (id == null ? "" : "\"id\": \"" + id + "\", ")
                + "\"nome\": \"" + nome + "\", "
                + "\"ordem\": " + ordem + ", "
                + "\"terminal\": " + terminal
                + "}";
    }

    private Resposta configurar(UUID projetoId, String etapas) throws Exception {
        var resposta = mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/projetos/{projetoId}/etapas", projetoId)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"etapas\": [" + etapas + "] }"))
                .andReturn()
                .getResponse();
        return new Resposta(resposta.getStatus(), resposta.getContentAsString());
    }

    private List<String> nomesDoFluxo(UUID projetoId) throws Exception {
        var resposta = mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/projetos/{projetoId}/etapas", projetoId)
                        .with(ana()))
                .andReturn()
                .getResponse();
        Assertions.assertThat(resposta.getStatus()).isEqualTo(200);
        return JsonPath.read(resposta.getContentAsString(), "$.etapas[*].nome");
    }

    private boolean fluxoConfigurado(UUID projetoId) throws Exception {
        var corpo = mockMvc.perform(MockMvcRequestBuilders.get("/v1/projetos").with(ana()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Boolean> encontrados = JsonPath.read(
                corpo, "$..[?(@.id == '" + projetoId + "')].fluxoConfigurado");
        Assertions.assertThat(encontrados).hasSize(1);
        return encontrados.getFirst();
    }

    private void arquivarTodasAsEtapas(UUID projetoId) throws Exception {
        try (Connection conexao = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var stmt = conexao.prepareStatement(
                        "UPDATE etapa SET arquivada_em = now() WHERE projeto_id = ?")) {
            stmt.setObject(1, projetoId);
            Assertions.assertThat(stmt.executeUpdate()).isPositive();
        }
    }
}
