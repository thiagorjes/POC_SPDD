package br.com.idsd.kanban.internal.tarefa;

import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Acesso a {@link Impedimento}.
 *
 * <p>{@link Repository} como os demais, e aqui a remocao seria especialmente
 * cara: o impedimento e a <b>unica</b> fonte da dimensao 3 de RN-002, e apagar
 * a linha em vez de registrar o desfecho apagaria tambem a serie de tempo de
 * impedimento que RF-016 agrega — sem deixar evento nenhum. O impedimento se
 * encerra por {@code desfecho}, nunca por {@code DELETE}.
 *
 * <p>Sem metodo declarado: o primeiro consumidor e a abertura de impedimento,
 * em TASK-04.1 (ACH-13).
 */
public interface ImpedimentoRepositorio extends Repository<Impedimento, UUID> {
}
