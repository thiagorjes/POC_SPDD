package br.com.idsd.kanban.internal.projeto;

import br.com.idsd.kanban.shared.RegraDeNegocioViolada;
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

    /**
     * Teto da ordem de uma etapa real, e a fronteira inferior da faixa de trabalho.
     *
     * <p>Ele existe porque {@link #FAIXA_DE_TRABALHO} precisa ser <b>inalcancavel
     * por validacao</b>, e nao por suposicao sobre o uso (ACH-04 da revisao de
     * TASK-02.2). Antes, a unica restricao sobre {@code ordem} era nao ser
     * negativa: uma requisicao pedindo {@code ordem} 1.000.000 colidia, dentro do
     * mesmo flush, com a vigente de ordem 0 ja deslocada — o passo que existe para
     * evitar a colisao a reintroduzia pelo outro lado. O teto tambem impede que a
     * soma do deslocamento estoure para negativo.
     */
    public static final int ORDEM_MAXIMA = 9_999;

    /** Teto do nome, contra a coluna {@code text} sem limite (ACH-09). */
    public static final int TAMANHO_MAXIMO_DO_NOME = 120;

    /** Teto de etapas por fluxo. Fluxo real nao chega perto; abuso chega (ACH-09). */
    public static final int MAXIMO_DE_ETAPAS = 100;

    /**
     * Distancia entre a faixa de trabalho do passo intermediario e qualquer ordem
     * real. Somar preserva a unicidade entre as deslocadas — a soma e injetora — e
     * {@link #ORDEM_MAXIMA} garante que nenhuma ordem pedida caia dentro dela.
     */
    public static final int FAIXA_DE_TRABALHO = 1_000_000;

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
     * Desloca a ordem para a faixa de trabalho do passo intermediario.
     *
     * <p>E o <b>unico</b> caminho que grava ordem fora da faixa real, e por isso
     * nao passa por {@link #ordemValida}: as duas faixas sao disjuntas por
     * construcao, e misturar as validacoes apagaria a fronteira que
     * {@link #ORDEM_MAXIMA} existe para manter. Restrito ao pacote — quem o chama
     * e {@code EtapaRepositorioImpl}, e nao ha uso legitimo fora dele.
     */
    void deslocarOrdem(int ordem) {
        if (ordem < FAIXA_DE_TRABALHO) {
            throw new IllegalStateException("deslocamento fora da faixa de trabalho");
        }
        this.ordem = ordem;
    }

    /**
     * Nome em branco nao e recusado pelo {@code NOT NULL} da coluna, e nome
     * ausente so seria recusado tarde e com mensagem de driver.
     *
     * <p>A recusa sai em {@code 422} e nao em {@code 400} (ACH-12 da revisao de
     * TASK-02.2): {@code IllegalArgumentException} e mapeada para {@code 400} pelo
     * tratador global, enquanto toda recusa de conteudo bem formado do sistema e
     * {@code 422}. Hoje as anotacoes de {@link FluxoRequisicao} cobrem o caso e
     * este caminho fica encoberto; no dia em que uma escrita nao vier da borda —
     * papel que esta entidade reivindica — a resposta precisa continuar a mesma.
     */
    private static String nomeValido(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioViolada(
                    "etapa-invalida",
                    "Etapa inválida",
                    "O nome da etapa não pode ficar em branco.");
        }
        if (nome.length() > TAMANHO_MAXIMO_DO_NOME) {
            throw new RegraDeNegocioViolada(
                    "etapa-invalida",
                    "Etapa inválida",
                    "O nome da etapa não pode passar de " + TAMANHO_MAXIMO_DO_NOME
                            + " caracteres.");
        }
        return nome;
    }

    /** A ordem posiciona a etapa no fluxo; posicao negativa nao e posicao. */
    private static int ordemValida(int ordem) {
        if (ordem < 0 || ordem > ORDEM_MAXIMA) {
            throw new RegraDeNegocioViolada(
                    "etapa-invalida",
                    "Etapa inválida",
                    "A ordem da etapa precisa estar entre 0 e " + ORDEM_MAXIMA + ".");
        }
        return ordem;
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
