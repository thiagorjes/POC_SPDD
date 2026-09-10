package br.com.idsd.kanban.internal.acesso;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso a {@link Usuario}. A busca e sempre pelo sujeito do token, nunca pelo e-mail. */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findBySubjectId(String subjectId);

    /**
     * Promove a administracao global <b>se e somente se</b> ainda nao houver
     * nenhuma, e devolve quantas linhas mudaram.
     *
     * <p>A unicidade de ADR-010 esta no proprio comando, e nao numa consulta
     * anterior a ele, porque consultar e depois atualizar em transacoes
     * concorrentes deixa as duas verem "nenhum admin global" e promoverem duas
     * pessoas. Aqui o {@code NOT EXISTS} e avaliado pelo banco na mesma escrita:
     * a segunda transacao atualiza zero linhas, e {@code 0} e a recusa que
     * SCN-021.1 exige.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Usuario u set u.adminGlobal = true
             where u.id = :id
               and u.adminGlobal = false
               and not exists (select 1 from Usuario outro where outro.adminGlobal = true)
            """)
    int promoverSeNaoHouverAdminGlobal(@Param("id") UUID id);
}
