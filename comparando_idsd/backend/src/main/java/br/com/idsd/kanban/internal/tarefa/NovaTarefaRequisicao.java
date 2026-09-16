package br.com.idsd.kanban.internal.tarefa;

import java.util.UUID;

/**
 * Corpo de {@code POST /v1/projetos/{projetoId}/tarefas} (RF-004).
 *
 * <p><b>Sem bloco de origem</b>, ao contrario de toda outra escrita sobre tarefa:
 * nao ha estado anterior a declarar, e exigi-lo aqui seria pedir a versao de uma
 * linha que ainda nao existe (SDR-002 comeca a valer a partir da segunda escrita).
 *
 * <p><b>Sem campo de ator, e a ausencia e o fechamento de ACH-12</b> da revisao de
 * TASK-02.4. {@code ator_id} e a autoria do unico registro de auditoria do sistema,
 * e no nucleo de escrita ele e parametro livre — conferi-lo la foi recusado com
 * razao, porque o job e o arnes legitimamente nao tem principal. A borda fecha o
 * caso de forma <b>estrutural e nao por checagem</b>: o ator e extraido do contexto
 * autenticado em {@link TarefaController}, e o corpo nao tem por onde propor um. Nao
 * ha o que conferir quando nao ha o que divergir — e acrescentar um campo de ator
 * aqui reabriria exatamente o achado.
 *
 * <p>Nenhum campo carrega anotacao de validacao, e os tres sao validados: a recusa
 * inteira — obrigatoriedade e teto de {@code titulo} e de {@code descricao}
 * (ACH-05), pertencimento de {@code raiaId} ao projeto (ACH-01) — vive em
 * {@link CriacaoDeTarefaService}. E deliberado: {@code CriacaoDeTarefaServiceTest}
 * verifica a recusa sem contexto Spring e sem passar por HTTP, e validacao por
 * anotacao so existe quando um validador roda. Teto que so vale pela rota web nao
 * e teto do dominio.
 */
public record NovaTarefaRequisicao(String titulo, String descricao, UUID raiaId) {
}
