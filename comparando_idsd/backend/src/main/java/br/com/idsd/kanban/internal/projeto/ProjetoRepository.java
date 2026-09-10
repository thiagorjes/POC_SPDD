package br.com.idsd.kanban.internal.projeto;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso a {@link Projeto}. */
public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {

    /**
     * Projetos que o sujeito alcanca, em uma ida ao banco.
     *
     * <p>Os dois sujeitos saem da mesma consulta: o participante pelo
     * {@code part.id is not null}, e a administracao global pelo parametro, que
     * dispensa a participacao (RN-035). Sem o parametro, seriam duas consultas ou
     * um {@code inner join} que esconderia metade dos casos.
     *
     * <p>Os dois {@code left join} sao explicitos de proposito. O primeiro carrega
     * a condicao do usuario para dentro do {@code ON} — leva-la ao {@code WHERE}
     * transformaria o join em interno e faria a administracao global ver apenas o
     * que ela participa. O segundo abre os papeis em linhas, e e o que evita a
     * consulta por projeto que o criterio de aceite proibe.
     *
     * <p>A ordenacao por nome existe para que a relacao seja estavel entre
     * requisicoes; sem ela o agrupamento em memoria herdaria a ordem do plano.
     */
    @Query("""
            select new br.com.idsd.kanban.internal.projeto.ProjetoConsulta(
                       p.id, p.nome, p.descricao, part.id, pp)
              from Projeto p
              left join Participacao part
                     on part.projeto = p and part.usuario.id = :usuarioId
              left join part.papeis pp
             where :adminGlobal = true or part.id is not null
             order by p.nome, p.id
            """)
    List<ProjetoConsulta> visiveisPara(
            @Param("usuarioId") UUID usuarioId, @Param("adminGlobal") boolean adminGlobal);

    /**
     * O mesmo alcance, para um projeto so.
     *
     * <p>Lista vazia significa <b>duas</b> coisas indistinguiveis aqui — projeto
     * inexistente ou projeto que o sujeito nao alcanca — e essa indistincao e
     * deliberada: as duas produzem {@code 404}, e uma consulta que as separasse
     * criaria a chance de a borda vazar a diferenca (SCN-002.3).
     */
    @Query("""
            select new br.com.idsd.kanban.internal.projeto.ProjetoConsulta(
                       p.id, p.nome, p.descricao, part.id, pp)
              from Projeto p
              left join Participacao part
                     on part.projeto = p and part.usuario.id = :usuarioId
              left join part.papeis pp
             where p.id = :projetoId
               and (:adminGlobal = true or part.id is not null)
            """)
    List<ProjetoConsulta> alcancadoPor(
            @Param("usuarioId") UUID usuarioId,
            @Param("projetoId") UUID projetoId,
            @Param("adminGlobal") boolean adminGlobal);
}
