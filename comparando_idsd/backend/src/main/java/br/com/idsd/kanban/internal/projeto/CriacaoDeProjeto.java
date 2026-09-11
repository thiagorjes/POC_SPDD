package br.com.idsd.kanban.internal.projeto;

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
 * @param nome nome do projeto; branco e recusado com {@code 422}
 * @param descricao texto livre, opcional
 * @param primeiroAdministradorId {@code id} de um {@code usuario} <b>ja
 *     existente</b> — a conta nasce na primeira entrada da pessoa, pelo
 *     autoprovisionamento da sessao, e nao aqui
 */
public record CriacaoDeProjeto(String nome, String descricao, UUID primeiroAdministradorId) {

    /**
     * Saida de {@code 201}.
     *
     * <p>{@code etapas} sai vazia, e isso e a resposta correta e nao um estado
     * transitorio: o projeto nasce sem fluxo (RN-038), e configura-lo e o segundo
     * passo obrigatorio do caminho de partida.
     */
    public record Criado(UUID id, String nome, String descricao, List<Object> etapas) {

        public static Criado de(Projeto projeto) {
            return new Criado(
                    projeto.getId(), projeto.getNome(), projeto.getDescricao(), List.of());
        }
    }
}
