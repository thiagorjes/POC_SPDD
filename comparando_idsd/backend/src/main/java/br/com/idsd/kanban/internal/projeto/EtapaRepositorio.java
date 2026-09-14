package br.com.idsd.kanban.internal.projeto;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Acesso a {@link Etapa}.
 *
 * <p>O nome em portugues diverge do {@code ...Repository} dos tres repositorios
 * do anel de configuracao, e a divergencia nao e escolha: a suite congelada
 * declara {@code EtapaRepositorio} como colaborador de {@code EtapaService}, e
 * ela esta fora do alcance de quem implementa. {@link RaiaRepositorio} segue o
 * mesmo nome para que o par nao fique dividido entre duas convencoes.
 *
 * <p><b>A base e {@link Repository} e nao {@code JpaRepository}, e isso e a
 * garantia.</b> Ate 2026-09-14 este arquivo estendia {@code JpaRepository},
 * que publica sete assinaturas de remocao fisica ({@code delete},
 * {@code deleteById}, {@code deleteAll}, {@code deleteAllById},
 * {@code deleteAllInBatch}, {@code deleteAllByIdInBatch},
 * {@code deleteInBatch}) — enquanto este mesmo javadoc afirmava que nenhuma
 * existe. Comentario nao revoga assinatura publicada (ACH-01 da revisao de
 * TASK-02.1). {@code Repository} e interface marcadora: so existe aqui o que
 * estiver declarado abaixo, e nenhuma remocao esta.
 *
 * <p>Isso importa porque a remocao de etapa e <b>logica</b>, pelo
 * {@code arquivada_em} da propria entidade: a serie de tempo por etapa segue o
 * identificador para sempre, e um {@code delete} alcancavel daqui e tudo de que
 * alguem precisaria para destrui-la sem que nenhum teste ficasse vermelho.
 *
 * <p>Escrita e {@link SubstituicaoDeFluxo}, o fragmento que nasceu em TASK-02.2
 * junto do tipo que ela recebe. Ele e uma interface a parte, e nao metodos soltos
 * aqui, porque tem implementacao propria — {@code EtapaRepositorioImpl} — e porque
 * mantem visivel, num arquivo so, tudo o que escreve etapa no sistema.
 */
public interface EtapaRepositorio extends Repository<Etapa, UUID>, SubstituicaoDeFluxo {

    /**
     * O fluxo vigente: sem as arquivadas, na ordem declarada. E o unico caminho
     * de leitura declarado aqui, e e exatamente o que o indice unico parcial
     * atende.
     */
    List<Etapa> findByProjetoIdAndArquivadaEmIsNullOrderByOrdemAsc(UUID projetoId);
}
