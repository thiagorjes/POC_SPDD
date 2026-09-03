package br.com.crudao.kanban.rbac;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioProjetoPapelRepository
    extends JpaRepository<UsuarioProjetoPapel, UsuarioProjetoPapel.Id> {

  /** Codigos das permissoes efetivas do usuario naquele projeto (uniao dos papeis acumulados). */
  @Query(
      """
      SELECT DISTINCT pe.codigo
        FROM UsuarioProjetoPapel upp
        JOIN Papel p ON p.id = upp.id.papelId
        JOIN p.permissoes pe
       WHERE upp.id.usuarioId = :usuarioId
         AND upp.id.projetoId = :projetoId
      """)
  List<String> findCodigosPermissao(
      @Param("usuarioId") UUID usuarioId, @Param("projetoId") UUID projetoId);

  @Query(
      """
      SELECT p.codigo
        FROM UsuarioProjetoPapel upp
        JOIN Papel p ON p.id = upp.id.papelId
       WHERE upp.id.usuarioId = :usuarioId
         AND upp.id.projetoId = :projetoId
      """)
  List<String> findCodigosPapel(
      @Param("usuarioId") UUID usuarioId, @Param("projetoId") UUID projetoId);

  List<UsuarioProjetoPapel> findByIdProjetoId(UUID projetoId);

  List<UsuarioProjetoPapel> findByIdUsuarioId(UUID usuarioId);

  boolean existsByIdUsuarioIdAndIdProjetoId(UUID usuarioId, UUID projetoId);

  @Query("SELECT DISTINCT upp.id.projetoId FROM UsuarioProjetoPapel upp WHERE upp.id.usuarioId = :usuarioId")
  List<UUID> findProjetoIdsDoUsuario(@Param("usuarioId") UUID usuarioId);

  void deleteByIdUsuarioIdAndIdProjetoId(UUID usuarioId, UUID projetoId);
}
