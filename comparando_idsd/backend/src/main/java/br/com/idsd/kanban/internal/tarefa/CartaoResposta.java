package br.com.idsd.kanban.internal.tarefa;

import br.com.idsd.kanban.internal.tempo.IntervaloTarefa;
import br.com.idsd.kanban.internal.tempo.TipoDeIntervalo;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * O cartao da tarefa, como o contrato o congela
 * ({@code contracts/board-e-tarefas.md}).
 *
 * <p><b>As tres dimensoes de RN-002 saem em campos separados</b>, e essa e a razao
 * de a forma ser unica e reusada pelo board: a etapa, a {@code condicao} e o
 * impedimento. {@code condicao} <b>nunca</b> vale {@code IMPEDIDA} — o valor nao
 * existe em {@link Condicao} —, e {@code impedimento} coexiste com qualquer
 * condicao nao terminal (RN-032).
 *
 * <p><b>As tres series de tempo nunca sao somadas</b> (RN-008). Elas saem lado a
 * lado, cada uma com o instante em que comecou e quanto ja correu, e o cartao nao
 * oferece campo algum que as agregue — {@code esperaTomada} e {@code impedimento}
 * podem estar preenchidos ao mesmo tempo, e somar os dois seria contar o mesmo
 * periodo duas vezes (SCN-003.3).
 *
 * <p>{@code decorrido} e calculado na leitura e nao lido da projecao: a coluna
 * {@code duracao} de {@code intervalo_tarefa} e gerada e so existe para intervalo
 * fechado. Estes aqui estao abertos.
 */
public record CartaoResposta(
        UUID id,
        String titulo,
        UUID etapaId,
        Condicao condicao,
        UUID raiaId,
        UUID responsavel,
        Long versao,
        Contagem esperaTomada,
        Impedido impedimento,
        Contagem permanencia) {

    /** Uma serie em curso: desde quando, e quanto ja correu em segundos. */
    public record Contagem(Instant desde, long decorrido) {

        static Contagem de(IntervaloTarefa intervalo, Instant agora) {
            return intervalo == null
                    ? null
                    : new Contagem(
                            intervalo.getInicio(),
                            Duration.between(intervalo.getInicio(), agora).toSeconds());
        }
    }

    /** A terceira dimensao, quando ha impedimento aberto. */
    public record Impedido(Instant desde, long decorrido, String motivo) {
    }

    /**
     * Monta o cartao a partir da tarefa e das series abertas dela.
     *
     * <p>O motivo chega de fora porque ele nao esta na serie de tempo: ele vive em
     * {@link Impedimento}, e quem le o impedimento e quem o tem. Na criacao nao ha
     * nenhum, e o parametro vem nulo.
     */
    public static CartaoResposta de(
            Tarefa tarefa, Collection<IntervaloTarefa> abertos, String motivoDoImpedimento) {
        Instant agora = Instant.now();
        IntervaloTarefa impedimento = primeiro(abertos, TipoDeIntervalo.IMPEDIMENTO);

        return new CartaoResposta(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getEtapaId(),
                tarefa.getCondicao(),
                tarefa.getRaiaId(),
                tarefa.getResponsavelId(),
                tarefa.getVersao(),
                Contagem.de(primeiro(abertos, TipoDeIntervalo.ESPERA_TOMADA), agora),
                impedimento == null
                        ? null
                        : new Impedido(
                                impedimento.getInicio(),
                                Duration.between(impedimento.getInicio(), agora).toSeconds(),
                                motivoDoImpedimento),
                Contagem.de(primeiro(abertos, TipoDeIntervalo.PERMANENCIA), agora));
    }

    private static IntervaloTarefa primeiro(
            Collection<IntervaloTarefa> abertos, TipoDeIntervalo tipo) {
        return abertos.stream().filter(i -> i.getTipo() == tipo).findFirst().orElse(null);
    }
}
