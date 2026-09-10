package br.com.idsd.kanban.internal.acesso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Pessoa conhecida do sistema.
 *
 * <p>O vinculo com o provedor e o {@code subject_id} do token, jamais o e-mail:
 * e-mail e mutavel no provedor e, em realm com autocadastro, atribuivel por quem
 * se registra. Nome e e-mail sao espelho do token, atualizados a cada entrada.
 *
 * <p>Nao ha senha nem cadastro local (ADR-006). A conta nasce por
 * autoprovisionamento na primeira entrada.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "subject_id", nullable = false, unique = true)
    private String subjectId;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "email", nullable = false)
    private String email;

    /** Alcance de escopo, nao vinculo de projeto (ADR-010). */
    @Column(name = "admin_global", nullable = false)
    private boolean adminGlobal;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Usuario() {
        // exigido pelo JPA
    }

    public Usuario(String subjectId, String nome, String email) {
        this.id = UUID.randomUUID();
        this.subjectId = subjectId;
        this.nome = nome;
        this.email = email;
        this.adminGlobal = false;
        this.criadoEm = Instant.now();
    }

    /** Reespelha do token o que o provedor pode ter mudado desde a ultima entrada. */
    public void espelharDoToken(String nome, String email) {
        this.nome = nome;
        this.email = email;
    }

    /**
     * Reflete no objeto em memoria a promocao ja gravada.
     *
     * <p>Nao ha setter livre de propriedade: a promocao a administracao global e
     * feita pela atualizacao condicional de {@link UsuarioRepository}, que e
     * quem garante a unicidade de ADR-010 no mesmo comando que promove. Este
     * metodo existe so para que o objeto ja carregado nao continue afirmando o
     * estado anterior, e nao para conceder o alcance.
     */
    void refletirPromocaoGravada() {
        this.adminGlobal = true;
    }

    public UUID getId() {
        return id;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAdminGlobal() {
        return adminGlobal;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
