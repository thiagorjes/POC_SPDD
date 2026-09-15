package br.com.idsd.kanban.internal.tempo;

import br.com.idsd.kanban.internal.tarefa.EventoTarefa;
import br.com.idsd.kanban.internal.tarefa.Tarefa;
import br.com.idsd.kanban.internal.tarefa.TipoDeEvento;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Traduz evento em efeito sobre as tres series de tempo.
 *
 * <p>O catalogo de {@code data-model.md} secao 4 esta transcrito uma unica vez,
 * na tabela abaixo, e essa unicidade e o ponto do arquivo: e o mesmo catalogo que
 * a escrita usa ao vivo e que a reconstrucao reexecuta sobre o log. Duas copias
 * fariam a projecao reconstruida divergir da gravada por diferenca de tabela, e a
 * divergencia apareceria como defeito da rotina de reconstrucao, que seria o
 * ultimo lugar onde alguem procuraria.
 *
 * <p>O tempo de todo intervalo aberto ou fechado aqui e o {@code ocorridoEm} do
 * evento, e nunca o relogio local. Fechar com um instante e abrir com outro na
 * mesma transicao deixaria um vao entre as duas contagens; e reler o relogio na
 * reconstrucao produziria uma projecao que nao tem nada a ver com o log.
 */
@Service
public class AplicadorDeIntervalos {

    /**
     * O catalogo.
     *
     * <p><b>{@code TAREFA_MOVIDA} nao fecha {@code IMPEDIMENTO}</b>, e e a linha
     * mais facil de errar: RN-009 manda o impedimento acompanhar a tarefa sem que
     * a transicao encerre nem reinicie a contagem (SCN-006.3).
     *
     * <p>{@code IMPEDIMENTO_ANOTADO} nao aparece porque nao tem efeito: a segunda
     * sinalizacao de RN-010 anexa a anotacao sem reiniciar contagem nenhuma
     * (SCN-009.3).
     */
    private static final Map<TipoDeEvento, Efeito> CATALOGO = catalogo();

    private final IntervaloTarefaRepositorio intervalos;

    public AplicadorDeIntervalos(IntervaloTarefaRepositorio intervalos) {
        this.intervalos = intervalos;
    }

    private static Map<TipoDeEvento, Efeito> catalogo() {
        var mapa = new EnumMap<TipoDeEvento, Efeito>(TipoDeEvento.class);
        mapa.put(TipoDeEvento.TAREFA_CRIADA, Efeito.abrindo(
                TipoDeIntervalo.PERMANENCIA, TipoDeIntervalo.ESPERA_TOMADA));
        mapa.put(TipoDeEvento.TAREFA_ASSUMIDA, Efeito.fechando(TipoDeIntervalo.ESPERA_TOMADA));
        mapa.put(TipoDeEvento.TAREFA_DEVOLVIDA, Efeito.abrindo(TipoDeIntervalo.ESPERA_TOMADA));
        mapa.put(TipoDeEvento.TAREFA_MOVIDA, new Efeito(
                Set.of(TipoDeIntervalo.PERMANENCIA, TipoDeIntervalo.ESPERA_TOMADA),
                false,
                List.of(TipoDeIntervalo.PERMANENCIA, TipoDeIntervalo.ESPERA_TOMADA)));
        mapa.put(TipoDeEvento.IMPEDIMENTO_ABERTO, Efeito.abrindo(TipoDeIntervalo.IMPEDIMENTO));
        mapa.put(TipoDeEvento.IMPEDIMENTO_ANOTADO, Efeito.nenhum());
        mapa.put(TipoDeEvento.IMPEDIMENTO_RESOLVIDO, Efeito.fechando(TipoDeIntervalo.IMPEDIMENTO));
        mapa.put(TipoDeEvento.TAREFA_CONCLUIDA, new Efeito(Set.of(), true, List.of()));
        mapa.put(TipoDeEvento.TAREFA_ENCERRADA_SEM_CONCLUSAO, Efeito.fechando(
                TipoDeIntervalo.PERMANENCIA, TipoDeIntervalo.ESPERA_TOMADA));
        mapa.put(TipoDeEvento.TAREFA_REABERTA, Efeito.abrindo(
                TipoDeIntervalo.PERMANENCIA, TipoDeIntervalo.ESPERA_TOMADA));
        return mapa;
    }

    /**
     * Aplica o efeito do evento sobre as series da tarefa.
     *
     * <p>Fecha antes de abrir, e a ordem importa: {@code TAREFA_MOVIDA} abre
     * {@code PERMANENCIA} no destino depois de fechar a da origem, e o indice
     * unico parcial sobre {@code (tarefa_id, tipo)} com {@code fim IS NULL}
     * recusaria os dois abertos ao mesmo tempo. A restricao esta no banco, de
     * modo que inverter a ordem nao produz projecao errada — produz erro, que e
     * o desfecho correto.
     *
     * <p>A etapa do intervalo aberto e a de destino do evento quando ele a
     * declara, e a corrente da tarefa quando nao. Em {@code IMPEDIMENTO} ela e
     * instantaneo da abertura e nao vinculo: sem ela, o bloco por etapa de RF-016
     * cairia todo num grupo nulo.
     *
     * <p>O episodio vem do <b>evento</b> e nao da tarefa. Na reabertura os dois
     * divergem por um instante — o evento ja e do episodio novo e a projecao
     * ainda nao —, e e o evento que manda (RN-019).
     */
    public IntervalosAplicados aplicar(EventoTarefa evento, Tarefa tarefa) {
        Efeito efeito = efeitoDe(evento.getTipo());

        var fechados = new ArrayList<IntervaloTarefa>();
        for (IntervaloTarefa aberto : intervalos.abertosDe(evento.getTarefaId())) {
            if (efeito.fechaTodos() || efeito.fecha().contains(aberto.getTipo())) {
                aberto.fechar(evento.getOcorridoEm());
                fechados.add(intervalos.save(aberto));
            }
        }

        var abertos = new ArrayList<IntervaloTarefa>();
        for (TipoDeIntervalo tipo : efeito.abre()) {
            abertos.add(intervalos.save(new IntervaloTarefa(
                    evento.getTarefaId(),
                    evento.getProjetoId(),
                    evento.getEtapaDestinoId() != null
                            ? evento.getEtapaDestinoId()
                            : tarefa.getEtapaId(),
                    tipo,
                    evento.getEpisodio(),
                    evento.getOcorridoEm())));
        }

        return new IntervalosAplicados(List.copyOf(fechados), List.copyOf(abertos));
    }

    /**
     * O catalogo, para quem reexecuta o log em vez de aplica-lo ao vivo.
     *
     * <p>Visivel ao pacote e nao publico: fora de {@code internal/tempo} nao ha
     * consumidor legitimo do efeito em separado — quem precisa dele precisa da
     * aplicacao inteira, por {@link #aplicar}. O unico que le so o catalogo e o
     * {@link ReconstrutorDeProjecao}, que nao pode reaplicar linha a linha porque
     * reescreve a projecao em bloco.
     */
    static Efeito efeitoDe(TipoDeEvento tipo) {
        Efeito efeito = CATALOGO.get(tipo);
        if (efeito == null) {
            throw new IllegalStateException("tipo de evento fora do catalogo de intervalos: " + tipo);
        }
        return efeito;
    }

    /**
     * O que a aplicacao produziu.
     *
     * <p>Existe por causa de {@code Impedimento.intervalo_id}: a abertura do
     * impedimento precisa do identificador do intervalo que ela acabou de abrir, e
     * descobri-lo por consulta depois deixaria a escolha a cargo de quem escreve a
     * consulta — com duas linhas candidatas se algo der errado.
     */
    public record IntervalosAplicados(
            List<IntervaloTarefa> fechados, List<IntervaloTarefa> abertos) {

        /** O intervalo aberto da serie indicada, ou {@code null} se nao houve. */
        public IntervaloTarefa aberto(TipoDeIntervalo tipo) {
            return abertos.stream().filter(i -> i.getTipo() == tipo).findFirst().orElse(null);
        }
    }

    /**
     * Uma linha do catalogo.
     *
     * @param fecha series a encerrar, quando houver intervalo aberto delas
     * @param fechaTodos encerra toda serie aberta, seja qual for — so
     *     {@code TAREFA_CONCLUIDA}, e a conclusao e o unico ponto em que o tempo
     *     de impedimento para sem que o impedimento tenha sido resolvido
     * @param abre series a iniciar, na ordem em que entram
     */
    record Efeito(Set<TipoDeIntervalo> fecha, boolean fechaTodos, List<TipoDeIntervalo> abre) {

        static Efeito nenhum() {
            return new Efeito(Set.of(), false, List.of());
        }

        static Efeito abrindo(TipoDeIntervalo... tipos) {
            return new Efeito(Set.of(), false, List.of(tipos));
        }

        static Efeito fechando(TipoDeIntervalo... tipos) {
            return new Efeito(Set.of(tipos), false, List.of());
        }
    }
}
