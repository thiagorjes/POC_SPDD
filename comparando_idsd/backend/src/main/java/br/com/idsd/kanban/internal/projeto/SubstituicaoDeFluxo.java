package br.com.idsd.kanban.internal.projeto;

import java.util.List;
import java.util.UUID;

/**
 * A escrita <b>decidida</b> de {@link Etapa}: criacao e descarga.
 *
 * <p>Nao e "a unica escrita de etapa no sistema", como este javadoc afirmava ate
 * 2026-09-14 (ACH-08). Sob JPA o dirty checking e uma segunda porta: as entidades
 * que {@link EtapaService} muta estao gerenciadas, e qualquer flush automatico as
 * grava sem passar por aqui. O que este fragmento concentra e o {@code persist} do
 * que ainda nao existe e o {@code flush} que decide <i>quando</i> a escrita chega
 * ao banco — e e disso que depende a ordem dos passos da substituicao.
 *
 * <p>Fragmento a parte, e nao {@code JpaRepository}, pela mesma razao registrada
 * em {@link EtapaRepositorio}: {@code JpaRepository} publicaria sete assinaturas
 * de remocao fisica, e a remocao de etapa e logica. Aqui so existe o que esta
 * declarado abaixo, e o que esta declarado abaixo nao remove nada.
 *
 * <p><b>Sao duas operacoes e nao uma porque o indice nao e adiavel.</b>
 * {@code etapa_projeto_ordem_unico} e unico parcial, {@code DEFERRABLE} so existe
 * em {@code UNIQUE CONSTRAINT} e restricao parcial nao e constraint no PostgreSQL
 * — de modo que trocar a ordem de duas etapas ativas viola o indice <i>no meio</i>
 * da transacao, ainda que o estado final seja valido. O passo intermediario esta
 * anotado em {@code V2026091412__correcoes_etapa_e_raia.sql} (ACH-07 da revisao de
 * TASK-02.1) e e exigencia do banco, nao escolha de desenho.
 */
public interface SubstituicaoDeFluxo {

    /**
     * Toma bloqueio pessimista sobre a linha do projeto (SDR-005).
     *
     * <p>Ela vem <b>antes da leitura do fluxo vigente</b>, que e a clausula que
     * SDR-005 fixa — e nao "a primeira instrucao do metodo", que e coisa diferente
     * e mais forte do que o DR pede. Serializa duas requisicoes
     * concorrentes ao mesmo projeto: a segunda so le o fluxo depois de a primeira
     * ter comitado, e decide sobre o fluxo ja substituido. Sem ele as duas leem o
     * mesmo conjunto vigente, e a etapa criada por uma nao esta no corpo da outra —
     * nao e arquivada nem reordenada, e a escrita se perde em silencio ou colide no
     * indice unico com {@code 500} intermitente. A transacao garante atomicidade;
     * isolamento de decisao, nao.
     *
     * <p><b>Fica aqui e nao em {@code ProjetoRepository}</b>, como o downstream de
     * SDR-005 antecipava, por restricao da suite congelada: {@code EtapaServiceTest}
     * nomeia {@code new EtapaService(EtapaRepositorio)} e esta fora do alcance de
     * quem implementa, de modo que um colaborador novo no servico nao cabe. A
     * decisao de SDR-005 — qual linha e travada, quando e por quanto tempo — e
     * cumprida inteira; o que muda e por qual porta ela passa.
     *
     * <p><b>Sobre a coexistencia com SDR-004, este javadoc nao afirma nada</b>, e
     * a mudanca e o conserto de ACH-06 da reexecucao de TASK-02.2. Ate 2026-09-14
     * ele dizia que a ordem de aquisicao era identica a do contador de {@code seq}
     * e que nao havia ciclo possivel — descrevendo codigo que ainda nao existe. A
     * obrigacao e real e fica registrada onde tem data e status: e o downstream de
     * SDR-004, e quem implementar o publicador confronta as duas ordens naquele
     * momento. Garantia sobre codigo futuro nao e garantia; e uma afirmacao que
     * ninguem vai reconferir porque ja esta escrita.
     *
     * @param projetoId o projeto a travar; deve existir, e a rota ja garantiu isso
     *     ao resolver a permissao
     */
    void bloquearProjeto(UUID projetoId);

    /**
     * Passo intermediario: tira as ordens vigentes do caminho.
     *
     * <p>Desloca a ordem de cada etapa ativa para {@link Etapa#FAIXA_DE_TRABALHO} —
     * faixa que nenhum fluxo real ocupa <b>porque {@link Etapa#ORDEM_MAXIMA} a
     * recusa na borda</b>, e nao porque se suponha que ninguem a peca — e descarrega
     * a alteracao, de modo que a reatribuicao seguinte nunca
     * encontre a ordem que pretende usar ainda ocupada.
     *
     * <p>O deslocamento e feito <b>pela entidade</b> e nao por {@code update} em
     * massa, e a diferenca importa: {@code update} em massa nao passa pelo contexto
     * de persistencia, e as etapas que o chamador ja tem em maos ficariam com a
     * ordem antiga em memoria. A etapa cuja ordem final coincide com a original
     * pareceria entao inalterada, o Hibernate nao emitiria {@code UPDATE} algum e a
     * linha ficaria no banco com a ordem deslocada — fluxo correto na resposta e
     * corrompido em disco.
     */
    void liberarOrdens(List<Etapa> vigentes);

    /**
     * Persiste todas as etapas tocadas pela substituicao — criadas, reconfiguradas
     * e arquivadas — na transacao em curso.
     *
     * @param projetoId o projeto cujo fluxo foi substituido; <b>delimita</b> o que
     *     esta chamada pode escrever, e etapa de outro projeto e recusada
     * @param tocadas as etapas alteradas ou criadas nesta operacao
     */
    void substituirFluxo(UUID projetoId, List<Etapa> tocadas);
}
