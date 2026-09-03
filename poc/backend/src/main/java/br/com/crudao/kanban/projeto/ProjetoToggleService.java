package br.com.crudao.kanban.projeto;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Conjunto fechado de toggles por projeto (RF-016). Nenhuma chave fora de {@link ChaveToggle}. */
@Service
@RequiredArgsConstructor
public class ProjetoToggleService {

  private final ProjetoToggleRepository projetoToggleRepository;

  /** Cria os 4 toggles com os defaults do catalogo. Chamado junto da criacao do projeto. */
  @Transactional
  public void criarDefaults(UUID projetoId) {
    for (ChaveToggle chave : ChaveToggle.values()) {
      projetoToggleRepository.save(new ProjetoToggle(projetoId, chave, chave.padrao()));
    }
  }

  @Transactional(readOnly = true)
  public Map<ChaveToggle, Boolean> obter(UUID projetoId) {
    Map<ChaveToggle, Boolean> resultado = new EnumMap<>(ChaveToggle.class);
    for (ChaveToggle chave : ChaveToggle.values()) {
      resultado.put(chave, chave.padrao());
    }
    for (ProjetoToggle toggle : projetoToggleRepository.findByIdProjetoId(projetoId)) {
      resultado.put(toggle.getChave(), toggle.isHabilitado());
    }
    return resultado;
  }

  @Transactional(readOnly = true)
  public Map<String, Boolean> obterComoMapa(UUID projetoId) {
    return obter(projetoId).entrySet().stream()
        .collect(
            java.util.stream.Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
  }

  /** Atualiza apenas as chaves informadas; as demais permanecem como estao. */
  @Transactional
  public void atualizar(UUID projetoId, Map<ChaveToggle, Boolean> valores) {
    valores.forEach(
        (chave, habilitado) ->
            projetoToggleRepository.save(new ProjetoToggle(projetoId, chave, habilitado)));
  }

  @Transactional(readOnly = true)
  public boolean habilitado(UUID projetoId, ChaveToggle chave) {
    return projetoToggleRepository
        .findByIdProjetoIdAndIdChave(projetoId, chave)
        .map(ProjetoToggle::isHabilitado)
        .orElse(chave.padrao());
  }

  @Transactional(readOnly = true)
  public List<ProjetoToggle> listar(UUID projetoId) {
    return projetoToggleRepository.findByIdProjetoId(projetoId);
  }
}
