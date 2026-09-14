package br.com.idsd.kanban.internal.projeto;

import java.util.List;
import java.util.UUID;

/**
 * Uma etapa do fluxo, como o contrato a devolve (RF-017).
 *
 * <p>O {@code id} vai no corpo porque ele e o que o cliente devolve no
 * {@code PUT} para preservar a etapa. Omiti-lo tornaria toda reconfiguracao uma
 * recriacao, e a serie de tempo por etapa — que segue o identificador — se
 * partiria a cada salvamento de tela.
 *
 * <p><b>Nao ha {@code arquivadaEm} aqui, e a ausencia e a regra.</b> A leitura
 * devolve o fluxo <i>vigente</i>; a etapa arquivada nao volta para a tela de
 * configuracao, porque nada na configuracao se faz com ela. Ela continua existindo
 * para o historico e para os intervalos, que a alcancam pelo identificador.
 */
public record EtapaResposta(UUID id, String nome, int ordem, boolean terminal) {

    public static EtapaResposta de(Etapa etapa) {
        return new EtapaResposta(
                etapa.getId(), etapa.getNome(), etapa.getOrdem(), etapa.isTerminal());
    }

    public static List<EtapaResposta> de(List<Etapa> etapas) {
        return etapas.stream().map(EtapaResposta::de).toList();
    }

    /**
     * O envelope das duas rotas de fluxo.
     *
     * <p>Objeto e nao array nu na raiz: resposta que e array nao tem onde crescer
     * sem quebrar o cliente, e a tela de configuracao ja tem candidato a segundo
     * membro — as raias, que sao a dimensao irma e nascem no mesmo lugar.
     */
    public record Fluxo(List<EtapaResposta> etapas) {

        public static Fluxo de(List<Etapa> etapas) {
            return new Fluxo(EtapaResposta.de(etapas));
        }
    }
}
