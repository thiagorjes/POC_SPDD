package br.com.idsd.kanban.internal.acesso;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso a {@link Usuario}. A busca e sempre pelo sujeito do token, nunca pelo e-mail. */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findBySubjectId(String subjectId);
}
