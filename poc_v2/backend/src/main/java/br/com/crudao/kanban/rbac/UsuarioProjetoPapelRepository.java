package br.com.crudao.kanban.rbac;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioProjetoPapelRepository
    extends JpaRepository<UsuarioProjetoPapel, UsuarioProjetoPapelId> {

  @Query(
      """
      SELECT DISTINCT perm.codigo
      FROM UsuarioProjetoPapel upp
      JOIN Papel p ON p.id = upp.id.papelId
      JOIN p.permissoes perm
      WHERE upp.id.usuarioId = :usuarioId AND upp.id.projetoId = :projetoId
      """)
  Set<String> buscarPermissoesEfetivas(
      @Param("usuarioId") UUID usuarioId, @Param("projetoId") UUID projetoId);

  @Query(
      """
      SELECT p.codigo
      FROM UsuarioProjetoPapel upp
      JOIN Papel p ON p.id = upp.id.papelId
      WHERE upp.id.usuarioId = :usuarioId AND upp.id.projetoId = :projetoId
      """)
  Set<String> buscarCodigosDePapel(
      @Param("usuarioId") UUID usuarioId, @Param("projetoId") UUID projetoId);

  boolean existsByIdUsuarioIdAndIdProjetoId(UUID usuarioId, UUID projetoId);

  List<UsuarioProjetoPapel> findByIdProjetoId(UUID projetoId);

  List<UsuarioProjetoPapel> findByIdUsuarioId(UUID usuarioId);

  @Query(
      "SELECT DISTINCT upp.id.projetoId FROM UsuarioProjetoPapel upp WHERE upp.id.usuarioId = :usuarioId")
  List<UUID> buscarProjetosDoUsuario(@Param("usuarioId") UUID usuarioId);

  void deleteByIdUsuarioIdAndIdProjetoIdAndIdPapelId(UUID usuarioId, UUID projetoId, UUID papelId);
}
