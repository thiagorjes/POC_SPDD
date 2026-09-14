package br.com.idsd.kanban.internal.projeto;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Quantas tarefas ativas uma etapa ainda contem.
 *
 * <p>Existe porque arquivar etapa que contem tarefa ativa e recusado (RF-017), e
 * a etapa e do dominio de projeto enquanto a tarefa e de outro. Sem esta porta, o
 * servico de fluxo importaria o repositorio de tarefa e os dois dominios ficariam
 * amarrados na direcao errada — configuracao passaria a depender de operacao.
 *
 * <p><b>Enquanto nao houver implementacao, a contagem e zero</b>, e isso e
 * deliberado e nao um vazio esquecido: a entidade de tarefa nasce em TASK-02.5, e
 * ate la nao existe tarefa alguma para impedir arquivamento. O caminho de recusa
 * ja esta escrito em {@link EtapaService} e fica inerte por falta de massa, nao
 * por falta de codigo — quem publicar o bean liga a regra sem tocar no servico.
 *
 * <p><b>"Tarefa ativa" exclui as terminais.</b> Tarefa em {@code CONCLUIDA} ou
 * {@code ENCERRADA_SEM_CONCLUSAO} nao impede o arquivamento: ela nao vai se mover
 * de novo, e exigir esvaziar a etapa por causa dela tornaria toda etapa terminal
 * inarquivavel para sempre. Quem implementar esta porta responde por essa
 * exclusao.
 *
 * <p><b>A contagem e em lote, e a assinatura e a garantia</b> (ACH-07 da
 * reexecucao de TASK-02.2). Ela e chamada dentro da transacao que ja segura o
 * bloqueio pessimista da linha de {@code projeto} (SDR-005), e uma pergunta por
 * etapa seriam ate cem idas ao banco com todo o projeto serializado atras — o
 * custo do lock passaria a ser proporcional a um {@code N+1} evitavel. Receber a
 * colecao inteira nao <i>pede</i> uma consulta so: ela torna a consulta unica
 * possivel, e a assinatura por etapa a tornava impossivel.
 */
public interface TarefasAtivasPorEtapa {

    /**
     * Quantas tarefas nao terminais ainda estao em cada etapa pedida.
     *
     * <p>Etapa sem tarefa ativa pode vir ausente do mapa ou com zero — quem chama
     * trata os dois como o mesmo desfecho, porque exigir a chave obrigaria toda
     * implementacao a completar o mapa depois de agrupar, sem que a diferenca
     * signifique nada.
     */
    Map<UUID, Long> contarEm(Collection<UUID> etapaIds);
}
