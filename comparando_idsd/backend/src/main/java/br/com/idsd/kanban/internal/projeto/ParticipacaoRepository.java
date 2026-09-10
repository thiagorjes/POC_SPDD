package br.com.idsd.kanban.internal.projeto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a {@link Participacao}.
 *
 * <p>Nao ha metodo que devolva participacao por usuario sem projeto <i>e</i>
 * agregue nada: leitura de tempo por pessoa e proibida estruturalmente (RN-014),
 * e a projecao que a suporta nem sequer tem coluna de pessoa.
 */
public interface ParticipacaoRepository extends JpaRepository<Participacao, UUID> {

    Optional<Participacao> findByUsuarioIdAndProjetoId(UUID usuarioId, UUID projetoId);

    /** Os projetos de que a pessoa participa — a lista de RF-002 sai daqui. */
    List<Participacao> findByUsuarioId(UUID usuarioId);
}
