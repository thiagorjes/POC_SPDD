package br.com.idsd.kanban.internal.tempo;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

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
 * <p>O primeiro consumidor e o {@link AplicadorDeIntervalos} desta task, e os
 * metodos abaixo sao os dele e os do {@link ReconstrutorDeProjecao}.
 */
public interface IntervaloTarefaRepositorio extends Repository<IntervaloTarefa, Long> {

    /**
     * Abre um intervalo novo ou grava o fechamento de um ja carregado.
     *
     * <p>{@code save} aqui e {@code UPDATE} de verdade — e legitimo, ao contrario
     * do que seria no log: a projecao e descartavel por construcao (SDR-001), e
     * fechar um intervalo e escrever {@code fim} sobre a linha que existe.
     */
    IntervaloTarefa save(IntervaloTarefa intervalo);

    /**
     * As series em curso da tarefa, em ordem estavel.
     *
     * <p>Uma consulta e nao uma por serie: o aplicador precisa das tres para
     * decidir o que fechar, e o caso de {@code TAREFA_CONCLUIDA} — "fecha todos
     * abertos" — nao sabe de antemao quais sao.
     *
     * <p>A ordenacao nao tem consumidor que dependa dela; existe para que a
     * projecao reconstruida saia na mesma ordem de insercao da gravada, e a
     * comparacao entre as duas nao dependa do plano do banco.
     */
    List<IntervaloTarefa> findByTarefaIdAndFimIsNullOrderByTipoAscIdAsc(UUID tarefaId);

    /** Atalho de leitura de {@link #findByTarefaIdAndFimIsNullOrderByTipoAscIdAsc}. */
    default List<IntervaloTarefa> abertosDe(UUID tarefaId) {
        return findByTarefaIdAndFimIsNullOrderByTipoAscIdAsc(tarefaId);
    }

    /** A projecao de tempo de um projeto inteiro, na ordem de insercao. */
    List<IntervaloTarefa> findByProjetoIdOrderByIdAsc(UUID projetoId);

    /**
     * Remove as linhas que o log nao produz.
     *
     * <p><b>So o {@link ReconstrutorDeProjecao} chama</b>, e a assinatura e em
     * lote de proposito. Um {@code delete(id)} publicado aqui apagaria tempo ja
     * contado sem deixar evento — o dano que SDR-001 nao tem como detectar,
     * porque o log nao registra o que a projecao perdeu. Em lote, o unico jeito
     * de chamar e tendo calculado antes qual e o excedente, que e o que a rotina
     * de reconstrucao faz.
     *
     * <p><b>Chame {@link #apagarPorIdEmLotes(List)}</b>, nao este. A lista vai
     * inteira para o {@code in} e o protocolo do PostgreSQL nao aceita mais de
     * 65535 parametros ligados por comando: acima disso a remocao falha no meio de
     * uma reconstrucao que ja reescreveu linhas (ACH-18). O metodo continua
     * publicado porque e ele que o loteamento chama.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from IntervaloTarefa i where i.id in :ids")
    void apagarPorId(@Param("ids") Collection<Long> ids);

    /** Teto folgado sobre o limite de parametros ligados do protocolo. */
    int LOTE_DE_REMOCAO = 1000;

    /**
     * {@link #apagarPorId} em lotes de {@value #LOTE_DE_REMOCAO}.
     *
     * <p>Nao e atomico por lote nem precisa ser: a chamada inteira roda dentro da
     * transacao da reconstrucao, e o que reverte reverte junto.
     */
    default void apagarPorIdEmLotes(List<Long> ids) {
        for (int i = 0; i < ids.size(); i += LOTE_DE_REMOCAO) {
            apagarPorId(ids.subList(i, Math.min(i + LOTE_DE_REMOCAO, ids.size())));
        }
    }
}
