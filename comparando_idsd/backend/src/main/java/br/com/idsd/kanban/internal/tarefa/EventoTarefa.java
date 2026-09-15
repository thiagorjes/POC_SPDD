package br.com.idsd.kanban.internal.tarefa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Anel de verdade: um evento gravado do ciclo de vida da tarefa.
 *
 * <p><b>Somente insercao.</b> A entidade nao publica acessor de escrita algum:
 * um evento se cria e se le, nunca se corrige. A garantia foi desenhada tripla —
 * nenhum campo e mutavel daqui, {@link EventoTarefaRepositorio} nao expoe
 * atualizacao nem remocao, e o grupo {@code aplicacao_kanban} recebe apenas
 * {@code SELECT, INSERT} nesta tabela (RNF-008). <b>A terceira perna esta
 * escrita e nao esta em vigor:</b> a aplicacao conecta como dono do schema e
 * superusuario, contra quem a revogacao e inerte — ver
 * {@link EventoTarefaRepositorio}, ACH-01 da revisao de TASK-02.3 e a pendencia
 * 21. Valem hoje as duas primeiras.
 *
 * <p>O construtor de criacao entrou nesta task, que e a do primeiro escritor
 * (ACH-13 da revisao de TASK-02.1: assinatura nasce com o consumidor). Ele nao
 * tem doze argumentos: recebe {@link Novo}, que e o que o servico decide, mais
 * os dois valores que sao do registrador e de mais ninguem — a sequencia obtida
 * do banco e o instante. Quem chama nao consegue inventar nenhum dos dois.
 *
 * <p>Nao ha associacao JPA para {@code Tarefa}. O log sobrevive a projecao, que
 * e descartavel e refeita do zero pela rotina de reconstrucao; uma associacao
 * inverteria a dependencia entre os dois aneis e faria o carregamento do
 * historico navegar para a tabela que ele existe para poder reconstruir.
 */
@Entity
@Table(name = "evento_tarefa")
public class EventoTarefa {

    /**
     * Ordem total de gravacao, atribuida pelo banco. E a ordem em que a rotina
     * de reconstrucao releh o log.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tarefa_id", nullable = false)
    private UUID tarefaId;

    /** Desnormalizado: todo filtro de consulta parte do projeto. */
    @Column(name = "projeto_id", nullable = false)
    private UUID projetoId;

    /**
     * Catalogo fechado de {@code data-model.md} secao 4, gravado como texto.
     *
     * <p>Persistido por nome e nao por ordinal: reordenar {@link TipoDeEvento}
     * com ordinal gravado reescreveria o significado de todo evento ja gravado,
     * e o log e a unica coisa do sistema que nao se corrige.
     *
     * <p><b>A coluna continua sem restricao de verificacao no banco</b>, e isso
     * e escolha: o catalogo cresce a cada epico, e amarra-lo ao esquema custaria
     * uma migration sobre a tabela mais sensivel do sistema a cada tipo novo.
     * O caminho de escrita e um so — o registrador —, e ele so aceita o enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoDeEvento tipo;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    /**
     * Quem agiu. Nulo em evento decorrente de configuracao — por exemplo a
     * devolucao ao pool por remocao de participacao (RN-027).
     *
     * <p>E o unico lugar do sistema onde a pessoa se liga a um instante, e isso
     * e deliberado: RN-014 proibe agregar <b>tempo</b> por pessoa, nao guardar
     * quem fez o que. Confundir as duas coisas apagaria o historico que
     * SCN-019.3 exige.
     */
    @Column(name = "ator_id")
    private UUID atorId;

    /** 1 na criacao; incrementa a cada reabertura (RN-019). */
    @Column(name = "episodio", nullable = false)
    private int episodio;

    @Column(name = "etapa_origem_id")
    private UUID etapaOrigemId;

    @Column(name = "etapa_destino_id")
    private UUID etapaDestinoId;

    @Column(name = "condicao_origem")
    private String condicaoOrigem;

    @Column(name = "condicao_destino")
    private String condicaoDestino;

    /**
     * Sequencia por projeto (SDR-004), atribuida no banco dentro da transacao
     * de escrita e unica por {@code (projeto_id, seq)}. E o que permite ao
     * cliente detectar lacuna e resincronizar (ADR-004).
     */
    @Column(name = "seq", nullable = false)
    private long seq;

    /**
     * Motivo do impedimento, desfecho, titulo na criacao. <b>Nunca dado de
     * cliente</b> (IDSD 4.10.1).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados")
    private String dados;

    protected EventoTarefa() {
        // exigido pelo JPA
    }

    /**
     * Grava um evento novo.
     *
     * <p>{@code seq} e {@code ocorridoEm} nao entram em {@link Novo} de
     * proposito: os dois sao do registrador. A sequencia vem do banco, dentro da
     * transacao (SDR-004), e deixar o servico informa-la abriria a porta ao
     * contador em memoria que SDR-004 existe para fechar. O instante e o do
     * registro, e um unico relogio para todos os eventos da mesma escrita e o
     * que faz intervalo fechado e intervalo aberto na mesma transicao casarem
     * no mesmo ponto — dois {@code Instant.now()} deixariam um vao.
     */
    public EventoTarefa(Novo novo, long seq, Instant ocorridoEm) {
        this.tarefaId = novo.tarefaId();
        this.projetoId = novo.projetoId();
        this.tipo = novo.tipo();
        this.atorId = novo.atorId();
        this.episodio = novo.episodio();
        this.etapaOrigemId = novo.etapaOrigemId();
        this.etapaDestinoId = novo.etapaDestinoId();
        this.condicaoOrigem = novo.condicaoOrigem() == null ? null : novo.condicaoOrigem().name();
        this.condicaoDestino = novo.condicaoDestino() == null ? null : novo.condicaoDestino().name();
        this.dados = novo.dados();
        this.seq = seq;
        this.ocorridoEm = ocorridoEm;
    }

    /**
     * O que o servico de dominio decide sobre um evento, antes de ele existir.
     *
     * <p>Record e nao a entidade porque o servico precisa descrever o evento sem
     * conseguir grava-lo: a gravacao passa pelo registrador, que e quem toma a
     * sequencia e agenda a publicacao. Passar a entidade pronta permitiria a
     * qualquer servico persistir por conta propria e sair do caminho unico.
     *
     * @param dados documento JSON com o que nao cabe em coluna — motivo,
     *     desfecho, titulo. <b>Nunca dado de cliente</b> (IDSD 4.10.1).
     */
    public record Novo(
            UUID tarefaId,
            UUID projetoId,
            TipoDeEvento tipo,
            UUID atorId,
            int episodio,
            UUID etapaOrigemId,
            UUID etapaDestinoId,
            Condicao condicaoOrigem,
            Condicao condicaoDestino,
            String dados) {

        /**
         * O caso comum: evento que nao muda etapa nem condicao e nao carrega
         * documento — tomada, devolucao, anotacao de impedimento.
         *
         * <p>Tudo o que ele preenche sai da propria tarefa, e essa e a razao de
         * existir: {@code tarefaId}, {@code projetoId} e {@code episodio}
         * copiados a mao em cada chamada sao tres oportunidades de o evento
         * discordar do estado que o originou.
         */
        public static Novo de(Tarefa tarefa, TipoDeEvento tipo, UUID atorId) {
            return new Novo(
                    tarefa.getId(),
                    tarefa.getProjetoId(),
                    tipo,
                    atorId,
                    tarefa.getEpisodioAtual(),
                    null,
                    null,
                    tarefa.getCondicao(),
                    tarefa.getCondicao(),
                    null);
        }
    }

    public Long getId() {
        return id;
    }

    public UUID getTarefaId() {
        return tarefaId;
    }

    public UUID getProjetoId() {
        return projetoId;
    }

    public TipoDeEvento getTipo() {
        return tipo;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    public UUID getAtorId() {
        return atorId;
    }

    public int getEpisodio() {
        return episodio;
    }

    public UUID getEtapaOrigemId() {
        return etapaOrigemId;
    }

    public UUID getEtapaDestinoId() {
        return etapaDestinoId;
    }

    public String getCondicaoOrigem() {
        return condicaoOrigem;
    }

    public String getCondicaoDestino() {
        return condicaoDestino;
    }

    public long getSeq() {
        return seq;
    }

    public String getDados() {
        return dados;
    }
}
