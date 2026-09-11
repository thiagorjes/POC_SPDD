package br.com.idsd.kanban.internal.projeto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Etapa do fluxo: a primeira das tres dimensoes de estado da tarefa.
 *
 * <p>A identidade e estavel de proposito. A serie de tempo por etapa segue o
 * {@code id}, nunca o nome, de modo que renomear vale dali em diante e nao
 * reescreve o historico (RN-023). Pelo mesmo motivo a remocao e <b>logica</b>:
 * historico e intervalos referenciam a etapa para sempre, e apagar a linha
 * destruiria a serie que RF-016 existe para produzir.
 *
 * <p>A {@code ordem} define adjacencia para a regra de transicao (RN-021) e e
 * unica por projeto <i>entre as ativas</i> — arquivar libera a ordem.
 */
@Entity
@Table(name = "etapa")
public class Etapa {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "projeto_id", nullable = false)
    private UUID projetoId;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "ordem", nullable = false)
    private int ordem;

    @Column(name = "terminal", nullable = false)
    private boolean terminal;

    @Column(name = "arquivada_em")
    private Instant arquivadaEm;

    protected Etapa() {
        // exigido pelo JPA
    }

    public Etapa(UUID projetoId, String nome, int ordem, boolean terminal) {
        this.id = UUID.randomUUID();
        this.projetoId = projetoId;
        this.nome = nome;
        this.ordem = ordem;
        this.terminal = terminal;
    }

    /**
     * Renomeia e reposiciona preservando o identificador — que e o que separa
     * "reconfigurar o fluxo" de "trocar a etapa por outra".
     */
    public void reconfigurar(String nome, int ordem, boolean terminal) {
        this.nome = nome;
        this.ordem = ordem;
        this.terminal = terminal;
    }

    /** Remocao logica. Nao existe remocao fisica de etapa. */
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

    public boolean isTerminal() {
        return terminal;
    }

    public Instant getArquivadaEm() {
        return arquivadaEm;
    }
}
