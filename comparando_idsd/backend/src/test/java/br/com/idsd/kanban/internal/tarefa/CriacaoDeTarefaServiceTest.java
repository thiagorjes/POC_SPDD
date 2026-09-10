package br.com.idsd.kanban.internal.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.idsd.kanban.internal.projeto.EtapaService;
import br.com.idsd.kanban.shared.RegraDeNegocioViolada;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SCN-004.2 — Criacao sem titulo.
 *
 * <p>Cenario tipado {@code unitario} no PRD: a regra nao precisa de banco nem de
 * contexto Spring, e mante-la fora do conteiner e o que preserva o ciclo curto
 * de quem implementa.
 */
@ExtendWith(MockitoExtension.class)
class CriacaoDeTarefaServiceTest {

    @Mock
    private EtapaService etapaService;

    @Mock
    private RegistradorDeEvento registradorDeEvento;

    private final UUID projetoId = UUID.randomUUID();
    private final UUID atorId = UUID.randomUUID();

    @Test
    @DisplayName("SCN-004.2 — criacao sem titulo e recusada e a razao e informada")
    void recusaCriacaoSemTitulo() {
        var servico = new CriacaoDeTarefaService(etapaService, registradorDeEvento);

        assertThatThrownBy(() -> servico.criar(projetoId, new NovaTarefaRequisicao(null, null, null), atorId))
                .isInstanceOf(RegraDeNegocioViolada.class)
                .extracting(erro -> ((RegraDeNegocioViolada) erro).detalhe())
                .asString()
                .isNotBlank();
    }

    @ParameterizedTest(name = "SCN-004.2 — titulo [{0}] e recusado")
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    @DisplayName("SCN-004.2 — titulo em branco conta como ausente")
    void recusaTituloEmBranco(String titulo) {
        var servico = new CriacaoDeTarefaService(etapaService, registradorDeEvento);

        assertThatThrownBy(() -> servico.criar(projetoId, new NovaTarefaRequisicao(titulo, null, null), atorId))
                .isInstanceOf(RegraDeNegocioViolada.class);
    }

    @Test
    @DisplayName("SCN-004.2 — recusa nao registra evento algum")
    void recusaNaoRegistraEvento() {
        var servico = new CriacaoDeTarefaService(etapaService, registradorDeEvento);

        assertThatThrownBy(() -> servico.criar(projetoId, new NovaTarefaRequisicao("  ", null, null), atorId))
                .isInstanceOf(RegraDeNegocioViolada.class);

        // O log e a verdade do sistema (SDR-001) e e imutavel (RNF-008). Evento
        // gravado numa criacao recusada nao teria como ser desfeito.
        verify(registradorDeEvento, never()).registrar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("SCN-004.2 — a razao da recusa nomeia o titulo, e nao um erro generico")
    void razaoDaRecusaIdentificaOCampo() {
        var servico = new CriacaoDeTarefaService(etapaService, registradorDeEvento);

        try {
            servico.criar(projetoId, new NovaTarefaRequisicao(null, null, null), atorId);
        } catch (RegraDeNegocioViolada erro) {
            assertThat(erro.detalhe().toLowerCase()).contains("titulo");
        }
    }
}
