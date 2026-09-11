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
     * O vinculo do sujeito com <b>um</b> projeto, existindo ele ou nao.
     *
     * <p>Esta consulta <b>nao</b> filtra por alcance, e a diferenca em relacao a
     * {@link #visiveisPara} e deliberada. A relacao precisa filtrar, porque o que
     * ela devolve <i>e</i> o alcance. O detalhe nao: quem decide se o sujeito
     * alcanca o projeto e o {@link ResolvedorDePermissao}, e reproduzir a regra
     * tambem aqui criaria a segunda fonte da decisao — foi assim que a garantia
     * de {@code 404} migrou para uma clausula {@code where} que nao menciona
     * regra nenhuma.
     *
     * <p>Lista vazia significa portanto uma coisa so: <b>o projeto nao existe</b>.
     * Projeto que existe e que o sujeito nao alcanca volta com
     * {@code participacaoId} nulo, e e o resolvedor que o transforma em
     * {@code 404} — o mesmo codigo dos dois casos, por SCN-002.3, mas por
     * decisao declarada e nao por coincidencia de consulta.
     */
    @Query("""
            select new br.com.idsd.kanban.internal.projeto.ProjetoConsulta(
                       p.id, p.nome, p.descricao, part.id, pp)
              from Projeto p
              left join Participacao part
                     on part.projeto = p and part.usuario.id = :usuarioId
              left join part.papeis pp
             where p.id = :projetoId
            """)
    List<ProjetoConsulta> alcancadoPor(
            @Param("usuarioId") UUID usuarioId, @Param("projetoId") UUID projetoId);
}
