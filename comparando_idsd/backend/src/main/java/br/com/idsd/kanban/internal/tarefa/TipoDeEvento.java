package br.com.idsd.kanban.internal.tarefa;

/**
 * Catalogo fechado dos tipos de evento do ciclo de vida da tarefa
 * ({@code data-model.md} secao 4).
 *
 * <p>Nasce aqui, e nao em TASK-02.3, pela regra de ACH-13 da revisao de
 * TASK-02.1: assinatura nasce com o consumidor. O consumidor e o registrador de
 * eventos, que e desta task — enquanto nao havia escritor, o enum teria valores
 * sem nenhum caminho que os gravasse.
 *
 * <p><b>O efeito de cada tipo sobre as tres series de tempo nao esta aqui</b>, e
 * a ausencia e deliberada. O tipo e fato do anel de verdade; o intervalo que ele
 * abre ou fecha e conhecimento do anel de projecao, e vive em
 * {@code AplicadorDeIntervalos}. Juntar os dois neste arquivo faria o log
 * depender da projecao que ele existe para poder reconstruir.
 *
 * <p>A coluna {@code tipo} de {@code evento_tarefa} nao tem restricao de
 * verificacao no banco — ver {@link EventoTarefa}. A enumeracao protege o
 * caminho que passa pela aplicacao, que e o unico caminho de escrita que existe.
 */
public enum TipoDeEvento {

    /** RF-004. */
    TAREFA_CRIADA,

    /** RF-007. */
    TAREFA_ASSUMIDA,

    /** RF-008, RN-027. */
    TAREFA_DEVOLVIDA,

    /** RF-005, RF-006. */
    TAREFA_MOVIDA,

    /** RF-009. */
    IMPEDIMENTO_ABERTO,

    /** RF-009, RN-010: a segunda sinalizacao, que nao abre impedimento novo. */
    IMPEDIMENTO_ANOTADO,

    /** RF-010. */
    IMPEDIMENTO_RESOLVIDO,

    /** RF-011. */
    TAREFA_CONCLUIDA,

    /** RF-012. */
    TAREFA_ENCERRADA_SEM_CONCLUSAO,

    /** RF-013: comeca outro episodio (RN-019). */
    TAREFA_REABERTA
}
