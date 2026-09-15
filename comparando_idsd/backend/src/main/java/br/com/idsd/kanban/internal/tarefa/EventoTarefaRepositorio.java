package br.com.idsd.kanban.internal.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
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
 * <p><b>As duas metades estao em vigor desde TASK-02.9.</b> A aplicacao deixou
 * de conectar como o dono do schema e passou a conectar como {@code kanban_app},
 * role de login membro de {@code aplicacao_kanban}, sem {@code SUPERUSER} e sem
 * posse de objeto — e e contra superusuario e contra dono que {@code REVOKE} era
 * decoracao. A perna de banco saiu de "escrita e inerte" para medida: contra
 * essa credencial, {@code UPDATE}, {@code DELETE} e {@code TRUNCATE} do log saem
 * em {@code permission denied}. A pendencia 21 esta fechada; este paragrafo
 * afirmava o contrario e era o ACH-07 da revisao de TASK-02.4.
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
     * O mesmo log, em lote, a partir de um ponto.
     *
     * <p>Existe para a reconstrucao nao carregar o log inteiro de uma vez
     * (ACH-14). O log e a unica tabela do sistema que cresce sem teto — um evento
     * por acao, para sempre, e nada o poda, porque poda-lo seria negar SDR-001 —,
     * de modo que a leitura integral e exaustao de heap adiada, e adiada para
     * dentro da transacao que segura o bloqueio exclusivo do projeto: o pior
     * momento possivel, porque a falha chega com as escritas ja barradas.
     *
     * <p>Paginacao por chave e nao por deslocamento: {@code OFFSET} relê e
     * descarta as linhas anteriores a cada lote, o que torna a varredura
     * quadratica justamente no projeto grande que motivou a mudanca.
     */
    List<EventoTarefa> findByProjetoIdAndIdGreaterThanOrderByIdAsc(
            UUID projetoId, Long apos, Pageable pagina);

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
