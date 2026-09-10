package br.com.idsd.kanban.internal.projeto;

import java.util.UUID;

/**
 * Uma linha da consulta de projetos visiveis.
 *
 * <p>Nao e um projeto: e o produto do join entre projeto, participacao e papel,
 * de modo que um projeto em que a pessoa acumula dois papeis aparece em duas
 * linhas. Quem consome agrupa por {@link #projetoId()}.
 *
 * <p>A projecao existe para que a relacao inteira saia em <b>uma</b> ida ao banco.
 * Carregar as entidades e perguntar os papeis depois produziria uma consulta por
 * projeto — o N+1 que o criterio de aceite da rota proibe —, e o join e explicito
 * pela mesma razao: caminho implicito sobre associacao opcional vira {@code inner
 * join} no Hibernate e sumiria com os projetos que a administracao global alcanca
 * sem participar.
 *
 * @param participacaoId {@code null} quando a pessoa nao participa do projeto e o
 *     alcanca apenas pela administracao global. E isso, e nao o conjunto de papeis
 *     vazio, que distingue participante sem papel de nao-participante.
 * @param papel {@code null} quando ha participacao sem papel algum, ou quando nao
 *     ha participacao.
 */
public record ProjetoConsulta(
        UUID projetoId,
        String nome,
        String descricao,
        UUID participacaoId,
        Papel papel) {

    public boolean participa() {
        return participacaoId != null;
    }
}
