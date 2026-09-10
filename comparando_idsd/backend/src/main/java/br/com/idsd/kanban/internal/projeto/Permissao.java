package br.com.idsd.kanban.internal.projeto;

/**
 * O que se pode fazer dentro de um projeto.
 *
 * <p>Enumeracao fechada, como {@link Papel}: permissao nao e dado de runtime, e
 * o cliente recebe o conjunto ja resolvido pelo servidor apenas para nao
 * apresentar acao que nao poderia executar (RNF-004). A decisao continua sendo
 * tomada no servidor, sobre a participacao real.
 */
public enum Permissao {

    /** Ler o board, o andamento e as tarefas do projeto. */
    LER,

    /** Criar, mover, assumir, devolver e alterar tarefa. */
    ESCREVER_TAREFA,

    /** Registrar o desfecho de um impedimento (BDR-002). */
    DESBLOQUEAR,

    /** Encerrar tarefa sem conclusao. */
    ENCERRAR,

    /** Reabrir tarefa concluida — privativo de {@code product_owner}. */
    REABRIR,

    /** Configurar o fluxo, as raias e a participacao do projeto. */
    CONFIGURAR
}
