package br.com.idsd.kanban.internal.tarefa;

import java.util.List;
import java.util.UUID;

/**
 * Corpo de {@code GET /v1/projetos/{projetoId}/board} (RF-003).
 *
 * <p><b>A grade e o produto cartesiano etapa x raia</b>, e vem completa: etapa sem
 * tarefa aparece com lista vazia, e nao omitida. Omitir a etapa vazia esconderia do
 * time uma etapa configurada — e a coluna vazia e justamente a informacao de que o
 * trabalho nao chegou ali (SCN-003.2).
 *
 * <p><b>Nenhum campo agrega as tres series</b> (RN-008). Elas saem lado a lado
 * dentro de cada {@link CartaoResposta}, e nao ha aqui total de projeto, total de
 * etapa nem total de cartao: o primeiro campo de soma seria o caminho mais curto
 * para recolapsar as dimensoes que a modelagem inteira existe para separar.
 *
 * <p>{@code seq} e o ultimo numero de sequencia de evento do projeto (SDR-004). Ele
 * viaja no board porque e o que o cliente compara com o {@code seq} recebido pelo
 * canal de tempo real para detectar lacuna (ADR-004): sem ele na leitura inicial,
 * nao ha marco a partir do qual medir o que faltou.
 */
public record BoardResposta(
        long seq, boolean acessoPorAdministracaoGlobal, List<EtapaDoBoard> etapas) {

    /**
     * Uma coluna do board. {@code ordem} e {@code terminal} viajam porque a tela
     * precisa saber onde a etapa esta e se dela nao se sai (RF-011).
     *
     * <p>{@code id} e {@code null} na <b>etapa sintetica</b> — ver
     * {@link #RaiaDoBoard}, porque a regra e a mesma nos dois eixos.
     */
    public record EtapaDoBoard(
            UUID id, String nome, int ordem, boolean terminal, List<RaiaDoBoard> raias) {
    }

    /**
     * Uma faixa dentro da coluna.
     *
     * <p><b>A grade tem faixa sintetica nos dois eixos, e e a mesma regra</b>
     * (ACH-02). Nenhum dos dois e total: a raia e opcional por RN-023 e a etapa e
     * arquivavel logicamente por RN-021. Nos dois casos existe cartao que nao cabe
     * em celula alguma, e cartao que nao cabe <b>some do board sem erro</b> — a pior
     * forma de perder trabalho, porque nao ha sintoma a investigar. A faixa de raia
     * e {@code id: null}, {@code "Sem raia"}, ultima da etapa; a de etapa e
     * {@code id: null}, {@code "Fora do fluxo"}, ordem apos a ultima,
     * {@code terminal: false}, ultima da lista.
     *
     * <p>A celula da etapa arquivada e alcancavel por <b>operacao permitida</b> e
     * nao por defeito: {@code ContagemDeTarefasAtivas} exclui as condicoes terminais
     * da contagem que barra o arquivamento, por decisao declarada, justamente para
     * que a etapa terminal seja arquivavel.
     *
     * <p><b>Regra: a sintetica existe quando ha ao menos um cartao que precise
     * dela.</b> Faixa sintetica vazia seria etapa ou raia que ninguem configurou
     * apresentada como se existisse, contra SCN-003.2, que trata a etapa vazia como
     * informacao sobre o <i>fluxo</i>. A consequencia a declarar e que o comprimento
     * das listas varia com os dados, e cliente e verificacao indexam por
     * {@code id}, nunca por posicao.
     *
     * <p><b>O eixo da raia tem um segundo caso, e ele nao e excecao a regra: e outro
     * caso.</b> Projeto que nao configurou raia alguma desenha com a faixa unica
     * mesmo sem cartao — ali a sintetica nao e o lugar de quem nao coube, e a grade
     * inteira, porque raia e organizacao opcional (RN-023) e sem ela um projeto novo
     * nao renderizaria. SCN-018.1 fixa os dois casos na suite congelada. O eixo da
     * etapa nao tem equivalente: projeto sem etapa nao tem cartao.
     *
     * <p>A faixa sintetica e <b>de leitura</b>. Nada nela e destino de escrita, e
     * sair dela e movimentacao comum: a {@code origem} de SDR-002 carrega a
     * {@code etapaId} real e arquivada que o cartao sempre teve.
     */
    public record RaiaDoBoard(UUID id, String nome, List<CartaoResposta> tarefas) {
    }
}
