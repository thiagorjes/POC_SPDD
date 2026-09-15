package br.com.idsd.kanban.internal.tempo;

/**
 * As tres series de tempo da tarefa ({@code data-model.md} secao 5).
 *
 * <p>Correm em paralelo: um mesmo instante pode estar coberto pelas tres da
 * mesma tarefa (SCN-003.3, SCN-016.1). <b>Nunca se somam entre si</b> (RN-008) —
 * e por isso que nao ha aqui, nem em {@link IntervaloTarefa}, nada que se pareca
 * com um total.
 *
 * <p>Como {@code TipoDeEvento}, nasce com o primeiro escritor, que e o aplicador
 * de intervalos desta task.
 */
public enum TipoDeIntervalo {

    /** Tempo na etapa vigente. Fecha e reabre a cada movimentacao. */
    PERMANENCIA,

    /** Tempo sem responsavel (RN-006). Fecha na tomada, reabre na devolucao. */
    ESPERA_TOMADA,

    /**
     * Tempo impedido (RF-009, RF-010).
     *
     * <p>Atravessa a movimentacao sem ser tocado: RN-009 manda o impedimento
     * acompanhar a tarefa sem que a transicao encerre nem reinicie a contagem.
     */
    IMPEDIMENTO
}
