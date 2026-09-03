package br.com.crudao.kanban.rbac;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

  Optional<Usuario> findByKeycloakSub(String keycloakSub);

  Optional<Usuario> findByEmailIgnoreCase(String email);

  List<Usuario> findAllByOrderByNomeAsc();
}
