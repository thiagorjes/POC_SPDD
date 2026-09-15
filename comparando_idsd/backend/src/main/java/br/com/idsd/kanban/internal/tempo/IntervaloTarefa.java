package br.com.idsd.kanban.internal.tempo;

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
     * A serie a que este intervalo pertence.
     *
     * <p>Persistido por nome, como {@code EventoTarefa.tipo} e
     * {@code Tarefa.condicao}: a coluna e texto e o ordinal gravado tornaria a
     * reordenacao do enum uma reescrita silenciosa de toda linha ja gravada.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoDeIntervalo tipo;

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

    /**
     * Abre um intervalo. O unico jeito de criar um.
     *
     * <p>Nao ha acessor de escrita para {@code inicio}: recuar o comeco de uma
     * contagem ja aberta reescreveria tempo, e tempo gravado e tao imutavel
     * quanto o evento que o produziu (RNF-008). O unico campo que muda depois e
     * o fim, por {@link #fechar(Instant)}.
     */
    public IntervaloTarefa(
            UUID tarefaId, UUID projetoId, UUID etapaId, TipoDeIntervalo tipo,
            int episodio, Instant inicio) {
        this.tarefaId = tarefaId;
        this.projetoId = projetoId;
        this.etapaId = etapaId;
        this.tipo = tipo;
        this.episodio = episodio;
        this.inicio = inicio;
    }

    /**
     * Encerra a contagem.
     *
     * <p>Fechar duas vezes e recusado em vez de ignorado. O segundo fechamento
     * so chega aqui por defeito de quem decide o efeito do evento — o catalogo
     * de {@link AplicadorDeIntervalos} manda fechar o que ja estava fechado —, e
     * o dano e mover o {@code fim} de um intervalo ja contado, que a leitura de
     * RF-016 ja pode ter agregado. Silenciar tornaria esse defeito invisivel.
     */
    public void fechar(Instant fim) {
        if (this.fim != null) {
            throw new IllegalStateException("intervalo ja fechado: " + id);
        }
        this.fim = fim;
    }

    /**
     * Reescreve a linha a partir do log. <b>Exclusivo da reconstrucao.</b>
     *
     * <p>Visivel ao pacote e nao publico, e nao existe acessor de escrita avulso
     * para nenhum destes campos: fora da rotina de reconstrucao, mover o inicio
     * ou o fim de um intervalo e reescrever tempo ja contado, que RNF-008 protege.
     * Aqui e legitimo pela razao oposta — o que esta sendo escrito <b>e</b> o que
     * o log diz, e a linha anterior e que era a suspeita.
     *
     * <p>A alternativa seria apagar tudo e inserir de novo, e ela nao esta
     * disponivel: {@code impedimento.intervalo_id} e chave estrangeira para esta
     * tabela, de modo que a linha referenciada nao pode ser removida sem que a
     * identidade do impedimento — que o log nao carrega — se perca junto.
     */
    void reescrever(UUID etapaId, int episodio, Instant inicio, Instant fim) {
        this.etapaId = etapaId;
        this.episodio = episodio;
        this.inicio = inicio;
        this.fim = fim;
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

    public TipoDeIntervalo getTipo() {
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
