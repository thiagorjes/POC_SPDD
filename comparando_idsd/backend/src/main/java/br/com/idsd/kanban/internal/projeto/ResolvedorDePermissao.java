package br.com.idsd.kanban.internal.projeto;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responde o que uma pessoa pode fazer num projeto.
 *
 * <p>A resposta sai sempre da <b>participacao real</b>, lida do banco — nunca de
 * papel, identificador de projeto ou claim vindos do cliente. O token diz quem a
 * pessoa e; o que ela pode e decidido aqui (ADR-003, RNF-004).
 *
 * <p>Quem nao participa nao tem permissao nenhuma, e o conjunto vazio e a
 * resposta correta: nao existe permissao por omissao.
 */
@Service
public class ResolvedorDePermissao {

    private final ParticipacaoRepository participacoes;

    public ResolvedorDePermissao(ParticipacaoRepository participacoes) {
        this.participacoes = participacoes;
    }

    /** Permissoes do sujeito no projeto, a partir da participacao gravada. */
    @Transactional(readOnly = true)
    public Set<Permissao> permissoesNoProjeto(UUID usuarioId, UUID projetoId) {
        return participacoes
                .findByUsuarioIdAndProjetoId(usuarioId, projetoId)
                .map(participacao -> permissoesDe(participacao.getPapeis()))
                .orElseGet(() -> EnumSet.noneOf(Permissao.class));
    }

    /**
     * Uniao das permissoes dos papeis. Papeis acumulam: {@code project_admin} +
     * {@code product_owner} tem tudo o que cada um tem, e nada alem — o catalogo
     * fechado e o que garante o "nada alem".
     */
    public Set<Permissao> permissoesDe(Collection<Papel> papeis) {
        Set<Permissao> permissoes = EnumSet.noneOf(Permissao.class);
        for (Papel papel : papeis) {
            permissoes.addAll(papel.getPermissoes());
        }
        return permissoes;
    }

    /** Atalho de decisao. Existe para que o chamador nao reimplemente o teste. */
    @Transactional(readOnly = true)
    public boolean pode(UUID usuarioId, UUID projetoId, Permissao permissao) {
        return permissoesNoProjeto(usuarioId, projetoId).contains(permissao);
    }
}
