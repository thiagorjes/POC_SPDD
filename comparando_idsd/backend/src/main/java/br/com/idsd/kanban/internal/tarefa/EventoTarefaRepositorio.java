package br.com.idsd.kanban.internal.tarefa;

import org.springframework.data.repository.Repository;

/**
 * Acesso a {@link EventoTarefa}.
 *
 * <p><b>Nenhum metodo de atualizacao ou remocao, e essa e a razao de o arquivo
 * existir.</b> A base e {@link Repository}, interface marcadora: so existe aqui
 * o que estiver declarado abaixo. {@code JpaRepository} publicaria
 * {@code save} — que faz {@code merge} sobre entidade com identificador, isto e,
 * {@code UPDATE} — e sete assinaturas de remocao, sobre a tabela que o sistema
 * inteiro assume ser imutavel (RNF-008, SDR-001). Foi o ACH-01 da revisao de
 * TASK-02.1, e aqui o custo de repeti-lo seria destruir a verdade.
 *
 * <p>A garantia e dupla: alem desta ausencia, a role de aplicacao recebe apenas
 * {@code SELECT, INSERT} na tabela — ver a migration de ordem 4. Dupla porque
 * nenhuma das duas metades basta: a role sozinha nao impede que alguem publique
 * o metodo e descubra tarde, e a ausencia sozinha nao sobrevive a um bug de
 * servico nem a alguem com o console aberto usando a credencial da aplicacao.
 *
 * <p>Nao ha metodo nenhum declarado ainda, nem de leitura nem de insercao. O
 * primeiro escritor e o registrador de eventos de EPIC-03, e o primeiro leitor
 * e o historico da tarefa: assinatura nasce com o consumidor (ACH-13 da revisao
 * de TASK-02.1). O que este arquivo declara hoje e que o log tem repositorio e
 * que ele nao escreve por cima de nada.
 */
public interface EventoTarefaRepositorio extends Repository<EventoTarefa, Long> {
}
