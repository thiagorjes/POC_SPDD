package br.com.idsd.kanban.internal.projeto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Um projeto em {@code GET /v1/projetos/{projetoId}} (RF-002).
 *
 * <p>Mesmos campos de {@link ProjetoResumo}. Sao dois registros e nao um porque a
 * relacao e o detalhe divergem nas tasks seguintes, e unificar agora obrigaria a
 * separar depois — com o custo de uma resposta ja publicada.
 */
public record ProjetoDetalhe(
        UUID id,
        String nome,
        String descricao,
        List<Papel> papeis,
        Set<Permissao> permissoes,
        boolean acessoPorAdministracaoGlobal) {
}
