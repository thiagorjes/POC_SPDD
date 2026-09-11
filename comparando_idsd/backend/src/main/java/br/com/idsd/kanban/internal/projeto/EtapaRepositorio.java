package br.com.idsd.kanban.internal.projeto;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a {@link Etapa}.
 *
 * <p>O nome em portugues diverge do {@code ...Repository} dos tres repositorios
 * do anel de configuracao, e a divergencia nao e escolha: a suite congelada
 * declara {@code EtapaRepositorio} como colaborador de {@code EtapaService}, e
 * ela esta fora do alcance de quem implementa. {@link RaiaRepositorio} segue o
 * mesmo nome para que o par nao fique dividido entre duas convencoes.
 *
 * <p>Nao ha metodo que apague etapa. A remocao e logica, pelo
 * {@code arquivada_em} da propria entidade: a serie de tempo por etapa segue o
 * identificador para sempre, e um {@code delete} exposto aqui e tudo de que
 * alguem precisaria para destrui-la sem que nenhum teste ficasse vermelho.
 */
public interface EtapaRepositorio extends JpaRepository<Etapa, UUID> {

    /**
     * O fluxo vigente: sem as arquivadas, na ordem declarada. E o unico caminho
     * de leitura declarado aqui, e e exatamente o que o indice unico parcial
     * atende — os demais que a configuracao do fluxo vier a precisar nascem com
     * o consumidor, e nao antes dele.
     */
    List<Etapa> findByProjetoIdAndArquivadaEmIsNullOrderByOrdemAsc(UUID projetoId);
}
