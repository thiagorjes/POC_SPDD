package br.com.crudao.kanban.projeto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Catalogo fechado de toggles (RF-016/BDR-001): nenhuma chave fora de {@link ChaveToggle}. */
@ExtendWith(MockitoExtension.class)
class ProjetoToggleServiceTest {

  private static final UUID PROJETO = UUID.randomUUID();

  @Mock private ProjetoToggleRepository projetoToggleRepository;
  @InjectMocks private ProjetoToggleService service;

  @Test
  @DisplayName("criarDefaults grava exatamente um registro por chave do catalogo")
  void criarDefaults() {
    service.criarDefaults(PROJETO);

    ArgumentCaptor<ProjetoToggle> captor = ArgumentCaptor.forClass(ProjetoToggle.class);
    verify(projetoToggleRepository, times(ChaveToggle.values().length)).save(captor.capture());
    assertThat(captor.getAllValues())
        .extracting(ProjetoToggle::getChave)
        .containsExactlyInAnyOrder(ChaveToggle.values());
    assertThat(captor.getAllValues())
        .allSatisfy(toggle -> assertThat(toggle.isHabilitado()).isEqualTo(toggle.getChave().padrao()));
  }

  @Test
  @DisplayName("obter parte dos defaults do catalogo e sobrepoe apenas o que esta persistido")
  void obterUsaDefaultsESobrepoe() {
    ChaveToggle chave = ChaveToggle.DEV_PODE_EXCLUIR_TAREFA;
    when(projetoToggleRepository.findByIdProjetoId(PROJETO))
        .thenReturn(List.of(new ProjetoToggle(PROJETO, chave, !chave.padrao())));

    Map<ChaveToggle, Boolean> resultado = service.obter(PROJETO);

    assertThat(resultado).hasSize(ChaveToggle.values().length);
    assertThat(resultado.get(chave)).isEqualTo(!chave.padrao());
    for (ChaveToggle outra : ChaveToggle.values()) {
      if (outra != chave) {
        assertThat(resultado.get(outra)).as(outra.name()).isEqualTo(outra.padrao());
      }
    }
  }

  @Test
  @DisplayName("obterComoMapa expoe as chaves como String para o contrato de API")
  void obterComoMapa() {
    when(projetoToggleRepository.findByIdProjetoId(PROJETO)).thenReturn(List.of());

    Map<String, Boolean> mapa = service.obterComoMapa(PROJETO);

    assertThat(mapa).containsKey(ChaveToggle.GESTOR_PODE_VER_BOARD.name());
    assertThat(mapa).hasSize(ChaveToggle.values().length);
  }

  @Test
  @DisplayName("atualizar grava somente as chaves informadas")
  void atualizarSomenteAsInformadas() {
    service.atualizar(PROJETO, Map.of(ChaveToggle.DEV_PODE_FINALIZAR_TAREFA, true));

    ArgumentCaptor<ProjetoToggle> captor = ArgumentCaptor.forClass(ProjetoToggle.class);
    verify(projetoToggleRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().getChave()).isEqualTo(ChaveToggle.DEV_PODE_FINALIZAR_TAREFA);
    assertThat(captor.getValue().isHabilitado()).isTrue();
  }

  @Test
  @DisplayName("habilitado devolve o valor persistido quando existe")
  void habilitadoPersistido() {
    ChaveToggle chave = ChaveToggle.DEV_PODE_EDITAR_TAREFA_INICIADA;
    when(projetoToggleRepository.findByIdProjetoIdAndIdChave(PROJETO, chave))
        .thenReturn(Optional.of(new ProjetoToggle(PROJETO, chave, true)));

    assertThat(service.habilitado(PROJETO, chave)).isTrue();
  }

  @Test
  @DisplayName("habilitado cai no padrao do catalogo quando o projeto nao tem o registro")
  void habilitadoPadrao() {
    ChaveToggle chave = ChaveToggle.DEV_PODE_EDITAR_TAREFA_INICIADA;
    when(projetoToggleRepository.findByIdProjetoIdAndIdChave(PROJETO, chave))
        .thenReturn(Optional.empty());

    assertThat(service.habilitado(PROJETO, chave)).isEqualTo(chave.padrao());
  }

  @Test
  @DisplayName("listar delega ao repositorio sem transformar")
  void listarDelega() {
    List<ProjetoToggle> persistidos =
        List.of(new ProjetoToggle(PROJETO, ChaveToggle.GESTOR_PODE_VER_BOARD, false));
    when(projetoToggleRepository.findByIdProjetoId(PROJETO)).thenReturn(persistidos);

    assertThat(service.listar(PROJETO)).isEqualTo(persistidos);
    verify(projetoToggleRepository).findByIdProjetoId(PROJETO);
    verify(projetoToggleRepository, times(0)).save(any());
  }
}
