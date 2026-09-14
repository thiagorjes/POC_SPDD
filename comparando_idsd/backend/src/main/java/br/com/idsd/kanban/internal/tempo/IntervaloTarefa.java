package br.com.idsd.kanban.internal.tempo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Um intervalo de uma das tres series de tempo da tarefa.
 *
 * <p>As series sao {@code PERMANENCIA}, {@code ESPERA_TOMADA} e
 * {@code IMPEDIMENTO}, e correm em paralelo: um mesmo instante pode estar
 * coberto pelas tres da mesma tarefa (SCN-003.3, SCN-016.1). Nao ha restricao
 * de nao sobreposicao <b>entre</b> tipos; ha, sim, no maximo um intervalo aberto
 * <b>por</b> tipo por tarefa, por indice unico parcial no banco.
 *
 * <p><b>Nao ha campo de pessoa, e nao havera</b> (RN-014). A ausencia e o que
 * torna estrutural a proibicao de agregar tempo por pessoa: sem coluna, nao ha
 * o que agrupar. Acrescenta-la nao deixaria nenhum teste de cenario vermelho, e
 * e por isso que a regra e a mais exposta a erosao silenciosa do sistema.
 *
 * <p><b>Nao ha campo de total</b> (RN-008). As tres series nunca se somam entre
 * si, e um campo de soma seria o convite a faze-lo.
 *
 * <p>A coluna gerada {@code duracao} <b>nao</b> e mapeada. Ela e calculada pelo
 * banco a partir de {@code (fim - inicio)} e so tem consumidor em RF-016, no
 * EPIC-07, que a le por consulta agregada e nao por navegacao de entidade;
 * mapea-la agora seria campo sem leitor, e ainda submeteria a validacao de
 * schema a conversao de {@code interval} para tipo Java, que nao tem
 * correspondencia estavel. {@code ddl-auto=validate} nao se incomoda com coluna
 * presente no banco e ausente do mapeamento.
 */
@Entity
@Table(name = "intervalo_tarefa")
public class IntervaloTarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tarefa_id", nullable = false)
    private UUID tarefaId;

    @Column(name = "projeto_id", nullable = false)
    private UUID projetoId;

    /**
     * Etapa vigente na <b>abertura</b> do intervalo.
     *
     * <p>Em intervalo de impedimento e instantaneo, e nao vinculo: sem ele, o
     * bloco de impedimento por etapa que RF-016 devolve cairia num unico grupo
     * nulo. Isso nao fere RN-009 — a regra proibe que a movimentacao encerre ou
     * reinicie a contagem do impedimento, e nao exige que o impedimento seja
     * anonimo quanto a etapa. O intervalo atravessa a movimentacao sem ser
     * tocado.
     */
    @Column(name = "etapa_id", nullable = false)
    private UUID etapaId;

    /**
     * {@code PERMANENCIA}, {@code ESPERA_TOMADA} ou {@code IMPEDIMENTO}.
     *
     * <p>Texto, e nao enumeracao em codigo, pela mesma razao de
     * {@code EventoTarefa.tipo}: o primeiro escritor nasce em EPIC-03, e e com
     * ele que o tipo fechado entra. Nenhum caminho grava intervalo ainda.
     */
    @Column(name = "tipo", nullable = false)
    private String tipo;

    /** RN-019: a serie e por episodio, e a reabertura comeca outro. */
    @Column(name = "episodio", nullable = false)
    private int episodio;

    @Column(name = "inicio", nullable = false)
    private Instant inicio;

    /** Nulo enquanto em curso. */
    @Column(name = "fim")
    private Instant fim;

    protected IntervaloTarefa() {
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

    public UUID getEtapaId() {
        return etapaId;
    }

    public String getTipo() {
        return tipo;
    }

    public int getEpisodio() {
        return episodio;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFim() {
        return fim;
    }
}
