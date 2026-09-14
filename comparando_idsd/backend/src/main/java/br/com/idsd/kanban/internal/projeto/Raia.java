package br.com.idsd.kanban.internal.projeto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Raia: divisao visual do board, e nada alem disso.
 *
 * <p>A raia <b>nao</b> restringe transicao e <b>nao</b> entra em agregacao de
 * tempo (RN-023). A garantia esta no esquema e nao na disciplina de quem
 * escreve a consulta, mas ela e <b>estreita</b>, e a versao generalizada que
 * este javadoc afirmava ate 2026-09-14 era falsa (ACH-02): {@code tarefa}
 * <b>carrega</b> {@code raia_id}, porque a raia e o agrupamento visual do
 * cartao no board.
 *
 * <p>O que nenhuma tabela carrega e raia na <b>serie de tempo</b>:
 * {@code evento_tarefa} e {@code intervalo_tarefa} nao tem coluna de raia, e
 * nenhuma rota agregada aceita filtro por raia. E isso que torna a agregacao
 * por raia inescrivivel — ela exigiria juntar a serie de tempo a {@code tarefa}
 * pelo estado <b>corrente</b>, e o estado corrente nao diz em que raia a tarefa
 * estava quando o intervalo correu. Ver TechSpec v1.11, {@code data-model.md}
 * §3 {@code raia}.
 *
 * <p>Como a etapa, a remocao e logica. A ordem <b>nao</b> e unica: ver a
 * migration de correcao, que removeu a restricao criada sem origem (ACH-06).
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
        this.projetoId = Objects.requireNonNull(projetoId, "projetoId e obrigatorio");
        this.nome = nomeValido(nome);
        this.ordem = ordemValida(ordem);
    }

    /** Renomeia e reposiciona preservando o identificador. */
    public void reconfigurar(String nome, int ordem) {
        this.nome = nomeValido(nome);
        this.ordem = ordemValida(ordem);
    }

    /**
     * Remocao logica. Nao existe remocao fisica de raia. O instante e atribuido
     * uma vez, pela mesma razao de {@link Etapa#arquivar(Instant)} (ACH-11).
     */
    public void arquivar(Instant quando) {
        Objects.requireNonNull(quando, "o instante do arquivamento e obrigatorio");
        if (arquivadaEm == null) {
            this.arquivadaEm = quando;
        }
    }

    /** Ver {@code Etapa.nomeValido}: o {@code NOT NULL} nao pega branco (ACH-12). */
    private static String nomeValido(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome da raia e obrigatorio");
        }
        return nome;
    }

    private static int ordemValida(int ordem) {
        if (ordem < 0) {
            throw new IllegalArgumentException("ordem da raia nao pode ser negativa");
        }
        return ordem;
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
