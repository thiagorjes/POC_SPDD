package br.com.idsd.kanban.internal.projeto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Entrada de {@code PUT /v1/projetos/{projetoId}/etapas} (RF-017).
 *
 * <p><b>O fluxo chega inteiro, e nao etapa a etapa.</b> A ordem e propriedade do
 * conjunto, e a edicao individual permitiria o estado intermediario sem etapa
 * terminal que RN-001 proibe — a substituicao atomica e o que garante que esse
 * estado nunca exista, nem mesmo dentro da transacao.
 *
 * <p>A ausencia tambem e informacao: etapa vigente <b>omitida</b> desta lista e
 * arquivada. Por isso nao ha rota de remocao, e por isso {@code id} e opcional
 * num lado so — presente, identifica a etapa a preservar; ausente, cria.
 *
 * <p>A validacao por anotacao protege o contrato HTTP e nomeia o campo em
 * {@code errors} para a tela. Ela nao substitui as invariantes de {@link Etapa},
 * que valem para toda escrita, inclusive a que nao vem da borda — a duplicacao
 * segue a mesma decisao tomada em {@link CriacaoDeProjeto}.
 *
 * <p><b>Ha teto de tamanho, e ele e de seguranca</b> (ACH-09 da revisao de
 * TASK-02.2). A coluna {@code nome} e {@code text} sem limite e a substituicao
 * grava tudo numa transacao so: sem {@code @Size} na lista e no nome, um
 * {@code PUT} de um sujeito autenticado consome armazenamento sem limite dentro
 * do envelope de escritas por minuto de RNF-010.
 *
 * <p><b>{@code @NotNull} no elemento nao e redundante.</b> {@code @Valid} sobre
 * a lista cascateia nos elementos nao-nulos e ignora os nulos, e Jackson aceita
 * {@code null} como item de array — o elemento nulo chegava ao servico e
 * derrubava a rota (ACH-03).
 *
 * @param etapas o fluxo desejado <b>por inteiro</b>; nunca um delta
 */
public record FluxoRequisicao(
        @NotNull(message = "É preciso informar as etapas do fluxo.")
        @Size(max = Etapa.MAXIMO_DE_ETAPAS,
                message = "O fluxo não pode passar de " + Etapa.MAXIMO_DE_ETAPAS + " etapas.")
        @Valid
        List<@NotNull(message = "A lista de etapas não pode conter item vazio.")
                EtapaDesejada> etapas) {

    /**
     * Uma etapa no fluxo desejado.
     *
     * @param id {@code null} <b>cria</b> a etapa; preenchido, identifica uma etapa
     *     vigente a preservar. Preservar o identificador e o que separa renomear de
     *     trocar a etapa por outra — a serie de tempo por etapa segue o {@code id}
     *     e nao o nome (RN-021, RN-022)
     * @param nome rotulo exibido; renomear vale dali em diante e nao reescreve
     *     historico
     * @param ordem posicao no fluxo, que define a adjacencia da regra de transicao
     * @param terminal se a etapa encerra o fluxo; ao menos uma por projeto (RN-001)
     */
    public record EtapaDesejada(
            UUID id,
            @NotBlank(message = "O nome da etapa não pode ficar em branco.")
            @Size(max = Etapa.TAMANHO_MAXIMO_DO_NOME,
                    message = "O nome da etapa não pode passar de "
                            + Etapa.TAMANHO_MAXIMO_DO_NOME + " caracteres.")
            String nome,
            @PositiveOrZero(message = "A ordem da etapa não pode ser negativa.")
            @Max(value = Etapa.ORDEM_MAXIMA,
                    message = "A ordem da etapa não pode passar de " + Etapa.ORDEM_MAXIMA + ".")
            int ordem,
            boolean terminal) {
    }
}
