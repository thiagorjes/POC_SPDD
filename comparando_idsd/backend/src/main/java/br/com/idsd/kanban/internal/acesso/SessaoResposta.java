package br.com.idsd.kanban.internal.acesso;

import java.util.UUID;

/**
 * Quem sou eu, na resposta de {@code GET /v1/sessao}.
 *
 * <p>Deliberadamente <b>sem papel e sem permissao</b>. Papel e permissao nao sao
 * atributos da pessoa: existem por projeto, no par usuario-projeto (BDR-001), e
 * quem os devolve e {@code GET /v1/projetos}. Um campo de papel aqui seria a
 * porta pela qual o cliente montaria autorizacao global a partir da sessao, que
 * e o que ADR-003 e RNF-004 recusam.
 *
 * <p>{@code adminGlobal} nao e excecao a isso: e alcance de escopo declarado no
 * proprio registro do usuario (RN-035), e nao papel concedido em projeto algum.
 */
public record SessaoResposta(UUID id, String nome, String email, boolean adminGlobal) {

    static SessaoResposta de(Usuario usuario) {
        return new SessaoResposta(
                usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.isAdminGlobal());
    }
}
