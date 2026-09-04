package br.com.crudao.kanban.rbac;

import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolucao da permissao efetiva: uniao das permissoes dos papeis que o usuario possui naquele
 * projeto (papeis sao acumulaveis, BDR-001).
 */
@Service
@RequiredArgsConstructor
public class PermissaoService {

  private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;

  /** Uniao das permissoes dos papeis do usuario no projeto informado. */
  @Transactional(readOnly = true)
  public Set<String> permissoesEfetivas(UUID usuarioId, UUID projetoId) {
    return usuarioProjetoPapelRepository.buscarPermissoesEfetivas(usuarioId, projetoId);
  }

  /** Codigos dos papeis do usuario no projeto. Usado para avaliar toggles condicionantes. */
  @Transactional(readOnly = true)
  public Set<String> papeis(UUID usuarioId, UUID projetoId) {
    return usuarioProjetoPapelRepository.buscarCodigosDePapel(usuarioId, projetoId);
  }

  /** Verdadeiro quando o usuario possui qualquer papel no projeto. */
  @Transactional(readOnly = true)
  public boolean membro(UUID usuarioId, UUID projetoId) {
    return usuarioProjetoPapelRepository.existsByIdUsuarioIdAndIdProjetoId(usuarioId, projetoId);
  }
}
