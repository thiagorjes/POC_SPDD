package br.com.idsd.kanban.internal.acesso;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Entrada de uma pessoa no sistema.
 *
 * <p>Nao ha cadastro local (ADR-006): a conta nasce na primeira entrada, a partir
 * do que o token afirma. O vinculo e o {@code sub}, e nome e e-mail sao espelho
 * reescrito a cada entrada — se o provedor os mudou, quem esta desatualizado e o
 * banco, nunca o token.
 *
 * <p><b>Promocao a administracao global (ADR-010).</b> A chave e o {@code sub}
 * configurado, <b>jamais o e-mail</b>: o e-mail e mutavel no provedor e, em realm
 * com autocadastro ou federacao, atribuivel por quem se registra — casar por
 * e-mail poria o bypass universal a uma requisicao de distancia de qualquer um. A
 * promocao e unica no sistema e auditada em {@code WARN}.
 */
@Service
public class SessaoService {

    private static final Logger LOG = LoggerFactory.getLogger(SessaoService.class);

    private final UsuarioRepository usuarios;

    /**
     * Sujeito designado a administracao global. Vazio significa que ninguem sera
     * promovido — a ausencia de configuracao nunca abre a promocao a todos.
     */
    private final String subjectDesignado;

    public SessaoService(
            UsuarioRepository usuarios,
            @Value("${idsd.admin-global.subject-id:}") String subjectDesignado) {
        this.usuarios = usuarios;
        this.subjectDesignado = subjectDesignado;
    }

    /**
     * Autoprovisiona, espelha e — se for o caso — promove, numa transacao so.
     *
     * <p>A verificacao de unicidade nao pode ficar fora desta transacao: ela e
     * feita dentro do proprio comando de promocao (ver
     * {@link UsuarioRepository#promoverSeNaoHouverAdminGlobal}), porque conferir
     * antes e escrever depois deixa duas entradas simultaneas promoverem duas
     * pessoas.
     */
    @Transactional
    public SessaoResposta entrar(String subjectId, String nome, String email) {
        return entrar(subjectId, nome, email, "sessao");
    }

    /**
     * A mesma entrada, dizendo por qual porta ela veio.
     *
     * <p>A promocao deixou de ser alcancavel so por {@code GET /v1/sessao} no dia
     * em que a criacao de projeto passou a garantir a conta pela mesma via, e o
     * registro auditado dizia quem foi promovido sem dizer por onde. Duas portas
     * para a escrita mais poderosa do sistema e um log que nao as distingue e o
     * tipo de lacuna que so aparece quando ja e tarde.
     *
     * @param via identificador curto da rota que provocou a entrada
     */
    @Transactional
    public SessaoResposta entrar(String subjectId, String nome, String email, String via) {
        Usuario usuario = provisionar(subjectId, nome, email);
        usuario.espelharDoToken(nome, email);
        promoverSeDesignado(usuario, via);
        return SessaoResposta.de(usuario);
    }

    private Usuario provisionar(String subjectId, String nome, String email) {
        return usuarios.findBySubjectId(subjectId)
                .orElseGet(() -> criarOuRecuperar(subjectId, nome, email));
    }

    /**
     * Cria a conta, aceitando perder a corrida.
     *
     * <p>Duas primeiras entradas simultaneas da mesma pessoa colidem na restricao
     * unica de {@code subject_id}, e essa colisao e o resultado correto: ela e o
     * que garante uma linha so. O que nao pode e virar erro para quem entrou —
     * quem perdeu a corrida le a linha que a outra gravou.
     */
    private Usuario criarOuRecuperar(String subjectId, String nome, String email) {
        try {
            return usuarios.saveAndFlush(new Usuario(subjectId, nome, email));
        } catch (DataIntegrityViolationException colisao) {
            return usuarios.findBySubjectId(subjectId).orElseThrow(() -> colisao);
        }
    }

    private void promoverSeDesignado(Usuario usuario, String via) {
        if (!StringUtils.hasText(subjectDesignado)
                || !subjectDesignado.equals(usuario.getSubjectId())
                || usuario.isAdminGlobal()) {
            return;
        }

        if (usuarios.promoverSeNaoHouverAdminGlobal(usuario.getId()) == 0) {
            // Ja existe administracao global. A recusa e silenciosa de proposito:
            // dizer a quem pediu que a vaga esta ocupada e informacao sobre o
            // estado do bypass que nao serve a quem nao foi promovido.
            LOG.info("Promocao a administracao global recusada: ja existe uma. sub={} via={}",
                    usuario.getSubjectId(), via);
            return;
        }

        usuario.refletirPromocaoGravada();
        LOG.warn("Promocao a administracao global: usuarioId={} sub={} via={} promovidoEm={}",
                usuario.getId(), usuario.getSubjectId(), via, Instant.now());
    }
}
