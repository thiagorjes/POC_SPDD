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
 */
public record ProjetoResumo(
        UUID id,
        String nome,
        String descricao,
        List<Papel> papeis,
        Set<Permissao> permissoes,
        boolean acessoPorAdministracaoGlobal) {

    /**
     * O envelope da relacao.
     *
     * <p>Os totais sao do conjunto inteiro e nao da fatia devolvida. Nao ha
     * paginacao por parametro nesta rota, e por isso {@code totalPages} e
     * {@code 1} sempre que houver conteudo e {@code 0} quando nao houver — a
     * forma existe para que a paginacao possa entrar depois sem quebrar o
     * cliente, nao para fingir que ja entrou.
     */
    public record Pagina(List<ProjetoResumo> conteudo, long totalElements, int totalPages) {

        public static Pagina de(List<ProjetoResumo> conteudo) {
            return new Pagina(conteudo, conteudo.size(), conteudo.isEmpty() ? 0 : 1);
        }
    }
}
