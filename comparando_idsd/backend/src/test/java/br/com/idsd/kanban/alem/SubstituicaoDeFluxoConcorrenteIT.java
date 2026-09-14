package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * SDR-005 — a substituicao do fluxo, sob concorrencia.
 *
 * <p>{@code PUT /v1/projetos/{id}/etapas} substitui o <b>conjunto</b> inteiro, e e
 * por isso que o dano concorrente aqui nao tem a forma de conflito: ele tem a
 * forma de <b>ausencia</b>. Duas requisicoes que leem o mesmo fluxo vigente
 * calculam cada uma sobre um conjunto que a outra ja mudou — a etapa que a
 * primeira criou nao esta no corpo da segunda, logo nao e arquivada nem
 * reordenada, e desaparece sem que nada falhe. Quando as duas disputam a mesma
 * posicao, o desfecho e o oposto e igualmente ruim: colisao no indice unico
 * parcial e {@code 500} intermitente.
 *
 * <p>A transacao garante atomicidade e nao garante isolamento de decisao. Versao
 * otimista tambem nao alcanca o caso, porque nao ha linha sobre a qual conflitar —
 * e essa e a razao registrada em SDR-005 para o bloqueio pessimista da linha de
 * {@code projeto}.
 *
 * <p>Cenario congelado algum descreve configuracao simultanea do mesmo projeto, e
 * e exatamente onde o defeito vive. Sem esta classe, remover o bloqueio deixa a
 * suite inteira verde.
 *
 * <p><b>O predicado que carrega o poder de falha e o status exato {@code 200}</b>,
 * e nao a forma do fluxo resultante. A primeira versao desta classe assertia
 * {@code < 500} e ficava verde com o bloqueio removido — medido em 2026-09-14,
 * ACH-02 a ACH-04 da reexecucao de TASK-02.2. A razao e que a falta de
 * serializacao nao aparece como {@code 5xx}: ela aparece como o {@code 409} que o
 * tradutor nominal desta mesma task produz a partir da violacao de
 * {@code etapa_projeto_ordem_unico}. Serializadas, todas as requisicoes desta
 * classe sao validas e nenhuma pode ser recusada; e so por isso que o status exato
 * separa os dois mundos.
 *
 * <p>Duas mutacoes deixam esta classe vermelha, e as duas foram medidas em
 * 2026-09-14: remover {@code bloquearProjeto}, e move-lo para <b>depois da leitura
 * do fluxo vigente</b>, que e a clausula de ordem de SDR-005. Move-lo para depois
 * das validacoes de forma da requisicao, que ainda antecedem a leitura, mantem os
 * dois verdes — e esta certo que mantenha: o que o DR exige e que o bloqueio
 * preceda a <b>leitura</b>, nao que seja a primeira instrucao do metodo.
 *
 * <p><b>Sobre a pressao: a barreira alinha a largada, nao a secao critica</b>
 * (ACH-09). Liberadas juntas, as requisicoes ainda atravessam roteamento,
 * autenticacao e resolucao de permissao antes de chegar ao ponto que disputa, e
 * essa distancia pode serializa-las por acidente. O contra-argumento nao e
 * teorico: com o bloqueio, a classe rodava em 0,845 s, e sem ele, em 16,22 s —
 * vinte vezes, o que mostra que na presenca do bloqueio ela mal exercitava
 * contencao. A contramedida e a mesma do precedente da casa,
 * {@link SeqSobConcorrenciaIT}: <b>repeticao</b>. Cada teste roda {@link #RODADAS}
 * rodadas sobre o mesmo projeto, e basta uma sobreposicao em qualquer delas para
 * o defeito aparecer. Repeticao nao transforma o teste em determinista — torna o
 * falso verde improvavel, e o custo e alguns segundos.
 *
 * <p><b>Sobre o pool: {@link #PERMUTACOES} e 8 contra {@code maximum-pool-size}
 * de 10</b> (ACH-20). A folga e deliberada e estreita. Sob o bloqueio, sete das
 * oito esperam segurando conexao, de modo que o numero de threads e diretamente
 * o numero de conexoes retidas; passar de 9 esgotaria o pool e a classe deixaria
 * de medir serializacao para medir {@code connection-timeout}, saindo {@code 503}
 * e vermelha por razao alheia ao que ela afirma. Quem aumentar {@code PERMUTACOES}
 * precisa aumentar o pool junto, ou tera trocado o teste sem perceber.
 *
 * <p><b>Toda espera tem teto</b> (ACH-19). Barreira, {@code Future.get} e os
 * proprios testes sao limitados no tempo: sem isso, a regressao que mais importa —
 * o bloqueio que nao libera — apareceria como suite pendurada, que nenhum CI
 * reporta como falha util.
 */
class SubstituicaoDeFluxoConcorrenteIT extends TesteDeIntegracao {

    private static final List<String> CORPO_A = List.of("A1", "A2", "A3");
    private static final List<String> CORPO_B = List.of("B1", "B2");
    private static final int PERMUTACOES = 8;
    private static final int RODADAS = 5;
    private static final int SEGUNDOS_ATE_DESISTIR = 30;

    @Test
    @Timeout(120)
    @DisplayName("duas substituicoes simultaneas deixam o fluxo de uma delas inteiro, e nunca a mistura das duas")
    void substituicoesSimultaneasNaoMisturamOsDoisCorpos() throws Exception {
        UUID projeto = projetoConfiguravel();
        cenario.fluxoPadrao(projeto, ana());

        for (int rodada = 0; rodada < RODADAS; rodada++) {
            umaRodadaDeDoisCorpos(projeto);
        }
    }

    private void umaRodadaDeDoisCorpos(UUID projeto) throws Exception {
        List<Integer> status = emParalelo(List.of(
                () -> configurar(projeto, CORPO_A),
                () -> configurar(projeto, CORPO_B)));

        // `200` <b>exato</b>, e nao uma faixa (ACH-03). Sob serializacao as duas
        // requisicoes sao validas: a segunda decide sobre o conjunto que a primeira
        // ja deixou, e nada nela pode ser recusado. Sem serializacao a perdedora
        // colide em `etapa_projeto_ordem_unico` e sai `409` pelo tradutor nominal
        // desta mesma task — abaixo de 500, e portanto invisivel a qualquer
        // predicado de faixa. `409` aqui nao e conflito legitimo: e o defeito.
        //
        // E a mesma assercao que prova a <b>ordem de aquisicao</b> (ACH-04), a
        // clausula central de SDR-005. Bloquear depois de ler o fluxo vigente faz a
        // segunda decidir sobre um conjunto obsoleto: ela arquiva etapas que ja nao
        // existem e grava nas posicoes que a primeira acabou de ocupar, e o desfecho
        // e este mesmo `409`. Mover a chamada de `bloquearProjeto` uma linha abaixo
        // torna este teste vermelho.
        Assertions.assertThat(status).allSatisfy(codigo ->
                Assertions.assertThat(codigo).isEqualTo(200));

        List<String> resultante = nomesDoFluxo(projeto);

        // A assercao nao e "venceu A" nem "venceu B": qual das duas chega primeiro
        // nao e propriedade do sistema. O que e propriedade, e o que SDR-005
        // promete, e que o fluxo final seja o corpo de uma delas <b>inteiro</b>.
        //
        // Esta linha <b>nao tem poder de falha sozinha</b>, e dize-lo e mais honesto
        // que remove-la (ACH-02). `CORPO_A` ocupa 1..3 e `CORPO_B` 1..2: as faixas se
        // sobrepoem, de modo que toda intercalacao colide antes no indice unico
        // parcial e reverte inteira — a mistura e inalcancavel, com ou sem bloqueio.
        // Faixas disjuntas nao sao alternativa: o contrato exige ordens contiguas a
        // partir de 1. Quem prova a serializacao aqui e o `200` acima; esta assercao
        // fica como rede para o dia em que a forma do conjunto mudar e a mistura
        // passar a caber.
        Assertions.assertThat(resultante).isIn(CORPO_A, CORPO_B);
    }

    @Test
    @Timeout(180)
    @DisplayName("substituicoes simultaneas que permutam posicoes nao violam o indice unico")
    void permutacoesSimultaneasNaoColidemNoIndice() throws Exception {
        UUID projeto = projetoConfiguravel();
        cenario.fluxoPadrao(projeto, ana());

        for (int rodada = 0; rodada < RODADAS; rodada++) {
            umaRodadaDePermutacoes(projeto);
        }
    }

    private void umaRodadaDePermutacoes(UUID projeto) throws Exception {
        // Oito requisicoes disputam as mesmas posicoes 1..N ao mesmo tempo, cada
        // uma com os nomes em uma rotacao diferente. Sem serializacao, duas delas
        // deslocam as vigentes para a faixa de trabalho simultaneamente e uma grava
        // a posicao que a outra ainda nao liberou — violacao do indice unico
        // parcial no meio da transacao, com estado final que seria valido sozinho.
        List<Callable<Integer>> rotacoes = new ArrayList<>();
        for (int i = 0; i < PERMUTACOES; i++) {
            int giro = i;
            rotacoes.add(() -> configurar(projeto, rotacionar(CORPO_A, giro)));
        }

        // `200` exato, pela mesma razao do teste acima: serializadas, as oito sao
        // validas. Foi aqui que a medicao de 2026-09-14 encontrou as 8 violacoes de
        // `etapa_projeto_ordem_unico` traduzidas para `409` passando por baixo de um
        // `isLessThan(500)`, com o bloqueio removido.
        Assertions.assertThat(emParalelo(rotacoes)).allSatisfy(codigo ->
                Assertions.assertThat(codigo).isEqualTo(200));

        // Integridade do desfecho, e nao qual rotacao venceu: as posicoes sao
        // contiguas a partir de 1, sem repeticao, e o conjunto de nomes e o mesmo.
        // Fluxo com ordem duplicada ou com etapa parada na faixa de trabalho e
        // estado que nenhuma requisicao pediu.
        Assertions.assertThat(ordensDoFluxo(projeto))
                .containsExactlyElementsOf(
                        java.util.stream.IntStream.rangeClosed(1, CORPO_A.size()).boxed().toList());
        Assertions.assertThat(nomesDoFluxo(projeto))
                .containsExactlyInAnyOrderElementsOf(CORPO_A);
    }

    @Test
    @Timeout(120)
    @DisplayName("corpo que nomeia etapa ja arquivada por outra substituicao sai 422 etapa-fora-do-fluxo")
    void corpoComIdDeEtapaArquivadaERecusado() throws Exception {
        UUID projeto = projetoConfiguravel();
        cenario.fluxoPadrao(projeto, ana());

        // O outro caminho da concorrencia, decidido na emenda de SDR-005 de
        // 2026-09-14 (ACH-11). Ate aqui a classe so exercitava corpos sem `id`,
        // em que cada requisicao arquiva tudo o que encontrar.
        //
        // A premissa obvia — duas requisicoes com os mesmos `id` — <b>nao</b>
        // produz a recusa, e vale registrar: corpo que preserva todos os `id`
        // vigentes e valido quantas vezes for enviado, porque nada foi arquivado.
        // A recusa exige que a primeira <b>reduza</b> o fluxo.
        List<UUID> ids = idsDoFluxo(projeto);

        // A primeira encolhe o fluxo e arquiva a ultima etapa.
        List<UUID> reduzido = ids.subList(0, ids.size() - 1);
        Assertions.assertThat(configurarComIds(projeto, reduzido, "X")).isEqualTo(200);

        // A segunda ainda carrega o `id` que deixou de existir. SDR-005 decidiu
        // que este desfecho e pretendido e que nao e `409`: nada no estado colide
        // com o pedido, o pedido e que se refere a coisa que deixou de existir, e
        // `409` convidaria a retentar o mesmo corpo.
        Assertions.assertThat(corpoDaRecusa(projeto, ids)).contains("etapa-fora-do-fluxo");
    }

    @Test
    @Timeout(120)
    @DisplayName("duas substituicoes simultaneas com id so podem terminar em 200 ou em 422, nunca em conflito")
    void substituicoesSimultaneasComIdNaoProduzemConflito() throws Exception {
        UUID projeto = projetoConfiguravel();
        cenario.fluxoPadrao(projeto, ana());

        List<UUID> ids = idsDoFluxo(projeto);
        List<UUID> reduzido = ids.subList(0, ids.size() - 1);

        // Qual das duas chega primeiro nao e propriedade do sistema, e por isso o
        // predicado nao fixa o par: se a que encolhe vencer, a outra sai `422`
        // porque nomeia etapa arquivada; se vencer a completa, as duas saem
        // `200`. O que <b>nenhuma</b> ordem pode produzir e `409` ou `5xx` —
        // sem serializacao as duas decidem sobre o fluxo intacto e colidem no
        // indice unico, que e exatamente o desfecho que esta linha recusa.
        List<Integer> status = emParalelo(List.of(
                () -> configurarComIds(projeto, ids, "X"),
                () -> configurarComIds(projeto, reduzido, "Y")));

        Assertions.assertThat(status).allSatisfy(codigo ->
                Assertions.assertThat(codigo).isIn(200, 422));
        Assertions.assertThat(status).contains(200);
    }

    // ------------------------------------------------------------- utilitarios

    private UUID projetoConfiguravel() {
        UUID projeto = cenario.projeto("Alfa");
        cenario.participante(
                projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        return projeto;
    }

    /**
     * Substitui o fluxo por etapas novas com os nomes dados — a ultima terminal — e
     * devolve o status.
     *
     * <p>Todas sao criadas sem {@code id}, de proposito, e a razao e a inversa da
     * que este javadoc afirmava antes (ACH-10): corpo <b>sem</b> {@code id} e o que
     * torna a requisicao sempre aceitavel, porque nao ha identificador a conferir
     * contra o fluxo vigente, e portanto o unico desfecho possivel alem de {@code
     * 200} e o defeito. Corpo <b>com</b> {@code id} tem desfecho legitimo diferente
     * sob serializacao — {@code 422 etapa-fora-do-fluxo}, decidido em SDR-005 —, o
     * que mistura recusa esperada com recusa defeituosa no mesmo predicado. Esse
     * caminho tem teste proprio, {@code substituicoesSimultaneasComOsMesmosId...},
     * e e la que o {@code 422} e fixado.
     */
    private int configurar(UUID projetoId, List<String> nomes) throws Exception {
        StringBuilder corpo = new StringBuilder("{ \"etapas\": [");
        for (int i = 0; i < nomes.size(); i++) {
            corpo.append(i == 0 ? "" : ",")
                    .append("{\"nome\": \"").append(nomes.get(i))
                    .append("\", \"ordem\": ").append(i + 1)
                    .append(", \"terminal\": ").append(i == nomes.size() - 1)
                    .append("}");
        }
        corpo.append("] }");

        return mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/projetos/{projetoId}/etapas", projetoId)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo.toString()))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    /**
     * Dispara as chamadas e so as libera quando todas estao prontas.
     *
     * <p>A barreira e o que torna a disputa real: sem ela, o custo de subir cada
     * thread basta para serializar as requisicoes por acidente, e o teste passaria
     * em verde sobre a implementacao que ele existe para reprovar.
     */
    private List<Integer> emParalelo(List<Callable<Integer>> chamadas) throws Exception {
        var largada = new CyclicBarrier(chamadas.size());
        List<Callable<Integer>> sincronizadas = chamadas.stream()
                .map(chamada -> (Callable<Integer>) () -> {
                    // Toda espera com teto (ACH-19). Barreira sem teto transforma
                    // "uma thread nao chegou" em suite pendurada, que o CI reporta
                    // como tempo esgotado do job inteiro e nao como esta classe.
                    largada.await(SEGUNDOS_ATE_DESISTIR, TimeUnit.SECONDS);
                    return chamada.call();
                })
                .toList();

        List<Integer> status = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(chamadas.size())) {
            for (var futuro : executor.invokeAll(sincronizadas)) {
                status.add(futuro.get(SEGUNDOS_ATE_DESISTIR, TimeUnit.SECONDS));
            }
        }
        return status;
    }

    private static List<String> rotacionar(List<String> nomes, int giro) {
        List<String> girados = new ArrayList<>(nomes.size());
        for (int i = 0; i < nomes.size(); i++) {
            girados.add(nomes.get((i + giro) % nomes.size()));
        }
        return girados;
    }

    /**
     * Substitui o fluxo reaproveitando os {@code id} dados, com o sufixo no nome
     * para que as duas requisicoes sejam distinguiveis.
     */
    private int configurarComIds(UUID projetoId, List<UUID> ids, String sufixo) throws Exception {
        StringBuilder corpo = new StringBuilder("{ \"etapas\": [");
        for (int i = 0; i < ids.size(); i++) {
            corpo.append(i == 0 ? "" : ",")
                    .append("{\"id\": \"").append(ids.get(i))
                    .append("\", \"nome\": \"E").append(i).append(sufixo)
                    .append("\", \"ordem\": ").append(i + 1)
                    .append(", \"terminal\": ").append(i == ids.size() - 1)
                    .append("}");
        }
        corpo.append("] }");
        return put(projetoId, corpo.toString()).getStatus();
    }

    private String corpoDaRecusa(UUID projetoId, List<UUID> ids) throws Exception {
        StringBuilder corpo = new StringBuilder("{ \"etapas\": [");
        for (int i = 0; i < ids.size(); i++) {
            corpo.append(i == 0 ? "" : ",")
                    .append("{\"id\": \"").append(ids.get(i))
                    .append("\", \"nome\": \"Z").append(i)
                    .append("\", \"ordem\": ").append(i + 1)
                    .append(", \"terminal\": ").append(i == ids.size() - 1)
                    .append("}");
        }
        corpo.append("] }");

        var resposta = put(projetoId, corpo.toString());
        Assertions.assertThat(resposta.getStatus()).isEqualTo(422);
        return resposta.getContentAsString();
    }

    private org.springframework.mock.web.MockHttpServletResponse put(UUID projetoId, String corpo)
            throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/projetos/{projetoId}/etapas", projetoId)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andReturn()
                .getResponse();
    }

    private List<String> nomesDoFluxo(UUID projetoId) throws Exception {
        return JsonPath.read(fluxo(projetoId), "$.etapas[*].nome");
    }

    private List<Integer> ordensDoFluxo(UUID projetoId) throws Exception {
        return JsonPath.read(fluxo(projetoId), "$.etapas[*].ordem");
    }

    private List<UUID> idsDoFluxo(UUID projetoId) throws Exception {
        List<String> ids = JsonPath.read(fluxo(projetoId), "$.etapas[*].id");
        return ids.stream().map(UUID::fromString).toList();
    }

    /**
     * Le o fluxo vigente, e <b>exige {@code 200} antes de extrair</b> (ACH-21).
     *
     * <p>A leitura era feita com varredura profunda ({@code $..nome}) sobre um corpo
     * cujo status ninguem conferia. Um {@code 403} ou um {@code 500} devolve
     * {@code problem+json}, que nao tem {@code nome} nem {@code ordem} — a
     * varredura devolveria lista vazia e as duas asercoes de integridade
     * passariam sobre uma resposta de erro. O caminho definido tem a mesma virtude
     * pelo outro lado: se a forma da resposta mudar, o teste quebra em vez de
     * silenciosamente parar de verificar.
     */
    private String fluxo(UUID projetoId) throws Exception {
        var resposta = mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/projetos/{projetoId}/etapas", projetoId)
                        .with(ana()))
                .andReturn()
                .getResponse();

        Assertions.assertThat(resposta.getStatus()).isEqualTo(200);
        return resposta.getContentAsString();
    }
}
