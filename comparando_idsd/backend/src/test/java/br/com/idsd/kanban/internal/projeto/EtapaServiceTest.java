package br.com.idsd.kanban.internal.projeto;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.idsd.kanban.shared.RegraDeNegocioViolada;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SCN-017.2 — Fluxo sem etapa terminal.
 *
 * <p>RN-001 exige ao menos uma etapa terminal por projeto, e o fluxo e
 * substituido inteiro numa operacao justamente para que nao exista estado
 * intermediario sem terminal. A regra e aritmetica e nao precisa de banco.
 */
@ExtendWith(MockitoExtension.class)
class EtapaServiceTest {

    @Mock
    private EtapaRepositorio repositorio;

    private final UUID projetoId = UUID.randomUUID();

    private EtapaService servico() {
        return new EtapaService(repositorio);
    }

    private static FluxoRequisicao.EtapaDesejada etapa(String nome, int ordem, boolean terminal) {
        return new FluxoRequisicao.EtapaDesejada(null, nome, ordem, terminal);
    }

    @Test
    @DisplayName("SCN-017.2 — fluxo em que nenhuma etapa e terminal e recusado")
    void recusaFluxoSemEtapaTerminal() {
        var requisicao = new FluxoRequisicao(List.of(
                etapa("Backlog", 1, false),
                etapa("Desenvolvimento", 2, false),
                etapa("Review", 3, false)));

        assertThatThrownBy(() -> servico().substituirFluxo(projetoId, requisicao))
                .isInstanceOf(RegraDeNegocioViolada.class);
    }

    @Test
    @DisplayName("SCN-017.2 — o fluxo vigente nao e alterado quando a configuracao e recusada")
    void recusaNaoPersisteNada() {
        var requisicao = new FluxoRequisicao(List.of(etapa("Backlog", 1, false)));

        assertThatThrownBy(() -> servico().substituirFluxo(projetoId, requisicao))
                .isInstanceOf(RegraDeNegocioViolada.class);

        verify(repositorio, never()).substituirFluxo(any(), any());
    }

    @Test
    @DisplayName("SCN-017.2 — lista vazia tambem e fluxo sem terminal, e e recusada")
    void recusaFluxoVazio() {
        assertThatThrownBy(() -> servico().substituirFluxo(projetoId, new FluxoRequisicao(List.of())))
                .isInstanceOf(RegraDeNegocioViolada.class);
    }

    @Test
    @DisplayName("SCN-017.1 — fluxo com ao menos uma etapa terminal e aceito")
    void aceitaFluxoComTerminal() {
        var requisicao = new FluxoRequisicao(List.of(
                etapa("Backlog", 1, false),
                etapa("Concluido", 2, true)));

        assertThatCode(() -> servico().substituirFluxo(projetoId, requisicao))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SCN-017.2 — mais de uma etapa terminal continua valendo: a regra e um piso")
    void aceitaMaisDeUmaTerminal() {
        var requisicao = new FluxoRequisicao(List.of(
                etapa("Backlog", 1, false),
                etapa("Cancelado", 2, true),
                etapa("Concluido", 3, true)));

        assertThatCode(() -> servico().substituirFluxo(projetoId, requisicao))
                .doesNotThrowAnyException();
    }
}
