package br.com.idsd.kanban.internal.projeto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Projeto: a fronteira de participacao e de visibilidade do sistema (BDR-001). */
@Entity
@Table(name = "projeto")
public class Projeto {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    /**
     * Contador de sequencia de eventos do projeto (SDR-004). Ainda sem uso: quem
     * o incrementa e o nucleo de escrita, em epico posterior.
     */
    @Column(name = "seq_atual", nullable = false)
    private long seqAtual;

    protected Projeto() {
        // exigido pelo JPA
    }

    public Projeto(String nome, String descricao) {
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.descricao = descricao;
        this.criadoEm = Instant.now();
        this.seqAtual = 0L;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public long getSeqAtual() {
        return seqAtual;
    }
}
