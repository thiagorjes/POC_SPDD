package br.com.idsd.kanban.internal.projeto;

import java.util.List;
import java.util.UUID;

/**
 * A unica escrita de {@link Etapa} no sistema.
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
     * Passo intermediario: tira as ordens vigentes do caminho.
     *
     * <p>Desloca a ordem de cada etapa ativa para uma faixa que nenhum fluxo real
     * ocupa e descarrega a alteracao, de modo que a reatribuicao seguinte nunca
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
     * @param projetoId o projeto cujo fluxo foi substituido
     * @param tocadas as etapas alteradas ou criadas nesta operacao
     */
    void substituirFluxo(UUID projetoId, List<Etapa> tocadas);
}
