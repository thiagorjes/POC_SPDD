package br.com.idsd.kanban.internal.projeto;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso a {@link Projeto}. */
public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {
}
