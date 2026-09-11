package br.com.idsd.kanban.internal.projeto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Um projeto na relacao de {@code GET /v1/projetos} (RF-002).
 *
 * <p>{@code permissoes} vem <b>resolvido pelo servidor</b> e existe para que o
 * cliente nao apresente acao que nao poderia executar (RNF-004). Ele nao e a
 * decisao: a recusa real acontece no servico, sobre a participacao gravada. Um
 * cliente que ignorasse este campo nao ganharia nada.
 *
 * <p>{@code acessoPorAdministracaoGlobal} torna o alcance <b>visivel na
 * resposta</b>, e nao implicito (SCN-021.2). Ele acompanha o item mesmo quando a
 * pessoa tambem participa do projeto, porque e o alcance que dispensa a checagem
 * de participacao.
 *
 * <p><b>Nao ha {@code descricao} aqui, e a ausencia e a regra.</b> A relacao
 * lista por participacao, entao o participante sem papel algum recebe este item
 * e recebe {@code 403} no detalhe: o que vem na relacao e, por construcao, o que
 * quem nao tem {@code LER} pode ver. {@code nome} vem porque e o que identifica
 * o vinculo e o que torna o {@code 403} acionavel — sem ele a recusa fala de um
 * identificador opaco. {@code descricao} nao tem nenhuma das duas propriedades:
 * e o primeiro campo que carrega informacao de negocio, e mante-la aqui
 * entregaria a quem o detalhe recusa justamente o conteudo que a recusa protege.
 * Ela vive so em {@link ProjetoDetalhe}, atras de {@code LER}. Ver o corolario
 * da regra {@code 403}/{@code 404} em {@code contracts/sessao-e-projetos.md}.
 */
public record ProjetoResumo(
        UUID id,
        String nome,
        List<Papel> papeis,
        Set<Permissao> permissoes,
        boolean acessoPorAdministracaoGlobal) {

    /**
     * O envelope da relacao.
     *
     * <p>Nao ha paginacao por parametro nesta rota: a resposta e sempre o conjunto
     * inteiro, e por isso os totais sao calculados <b>sobre o que vai no corpo</b>
     * — {@code totalPages} e {@code 1} quando ha conteudo e {@code 0} quando nao
     * ha. A forma existe para que a paginacao possa entrar depois sem quebrar o
     * cliente, nao para fingir que ja entrou. Quem introduzir a fatia precisa
     * trocar {@link #de(List)} por um construtor que receba o total da consulta:
     * daquele dia em diante calcular sobre a fatia passa a mentir.
     */
    public record Pagina(List<ProjetoResumo> conteudo, long totalElements, int totalPages) {

        public static Pagina de(List<ProjetoResumo> conteudo) {
            return new Pagina(conteudo, conteudo.size(), conteudo.isEmpty() ? 0 : 1);
        }
    }
}
