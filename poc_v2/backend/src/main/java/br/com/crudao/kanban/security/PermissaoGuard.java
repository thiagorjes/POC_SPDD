package br.com.crudao.kanban.security;

import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.ProjetoFinalizadoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.projeto.ChaveToggle;
import br.com.crudao.kanban.projeto.Projeto;
import br.com.crudao.kanban.projeto.ProjetoRepository;
import br.com.crudao.kanban.projeto.ProjetoToggleService;
import br.com.crudao.kanban.rbac.PermissaoService;
import br.com.crudao.kanban.rbac.Usuario;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autoridade unica de autorizacao (RNF-003, BDR-001). Toda regra exibida na UI e revalidada aqui: a
 * UI e conveniencia, o backend e autoridade.
 */
@Component
@RequiredArgsConstructor
public class PermissaoGuard {

  private final PermissaoService permissaoService;
  private final ProjetoRepository projetoRepository;
  private final ProjetoToggleService projetoToggleService;
  private final UsuarioAtualProvider usuarioAtualProvider;

  /**
   * Exige a permissao efetiva no projeto. Admin global possui bypass incondicional desta checagem
   * (ADR-007) — mas nao da checagem de projeto ativo (RN-015).
   */
  @Transactional(readOnly = true)
  public void exigir(String permissao, UUID projetoId) {
    Usuario usuario = usuarioAtualProvider.atual();
    if (usuario.isAdminGlobal()) {
      return;
    }
    if (!permissaoService.permissoesEfetivas(usuario.getId(), projetoId).contains(permissao)) {
      throw new PermissaoNegadaException(
          "Voce nao possui a permissao necessaria para esta acao neste projeto.");
    }
  }

  /**
   * Toggle condicionante: quando o usuario depende exclusivamente do papel informado (ex.: {@code
   * dev}) e o toggle esta desabilitado, a acao e negada (RF-016).
   */
  @Transactional(readOnly = true)
  public void exigirToggle(ChaveToggle chave, UUID projetoId, String papelCondicionante) {
    Usuario usuario = usuarioAtualProvider.atual();
    if (usuario.isAdminGlobal()) {
      return;
    }
    if (!dependeApenasDoPapel(usuario, projetoId, papelCondicionante)) {
      return;
    }
    if (!projetoToggleService.habilitado(projetoId, chave)) {
      throw new PermissaoNegadaException(
          "Esta acao esta desabilitada para o seu papel na configuracao do projeto.");
    }
  }

  /** RN-015: projeto finalizado e somente leitura para todos, sem bypass algum. */
  @Transactional(readOnly = true)
  public void exigirProjetoAtivo(UUID projetoId) {
    Projeto projeto =
        projetoRepository
            .findById(projetoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto nao encontrado."));
    if (!projeto.ativo()) {
      throw new ProjetoFinalizadoException("O projeto esta finalizado e nao aceita alteracoes.");
    }
  }

  /** Verdadeiro quando o usuario possui qualquer papel no projeto ou e admin global. */
  @Transactional(readOnly = true)
  public boolean membro(UUID projetoId) {
    Usuario usuario = usuarioAtualProvider.atual();
    return usuario.isAdminGlobal() || permissaoService.membro(usuario.getId(), projetoId);
  }

  /** Conjunto de permissoes efetivas do usuario atual no projeto, para a UI condicional. */
  @Transactional(readOnly = true)
  public Set<String> permissoesEfetivas(UUID projetoId) {
    Usuario usuario = usuarioAtualProvider.atual();
    if (usuario.isAdminGlobal()) {
      return Set.copyOf(CatalogoPermissoes.TODAS);
    }
    return permissaoService.permissoesEfetivas(usuario.getId(), projetoId);
  }

  /** Verificacao nao lancante, usada para montar destinos permitidos no snapshot do board. */
  @Transactional(readOnly = true)
  public boolean possui(String permissao, UUID projetoId) {
    Usuario usuario = usuarioAtualProvider.atual();
    return usuario.isAdminGlobal()
        || permissaoService.permissoesEfetivas(usuario.getId(), projetoId).contains(permissao);
  }

  /** Papeis do usuario atual no projeto. */
  @Transactional(readOnly = true)
  public Set<String> papeis(UUID projetoId) {
    return permissaoService.papeis(usuarioAtualProvider.atual().getId(), projetoId);
  }

  private boolean dependeApenasDoPapel(Usuario usuario, UUID projetoId, String papel) {
    Set<String> papeis = permissaoService.papeis(usuario.getId(), projetoId);
    return papeis.contains(papel) && papeis.stream().allMatch(atual -> atual.equals(papel));
  }
}
