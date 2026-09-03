package br.com.crudao.kanban.rbac;

import br.com.crudao.kanban.common.BusinessException;
import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.rbac.dto.MembroProjetoResponse;
import br.com.crudao.kanban.rbac.dto.PapelResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Associacao usuario x papel escopada ao projeto (RF-015). Papeis sao cumulativos (BDR-001). */
@Service
@RequiredArgsConstructor
public class UsuarioProjetoPapelService {

  private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
  private final UsuarioRepository usuarioRepository;
  private final PapelRepository papelRepository;

  @Transactional(readOnly = true)
  public List<MembroProjetoResponse> listarMembros(UUID projetoId) {
    List<UsuarioProjetoPapel> vinculos = usuarioProjetoPapelRepository.findByIdProjetoId(projetoId);
    Map<UUID, String> codigosPorPapelId =
        papelRepository.findAll().stream()
            .collect(Collectors.toMap(Papel::getId, Papel::getCodigo));

    Map<UUID, List<String>> papeisPorUsuario = new LinkedHashMap<>();
    for (UsuarioProjetoPapel vinculo : vinculos) {
      papeisPorUsuario
          .computeIfAbsent(vinculo.getId().getUsuarioId(), k -> new ArrayList<>())
          .add(codigosPorPapelId.get(vinculo.getId().getPapelId()));
    }

    Map<UUID, Usuario> usuarios =
        usuarioRepository.findAllById(papeisPorUsuario.keySet()).stream()
            .collect(Collectors.toMap(Usuario::getId, u -> u));

    List<MembroProjetoResponse> membros = new ArrayList<>(papeisPorUsuario.size());
    papeisPorUsuario.forEach(
        (usuarioId, papeis) -> {
          Usuario usuario = usuarios.get(usuarioId);
          if (usuario != null) {
            membros.add(
                new MembroProjetoResponse(
                    usuarioId, usuario.getNome(), usuario.getEmail(), papeis));
          }
        });
    return membros;
  }

  /** Catalogo fechado de papeis com suas permissoes — leitura da matriz da TL-09 (RF-013). */
  @Transactional(readOnly = true)
  public List<PapelResponse> listarPapeis() {
    return papelRepository.findAll().stream()
        .map(
            papel ->
                new PapelResponse(
                    papel.getCodigo(),
                    papel.getNome(),
                    papel.isProtegido(),
                    papel.getPermissoes().stream()
                        .map(Permissao::getCodigo)
                        .collect(Collectors.toSet())))
        .toList();
  }

  @Transactional
  public void associar(UUID projetoId, UUID usuarioId, String codigoPapel, Usuario autor) {
    Papel papel = papelPorCodigo(codigoPapel);
    exigirPapelDelegavel(papel, autor);

    if (!usuarioRepository.existsById(usuarioId)) {
      throw RecursoNaoEncontradoException.de("Usuario", usuarioId);
    }
    usuarioProjetoPapelRepository.save(
        new UsuarioProjetoPapel(usuarioId, projetoId, papel.getId(), autor.getId()));
  }

  @Transactional
  public void desassociar(UUID projetoId, UUID usuarioId, String codigoPapel, Usuario autor) {
    if (codigoPapel == null) {
      usuarioProjetoPapelRepository.deleteByIdUsuarioIdAndIdProjetoId(usuarioId, projetoId);
      return;
    }
    Papel papel = papelPorCodigo(codigoPapel);
    exigirPapelDelegavel(papel, autor);
    usuarioProjetoPapelRepository.deleteById(
        new UsuarioProjetoPapel.Id(usuarioId, projetoId, papel.getId()));
  }

  private Papel papelPorCodigo(String codigo) {
    if (!Papeis.TODOS.contains(codigo)) {
      throw new BusinessException(
          "PAPEL_INVALIDO",
          "O papel informado nao pertence ao catalogo de papeis do sistema.",
          HttpStatus.UNPROCESSABLE_ENTITY);
    }
    return papelRepository
        .findByCodigo(codigo)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Papel nao encontrado: " + codigo));
  }

  /** O papel {@code admin} e protegido: so o admin global pode conceder ou revogar (RN-006). */
  private void exigirPapelDelegavel(Papel papel, Usuario autor) {
    if (papel.isProtegido() && !autor.isAdminGlobal()) {
      throw new PermissaoNegadaException(
          "O papel administrativo do sistema nao pode ser delegado.");
    }
  }
}
