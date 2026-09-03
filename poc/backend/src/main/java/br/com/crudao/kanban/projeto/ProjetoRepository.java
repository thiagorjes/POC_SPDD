package br.com.crudao.kanban.projeto;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {

  boolean existsByNomeIgnoreCase(String nome);

  List<Projeto> findByIdInOrderByNomeAsc(Collection<UUID> ids);

  List<Projeto> findAllByOrderByNomeAsc();
}
