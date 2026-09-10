package br.com.idsd.kanban.internal.projeto;

import br.com.idsd.kanban.internal.acesso.UsuarioRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responde o que uma pessoa pode fazer num projeto.
 *
 * <p>A resposta sai sempre do que esta <b>gravado</b> — participacao real e
 * marca de administracao global no proprio registro do usuario. Nunca de papel,
 * identificador de projeto ou claim vindos do cliente: o token diz quem a pessoa
 * e, e o que ela pode e decidido aqui (ADR-003, RNF-004).
 *
 * <p><b>Sao dois os sujeitos que este servico resolve</b>, e nao um. O participante
 * tem as permissoes dos papeis do vinculo. A administracao global dispensa a
 * participacao para ver e agir em qualquer projeto (RN-035, ADR-010, SCN-021.2), e
 * por construcao ela <i>nunca</i> participa — quem cria projeto nao vira
 * participante (RN-037), justamente para que a marca de alcance global continue
 * aparecendo. Resolver so o primeiro sujeito faria este servico responder errado
 * para o segundo em todo projeto, e obrigaria cada rota a escrever o proprio
 * desvio ao lado da chamada, que e o que o ADR-010 existe para eliminar.
 *
 * <p>O que este servico <b>nao</b> resolve, e por que: criar projeto (RN-036) nao
 * e permissao de projeto e nao esta em {@link Permissao} — nenhum papel a possui,
 * porque nenhum papel existe antes de o projeto existir; a checagem dela e sobre
 * {@code adminGlobal} direto. E a imutabilidade do historico (RNF-008) nao passa
 * por aqui: ela e garantida na role de banco, e o alcance global nao a contorna
 * (SCN-021.3).
 */
@Service
public class ResolvedorDePermissao {

    /** Alcance da administracao global: ver e agir. Nao e imunidade (RN-035). */
    private static final Set<Permissao> ALCANCE_GLOBAL =
            Collections.unmodifiableSet(EnumSet.allOf(Permissao.class));

    private final ParticipacaoRepository participacoes;
    private final UsuarioRepository usuarios;

    public ResolvedorDePermissao(ParticipacaoRepository participacoes, UsuarioRepository usuarios) {
        this.participacoes = participacoes;
        this.usuarios = usuarios;
    }

    /**
     * Como o sujeito alcanca o projeto.
     *
     * <p>Devolve tambem <b>de onde</b> o acesso vem, e nao so o conjunto de
     * permissoes, por duas razoes que sao de contrato e nao de conveniencia. A
     * marca {@code porAdministracaoGlobal} e o que satisfaz "me e indicado que
     * estou agindo pelo alcance de administracao global" (SCN-021.2), e ela
     * precisa vir de quem decidiu o acesso — deriva-la de novo na borda seria a
     * segunda fonte da mesma decisao. E {@code participa} distingue quem esta no
     * projeto sem papel de quem nao esta nele: os dois tem conjunto de permissoes
     * vazio, e a TechSpec §8 pede respostas diferentes para os dois (`403` contra
     * `404`, SCN-002.3). Este servico nao escolhe entre elas — essa decisao esta
     * em aberto com o {@code /techspec} —, mas entrega ao chamador o material
     * para escolher, em vez de colapsar os dois casos num vazio indistinguivel.
     */
    public record Acesso(Set<Permissao> permissoes, boolean participa, boolean porAdministracaoGlobal) {

        public boolean tem(Permissao permissao) {
            return permissoes.contains(permissao);
        }

        /** Nem participa nem alcanca: o projeto nao existe para este sujeito. */
        public boolean semAlcance() {
            return !participa && !porAdministracaoGlobal;
        }
    }

    /** Resolve o acesso do sujeito ao projeto a partir do que esta gravado. */
    @Transactional(readOnly = true)
    public Acesso acessoAoProjeto(UUID usuarioId, UUID projetoId) {
        var participacao = participacoes.findByUsuarioIdAndProjetoId(usuarioId, projetoId);
        boolean adminGlobal = usuarios.findById(usuarioId)
                .map(usuario -> usuario.isAdminGlobal())
                .orElse(false);

        if (adminGlobal) {
            // O alcance global nao se soma aos papeis: ele ja e o conjunto todo.
            // A marca acompanha o acesso mesmo que a pessoa tambem participe,
            // porque e o alcance que dispensa a checagem de participacao.
            return new Acesso(ALCANCE_GLOBAL, participacao.isPresent(), true);
        }

        return participacao
                .map(p -> new Acesso(permissoesDe(p.getPapeis()), true, false))
                .orElseGet(() -> new Acesso(
                        Collections.unmodifiableSet(EnumSet.noneOf(Permissao.class)), false, false));
    }

    /** Permissoes do sujeito no projeto. Atalho sobre {@link #acessoAoProjeto}. */
    @Transactional(readOnly = true)
    public Set<Permissao> permissoesNoProjeto(UUID usuarioId, UUID projetoId) {
        return acessoAoProjeto(usuarioId, projetoId).permissoes();
    }

    /**
     * Uniao das permissoes dos papeis. Papeis acumulam: {@code project_admin} +
     * {@code product_owner} tem tudo o que cada um tem, e nada alem — o catalogo
     * fechado e o que garante o "nada alem".
     *
     * <p>Deliberadamente <b>nao publico</b>: e calculo de permissao a partir de
     * papeis fornecidos pelo chamador, e expo-lo convidaria a montar autorizacao
     * com papel vindo do token ou do corpo da requisicao, que e exatamente o que
     * RNF-004 e BDR-001 proibem. Quem precisa de permissao chama
     * {@link #acessoAoProjeto}, que le do banco.
     */
    Set<Permissao> permissoesDe(Collection<Papel> papeis) {
        Set<Permissao> permissoes = EnumSet.noneOf(Permissao.class);
        for (Papel papel : papeis) {
            permissoes.addAll(papel.getPermissoes());
        }
        return Collections.unmodifiableSet(permissoes);
    }

    /** Atalho de decisao. Existe para que o chamador nao reimplemente o teste. */
    @Transactional(readOnly = true)
    public boolean pode(UUID usuarioId, UUID projetoId, Permissao permissao) {
        return acessoAoProjeto(usuarioId, projetoId).tem(permissao);
    }
}
