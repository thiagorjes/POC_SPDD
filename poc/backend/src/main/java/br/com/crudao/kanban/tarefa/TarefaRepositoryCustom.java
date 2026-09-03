package br.com.crudao.kanban.tarefa;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Agregacoes de dashboard que nao se expressam bem como query derivada. */
public interface TarefaRepositoryCustom {

  /**
   * Media de permanencia por etapa e media de impedimento por etapa, em segundos, restritas a
   * janela informada. Intervalos abertos sao computados ate {@code now()} do banco.
   */
  List<AgregadoEtapa> agregarLeadTimePorEtapa(UUID projetoId, Instant inicio, Instant fim);

  /** Linha do agregado por etapa. Duracoes em segundos (contrato de API). */
  record AgregadoEtapa(
      UUID etapaId, long amostras, long mediaPermanenciaSegundos, long mediaImpedimentoSegundos) {}
}
