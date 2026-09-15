package br.com.idsd.kanban.internal.tempo;

import br.com.idsd.kanban.internal.tarefa.EventoTarefa;
import br.com.idsd.kanban.internal.tarefa.EventoTarefaRepositorio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Refaz a projecao de tempo a partir do log ({@code data-model.md} secao 9).
 *
 * <p>A alegacao de SDR-001 — o log e a verdade, a projecao e leitura descartavel
 * — so vale se houver como refazer a projecao. Se ela nao puder ser
 * reconstruida, ela <b>e</b> a verdade na pratica e a decisao de arquitetura e
 * ficcao. Esta rotina e o que torna a alegacao verificavel.
 *
 * <p><b>Operacao administrativa, sem rota.</b> Nao ha superficie HTTP: quem a
 * executa tem acesso ao processo. Uma rota exigiria decidir quem pode disparar
 * uma varredura que bloqueia as escritas do projeto, e nenhum requisito pede
 * isso.
 *
 * <p><b>O escopo desta rotina e {@code intervalo_tarefa}, e so.</b> A TechSpec
 * secao 9 diz "reescreve {@code tarefa}, {@code intervalo_tarefa} e
 * {@code impedimento} do zero", e isso <b>nao e alcancavel com o log como ele
 * esta especificado hoje</b>: {@code tarefa.raia_id} nao aparece em coluna nem
 * em documento de evento nenhum do catalogo da secao 4; {@code impedimento.id} e
 * um {@code uuid} que evento algum carrega, de modo que recriar a linha trocaria
 * o identificador que o contrato ja devolveu ao cliente; e {@code titulo} e
 * {@code descricao} vivem em {@code evento_tarefa.dados}, cujo formato nenhum
 * escritor fixou ainda. Reconstruir esses tres campos exigiria inventar o que a
 * spec nao decidiu, e a etapa que decide nao e esta. Achado registrado no
 * historico da task, destino {@code /techspec}.
 */
@Service
public class ReconstrutorDeProjecao {

    private static final Logger LOG = LoggerFactory.getLogger(ReconstrutorDeProjecao.class);

    /** Eventos por ida ao banco na reexecucao do log (ACH-14). */
    private static final int LOTE_DE_LEITURA = 500;

    private final EventoTarefaRepositorio eventos;
    private final IntervaloTarefaRepositorio intervalos;
    private final TransactionTemplate transacoes;

    @PersistenceContext
    private EntityManager em;

    public ReconstrutorDeProjecao(
            EventoTarefaRepositorio eventos,
            IntervaloTarefaRepositorio intervalos,
            TransactionTemplate transacoes) {
        this.eventos = eventos;
        this.intervalos = intervalos;
        this.transacoes = transacoes;
    }

    /**
     * A chave do bloqueio consultivo de um projeto.
     *
     * <p>Publica porque {@code RegistradorDeEvento} precisa da <b>mesma</b>
     * chave: bloqueio consultivo nao protege recurso nenhum por si — ele so
     * relaciona quem calcula o mesmo numero. Duas derivacoes, ainda que ambas
     * corretas, produziriam dois bloqueios que se ignoram, e a janela sem escrita
     * deixaria de existir sem que nada falhasse.
     *
     * <p>Os 64 bits que {@code pg_advisory_xact_lock} aceita saem de um resumo
     * SHA-256 dos 16 bytes do UUID, e <b>nao</b> do {@code xor} das duas metades
     * (ACH-16). Colisao continua possivel — 64 bits nao comportam 128 —, e o
     * efeito de uma colisao acidental segue benigno: um projeto espera a
     * reconstrucao de outro. O que muda e a colisao <b>construida</b>: com o
     * {@code xor}, casar a chave de um projeto alheio era aritmetica de um passo
     * para quem tivesse qualquer influencia sobre o identificador gerado, e o
     * efeito, somado ao bloqueio dos escritores, alcancaria projeto que o autor
     * nao acessa. Resumo criptografico nao se inverte assim.
     */
    public static long chaveDeBloqueio(UUID projetoId) {
        var bytes = ByteBuffer.allocate(16)
                .putLong(projetoId.getMostSignificantBits())
                .putLong(projetoId.getLeastSignificantBits())
                .array();
        try {
            return ByteBuffer.wrap(MessageDigest.getInstance("SHA-256").digest(bytes)).getLong();
        } catch (NoSuchAlgorithmException impossivel) {
            throw new IllegalStateException("SHA-256 ausente da plataforma", impossivel);
        }
    }

    /**
     * Reconstroi todos os projetos que tem log, um por transacao.
     *
     * <p>Uma transacao por projeto e nao uma para tudo: a janela sem escrita dura
     * o tempo da transacao que segura o bloqueio, e uma transacao unica
     * bloquearia o sistema inteiro pelo tempo do maior projeto.
     */
    public void reconstruirTudo() {
        exigirAutorizacaoAdministrativa();
        for (UUID projetoId : eventos.projetosComLog()) {
            reconstruir(projetoId);
        }
    }

    /**
     * Reconstroi um projeto.
     *
     * <p><b>Toma o bloqueio consultivo primeiro</b>, e esperando: e a rotina que
     * espera, nunca a escrita — quem escreve durante a janela recebe {@code 409}
     * de {@code RegistradorDeEvento}. Reconstruir concorrentemente com escritas
     * novas produz projecao divergente do log, que e o defeito exato que esta
     * rotina existe para curar, e por isso o bloqueio nao e opcional.
     *
     * <p>{@code pg_advisory_xact_lock} e nao {@code pg_advisory_lock}: o sufixo
     * {@code xact} solta o bloqueio no fim da transacao, aconteca o que
     * acontecer. A versao de sessao sobrevive a excecao e deixaria o projeto
     * permanentemente irreescrevivel ate a conexao ser devolvida ao pool — e
     * devolvida ainda bloqueada.
     *
     * <p><b>E idempotente</b>: reconstruir duas vezes produz o mesmo resultado,
     * porque o resultado e funcao do log e de mais nada.
     */
    public void reconstruir(UUID projetoId) {
        exigirAutorizacaoAdministrativa();
        transacoes.executeWithoutResult(status -> reconstruirNaTransacao(projetoId));
    }

    /**
     * O gate de ACH-17.
     *
     * <p>Ausencia de autenticacao <b>autoriza</b>, e a inversao e deliberada: a
     * rotina nasceu como operacao de processo e continua sendo uma: job agendado,
     * console administrativo, arnes de teste. Nenhum deles tem principal. O que o
     * gate impede e o caso oposto e perigoso — chegar aqui <b>por dentro de uma
     * requisicao autenticada qualquer</b>, que e exatamente o que aconteceria no
     * dia em que a rotina ganhasse rota sem ninguem decidir quem pode disparar
     * uma varredura que bloqueia as escritas do projeto.
     */
    private void exigirAutorizacaoAdministrativa() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            return;
        }
        boolean admin = autenticacao.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN_GLOBAL".equals(a.getAuthority()));
        if (!admin) {
            throw new AccessDeniedException(
                    "reconstrucao da projecao e operacao administrativa");
        }
    }

    /**
     * O corpo, sempre dentro de uma transacao propria.
     *
     * <p>{@link TransactionTemplate} e nao {@code @Transactional}, e a diferenca
     * nao e de estilo: {@link #reconstruirTudo()} chama {@link #reconstruir} pelo
     * {@code this}, que nao passa pelo proxy — a anotacao seria ignorada, e o
     * bloqueio consultivo {@code xact} soltaria no fim da consulta que o toma em
     * vez de no fim da rotina. A janela sem escrita deixaria de existir sem que
     * nada falhasse, que e a forma mais cara de este defeito aparecer.
     */
    private void reconstruirNaTransacao(UUID projetoId) {
        em.createNativeQuery("SELECT pg_advisory_xact_lock(:chave)")
                .setParameter("chave", chaveDeBloqueio(projetoId))
                .getSingleResult();

        Map<Chave, List<Linha>> desejado = reexecutarOLog(projetoId);
        Map<Chave, List<IntervaloTarefa>> existente = agrupar(
                intervalos.findByProjetoIdOrderByIdAsc(projetoId));

        // ACH-08: o pareamento por posicao dentro de (tarefa_id, tipo) nao
        // pressupoe que a linha gravada esteja correta — pressupoe apenas que
        // exista. Todo campo mutavel e sobrescrito por reescrever(), e o que
        // sobra e apagado, de modo que projecao corrompida sai desta rotina igual
        // a projecao ausente. O que o pareamento preserva de proposito e o
        // identificador, porque impedimento.intervalo_id o referencia e o log nao
        // o carrega — preserva-lo e o motivo de a rotina nao ser apaga-e-insere.
        var excedente = new ArrayList<Long>();
        for (var grupo : existente.entrySet()) {
            List<Linha> linhas = desejado.getOrDefault(grupo.getKey(), List.of());
            List<IntervaloTarefa> atuais = grupo.getValue();
            for (int i = 0; i < atuais.size(); i++) {
                if (i < linhas.size()) {
                    Linha linha = linhas.get(i);
                    atuais.get(i).reescrever(
                            linha.etapaId(), linha.episodio(), linha.inicio(), linha.fim());
                    intervalos.save(atuais.get(i));
                } else {
                    excedente.add(atuais.get(i).getId());
                }
            }
        }
        if (!excedente.isEmpty()) {
            intervalos.apagarPorIdEmLotes(excedente);
        }

        // As faltantes entram depois das remocoes, senao o indice unico parcial
        // sobre (tarefa_id, tipo) com fim IS NULL recusaria a insercao de uma
        // serie em curso enquanto a linha excedente da mesma serie ainda existe.
        em.flush();
        int inseridos = 0;
        for (var grupo : desejado.entrySet()) {
            List<IntervaloTarefa> atuais = existente.getOrDefault(grupo.getKey(), List.of());
            for (int i = atuais.size(); i < grupo.getValue().size(); i++) {
                Linha linha = grupo.getValue().get(i);
                intervalos.save(new IntervaloTarefa(
                        grupo.getKey().tarefaId(), projetoId, linha.etapaId(),
                        grupo.getKey().tipo(), linha.episodio(), linha.inicio()));
                inseridos++;
            }
        }

        LOG.info(
                "Projecao de tempo reconstruida: projeto={} inseridos={} removidos={}",
                projetoId, inseridos, excedente.size());
    }

    /**
     * Reexecuta o log e devolve os intervalos que ele determina.
     *
     * <p>Em ordem de {@code id} — a ordem total de gravacao, e nao a de
     * {@code ocorrido_em}, que e do relogio da instancia que gravou. O catalogo
     * consultado e o mesmo de {@link AplicadorDeIntervalos}: se fosse outro, a
     * projecao reconstruida divergiria da gravada por diferenca de tabela, e a
     * divergencia apareceria como defeito desta rotina.
     *
     * <p><b>Em lotes, com o contexto limpo a cada um</b> (ACH-14). O log nao tem
     * teto, e carrega-lo inteiro punha duas copias de cada evento na heap — a
     * linha e a entidade gerenciada — dentro da transacao que segura o bloqueio.
     * O {@code clear} e seguro aqui e so aqui: nada do que esta fase produz e
     * entidade gerenciada, so {@link Linha}, e os intervalos existentes ainda nao
     * foram carregados. Limpar depois deles desanexaria justamente as linhas que
     * a rotina vai reescrever.
     *
     * <p>O que resta na memoria e o mapa de {@link Linha}, proporcional aos
     * intervalos que o log determina — isto e, as tarefas e episodios do projeto —
     * e nao ao numero de eventos. Cresce, e muito mais devagar: e o log que nao
     * tem poda, porque poda-lo seria negar SDR-001.
     */
    private Map<Chave, List<Linha>> reexecutarOLog(UUID projetoId) {
        var resultado = new LinkedHashMap<Chave, List<Linha>>();
        var abertos = new HashMap<UUID, Map<TipoDeIntervalo, Linha>>();
        var etapaCorrente = new HashMap<UUID, UUID>();

        long ultimoLido = 0L;
        List<EventoTarefa> lote;
        while (!(lote = eventos.findByProjetoIdAndIdGreaterThanOrderByIdAsc(
                projetoId, ultimoLido, PageRequest.of(0, LOTE_DE_LEITURA))).isEmpty()) {
        for (EventoTarefa evento : lote) {
            UUID tarefaId = evento.getTarefaId();
            Instant quando = evento.getOcorridoEm();
            if (evento.getEtapaDestinoId() != null) {
                etapaCorrente.put(tarefaId, evento.getEtapaDestinoId());
            }
            var seriesDaTarefa = abertos.computeIfAbsent(tarefaId, id -> new HashMap<>());
            var efeito = AplicadorDeIntervalos.efeitoDe(evento.getTipo());

            for (TipoDeIntervalo tipo : List.copyOf(seriesDaTarefa.keySet())) {
                if (efeito.fechaTodos() || efeito.fecha().contains(tipo)) {
                    seriesDaTarefa.remove(tipo).fechar(quando);
                }
            }
            for (TipoDeIntervalo tipo : efeito.abre()) {
                var linha = new Linha(etapaCorrente.get(tarefaId), evento.getEpisodio(), quando);
                seriesDaTarefa.put(tipo, linha);
                resultado.computeIfAbsent(new Chave(tarefaId, tipo), c -> new ArrayList<>())
                        .add(linha);
            }
        }
            ultimoLido = lote.get(lote.size() - 1).getId();
            em.clear();
        }
        return resultado;
    }

    private Map<Chave, List<IntervaloTarefa>> agrupar(List<IntervaloTarefa> linhas) {
        var mapa = new LinkedHashMap<Chave, List<IntervaloTarefa>>();
        for (IntervaloTarefa linha : linhas) {
            mapa.computeIfAbsent(new Chave(linha.getTarefaId(), linha.getTipo()), c -> new ArrayList<>())
                    .add(linha);
        }
        return mapa;
    }

    /**
     * O par que casa linha gravada com linha reexecutada.
     *
     * <p>O episodio fica <b>fora</b> da chave de proposito: ele e um dos campos
     * que a reconstrucao corrige, e um episodio errado na linha gravada faria o
     * par nao casar e a rotina trocar correcao por remocao e insercao — perdendo
     * o identificador que {@code impedimento.intervalo_id} referencia. Dentro de
     * uma mesma tarefa e serie, a ordem de insercao ja e a ordem cronologica.
     */
    private record Chave(UUID tarefaId, TipoDeIntervalo tipo) {
    }

    /** Um intervalo como o log o determina, antes de virar linha. */
    private static final class Linha {
        private final UUID etapaId;
        private final int episodio;
        private final Instant inicio;
        private Instant fim;

        Linha(UUID etapaId, int episodio, Instant inicio) {
            this.etapaId = etapaId;
            this.episodio = episodio;
            this.inicio = inicio;
        }

        void fechar(Instant quando) {
            this.fim = quando;
        }

        UUID etapaId() {
            return etapaId;
        }

        int episodio() {
            return episodio;
        }

        Instant inicio() {
            return inicio;
        }

        Instant fim() {
            return fim;
        }
    }
}
