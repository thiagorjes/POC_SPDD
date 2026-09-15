package br.com.idsd.kanban.internal.tarefa;

import br.com.idsd.kanban.internal.projeto.TarefasAtivasPorEtapa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao de {@link TarefasAtivasPorEtapa} — RF-017, RN-020.
 *
 * <p><b>Publicar este bean e o que liga a recusa</b>, e por isso ele nasce aqui e
 * nao junto da porta. {@code EtapaService} recebe a porta em {@code Optional} e,
 * sem implementacao, conta zero: o caminho de recusa esta escrito desde TASK-02.2 e
 * ficava inerte por falta de massa, nao por falta de codigo — antes desta task nao
 * havia tarefa alguma para impedir arquivamento.
 *
 * <p><b>A direcao da dependencia e obrigatoria.</b> O dominio de tarefa implementa
 * a porta do dominio de projeto, e nunca o contrario: importar o repositorio de
 * tarefa dentro de {@code internal/projeto} faria configuracao depender de
 * operacao, que e o atalho que a porta existe para fechar.
 *
 * <p><b>Terminal nao conta.</b> Tarefa em {@link Condicao#CONCLUIDA} ou
 * {@link Condicao#ENCERRADA_SEM_CONCLUSAO} nao impede o arquivamento: ela nao vai
 * se mover de novo, e conta-la tornaria toda etapa terminal inarquivavel para
 * sempre — a etapa onde as tarefas terminam e justamente a que mais as acumula.
 */
@Component
public class ContagemDeTarefasAtivas implements TarefasAtivasPorEtapa {

    /** O complemento de "ativa": as condicoes de onde nao ha saida por movimento. */
    private static final Set<Condicao> TERMINAIS =
            EnumSet.of(Condicao.CONCLUIDA, Condicao.ENCERRADA_SEM_CONCLUSAO);

    @PersistenceContext
    private EntityManager em;

    /**
     * Uma consulta para todas as etapas, e nao uma por etapa.
     *
     * <p>Ela roda dentro da transacao que ja segura o bloqueio pessimista da linha
     * de {@code projeto} (SDR-005): cada ida ao banco aqui e tempo em que mais
     * ninguem configura o projeto, e ate cem etapas fariam disso um {@code N+1} com
     * todo o projeto serializado atras.
     *
     * <p>Colecao vazia sai sem consulta: {@code in ()} nao e SQL valido, e perguntar
     * por nada tem uma resposta que nao precisa do banco.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Long> contarEm(Collection<UUID> etapaIds) {
        if (etapaIds == null || etapaIds.isEmpty()) {
            return Map.of();
        }

        var contagem = new HashMap<UUID, Long>();
        em.createQuery(
                        "select t.etapaId, count(t) from Tarefa t"
                                + " where t.etapaId in :etapaIds and t.condicao not in :terminais"
                                + " group by t.etapaId",
                        Object[].class)
                .setParameter("etapaIds", etapaIds)
                .setParameter("terminais", TERMINAIS)
                .getResultList()
                .forEach(linha -> contagem.put((UUID) linha[0], (Long) linha[1]));
        return Map.copyOf(contagem);
    }
}
