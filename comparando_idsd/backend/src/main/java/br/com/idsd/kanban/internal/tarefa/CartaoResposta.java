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
 *
 * <p><b>As tres series sao anulaveis, inclusive {@code permanencia}</b> (ACH-05).
 * Permanencia e intervalo aberto, e tarefa em condicao terminal nao tem nenhum —
 * fabricar um faria {@code decorrido} continuar crescendo depois da conclusao, que
 * e o tipo de numero de que ninguem desconfia ate soma-lo.
 *
 * <p><b>{@code assumidaEm} e do cartao</b> (ACH-08). O contrato o prometia na saida
 * da tomada e em {@code estadoAtual} do {@code 409}, e nos dois casos o corpo <b>e</b>
 * o cartao — a omissao era da descricao do cartao, e nao das promessas.
 */
public record CartaoResposta(
        UUID id,
        String titulo,
        UUID etapaId,
        Condicao condicao,
        UUID raiaId,
        Responsavel responsavel,
        Instant assumidaEm,
        Long versao,
        Contagem esperaTomada,
        Impedido impedimento,
        Contagem permanencia) {

    /**
     * Quem respondeu pela tarefa, com o nome ja resolvido.
     *
     * <p>Sai como objeto e nao como identificador solto porque o cartao e lido por
     * uma tela que precisa <b>escrever o nome</b>, e devolver so o {@code id}
     * obrigaria o cliente a uma segunda consulta por cartao — o N+1 empurrado para
     * fora do servidor, que continua sendo N+1. Aqui ele nao existe: o nome vem na
     * mesma consulta que traz a tarefa.
     *
     * <p>Nao e recorte por pessoa: nomear quem responde pela tarefa <i>agora</i> e
     * estado corrente, e RN-014 proibe e o esquema impossibilita e outra coisa —
     * tempo agregado por pessoa, que exigiria coluna de pessoa na serie de tempo.
     */
    public record Responsavel(UUID id, String nome) {
    }

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
     * <p><b>O bloco de impedimento chega pronto, e nao em pedaços</b> (ACH-07). A
     * versao anterior recebia o motivo e casava-o aqui com o intervalo aberto de
     * {@code IMPEDIMENTO}: duas fontes para um bloco so, e a discordancia entre elas
     * saia como {@code motivo: null} — valor que a coluna proibe e que so pode
     * significar projecao incoerente — sem log e sem erro. Agora quem tem as duas
     * fontes as concilia antes de chamar, e o cartao recebe o bloco ja decidido. Na
     * criacao nao ha impedimento, e o parametro vem nulo.
     *
     * <p>{@code agora} chega de fora pela razao que ACH-11 registrou: ele precisa ser
     * <b>um so</b> para o board inteiro. Lido aqui, cada cartao mediria o
     * {@code decorrido} contra um relogio proprio, dentro de uma transacao
     * {@code readOnly} que existe declaradamente para que tudo veja o mesmo instante.
     *
     * <p>O nome do responsavel chega de fora pela mesma razao — ele vive em
     * {@code usuario} — e por uma segunda: quem monta o board resolve os nomes em
     * uma consulta so, e o cartao nao pode ter porta para buscar o seu, sob pena
     * de reintroduzir por dentro o N+1 que o criterio de desempenho proibe. Na
     * criacao a tarefa nasce sem responsavel (RN-006) e os dois vem nulos.
     */
    public static CartaoResposta de(
            Tarefa tarefa,
            Collection<IntervaloTarefa> abertos,
            Impedido impedimento,
            String nomeDoResponsavel,
            Instant agora) {

        return new CartaoResposta(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getEtapaId(),
                tarefa.getCondicao(),
                tarefa.getRaiaId(),
                tarefa.getResponsavelId() == null
                        ? null
                        : new Responsavel(
                                tarefa.getResponsavelId(),
                                nomeDoResponsavel == null ? NOME_INDISPONIVEL : nomeDoResponsavel),
                tarefa.getAssumidaEm(),
                tarefa.getVersao(),
                Contagem.de(primeiro(abertos, TipoDeIntervalo.ESPERA_TOMADA), agora),
                impedimento,
                Contagem.de(primeiro(abertos, TipoDeIntervalo.PERMANENCIA), agora));
    }

    /**
     * O nome de quem responde pela tarefa quando a linha do usuario nao esta la
     * (ACH-17).
     *
     * <p>A juncao externa que traz o nome e o reconhecimento de que a linha pode
     * faltar, e faltando ela o cartao saia {@code {id, nome: null}} — forma que o
     * contrato nao preve e que o cliente desreferencia. Sai um rotulo em vez do
     * nulo, e o {@code id} continua saindo: o vinculo e verdade da projecao mesmo
     * quando o nome nao esta ao alcance, e omitir o responsavel inteiro diria que a
     * tarefa esta sem dono, que e outra coisa.
     */
    static final String NOME_INDISPONIVEL = "Responsável indisponível";

    private static IntervaloTarefa primeiro(
            Collection<IntervaloTarefa> abertos, TipoDeIntervalo tipo) {
        return abertos.stream().filter(i -> i.getTipo() == tipo).findFirst().orElse(null);
    }
}
