package br.com.idsd.kanban.internal.projeto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
 * @param etapas o fluxo desejado <b>por inteiro</b>; nunca um delta
 */
public record FluxoRequisicao(
        @NotNull(message = "É preciso informar as etapas do fluxo.")
        @Valid
        List<EtapaDesejada> etapas) {

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
            String nome,
            @PositiveOrZero(message = "A ordem da etapa não pode ser negativa.")
            int ordem,
            boolean terminal) {
    }
}
