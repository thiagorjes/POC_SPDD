package br.com.idsd.kanban.internal.tarefa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * um evento se cria e se le, nunca se corrige. A garantia e tripla —
 * nenhum campo e mutavel daqui, {@link EventoTarefaRepositorio} nao expoe
 * atualizacao nem remocao, e a role de aplicacao recebe apenas
 * {@code SELECT, INSERT} nesta tabela (RNF-008).
 *
 * <p>O construtor de criacao ainda nao existe, e a ausencia segue a regra que
 * ACH-13 da revisao de TASK-02.1 instituiu: assinatura nasce com o consumidor.
 * O primeiro evento gravado e o de EPIC-03, e e la que ele entra — com os
 * campos que aquela escrita de fato preenche, em vez de um construtor de doze
 * argumentos desenhado no escuro.
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
     * <p>Nao ha enumeracao em codigo ainda, e nem a coluna tem restricao de
     * verificacao. As duas ausencias sao a mesma decisao: o catalogo cresce a
     * cada epico, e antecipa-lo aqui criaria valores sem nenhum caminho que os
     * grave — e uma migration sobre a tabela mais sensivel do sistema a cada
     * tipo novo. O enum nasce com a primeira escrita, em EPIC-03.
     */
    @Column(name = "tipo", nullable = false)
    private String tipo;

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

    public Long getId() {
        return id;
    }

    public UUID getTarefaId() {
        return tarefaId;
    }

    public UUID getProjetoId() {
        return projetoId;
    }

    public String getTipo() {
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
