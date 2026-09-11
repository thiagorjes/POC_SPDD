package br.com.idsd.kanban.suporte;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * Conta idas ao banco durante um trecho, para verificar ausencia de N+1.
 *
 * <p><b>O que se afirma nao e um numero.</b> Afirmar "exatamente 3 consultas"
 * produz teste que quebra a cada mudanca inocua e que, quebrando por nada, acaba
 * desabilitado — e teste desabilitado nao verifica coisa alguma. O que importa
 * nao e quantas consultas a rota faz, e sim que a contagem <b>nao cresca com o
 * volume de dados</b>: essa e literalmente a definicao de N+1, e e invariante a
 * refatoracao honesta. Quem usar este arnes mede o mesmo caminho duas vezes,
 * sobre massas diferentes, e afirma que as duas contagens sao iguais — nunca que
 * alguma delas vale um numero.
 *
 * <p>Existe porque o criterio de ausencia de N+1 vinha sendo marcado por
 * inspecao do JPQL. Criterio que nao pode falhar nao e criterio, e N+1 e
 * justamente o defeito que reaparece em silencio: ele entra quando alguem troca
 * uma projecao por navegacao de associacao, o resultado continua correto e todo
 * teste continua verde — so fica lento.
 *
 * <p>Origem: ACH-09 da revisao de TASK-01.5.
 */
public final class ContagemDeConsultas {

    private final Statistics estatisticas;

    public ContagemDeConsultas(EntityManagerFactory fabrica) {
        this.estatisticas = fabrica.unwrap(SessionFactory.class).getStatistics();
        this.estatisticas.setStatisticsEnabled(true);
    }

    /** Quantas consultas o trecho dispara. */
    public long durante(Trecho trecho) throws Exception {
        estatisticas.clear();
        trecho.executar();
        return estatisticas.getPrepareStatementCount();
    }

    /** Um trecho de codigo que vai ao banco. */
    @FunctionalInterface
    public interface Trecho {
        void executar() throws Exception;
    }
}
