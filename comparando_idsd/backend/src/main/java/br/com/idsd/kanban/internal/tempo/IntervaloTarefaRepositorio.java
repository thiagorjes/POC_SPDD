package br.com.idsd.kanban.internal.tempo;

import org.springframework.data.repository.Repository;

/**
 * Acesso a {@link IntervaloTarefa}.
 *
 * <p>{@link Repository} como os demais (ACH-01). A remocao em massa que a
 * reconstrucao da projecao precisa e operacao administrativa, com bloqueio
 * consultivo por projeto e janela sem escrita; ela nao passa por um
 * {@code deleteAll} publicado a todo o sistema.
 *
 * <p>Nao ha nem havera metodo que agregue por pessoa: nao ha coluna de pessoa
 * para agrupar (RN-014) — ver {@link IntervaloTarefa}. A ausencia do metodo e
 * consequencia da ausencia da coluna, e nao uma segunda barreira independente.
 *
 * <p>Sem metodo declarado: o primeiro consumidor e o aplicador de intervalos de
 * EPIC-03 (ACH-13).
 */
public interface IntervaloTarefaRepositorio extends Repository<IntervaloTarefa, Long> {
}
