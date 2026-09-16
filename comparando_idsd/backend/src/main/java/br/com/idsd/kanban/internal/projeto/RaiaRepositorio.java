package br.com.idsd.kanban.internal.projeto;

import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Acesso a {@link Raia}.
 *
 * <p>Nao ha metodo declarado, e a ausencia e deliberada. O finder de raias
 * vigentes existia aqui sem consumidor algum — o primeiro nasce em TASK-06.1 —,
 * contrariando a regra que {@link EtapaRepositorio} institui na mesma entrega:
 * metodo nasce com o consumidor, e nao antes dele (ACH-13 da revisao de
 * TASK-02.1). O que o arquivo declara hoje e que a raia tem repositorio, nao
 * que ela ja tenha caminho de leitura.
 *
 * <p>A base e {@link Repository} pela mesma razao de {@link EtapaRepositorio}:
 * {@code JpaRepository} publicaria remocao fisica num agregado cuja remocao e
 * logica (ACH-01).
 *
 * <p>Nao ha nem havera metodo que agregue por raia. A garantia e do esquema e e
 * <b>estreita</b>: {@code tarefa} carrega {@code raia_id}, porque a raia e o
 * agrupamento visual do cartao; a serie de tempo nao carrega, e e isso que
 * torna a agregacao por raia inescrivivel — ver {@link Raia}.
 */
public interface RaiaRepositorio extends Repository<Raia, UUID> {

    /**
     * A raia existe, e vigente e <b>pertence a este projeto</b>?
     *
     * <p>Nasce com o consumidor, que e a criacao de tarefa — ACH-01 da revisao de
     * TASK-02.5. A chave estrangeira de {@code tarefa.raia_id} e global e nao
     * escopada por projeto, de modo que ela aceita raia de qualquer projeto e
     * raia arquivada; pertencimento e vigencia sao regra, e regra nao cabe numa
     * FK. Sem esta pergunta, quem escreve no projeto A cria tarefa apontando para
     * raia do projeto B, e o identificador alheio volta no cartao.
     *
     * <p>Devolve booleano e nao a entidade de proposito: quem chama decide sobre
     * o pertencimento e nao precisa do nome nem da ordem da raia, e carregar a
     * linha inteira daria acesso a dado de outro projeto no caminho que existe
     * justamente para recusa-lo.
     */
    boolean existsByIdAndProjetoIdAndArquivadaEmIsNull(UUID id, UUID projetoId);
}
