package br.com.idsd.kanban.internal.projeto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Raia: divisao visual do board, e nada alem disso.
 *
 * <p>A raia <b>nao</b> restringe transicao e <b>nao</b> entra em agregacao de
 * tempo. Essa ausencia e garantida pelo esquema e nao por disciplina: nenhuma
 * tabela do anel de projecao carrega {@code raia_id}, entao nao ha por onde uma
 * consulta agregar por raia sem que alguem primeiro altere uma migration.
 *
 * <p>Como a etapa, a remocao e logica e a ordem e unica por projeto entre as
 * ativas.
 */
@Entity
@Table(name = "raia")
public class Raia {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "projeto_id", nullable = false)
    private UUID projetoId;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "ordem", nullable = false)
    private int ordem;

    @Column(name = "arquivada_em")
    private Instant arquivadaEm;

    protected Raia() {
        // exigido pelo JPA
    }

    public Raia(UUID projetoId, String nome, int ordem) {
        this.id = UUID.randomUUID();
        this.projetoId = projetoId;
        this.nome = nome;
        this.ordem = ordem;
    }

    /** Renomeia e reposiciona preservando o identificador. */
    public void reconfigurar(String nome, int ordem) {
        this.nome = nome;
        this.ordem = ordem;
    }

    /** Remocao logica. Nao existe remocao fisica de raia. */
    public void arquivar(Instant quando) {
        this.arquivadaEm = quando;
    }

    public boolean estaArquivada() {
        return arquivadaEm != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjetoId() {
        return projetoId;
    }

    public String getNome() {
        return nome;
    }

    public int getOrdem() {
        return ordem;
    }

    public Instant getArquivadaEm() {
        return arquivadaEm;
    }
}
