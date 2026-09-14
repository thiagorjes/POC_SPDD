package br.com.idsd.kanban.internal.projeto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Entrada de {@code POST /v1/projetos} (RF-022).
 *
 * <p>O projeto e a sua primeira administradora chegam <b>num envio so</b>, e isso
 * nao e conveniencia de contrato: projeto sem participacao nenhuma e projeto
 * inalcancavel, porque conceder participacao exige alguem ja dentro dele. Separar
 * em duas chamadas reintroduziria, entre uma e outra, exatamente o estado que esta
 * rota existe para eliminar.
 *
 * <p><b>A fronteira valida por anotacao</b> (ACH-03 da revisao de TASK-01.8). A
 * verificacao equivalente continua no servico, e a duplicacao e deliberada: a
 * anotacao protege o contrato HTTP e nomeia o campo em {@code errors} para a tela,
 * enquanto a do servico e a invariante de quem grava — chamada de dentro do
 * sistema nao passa pelo {@code @Valid} da borda.
 *
 * @param nome nome do projeto; branco e recusado com {@code 422}
 * @param descricao texto livre, opcional
 * @param primeiroAdministradorId {@code id} de um {@code usuario} <b>ja
 *     existente</b> — a conta nasce na primeira entrada da pessoa, pelo
 *     autoprovisionamento da sessao, e nao aqui
 */
public record CriacaoDeProjeto(
        @NotBlank(message = "O nome do projeto não pode ficar em branco.")
        String nome,
        String descricao,
        @NotNull(message = "É preciso nomear quem será a primeira administradora do projeto.")
        UUID primeiroAdministradorId) {

    /**
     * Saida de {@code 201}.
     *
     * <p>{@code etapas} sai vazia, e isso e a resposta correta e nao um estado
     * transitorio: o projeto nasce sem fluxo (RN-038), e configura-lo e o segundo
     * passo obrigatorio do caminho de partida.
     *
     * <p><b>A colecao deixou de ser constante em TASK-02.2</b>, que era o gatilho
     * escrito aqui por ACH-07 da revisao de TASK-01.8. Enquanto a tabela
     * {@code etapa} nao existia, {@code List.of()} era verdadeiro e nao verificado:
     * a assercao {@code etapas: []} ficava verde por construcao, e teria continuado
     * verde no dia em que a resposta passasse a estar errada. Agora o fluxo chega de
     * quem o le, e o valor vazio do projeto recem-criado e um <i>resultado</i> e nao
     * uma promessa.
     */
    public record Criado(UUID id, String nome, String descricao, List<EtapaResposta> etapas) {

        public static Criado de(Projeto projeto, List<EtapaResposta> etapas) {
            return new Criado(
                    projeto.getId(), projeto.getNome(), projeto.getDescricao(), List.copyOf(etapas));
        }
    }
}
