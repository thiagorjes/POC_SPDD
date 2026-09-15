package br.com.idsd.kanban.internal.tarefa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Dimensao 3 de RN-002, e a unica fonte dela.
 *
 * <p>A marca de impedimento da tarefa <b>e</b> a existencia de uma linha destas
 * com {@link #getDesfecho()} nulo. Nao ha copia em {@code tarefa}, e por isso
 * nenhuma escrita sobre a tarefa a apaga: mover, devolver, assumir e renomear
 * preservam a marca (RN-032) sem que ninguem precise lembrar disso. So o
 * registro do desfecho a apaga (RN-010, RF-010).
 *
 * <p>No maximo um impedimento aberto por tarefa, garantido por indice unico
 * parcial em {@code (tarefa_id)} com {@code desfecho IS NULL} — no banco, e nao
 * so na regra de servico. A segunda sinalizacao anexa em {@link #getAnotacoes()}
 * sem criar impedimento novo nem reiniciar contagem (SCN-009.3).
 *
 * <p>Como {@link Tarefa}, a forma e JavaBean por exigencia da suite congelada:
 * {@code ImpedimentoServiceTest} monta {@code new Impedimento()} e atribui
 * campo a campo.
 */
@Entity
@Table(name = "impedimento")
public class Impedimento {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tarefa_id", nullable = false)
    private UUID tarefaId;

    /** Desnormalizado: RF-014 filtra por conjunto de projetos e RF-015 por um. */
    @Column(name = "projeto_id", nullable = false)
    private UUID projetoId;

    /**
     * O intervalo de tipo {@code IMPEDIMENTO} que esta linha abriu. O desfecho
     * precisa saber qual intervalo fechar, e descobri-lo por consulta deixaria
     * a escolha a cargo de quem escreve a consulta.
     */
    @Column(name = "intervalo_id", nullable = false)
    private Long intervaloId;

    /** Obrigatorio (RF-009, SCN-009.2). */
    @Column(name = "motivo", nullable = false)
    private String motivo;

    /**
     * Sinalizacoes posteriores a primeira (RN-010). Documento JSON, e nao
     * tabela filha, porque nao ha leitura que as consulte separadamente: elas
     * saem sempre junto do impedimento a que pertencem.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "anotacoes", nullable = false)
    private String anotacoes = "[]";

    /** Habilita SCN-010.2: quem sinalizou pode resolver. */
    @Column(name = "aberto_por", nullable = false)
    private UUID abertoPor;

    /** Nulo enquanto aberto. E o campo que a dimensao 3 le. */
    @Column(name = "desfecho")
    private String desfecho;

    @Column(name = "resolvido_por")
    private UUID resolvidoPor;

    /**
     * Instante do desfecho.
     *
     * <p>Nao consta da tabela de campos de TASK-02.3 nem de
     * {@code data-model.md} secao 5, e entrou porque a suite congelada o exige:
     * {@code ImpedimentoServiceTest} le e escreve {@code resolvidoEm} para
     * verificar que a resolucao repetida de SCN-010.3 nao reescreve o instante
     * ja registrado. Divergencia registrada no historico da task.
     */
    @Column(name = "resolvido_em")
    private Instant resolvidoEm;

    /**
     * Bloqueio otimista de SDR-002, como em {@link Tarefa}.
     *
     * <p>Existe por causa de {@link #getAnotacoes()}. A segunda sinalizacao de
     * RN-010 nao insere linha: ela le o array, anexa um elemento e regrava o
     * documento inteiro (SCN-009.3). Sem versao, duas sinalizacoes concorrentes
     * sobre a mesma tarefa leem o mesmo array e a ultima grava por cima — uma
     * anotacao some sem erro e sem rastro. O indice unico parcial nao cobre o
     * caso: ele impede dois impedimentos <b>abertos</b>, nao duas escritas no
     * mesmo. ACH-03 da revisao de TASK-02.3, decidido na TechSpec v1.15.
     */
    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTarefaId() {
        return tarefaId;
    }

    public void setTarefaId(UUID tarefaId) {
        this.tarefaId = tarefaId;
    }

    public UUID getProjetoId() {
        return projetoId;
    }

    public void setProjetoId(UUID projetoId) {
        this.projetoId = projetoId;
    }

    public Long getIntervaloId() {
        return intervaloId;
    }

    public void setIntervaloId(Long intervaloId) {
        this.intervaloId = intervaloId;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getAnotacoes() {
        return anotacoes;
    }

    public void setAnotacoes(String anotacoes) {
        this.anotacoes = anotacoes;
    }

    public UUID getAbertoPor() {
        return abertoPor;
    }

    public void setAbertoPor(UUID abertoPor) {
        this.abertoPor = abertoPor;
    }

    public String getDesfecho() {
        return desfecho;
    }

    public void setDesfecho(String desfecho) {
        this.desfecho = desfecho;
    }

    public UUID getResolvidoPor() {
        return resolvidoPor;
    }

    public void setResolvidoPor(UUID resolvidoPor) {
        this.resolvidoPor = resolvidoPor;
    }

    public Instant getResolvidoEm() {
        return resolvidoEm;
    }

    public void setResolvidoEm(Instant resolvidoEm) {
        this.resolvidoEm = resolvidoEm;
    }

    public Long getVersao() {
        return versao;
    }

    public void setVersao(Long versao) {
        this.versao = versao;
    }
}
