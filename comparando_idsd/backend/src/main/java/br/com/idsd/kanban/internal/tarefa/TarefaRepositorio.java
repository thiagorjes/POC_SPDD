package br.com.idsd.kanban.internal.tarefa;

import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Acesso a {@link Tarefa}.
 *
 * <p>A base e {@link Repository} e nao {@code JpaRepository}, seguindo o que
 * ACH-01 da revisao de TASK-02.1 instituiu para o anel de configuracao. Aqui a
 * razao e outra e mais simples: a projecao e apagada e refeita <b>inteira</b>
 * pela rotina de reconstrucao ({@code data-model.md} secao 9), que e operacao
 * administrativa com bloqueio consultivo por projeto. Remocao avulsa de tarefa
 * nao e operacao do sistema — nao ha rota que a peca —, e publicar sete
 * assinaturas capazes de faze-la nao serve a consumidor nenhum.
 *
 * <p>Sem metodo declarado: o primeiro consumidor e a criacao de tarefa, em
 * TASK-02.5. Assinatura nasce com o consumidor (ACH-13).
 */
public interface TarefaRepositorio extends Repository<Tarefa, UUID> {
}
