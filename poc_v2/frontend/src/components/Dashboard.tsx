'use client';

import { duracao } from '@/lib/format';
import type { DashboardResponse } from '@/lib/types';

const ALTURA_MAXIMA_BARRA = 120;

/**
 * TL-07 — KPIs e lead-time medio por etapa (RF-007). Calculo sob demanda no periodo filtrado; o
 * estado vazio aparece quando nenhuma etapa tem amostras.
 */
export function Dashboard({ dados }: { dados: DashboardResponse }) {
  const comAmostras = dados.etapas.filter((etapa) => etapa.amostras > 0);
  if (comAmostras.length === 0) {
    return (
      <div className="empty-state">
        Ainda não há movimentações suficientes para calcular lead-time neste projeto.
      </div>
    );
  }

  const maiorMedia = Math.max(...comAmostras.map((etapa) => etapa.mediaSegundos), 1);
  const mediaGeral =
    comAmostras.reduce((total, etapa) => total + etapa.mediaSegundos, 0) / comAmostras.length;

  return (
    <section aria-label="KPIs">
      <div className="kpi-grid">
        <div className="card kpi-card">
          <div className="kpi-value">{duracao(Math.round(mediaGeral))}</div>
          <div className="kpi-label">Lead-time médio geral</div>
        </div>
        <div className="card kpi-card">
          <div className="kpi-value">{duracao(dados.impedimentoMedioTotalSegundos)}</div>
          <div className="kpi-label">Tempo médio de impedimento</div>
        </div>
        <div className="card kpi-card">
          <div className="kpi-value">{dados.totalTarefas}</div>
          <div className="kpi-label">Tarefas no período</div>
        </div>
      </div>

      <div className="card">
        <h2 className="section-title">Lead-time médio por etapa</h2>
        <div
          className="bar-chart"
          role="img"
          aria-label={`Gráfico de lead-time médio por etapa: ${comAmostras
            .map((etapa) => `${etapa.nome} ${duracao(etapa.mediaSegundos)}`)
            .join(', ')}`}
        >
          {comAmostras.map((etapa) => (
            <div key={etapa.etapaId} className="bar-wrap">
              <div
                className="bar"
                style={{
                  height: `${Math.round((etapa.mediaSegundos / maiorMedia) * ALTURA_MAXIMA_BARRA)}px`,
                }}
              />
              {etapa.nome}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
