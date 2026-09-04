'use client';

import { useCallback, useEffect, useState } from 'react';
import { Erro, Skeleton } from './Estados';
import { BarChart } from './ui/BarChart';
import { Badge } from './ui/Badge';
import { EstadoVazio } from './ui/EstadoVazio';
import { KpiCard } from './ui/KpiCard';
import { api } from '@/lib/api';
import type { Dashboard as DashboardDados } from '@/lib/types';

function horas(segundos: number): string {
  return (segundos / 3600).toFixed(1);
}

/** Presets do prototipo (TL-07), convertidos no par inicio/fim que a API ja aceita. */
const PRESETS = [
  { dias: 30, rotulo: 'Últimos 30 dias' },
  { dias: 90, rotulo: 'Últimos 90 dias' },
];

function periodoParaIntervalo(dias: number): { inicio: string; fim: string } {
  const fim = new Date();
  const inicio = new Date(fim.getTime() - dias * 24 * 60 * 60 * 1000);
  return { inicio: inicio.toISOString(), fim: fim.toISOString() };
}

/** TL-07. Etapa com {@code amostras === 0} nao vira barra: media zero sem medicao seria mentira. */
export function Dashboard({ projetoId }: { projetoId: string }) {
  const [dados, setDados] = useState<DashboardDados | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [dias, setDias] = useState(30);

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro(null);
    const { inicio, fim } = periodoParaIntervalo(dias);
    try {
      setDados(await api.dashboard(projetoId, inicio, fim));
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar o dashboard.');
    } finally {
      setCarregando(false);
    }
  }, [projetoId, dias]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const filtro = (
    <div className="form-field" style={{ marginBottom: 'var(--espaco-scale-md)', maxWidth: 240 }}>
      <label htmlFor="periodo">Período</label>
      <select id="periodo" value={dias} onChange={(e) => setDias(Number(e.target.value))}>
        {PRESETS.map((p) => (
          <option key={p.dias} value={p.dias}>
            {p.rotulo}
          </option>
        ))}
      </select>
    </div>
  );

  if (carregando) {
    return (
      <>
        {filtro}
        <Skeleton linhas={4} />
      </>
    );
  }
  if (erro) {
    return (
      <>
        {filtro}
        <Erro mensagem={erro} aoTentarNovamente={carregar} />
      </>
    );
  }
  if (!dados) {
    return filtro;
  }

  const comAmostras = dados.etapas
    .filter((e) => e.amostras > 0)
    .sort((a, b) => a.ordem - b.ordem);

  return (
    <>
      {filtro}

      {/*
        Apenas dois KPIs: o terceiro do prototipo ("tarefas concluidas no periodo") nao tem campo
        correspondente na resposta do dashboard e nao deve ser fabricado.
      */}
      <div className="kpi-grid">
        <KpiCard
          valor={`${horas(dados.impedimentoMedioTotalSegundos)} h`}
          rotulo="Tempo médio de impedimento"
        />
        <KpiCard
          valor={`${comAmostras.length} / ${dados.etapas.length}`}
          rotulo="Etapas com medição"
        />
      </div>

      {comAmostras.length === 0 ? (
        <EstadoVazio mensagem="Ainda não há movimentações suficientes para calcular lead-time no período selecionado." />
      ) : (
        <>
          <section className="card" style={{ marginBottom: 'var(--espaco-scale-lg)' }}>
            <h2>Lead-time médio por etapa</h2>
            <BarChart
              series={comAmostras.map((e) => ({
                rotulo: e.etapaNome,
                valor: e.mediaPermanenciaSegundos,
                textoValor: `${horas(e.mediaPermanenciaSegundos)} h`,
              }))}
              descricaoAcessivel={`Lead-time médio por etapa: ${comAmostras
                .map((e) => `${e.etapaNome}, ${horas(e.mediaPermanenciaSegundos)} horas`)
                .join('; ')}.`}
            />
          </section>

          <table>
            <caption>Detalhamento por etapa</caption>
            <thead>
              <tr>
                <th>Etapa</th>
                <th>Amostras</th>
                <th>Permanência média (h)</th>
                <th>Impedimento médio (h)</th>
              </tr>
            </thead>
            <tbody>
              {dados.etapas.map((etapa) => (
                <tr key={etapa.etapaId}>
                  <td>{etapa.etapaNome}</td>
                  <td>
                    {etapa.amostras === 0 ? <Badge variante="neutro">sem dados</Badge> : etapa.amostras}
                  </td>
                  <td>{horas(etapa.mediaPermanenciaSegundos)}</td>
                  <td>{horas(etapa.mediaImpedimentoSegundos)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </>
  );
}
