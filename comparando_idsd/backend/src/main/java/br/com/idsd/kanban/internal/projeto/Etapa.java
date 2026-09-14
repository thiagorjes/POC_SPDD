package br.com.idsd.kanban.internal.projeto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Etapa do fluxo: a primeira das tres dimensoes de estado da tarefa.
 *
 * <p>A identidade e estavel de proposito. A serie de tempo por etapa segue o
 * {@code id}, nunca o nome, de modo que renomear vale dali em diante e nao
 * reescreve o historico (RN-021, RN-022). Pelo mesmo motivo a remocao e
 * <b>logica</b>: historico e intervalos referenciam a etapa para sempre, e
 * apagar a linha destruiria a serie que RF-016 existe para produzir.
 *
 * <p>A {@code ordem} define adjacencia para a regra de transicao (RN-005) e e
 * unica por projeto <i>entre as ativas</i> — arquivar libera a ordem.
 *
 * <p>As duas citacoes de regra acima foram corrigidas em 2026-09-14 (ACH-10 da
 * revisao de TASK-02.1): estavam em RN-023, que e a regra da raia, e em RN-021,
 * que e a do rename. Referencia trocada em comentario e o tipo de erro que so
 * aparece quando alguem vai conferir a regra e encontra outra.
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
        this.projetoId = Objects.requireNonNull(projetoId, "projetoId e obrigatorio");
        this.nome = nomeValido(nome);
        this.ordem = ordemValida(ordem);
        this.terminal = terminal;
    }

    /**
     * Renomeia e reposiciona preservando o identificador — que e o que separa
     * "reconfigurar o fluxo" de "trocar a etapa por outra".
     */
    public void reconfigurar(String nome, int ordem, boolean terminal) {
        this.nome = nomeValido(nome);
        this.ordem = ordemValida(ordem);
        this.terminal = terminal;
    }

    /**
     * Remocao logica. Nao existe remocao fisica de etapa.
     *
     * <p>O instante e atribuido <b>uma vez</b>: arquivar de novo nao reescreve a
     * data de saida, que e dado da serie de tempo e nao estado corrente
     * (ACH-11). Reatribuir seria mover para frente o momento em que a etapa
     * deixou o fluxo, sem que nada registrasse a mudanca.
     */
    public void arquivar(Instant quando) {
        Objects.requireNonNull(quando, "o instante do arquivamento e obrigatorio");
        if (arquivadaEm == null) {
            this.arquivadaEm = quando;
        }
    }

    /**
     * Nome em branco nao e recusado pelo {@code NOT NULL} da coluna, e nome
     * ausente so seria recusado tarde e com mensagem de driver. A recusa de
     * borda, com {@code 422} e campo nomeado, chega com as rotas em TASK-02.2 —
     * esta aqui e a invariante de quem grava, e nao substitui aquela (ACH-12).
     */
    private static String nomeValido(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome da etapa e obrigatorio");
        }
        return nome;
    }

    /** A ordem posiciona a etapa no fluxo; posicao negativa nao e posicao. */
    private static int ordemValida(int ordem) {
        if (ordem < 0) {
            throw new IllegalArgumentException("ordem da etapa nao pode ser negativa");
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

    public boolean isTerminal() {
        return terminal;
    }

    public Instant getArquivadaEm() {
        return arquivadaEm;
    }
}
