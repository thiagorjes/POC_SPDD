package br.com.idsd.kanban.internal.projeto;

import br.com.idsd.kanban.internal.acesso.Usuario;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Vinculo de uma pessoa com um projeto, com os papeis que ela exerce ali.
 *
 * <p>E a unidade de autorizacao do sistema (BDR-001): papel nao e global, e o
 * mesmo sujeito pode ser {@code project_admin} num projeto e {@code gestor} em
 * outro. Por isso o provedor de identidade nao carrega papel no token — quem
 * responde "o que esta pessoa pode aqui" e esta tabela (ADR-003).
 */
@Entity
@Table(name = "participacao")
public class Participacao {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    /**
     * Papeis acumulaveis. {@code Set} e nao {@code List}: a chave primaria da
     * tabela e {@code (participacao_id, papel)}, entao papel repetido nao e
     * estado possivel.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "participacao_papel",
            joinColumns = @JoinColumn(name = "participacao_id", nullable = false))
    @Column(name = "papel", nullable = false)
    @Convert(converter = Papel.ConversorJpa.class)
    private Set<Papel> papeis = EnumSet.noneOf(Papel.class);

    protected Participacao() {
        // exigido pelo JPA
    }

    public Participacao(Usuario usuario, Projeto projeto, Collection<Papel> papeis) {
        this.id = UUID.randomUUID();
        this.usuario = usuario;
        this.projeto = projeto;
        this.criadaEm = Instant.now();
        this.papeis = papeis.isEmpty() ? EnumSet.noneOf(Papel.class) : EnumSet.copyOf(papeis);
    }

    public void conceder(Papel papel) {
        papeis.add(papel);
    }

    public void revogar(Papel papel) {
        papeis.remove(papel);
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Projeto getProjeto() {
        return projeto;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Set<Papel> getPapeis() {
        return java.util.Collections.unmodifiableSet(papeis);
    }
}
