package br.com.idsd.kanban.internal.tarefa;

import br.com.idsd.kanban.internal.projeto.Etapa;
import br.com.idsd.kanban.internal.projeto.Raia;
import br.com.idsd.kanban.internal.tempo.IntervaloTarefa;
import br.com.idsd.kanban.internal.tempo.TipoDeIntervalo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Monta o board de um projeto (RF-003).
 *
 * <p><b>Cinco consultas, e cinco sempre</b>: o projeto, as etapas vigentes, as raias
 * vigentes, as tarefas do projeto com o nome de quem responde por elas, os
 * intervalos abertos e os impedimentos abertos — cada uma por projeto, nenhuma por
 * cartao. O numero nao cresce com a quantidade de tarefas, e e esse o criterio: o
 * board e a leitura mais exercitada do produto, e uma consulta por cartao estoura o
 * envelope de desempenho no primeiro projeto real. A juncao e feita em memoria de
 * proposito, porque uma consulta unica com quatro juncoes devolveria o produto
 * cartesiano das series e o trabalho de desdobra-la seria o mesmo, com o trafego
 * multiplicado.
 *
 * <p>A conta e de <b>seis</b> chamadas e cinco de projecao, porque a leitura do
 * projeto nao monta grade nenhuma: ela traz o {@code seq}. O criterio de aceite
 * exige que o numero seja <b>fixo</b>, e ele e — a etapa e a raia sao tabelas
 * distintas e uni-las exigiria {@code union}, que nao compra nada.
 *
 * <p><b>O recorte de RN-039 e aplicado na consulta</b> e nao depois (SDR-007): a
 * tarefa terminal so entra no board enquanto o desfecho dela for dos ultimos
 * {@link #JANELA_DE_TERMINAIS}. Recortar em memoria manteria o payload crescendo sem
 * teto com a idade do projeto, que e exatamente o que RNF-011 proibe. O recorte e
 * <b>da tela</b>: nada sai do registro (RN-022, RNF-008).
 *
 * <p>Este objeto nao decide acesso. Quem o chama ja recusou quem nao pode ler — a
 * consulta nao e o lugar de decidir autorizacao, porque a proxima chamada dela
 * poderia esquecer. O que ele decide e <b>existencia</b>: projeto ausente sai
 * {@code 404} aqui e nao como {@code NullPointerException} tres quadros adiante
 * (ACH-06).
 */
@Service
public class BoardQuery {

    /** A janela de RN-039: alem dela, a tarefa terminal sai da tela e nao do registro. */
    public static final Duration JANELA_DE_TERMINAIS = Duration.ofDays(30);

    @PersistenceContext private EntityManager em;

    /**
     * O board do projeto, com a grade completa.
     *
     * <p>Somente leitura, e numa transacao so: as consultas precisam enxergar o mesmo
     * instante do banco. Sem isso, um evento gravado entre a leitura das tarefas e a
     * dos intervalos produziria cartao com serie de outro estado — uma incoerencia
     * rara, invisivel e impossivel de reproduzir depois.
     *
     * <p><b>{@code agora} e resolvido uma vez</b> e desce para todo cartao (ACH-11):
     * dois relogios dentro da mesma leitura produziriam {@code decorrido} incomparavel
     * entre cartoes da mesma tela.
     */
    @Transactional(readOnly = true)
    public BoardResposta montar(UUID projetoId, boolean acessoPorAdministracaoGlobal) {
        Instant agora = Instant.now();
        Instant corte = agora.minus(JANELA_DE_TERMINAIS);

        List<Long> seqs = em.createQuery(
                        "select p.seqAtual from Projeto p where p.id = :projeto", Long.class)
                .setParameter("projeto", projetoId)
                .getResultList();
        if (seqs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto não encontrado");
        }
        long seq = seqs.get(0);

        List<Etapa> etapas = em.createQuery(
                        "select e from Etapa e where e.projetoId = :projeto "
                                + "and e.arquivadaEm is null order by e.ordem asc",
                        Etapa.class)
                .setParameter("projeto", projetoId)
                .getResultList();

        List<Raia> raias = em.createQuery(
                        "select r from Raia r where r.projetoId = :projeto "
                                + "and r.arquivadaEm is null order by r.ordem asc",
                        Raia.class)
                .setParameter("projeto", projetoId)
                .getResultList();

        // O nome do responsavel vem junto, por juncao externa: resolve-lo depois,
        // cartao a cartao, seria o N+1 que esta classe existe para nao ter.
        //
        // A ordem tem desempate por `id` porque `criadaEm` nao e unica: duas tarefas
        // criadas no mesmo instante trocariam de lugar entre duas leituras iguais, e
        // cartao que dança na tela sem nada ter mudado nao tem sintoma a investigar
        // (ACH-14).
        //
        // `tornouSeTerminalEm is null` mantem o cartao na tela em vez de escondê-lo:
        // a coluna e projetada, e projecao ausente e desconhecimento — sumir com o
        // cartao seria tratar desconhecimento como desfecho antigo.
        List<Object[]> tarefas = em.createQuery(
                        "select t, u.nome from Tarefa t "
                                + "left join br.com.idsd.kanban.internal.acesso.Usuario u "
                                + "on u.id = t.responsavelId "
                                + "where t.projetoId = :projeto "
                                + "and (t.condicao not in :terminais "
                                + "  or t.tornouSeTerminalEm is null "
                                + "  or t.tornouSeTerminalEm >= :corte) "
                                + "order by t.criadaEm asc, t.id asc",
                        Object[].class)
                .setParameter("projeto", projetoId)
                .setParameter("terminais", Condicao.terminais())
                .setParameter("corte", corte)
                .getResultList();

        Map<UUID, List<IntervaloTarefa>> abertosPorTarefa = new HashMap<>();
        for (IntervaloTarefa intervalo : em.createQuery(
                        "select i from IntervaloTarefa i where i.projetoId = :projeto "
                                + "and i.fim is null",
                        IntervaloTarefa.class)
                .setParameter("projeto", projetoId)
                .getResultList()) {
            abertosPorTarefa
                    .computeIfAbsent(intervalo.getTarefaId(), t -> new ArrayList<>())
                    .add(intervalo);
        }

        // So o motivo, e so do impedimento aberto: o desfecho encerra a dimensao 3,
        // e o unico parcial do esquema garante que nao ha dois abertos na mesma
        // tarefa. O historico de anotacoes nao pertence ao cartao.
        Map<UUID, String> motivoPorTarefa = new HashMap<>();
        for (Object[] linha : em.createQuery(
                        "select i.tarefaId, i.motivo from Impedimento i "
                                + "where i.projetoId = :projeto and i.desfecho is null",
                        Object[].class)
                .setParameter("projeto", projetoId)
                .getResultList()) {
            motivoPorTarefa.put((UUID) linha[0], (String) linha[1]);
        }

        return new BoardResposta(
                seq,
                acessoPorAdministracaoGlobal,
                grade(etapas, raias, tarefas, abertosPorTarefa, motivoPorTarefa, agora));
    }

    /**
     * A grade: cada etapa vigente com as raias que os cartoes dela exigem.
     *
     * <p>A celula vazia de etapa <b>configurada</b> e preenchida com lista vazia e
     * nunca omitida (SCN-003.2). A faixa <b>sintetica</b> segue a regra oposta e
     * unica: existe quando, e so quando, ha cartao que precise dela.
     */
    private List<BoardResposta.EtapaDoBoard> grade(
            List<Etapa> etapas,
            List<Raia> raias,
            List<Object[]> tarefas,
            Map<UUID, List<IntervaloTarefa>> abertos,
            Map<UUID, String> motivos,
            Instant agora) {

        // Um conjunto montado uma vez, contra uma varredura da lista de raias por
        // cartao: com raias e tarefas crescendo juntas, o custo anterior era o
        // produto das duas dentro da leitura mais exercitada do produto (ACH-15).
        Set<UUID> raiasVigentes = new HashSet<>();
        for (Raia raia : raias) {
            raiasVigentes.add(raia.getId());
        }
        Set<UUID> etapasVigentes = new HashSet<>();
        for (Etapa etapa : etapas) {
            etapasVigentes.add(etapa.getId());
        }

        Map<UUID, Map<UUID, List<CartaoResposta>>> cartoes = new HashMap<>();
        boolean algumSemRaia = false;
        boolean algumForaDoFluxo = false;
        for (Object[] linha : tarefas) {
            Tarefa tarefa = (Tarefa) linha[0];

            UUID raia =
                    raiasVigentes.contains(tarefa.getRaiaId()) ? tarefa.getRaiaId() : null;
            algumSemRaia = algumSemRaia || raia == null;

            UUID etapa =
                    etapasVigentes.contains(tarefa.getEtapaId()) ? tarefa.getEtapaId() : null;
            algumForaDoFluxo = algumForaDoFluxo || etapa == null;

            cartoes.computeIfAbsent(etapa, e -> new HashMap<>())
                    .computeIfAbsent(raia, r -> new ArrayList<>())
                    .add(CartaoResposta.de(
                            tarefa,
                            doEpisodioAtual(tarefa, abertos.get(tarefa.getId())),
                            impedimento(tarefa, abertos.get(tarefa.getId()), motivos, agora),
                            (String) linha[1],
                            agora));
        }

        List<BoardResposta.RaiaDoBoard> modelo = new ArrayList<>();
        for (Raia raia : raias) {
            modelo.add(new BoardResposta.RaiaDoBoard(raia.getId(), raia.getNome(), null));
        }
        // A regra "so quando ha cartao que precise" vale para a faixa sintetica que
        // convive com raias configuradas. O projeto que nao configurou <b>nenhuma</b>
        // e outro caso: ali a faixa unica nao e excecao, e a grade inteira — raia e
        // organizacao opcional (RN-023), e sem ela o board de um projeto novo nao
        // renderizaria. A suite congelada fixa os dois casos, e SCN-018.1 e o segundo.
        if (raias.isEmpty() || algumSemRaia) {
            modelo.add(new BoardResposta.RaiaDoBoard(null, SEM_RAIA, null));
        }

        List<BoardResposta.EtapaDoBoard> grade = new ArrayList<>();
        int ultimaOrdem = 0;
        for (Etapa etapa : etapas) {
            grade.add(new BoardResposta.EtapaDoBoard(
                    etapa.getId(),
                    etapa.getNome(),
                    etapa.getOrdem(),
                    etapa.isTerminal(),
                    faixas(modelo, cartoes.get(etapa.getId()))));
            ultimaOrdem = Math.max(ultimaOrdem, etapa.getOrdem());
        }
        if (algumForaDoFluxo) {
            // A etapa sintetica e a ultima da lista e nao e terminal: dela se sai por
            // movimentacao comum, e o `origem` de SDR-002 carrega a etapa real e
            // arquivada que o cartao sempre teve.
            grade.add(new BoardResposta.EtapaDoBoard(
                    null, FORA_DO_FLUXO, ultimaOrdem + 1, false, faixas(modelo, cartoes.get(null))));
        }
        return grade;
    }

    /** As faixas de uma etapa, na ordem do modelo, com a celula vazia preenchida. */
    private static List<BoardResposta.RaiaDoBoard> faixas(
            List<BoardResposta.RaiaDoBoard> modelo, Map<UUID, List<CartaoResposta>> daEtapa) {
        List<BoardResposta.RaiaDoBoard> faixas = new ArrayList<>();
        for (BoardResposta.RaiaDoBoard raia : modelo) {
            // `daEtapa` e HashMap e nao Map.of(): a faixa sintetica tem chave nula, e
            // mapa imutavel recusa consulta por chave nula com NullPointerException.
            List<CartaoResposta> daCelula = daEtapa == null ? null : daEtapa.get(raia.id());
            faixas.add(new BoardResposta.RaiaDoBoard(
                    raia.id(), raia.nome(), daCelula == null ? List.of() : daCelula));
        }
        return faixas;
    }

    /**
     * Os intervalos abertos do episodio corrente da tarefa (ACH-13).
     *
     * <p>Intervalo aberto de episodio anterior e projecao incoerente e nao historia:
     * a reabertura de RN-019 fecha o episodio, e um remanescente faria o cartao
     * exibir uma espera que comecou antes da conclusao anterior — {@code decorrido}
     * inflado por meses, num campo que ninguem confere.
     */
    private static List<IntervaloTarefa> doEpisodioAtual(
            Tarefa tarefa, List<IntervaloTarefa> abertos) {
        if (abertos == null) {
            return List.of();
        }
        List<IntervaloTarefa> doEpisodio = new ArrayList<>();
        for (IntervaloTarefa intervalo : abertos) {
            if (intervalo.getEpisodio() == tarefa.getEpisodioAtual()) {
                doEpisodio.add(intervalo);
            }
        }
        return doEpisodio;
    }

    /**
     * O bloco da terceira dimensao, conciliado <b>aqui</b> (ACH-07).
     *
     * <p>As duas fontes sao o intervalo aberto de {@code IMPEDIMENTO}, que da o
     * instante, e a linha de {@code impedimento} aberta, que da o motivo. Quem tem as
     * duas e esta classe, e e ela que decide: sem as duas nao ha bloco. Montar o
     * bloco a partir de uma so produzia {@code motivo: null} — valor que a coluna
     * proibe e que so pode significar projecao incoerente — sem log e sem erro.
     */
    private static CartaoResposta.Impedido impedimento(
            Tarefa tarefa, List<IntervaloTarefa> abertos, Map<UUID, String> motivos, Instant agora) {
        String motivo = motivos.get(tarefa.getId());
        if (motivo == null || abertos == null) {
            return null;
        }
        for (IntervaloTarefa intervalo : abertos) {
            if (intervalo.getTipo() == TipoDeIntervalo.IMPEDIMENTO
                    && intervalo.getEpisodio() == tarefa.getEpisodioAtual()) {
                return new CartaoResposta.Impedido(
                        intervalo.getInicio(),
                        Duration.between(intervalo.getInicio(), agora).toSeconds(),
                        motivo);
            }
        }
        return null;
    }

    /** Rotulo da faixa que recebe os cartoes sem raia. */
    private static final String SEM_RAIA = "Sem raia";

    /** Rotulo da coluna que recebe os cartoes em etapa arquivada (RN-021). */
    private static final String FORA_DO_FLUXO = "Fora do fluxo";
}
