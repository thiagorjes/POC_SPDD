package br.com.crudao.kanban.projeto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjetoToggleRepository extends JpaRepository<ProjetoToggle, ProjetoToggleId> {

  List<ProjetoToggle> findByIdProjetoId(UUID projetoId);

  Optional<ProjetoToggle> findByIdProjetoIdAndIdChave(UUID projetoId, ChaveToggle chave);
}
