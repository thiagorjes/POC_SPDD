package br.com.crudao.kanban.security;

import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.ProjetoFinalizadoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.projeto.ChaveToggle;
import br.com.crudao.kanban.projeto.Projeto;
import br.com.crudao.kanban.projeto.ProjetoRepository;
import br.com.crudao.kanban.projeto.ProjetoToggle;
import br.com.crudao.kanban.projeto.ProjetoToggleRepository;
import br.com.crudao.kanban.rbac.Papeis;
import br.com.crudao.kanban.rbac.PermissaoService;
import br.com.crudao.kanban.rbac.Usuario;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ponto unico de decisao de autorizacao. As tres checagens acontecem sempre nesta ordem: permissao
 * efetiva, toggle do projeto e projeto ativo.
 *
 * <p>O {@code projetoId} recebido aqui e sempre derivado do recurso pelo chamador — nunca de
 * parametro enviado pelo cliente, que e exatamente o vetor de escalonamento entre projetos que
 * RNF-003 proibe.
 */
@Component
@RequiredArgsConstructor
public class PermissaoGuard {

  private final PermissaoService permissaoService;
  private final ProjetoRepository projetoRepository;
  private final ProjetoToggleRepository projetoToggleRepository;
  private final UsuarioAtualProvider usuarioAtualProvider;

  public Usuario usuarioAtual() {
    return usuarioAtualProvider.obrigatorio();
  }

  /** Exige a permissao no projeto. Admin global tem bypass incondicional (ADR-007). */
  @Transactional(readOnly = true)
  public void exigir(String permissao, UUID projetoId) {
    Usuario usuario = usuarioAtual();
    if (usuario.isAdminGlobal()) {
      return;
    }
    if (!permissaoService.permissoesEfetivas(usuario.getId(), projetoId).contains(permissao)) {
      throw new PermissaoNegadaException(
          "Voce nao tem permissao para executar esta acao neste projeto.");
    }
  }

  /**
   * Toggle condicionante: se o usuario so possui o papel condicionante (ex.: {@code dev}) e o
   * toggle esta desabilitado, a acao e negada. Papeis administrativos nao dependem do toggle.
   */
  @Transactional(readOnly = true)
  public void exigirToggle(ChaveToggle chave, UUID projetoId, String papelCondicionante) {
    Usuario usuario = usuarioAtual();
    if (usuario.isAdminGlobal()) {
      return;
    }
    Set<String> papeis = permissaoService.papeisNoProjeto(usuario.getId(), projetoId);
    boolean administrativo = papeis.stream().anyMatch(Papeis.ADMINISTRATIVOS::contains);
    if (administrativo || !papeis.contains(papelCondicionante)) {
      return;
    }
    if (!toggleHabilitado(projetoId, chave)) {
      throw new PermissaoNegadaException(
          "Esta acao esta desabilitada para o seu papel na configuracao do projeto.");
    }
  }

  /** Projeto finalizado e somente leitura para <b>todos</b>, inclusive admin global (RN-015). */
  @Transactional(readOnly = true)
  public void exigirProjetoAtivo(UUID projetoId) {
    if (!projeto(projetoId).isAtivo()) {
      throw new ProjetoFinalizadoException(
          "O projeto esta finalizado e nao aceita alteracoes. Reabra o projeto para editar.");
    }
  }

  @Transactional(readOnly = true)
  public boolean membro(UUID projetoId) {
    Usuario usuario = usuarioAtual();
    return usuario.isAdminGlobal() || permissaoService.membro(usuario.getId(), projetoId);
  }

  @Transactional(readOnly = true)
  public Set<String> permissoesEfetivas(UUID projetoId) {
    Usuario usuario = usuarioAtual();
    if (usuario.isAdminGlobal()) {
      return br.com.crudao.kanban.rbac.Permissoes.TODAS;
    }
    return permissaoService.permissoesEfetivas(usuario.getId(), projetoId);
  }

  @Transactional(readOnly = true)
  public boolean possui(String permissao, UUID projetoId) {
    Usuario usuario = usuarioAtual();
    return usuario.isAdminGlobal()
        || permissaoService.permissoesEfetivas(usuario.getId(), projetoId).contains(permissao);
  }

  @Transactional(readOnly = true)
  public Set<String> papeisNoProjeto(UUID projetoId) {
    return permissaoService.papeisNoProjeto(usuarioAtual().getId(), projetoId);
  }

  @Transactional(readOnly = true)
  public boolean toggleHabilitado(UUID projetoId, ChaveToggle chave) {
    return projetoToggleRepository
        .findByIdProjetoIdAndIdChave(projetoId, chave)
        .map(ProjetoToggle::isHabilitado)
        .orElse(chave.padrao());
  }

  private Projeto projeto(UUID projetoId) {
    return projetoRepository
        .findById(projetoId)
        .orElseThrow(() -> RecursoNaoEncontradoException.de("Projeto", projetoId));
  }
}
