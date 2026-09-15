package br.com.idsd.kanban.shared;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Porta de saida do broadcast: por onde um evento ja comitado sai da aplicacao.
 *
 * <p><b>E o unico publicador.</b> Nao ha gatilho de {@code NOTIFY} no esquema, e
 * a ausencia e decisao registrada ({@code data-model.md} secao 8): dois
 * publicadores para o mesmo evento fariam cada mudanca gerar dois broadcasts, e
 * o cliente contabilizaria {@code seq} repetido como inconsistencia — destruindo
 * a deteccao de lacuna que ADR-004 e SDR-004 constroem.
 *
 * <p><b>A chamada sai depois do commit</b>, agendada em
 * {@code TransactionSynchronization.afterCommit} pelo registrador de eventos.
 * Publicar de dentro da transacao arriscaria anunciar mudanca que sofre
 * rollback, e evento anunciado e depois desmentido e pior que evento atrasado.
 * A janela de perda que isso abre — commit feito, processo morto antes do envio
 * — e fechada pela varredura de retomada do listener descrita em SDR-004, que
 * nasce em EPIC-08.
 *
 * <p>Vive em {@code shared} porque quem a chama e {@code internal/tarefa} e quem
 * a implementa de verdade sera o adaptador STOMP/LISTEN de EPIC-08: a porta nao
 * pode morar em nenhum dos dois lados.
 */
public interface EventoBoardPublisher {

    void publicar(EventoPublicado evento);

    /**
     * O que viaja no broadcast.
     *
     * <p>Record e nao a entidade: {@code EventoTarefa} e JPA e nao sai do
     * service, e o corpo do {@code NOTIFY} tem limite de tamanho no PostgreSQL —
     * carregar o documento de {@code dados} aqui arriscaria estourar o canal por
     * causa de um motivo de impedimento longo. O cliente recebe o aviso e o
     * numero; o conteudo ele busca por {@code GET}.
     */
    record EventoPublicado(
            UUID projetoId, UUID tarefaId, String tipo, long seq, Instant ocorridoEm) {
    }

    /**
     * Implementacao vazia, e e a desta task.
     *
     * <p>O adaptador real depende do canal {@code board_events} e da sessao
     * STOMP, que nascem em EPIC-08 (TASK-08.1, TASK-08.2). O que esta task
     * precisa entregar e que o <b>gancho</b> exista e dispare no lugar certo:
     * com esta implementacao, mover a publicacao para dentro da transacao nao
     * quebraria nada visivel, e e por isso que o criterio 4 se verifica com um
     * espiao no lugar deste bean, e nao com ele.
     *
     * <p>Sai de cena quando o adaptador chegar — que precisara declarar-se
     * {@code @Primary} ou substituir este bean, e nao conviver com ele: dois
     * publicadores e exatamente o que o cabecalho desta interface proibe.
     */
    @Component
    class Silencioso implements EventoBoardPublisher {

        @Override
        public void publicar(EventoPublicado evento) {
            // sem canal para escrever ainda — ver o javadoc da classe
        }
    }
}
