package br.com.idsd.kanban.internal.projeto;

/**
 * O que se pode fazer dentro de um projeto.
 *
 * <p>Enumeracao fechada, como {@link Papel}: permissao nao e dado de runtime, e
 * o cliente recebe o conjunto ja resolvido pelo servidor apenas para nao
 * apresentar acao que nao poderia executar (RNF-004). A decisao continua sendo
 * tomada no servidor, sobre a participacao real.
 *
 * <p><b>Acrescentar valor aqui amplia, no mesmo commit, o alcance da
 * administracao global.</b> Ele e {@code EnumSet.allOf(Permissao.class)} por
 * decisao — RN-035 diz que o alcance e de escopo e nao de imunidade, e lista
 * explicita teria o defeito oposto: a permissao nova ficaria de fora em
 * silencio, e o alcance mais poderoso do sistema pararia de valer para uma parte
 * do produto sem que nada falhasse. O custo assumido e este aviso: quem
 * acrescenta uma constante decide, junto, que a administracao global a tem.
 *
 * <p>Origem do aviso: ACH-12 da revisao de TASK-01.5.
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
