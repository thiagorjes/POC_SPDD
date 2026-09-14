package br.com.idsd.kanban.internal.tarefa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Estado corrente da tarefa — anel de projecao.
 *
 * <p>Nada aqui e verdade por si. A linha e derivada de {@code evento_tarefa} e
 * reconstruivel do zero (SDR-001, {@code data-model.md} secao 9); e isso que
 * permite corrigir a projecao sem negociar com o historico.
 *
 * <p><b>Nao ha campo de impedimento, e nao havera.</b> As tres dimensoes de
 * RN-002 sao ortogonais, e a terceira e derivada da existencia de
 * {@link Impedimento} com desfecho nulo. Um campo aqui devolveria a RN-032 a
 * condicao de disciplina de quem escreve o servico — que e exatamente o
 * defeito que INC-01 localizou, quando {@code IMPEDIDA} era valor de
 * {@link Condicao} e toda movimentacao o apagava em silencio.
 *
 * <p><b>A forma e JavaBean — construtor publico sem argumentos e acessores de
 * escrita — e a escolha nao e de quem implementa.</b> A suite congelada monta
 * {@code new Tarefa()} e atribui campo a campo em {@code TomadaServiceTest},
 * e a suite esta fora do alcance desta etapa. Diverge de {@code Etapa} e
 * {@code Raia}, que nascem por construtor e so mudam por metodo de dominio; a
 * divergencia esta registrada no historico da task.
 */
@Entity
@Table(name = "tarefa")
public class Tarefa {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "projeto_id", nullable = false)
    private UUID projetoId;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descricao")
    private String descricao;

    /** Dimensao 1 de RN-002. */
    @Column(name = "etapa_id", nullable = false)
    private UUID etapaId;

    /** Agrupamento visual do cartao (RN-023). A serie de tempo nao o carrega. */
    @Column(name = "raia_id")
    private UUID raiaId;

    /**
     * Dimensao 2 de RN-002. Persistida por nome, e nao por ordinal: a restricao
     * de verificacao da coluna fala em texto, e reordenar o enum com ordinal
     * gravado reescreveria o significado de toda linha ja gravada.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "condicao", nullable = false)
    private Condicao condicao;

    /** Nulo quando aguardando tomada (RN-006). */
    @Column(name = "responsavel_id")
    private UUID responsavelId;

    @Column(name = "assumida_em")
    private Instant assumidaEm;

    /** 1 na criacao; incrementa a cada reabertura (RN-019, RN-034). */
    @Column(name = "episodio_atual", nullable = false)
    private int episodioAtual = 1;

    /**
     * Bloqueio otimista de SDR-002. E o que sustenta a origem declarada: duas
     * escritas concorrentes sobre a mesma tarefa nao se sobrepoem em silencio.
     */
    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProjetoId() {
        return projetoId;
    }

    public void setProjetoId(UUID projetoId) {
        this.projetoId = projetoId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public UUID getEtapaId() {
        return etapaId;
    }

    public void setEtapaId(UUID etapaId) {
        this.etapaId = etapaId;
    }

    public UUID getRaiaId() {
        return raiaId;
    }

    public void setRaiaId(UUID raiaId) {
        this.raiaId = raiaId;
    }

    public Condicao getCondicao() {
        return condicao;
    }

    public void setCondicao(Condicao condicao) {
        this.condicao = condicao;
    }

    public UUID getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(UUID responsavelId) {
        this.responsavelId = responsavelId;
    }

    public Instant getAssumidaEm() {
        return assumidaEm;
    }

    public void setAssumidaEm(Instant assumidaEm) {
        this.assumidaEm = assumidaEm;
    }

    public int getEpisodioAtual() {
        return episodioAtual;
    }

    public void setEpisodioAtual(int episodioAtual) {
        this.episodioAtual = episodioAtual;
    }

    public Long getVersao() {
        return versao;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(Instant criadaEm) {
        this.criadaEm = criadaEm;
    }
}
