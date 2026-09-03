package br.com.crudao.kanban.raia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RaiaRepository extends JpaRepository<Raia, UUID> {

  List<Raia> findByProjetoIdOrderByOrdemAsc(UUID projetoId);

  Optional<Raia> findByProjetoIdAndPadraoTrue(UUID projetoId);

  @Query("SELECT COALESCE(MAX(r.ordem), -1) FROM Raia r WHERE r.projetoId = :projetoId")
  int findMaiorOrdem(@Param("projetoId") UUID projetoId);

  long countByProjetoId(UUID projetoId);
}
