package br.com.idsd.kanban.internal.projeto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementacao do fragmento de escrita de {@link EtapaRepositorio}.
 *
 * <p>O nome <b>nao e livre</b>: Spring Data casa o fragmento pelo sufixo
 * {@code Impl} sobre o nome da interface do repositorio. Renomear esta classe faz
 * o contexto subir sem a escrita, e a falha so aparece na primeira substituicao de
 * fluxo.
 */
class EtapaRepositorioImpl implements SubstituicaoDeFluxo {

    private static final Logger LOG = LoggerFactory.getLogger(EtapaRepositorioImpl.class);

    @PersistenceContext
    private EntityManager em;

    /**
     * {@inheritDoc}
     *
     * <p>{@code find} com {@link LockModeType#PESSIMISTIC_WRITE} emite
     * {@code SELECT ... FOR UPDATE} sobre a linha de {@code projeto}. E
     * {@code find} e nao consulta derivada porque o bloqueio precisa ir ao banco
     * ainda que a entidade ja esteja no contexto de persistencia desta transacao —
     * {@code find} com lock explicito promove o bloqueio da instancia gerenciada,
     * que e exatamente o caso da rota, onde o projeto pode ter sido carregado pela
     * resolucao de permissao.
     *
     * <p><b>O teto de espera nao esta aqui</b>, e a ausencia e deliberada: ele
     * vive em {@code lock_timeout} na sessao (ver {@code application.yml}), porque
     * o hint {@code jakarta.persistence.lock.timeout} so tem traducao garantida
     * para espera zero no dialeto PostgreSQL. Hint positivo seria um teto que este
     * metodo afirma ter e que o banco nao aplica. O teto da transacao inteira esta
     * em {@code @Transactional(timeout)} na rota que chama — ACH-01 da reexecucao
     * de TASK-02.2.
     *
     * <p>Projeto ausente aqui nao e entrada de usuario: a rota resolveu o
     * {@code 404} antes de chegar ao servico. E defeito de estado, e por isso a
     * recusa tem a mesma forma da de {@link #substituirFluxo}.
     *
     * <p><b>O identificador nao entra na mensagem</b> (ACH-17). Mensagem de
     * excecao viaja para lugares que quem a escreve nao escolhe, e o tratador
     * global registra a excecao inteira no log — o {@code projetoId} chega la pelo
     * caminho que tem {@code traceId} e nivel controlado, nao pelo texto.
     */
    @Override
    public void bloquearProjeto(UUID projetoId) {
        Projeto projeto = em.find(Projeto.class, projetoId, LockModeType.PESSIMISTIC_WRITE);
        if (projeto == null) {
            LOG.error("Projeto inexistente na substituicao de fluxo: {}", projetoId);
            throw new IllegalStateException("projeto inexistente na substituicao de fluxo");
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>A faixa e {@link Etapa#FAIXA_DE_TRABALHO}, e ela e inalcancavel por
     * requisicao porque {@link Etapa#ORDEM_MAXIMA} a torna assim — validacao, e nao
     * suposicao sobre o uso (ACH-04). O deslocamento usa
     * {@code Etapa#deslocarOrdem}, o unico caminho que grava fora da faixa real.
     */
    @Override
    public void liberarOrdens(List<Etapa> vigentes) {
        for (Etapa etapa : vigentes) {
            etapa.deslocarOrdem(etapa.getOrdem() + Etapa.FAIXA_DE_TRABALHO);
        }
        em.flush();
    }

    /**
     * {@inheritDoc}
     *
     * <p>A etapa <b>ja gerenciada</b> nao precisa de nada alem do flush: ela foi
     * carregada nesta mesma transacao, e a reconfiguracao e o arquivamento ja estao
     * no contexto. O que sobra e criacao, e criacao e {@code persist}.
     *
     * <p>Nao ha {@code merge} aqui, e a ausencia e intencional. {@code merge} numa
     * entidade destacada com {@code id} atribuido emite um {@code select} que sempre
     * erra para etapa nova e — pior — <b>aceitaria</b> uma etapa vinda de fora desta
     * transacao, que e como escrita nao decidida por {@link EtapaService} entraria no
     * fluxo.
     *
     * <p>O {@code flush} explicito traz a eventual violacao do indice unico parcial
     * para dentro desta chamada, em vez de deixa-la estourar no commit, longe de quem
     * a causou.
     */
    @Override
    public void substituirFluxo(UUID projetoId, List<Etapa> tocadas) {
        for (Etapa etapa : tocadas) {
            // O `projetoId` deixa de ser parametro decorativo (ACH-13): ele delimita
            // de fato o que esta chamada pode escrever. Etapa de outro projeto aqui e
            // defeito de quem chamou, e nao entrada de usuario — a borda ja recusou o
            // id estranho ao fluxo —, e por isso a recusa e de estado e nao de regra.
            if (!projetoId.equals(etapa.getProjetoId())) {
                throw new IllegalStateException(
                        "etapa de outro projeto na substituicao de fluxo");
            }
            if (!em.contains(etapa)) {
                em.persist(etapa);
            }
        }
        em.flush();
    }
}
