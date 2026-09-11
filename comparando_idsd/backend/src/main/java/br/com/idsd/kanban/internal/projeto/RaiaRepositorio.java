package br.com.idsd.kanban.internal.projeto;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a {@link Raia}.
 *
 * <p>Nao ha metodo que agregue nada por raia, e nunca havera: a raia e divisao
 * visual, e a ausencia de {@code raia_id} no anel de projecao e o que torna a
 * agregacao por raia impossivel de escrever por engano.
 *
 * <p>Como em {@link EtapaRepositorio}, nao ha remocao — ela e logica.
 */
public interface RaiaRepositorio extends JpaRepository<Raia, UUID> {

    /** As raias vigentes do projeto, sem as arquivadas, na ordem declarada. */
    List<Raia> findByProjetoIdAndArquivadaEmIsNullOrderByOrdemAsc(UUID projetoId);
}
