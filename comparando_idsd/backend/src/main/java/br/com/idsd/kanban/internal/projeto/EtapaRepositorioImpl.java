package br.com.idsd.kanban.internal.projeto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

/**
 * Implementacao do fragmento de escrita de {@link EtapaRepositorio}.
 *
 * <p>O nome <b>nao e livre</b>: Spring Data casa o fragmento pelo sufixo
 * {@code Impl} sobre o nome da interface do repositorio. Renomear esta classe faz
 * o contexto subir sem a escrita, e a falha so aparece na primeira substituicao de
 * fluxo.
 */
class EtapaRepositorioImpl implements SubstituicaoDeFluxo {

    /**
     * Distancia entre a faixa de trabalho e qualquer ordem real.
     *
     * <p>Somar preserva a unicidade entre as deslocadas — a soma e injetora — e o
     * valor e alto o bastante para que nenhuma ordem final de um fluxo de verdade
     * caia dentro da faixa. Fosse baixo, o passo que existe para evitar a colisao a
     * reintroduziria pelo outro lado.
     */
    private static final int FAIXA_DE_TRABALHO = 1_000_000;

    @PersistenceContext
    private EntityManager em;

    @Override
    public void liberarOrdens(List<Etapa> vigentes) {
        for (Etapa etapa : vigentes) {
            etapa.reconfigurar(
                    etapa.getNome(), etapa.getOrdem() + FAIXA_DE_TRABALHO, etapa.isTerminal());
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
            if (!em.contains(etapa)) {
                em.persist(etapa);
            }
        }
        em.flush();
    }
}
