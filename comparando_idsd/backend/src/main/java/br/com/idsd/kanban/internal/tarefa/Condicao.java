package br.com.idsd.kanban.internal.tarefa;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Dimensao 2 de RN-002: a condicao da tarefa.
 *
 * <p>O dominio e o de RN-003, e ele existe em dois lugares de proposito: aqui,
 * onde protege o caminho que passa pela aplicacao, e na restricao de
 * verificacao de {@code tarefa.condicao}, que protege o resto.
 *
 * <p><b>{@code IMPEDIDA} nao pertence a este dominio</b>, e a ausencia e a
 * emenda mais cara do PRD (v1.1). Enquanto o impedimento foi valor de
 * condicao, toda movimentacao o sobrescrevia em silencio — INC-01 —, porque
 * mover e escrever condicao. Como dimensao 3 derivada da existencia de
 * {@code impedimento} aberto, nenhuma escrita sobre a tarefa o alcanca.
 */
public enum Condicao {

    /** Sem responsavel (RN-006). A tarefa espera tomada. */
    AGUARDANDO_TOMADA,

    /** Alguem assumiu. */
    EM_CURSO,

    /** Chegou a etapa terminal (RF-011). So sai por reabertura do PO (RN-017). */
    CONCLUIDA,

    /** Terminal absoluto (RN-018): nao ha aresta de saida. */
    ENCERRADA_SEM_CONCLUSAO;

    /**
     * As condicoes de onde nao ha saida por movimento.
     *
     * <p>O conjunto mora aqui e nao em cada consumidor porque ja sao tres os que
     * o consultam por razoes diferentes — o arquivamento de etapa, que nao conta
     * tarefa terminal; o recorte do board, que so recorta tarefa terminal
     * (RN-039); e a propria leitura do cartao. Duas copias divergem no dia em que
     * uma condicao terminal nova nascer, e a divergencia apareceria como cartao
     * que some do board, que e o sintoma mais dificil de atribuir.
     */
    public boolean terminal() {
        return TERMINAIS.contains(this);
    }

    /** O mesmo conjunto, para quem precisa dele como parametro de consulta. */
    public static Set<Condicao> terminais() {
        return TERMINAIS;
    }

    private static final Set<Condicao> TERMINAIS =
            Collections.unmodifiableSet(EnumSet.of(CONCLUIDA, ENCERRADA_SEM_CONCLUSAO));
}
