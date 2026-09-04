package br.com.crudao.kanban.rbac;

import br.com.crudao.kanban.common.BusinessException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.rbac.dto.MembroProjetoResponse;
import br.com.crudao.kanban.security.PermissaoGuard;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Associacao usuario x projeto x papel (RF-015). Papeis sao acumulaveis e o catalogo e fechado
 * (BDR-001): o papel {@code admin} e global e protegido, nao pode ser atribuido por projeto
 * (RN-006).
 */
@Service
@RequiredArgsConstructor
public class UsuarioProjetoPapelService {

  private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
  private final UsuarioRepository usuarioRepository;
  private final PapelRepository papelRepository;
  private final PermissaoGuard permissaoGuard;

  /** Membros do projeto com os papeis acumulados de cada um. */
  @Transactional(readOnly = true)
  public List<MembroProjetoResponse> listarMembros(UUID projetoId) {
    permissaoGuard.exigir(CodigoPermissao.USUARIO_ASSOCIAR, projetoId);

    Map<UUID, List<String>> papeisPorUsuario = new LinkedHashMap<>();
    for (UsuarioProjetoPapel associacao :
        usuarioProjetoPapelRepository.findByIdProjetoId(projetoId)) {
      String codigo =
          papelRepository
              .findById(associacao.getId().getPapelId())
              .map(Papel::getCodigo)
              .orElseThrow(() -> new RecursoNaoEncontradoException("Papel nao encontrado."));
      papeisPorUsuario
          .computeIfAbsent(associacao.getId().getUsuarioId(), chave -> new ArrayList<>())
          .add(codigo);
    }

    List<MembroProjetoResponse> membros = new ArrayList<>(papeisPorUsuario.size());
    for (Map.Entry<UUID, List<String>> entrada : papeisPorUsuario.entrySet()) {
      Usuario usuario =
          usuarioRepository
              .findById(entrada.getKey())
              .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado."));
      membros.add(
          new MembroProjetoResponse(
              usuario.getId(), usuario.getNome(), usuario.getEmail(), entrada.getValue()));
    }
    return membros;
  }

  /** Atribui um papel do catalogo ao usuario no projeto. Idempotente. */
  @Transactional
  public void associar(UUID projetoId, UUID usuarioId, String codigoPapel, Usuario autor) {
    permissaoGuard.exigir(CodigoPermissao.USUARIO_ASSOCIAR, projetoId);
    permissaoGuard.exigirProjetoAtivo(projetoId);

    Papel papel = buscarPapel(codigoPapel);
    if (papel.isGlobal() || papel.isProtegido()) {
      throw new BusinessException(
          "PAPEL_PROTEGIDO", "O papel informado nao pode ser atribuido no escopo de um projeto.");
    }
    if (!usuarioRepository.existsById(usuarioId)) {
      throw new RecursoNaoEncontradoException("Usuario nao encontrado.");
    }
    UsuarioProjetoPapelId chave = new UsuarioProjetoPapelId(usuarioId, projetoId, papel.getId());
    if (usuarioProjetoPapelRepository.existsById(chave)) {
      return;
    }
    usuarioProjetoPapelRepository.save(
        new UsuarioProjetoPapel(usuarioId, projetoId, papel.getId(), autor.getId()));
  }

  /** Remove um papel do usuario no projeto. Idempotente. */
  @Transactional
  public void desassociar(UUID projetoId, UUID usuarioId, String codigoPapel) {
    permissaoGuard.exigir(CodigoPermissao.USUARIO_ASSOCIAR, projetoId);
    permissaoGuard.exigirProjetoAtivo(projetoId);

    Papel papel = buscarPapel(codigoPapel);
    usuarioProjetoPapelRepository.deleteByIdUsuarioIdAndIdProjetoIdAndIdPapelId(
        usuarioId, projetoId, papel.getId());
  }

  private Papel buscarPapel(String codigoPapel) {
    return papelRepository
        .findByCodigo(codigoPapel)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Papel nao encontrado."));
  }
}
