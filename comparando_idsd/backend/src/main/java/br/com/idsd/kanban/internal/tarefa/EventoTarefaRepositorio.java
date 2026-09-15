package br.com.idsd.kanban.internal.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
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
 * <p>A garantia foi <b>desenhada</b> dupla: alem desta ausencia, a migration de
 * ordem 4 concede ao grupo {@code aplicacao_kanban} apenas
 * {@code SELECT, INSERT} na tabela. Dupla porque nenhuma das duas metades basta
 * — a role sozinha nao impede que alguem publique o metodo e descubra tarde, e a
 * ausencia sozinha nao sobrevive a um bug de servico nem a alguem com o console
 * aberto usando a credencial da aplicacao.
 *
 * <p><b>Hoje so a metade deste arquivo esta em vigor.</b> A aplicacao conecta
 * com {@code BANCO_USUARIO}, que e o {@code POSTGRES_USER} da imagem —
 * superusuario e dono do schema —, e contra ele toda revogacao e inerte. A
 * concessao da migration esta correta e foi medida com uma role fabricada, mas
 * nenhuma conexao do produto passa por ela. Enquanto isso durar, RNF-008 e
 * garantido apenas por esta ausencia de metodo. Ver ACH-01 da revisao de
 * TASK-02.3 e a pendencia 21 do estado operacional.
 *
 * <p><b>Nao ha metodo de insercao tambem, e isso e outra coisa.</b> Quem grava e
 * {@code RegistradorDeEvento}, por {@code EntityManager.persist} — a operacao que
 * so cria. Publicar um {@code save} aqui, ainda que hoje ele sempre caisse em
 * {@code persist} por o identificador nascer nulo, poria na interface a
 * assinatura que faz {@code merge} sobre entidade com identificador, e o dia em
 * que alguem a chamasse com um evento carregado nao haveria nada neste arquivo
 * para impedir o {@code UPDATE}.
 *
 * <p>Os metodos de leitura abaixo sao da rotina de reconstrucao
 * ({@code data-model.md} secao 9).
 */
public interface EventoTarefaRepositorio extends Repository<EventoTarefa, Long> {

    /**
     * O log de um projeto, na ordem total de gravacao.
     *
     * <p>Por {@code id} e nao por {@code ocorrido_em}: o instante e do relogio da
     * instancia que gravou, e duas instancias podem produzi-lo fora de ordem. A
     * chave sequencial e atribuida pelo banco no {@code INSERT}, e e ela que
     * define a ordem em que os eventos de fato entraram.
     */
    List<EventoTarefa> findByProjetoIdOrderByIdAsc(UUID projetoId);

    /**
     * Os projetos que tem log.
     *
     * <p>Sai de {@code evento_tarefa} e nao de {@code projeto} porque a
     * reconstrucao e sobre o que existe no anel de verdade: projeto sem evento
     * nao tem projecao a refazer, e varre-lo custaria um bloqueio consultivo e
     * uma transacao para nao fazer nada.
     */
    @Query("select distinct e.projetoId from EventoTarefa e")
    List<UUID> projetosComLog();
}
