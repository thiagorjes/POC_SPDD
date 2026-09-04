package br.com.crudao.kanban.projeto;

import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leitura e escrita do conjunto fechado de toggles do projeto (RF-016). */
@Service
@RequiredArgsConstructor
public class ProjetoToggleService {

  private final ProjetoToggleRepository projetoToggleRepository;

  /** Cria os quatro toggles com os valores default no momento da criacao do projeto. */
  @Transactional
  public void criarPadroes(UUID projetoId) {
    for (ChaveToggle chave : ChaveToggle.values()) {
      projetoToggleRepository.save(new ProjetoToggle(projetoId, chave, chave.padrao()));
    }
  }

  /** Mapa completo de toggles do projeto; chave ausente assume o default da enum. */
  @Transactional(readOnly = true)
  public Map<ChaveToggle, Boolean> obter(UUID projetoId) {
    Map<ChaveToggle, Boolean> resultado = new EnumMap<>(ChaveToggle.class);
    for (ChaveToggle chave : ChaveToggle.values()) {
      resultado.put(chave, chave.padrao());
    }
    projetoToggleRepository
        .findByIdProjetoId(projetoId)
        .forEach(toggle -> resultado.put(toggle.getId().getChave(), toggle.isHabilitado()));
    return resultado;
  }

  /** Estado de um toggle isolado. */
  @Transactional(readOnly = true)
  public boolean habilitado(UUID projetoId, ChaveToggle chave) {
    return projetoToggleRepository
        .findByIdProjetoIdAndIdChave(projetoId, chave)
        .map(ProjetoToggle::isHabilitado)
        .orElse(chave.padrao());
  }

  /** Atualiza um toggle existente. Chave fora do catalogo nao existe (enum fechada). */
  @Transactional
  public void definir(UUID projetoId, ChaveToggle chave, boolean habilitado) {
    ProjetoToggle toggle =
        projetoToggleRepository
            .findByIdProjetoIdAndIdChave(projetoId, chave)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Toggle nao configurado para este projeto: " + chave.name()));
    toggle.setHabilitado(habilitado);
    projetoToggleRepository.save(toggle);
  }
}
