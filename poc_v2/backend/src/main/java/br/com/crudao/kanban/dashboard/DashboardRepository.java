package br.com.crudao.kanban.dashboard;

import br.com.crudao.kanban.leadtime.PeriodoEtapa;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Agregacao de lead time do dashboard. Consulta unica sobre {@code periodo_etapa} com LEFT JOIN da
 * agregacao de {@code periodo_impedimento}; o intervalo aberto e computado ate {@code now()} do
 * banco, nunca com relogio da JVM (R-6). Os aliases sao citados em camelCase para casar com a
 * projecao por interface sem depender da estrategia de naming.
 */
public interface DashboardRepository extends Repository<PeriodoEtapa, UUID> {

  @Query(
      value =
          """
          SELECT e.id                                   AS "etapaId",
                 e.nome                                 AS "nome",
                 e.ordem                                AS "ordem",
                 COALESCE(pe.media_segundos, 0)::bigint AS "mediaSegundos",
                 COALESCE(pe.amostras, 0)::bigint       AS "amostras",
                 COALESCE(pi.media_segundos, 0)::bigint AS "impedimentoMedioSegundos"
            FROM etapa e
            LEFT JOIN (
                 SELECT etapa_id,
                        AVG(EXTRACT(EPOCH FROM (COALESCE(encerrado_em, now()) - iniciado_em)))
                            AS media_segundos,
                        COUNT(DISTINCT tarefa_id) AS amostras
                   FROM periodo_etapa
                  WHERE projeto_id = :projetoId
                    AND iniciado_em >= :inicio
                    AND iniciado_em < :fim
                  GROUP BY etapa_id
            ) pe ON pe.etapa_id = e.id
            LEFT JOIN (
                 SELECT etapa_id,
                        AVG(EXTRACT(EPOCH FROM (COALESCE(encerrado_em, now()) - iniciado_em)))
                            AS media_segundos
                   FROM periodo_impedimento
                  WHERE projeto_id = :projetoId
                    AND iniciado_em >= :inicio
                    AND iniciado_em < :fim
                  GROUP BY etapa_id
            ) pi ON pi.etapa_id = e.id
           WHERE e.workflow_id = :workflowId
           ORDER BY e.ordem
          """,
      nativeQuery = true)
  List<AgregadoEtapa> agregarPorEtapa(
      @Param("projetoId") UUID projetoId,
      @Param("workflowId") UUID workflowId,
      @Param("inicio") Instant inicio,
      @Param("fim") Instant fim);

  @Query(
      value =
          """
          SELECT COUNT(DISTINCT tarefa_id)
            FROM periodo_etapa
           WHERE projeto_id = :projetoId
             AND iniciado_em >= :inicio
             AND iniciado_em < :fim
          """,
      nativeQuery = true)
  long contarTarefasNoPeriodo(
      @Param("projetoId") UUID projetoId,
      @Param("inicio") Instant inicio,
      @Param("fim") Instant fim);

  /** Projecao de uma linha agregada por etapa. */
  interface AgregadoEtapa {
    UUID getEtapaId();

    String getNome();

    int getOrdem();

    long getMediaSegundos();

    long getAmostras();

    long getImpedimentoMedioSegundos();
  }
}
