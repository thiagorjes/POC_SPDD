package br.com.idsd.kanban.internal.projeto;

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
 */
public interface TarefasAtivasPorEtapa {

    /** Quantas tarefas nao terminais ainda estao na etapa. */
    long contarEm(UUID etapaId);
}
