package br.com.crudao.kanban.rbac;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolucao de permissoes efetivas: uniao das permissoes de todos os papeis que o usuario acumula
 * <b>naquele projeto</b> (BDR-001). Nao aplica bypass de admin global — isso e responsabilidade do
 * {@code PermissaoGuard}, que e o unico ponto de decisao de autorizacao.
 */
@Service
@RequiredArgsConstructor
public class PermissaoService {

  private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
  private final PapelRepository papelRepository;

  @Transactional(readOnly = true)
  public Set<String> permissoesEfetivas(UUID usuarioId, UUID projetoId) {
    return new HashSet<>(usuarioProjetoPapelRepository.findCodigosPermissao(usuarioId, projetoId));
  }

  @Transactional(readOnly = true)
  public Set<String> papeisNoProjeto(UUID usuarioId, UUID projetoId) {
    return new HashSet<>(usuarioProjetoPapelRepository.findCodigosPapel(usuarioId, projetoId));
  }

  @Transactional(readOnly = true)
  public boolean membro(UUID usuarioId, UUID projetoId) {
    return usuarioProjetoPapelRepository.existsByIdUsuarioIdAndIdProjetoId(usuarioId, projetoId);
  }

  @Transactional(readOnly = true)
  public Papel porCodigo(String codigo) {
    return papelRepository
        .findByCodigo(codigo)
        .orElseThrow(
            () ->
                new br.com.crudao.kanban.common.RecursoNaoEncontradoException(
                    "Papel nao encontrado: " + codigo));
  }
}
