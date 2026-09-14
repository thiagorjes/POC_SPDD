package br.com.idsd.kanban.internal.tarefa;

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
    ENCERRADA_SEM_CONCLUSAO
}
