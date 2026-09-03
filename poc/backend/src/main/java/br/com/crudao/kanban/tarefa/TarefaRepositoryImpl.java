package br.com.crudao.kanban.tarefa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Agregacao sob demanda em query unica, indexada por {@code (projeto_id, etapa_id, iniciado_em)}.
 * Materializacao assincrona fica como evolucao condicionada a volume medido (ADR-002).
 */
@Repository
@RequiredArgsConstructor
public class TarefaRepositoryImpl implements TarefaRepositoryCustom {

  private static final String SQL_AGREGADO =
      """
      WITH etapas AS (
          SELECT e.id AS etapa_id
            FROM etapa e
            JOIN workflow w ON w.id = e.workflow_id
           WHERE w.projeto_id = :projetoId AND w.ativo
      ),
      permanencia AS (
          SELECT pe.etapa_id,
                 COUNT(*)                                                     AS amostras,
                 AVG(EXTRACT(EPOCH FROM (COALESCE(pe.encerrado_em, now()) - pe.iniciado_em))) AS media
            FROM periodo_etapa pe
           WHERE pe.projeto_id = :projetoId
             AND pe.iniciado_em >= :inicio
             AND pe.iniciado_em <  :fim
           GROUP BY pe.etapa_id
      ),
      impedimento AS (
          SELECT pi.etapa_id,
                 AVG(EXTRACT(EPOCH FROM (COALESCE(pi.encerrado_em, now()) - pi.iniciado_em))) AS media
            FROM periodo_impedimento pi
           WHERE pi.projeto_id = :projetoId
             AND pi.iniciado_em >= :inicio
             AND pi.iniciado_em <  :fim
           GROUP BY pi.etapa_id
      )
      SELECT et.etapa_id,
             COALESCE(p.amostras, 0) AS amostras,
             COALESCE(p.media, 0)    AS media_permanencia,
             COALESCE(i.media, 0)    AS media_impedimento
        FROM etapas et
        LEFT JOIN permanencia p ON p.etapa_id = et.etapa_id
        LEFT JOIN impedimento i ON i.etapa_id = et.etapa_id
      """;

  private final EntityManager entityManager;

  @Override
  @SuppressWarnings("unchecked")
  public List<AgregadoEtapa> agregarLeadTimePorEtapa(UUID projetoId, Instant inicio, Instant fim) {
    Query query =
        entityManager
            .createNativeQuery(SQL_AGREGADO)
            .setParameter("projetoId", projetoId)
            .setParameter("inicio", OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC))
            .setParameter("fim", OffsetDateTime.ofInstant(fim, ZoneOffset.UTC));

    List<Object[]> linhas = query.getResultList();
    return linhas.stream().map(TarefaRepositoryImpl::paraAgregado).toList();
  }

  private static AgregadoEtapa paraAgregado(Object[] linha) {
    return new AgregadoEtapa(
        (UUID) linha[0],
        ((Number) linha[1]).longValue(),
        paraSegundos(linha[2]),
        paraSegundos(linha[3]));
  }

  private static long paraSegundos(Object valor) {
    if (valor == null) {
      return 0L;
    }
    return valor instanceof BigDecimal decimal
        ? decimal.longValue()
        : ((Number) valor).longValue();
  }
}
