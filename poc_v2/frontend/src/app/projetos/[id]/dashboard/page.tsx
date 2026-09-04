'use client';

import { useParams } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { Dashboard } from '@/components/Dashboard';
import { EstadoErro, Skeleton } from '@/components/Skeleton';
import { api, mensagemDeErro } from '@/lib/api';
import type { DashboardResponse, ProjetoResponse } from '@/lib/types';

const JANELAS = [
  { rotulo: 'Últimos 30 dias', dias: 30 },
  { rotulo: 'Últimos 90 dias', dias: 90 },
  { rotulo: 'Últimos 365 dias', dias: 365 },
];

/** TL-07 — Dashboard do projeto (RF-007). Leitura permitida tambem em projeto finalizado (UC-002). */
export default function DashboardPage() {
  const projetoId = String(useParams().id);
  const [dias, setDias] = useState(30);
  const [dados, setDados] = useState<DashboardResponse | null>(null);
  const [projeto, setProjeto] = useState<ProjetoResponse | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    void api
      .projeto(projetoId)
      .then(setProjeto)
      .catch(() => setProjeto(null));
  }, [projetoId]);

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro(null);
    try {
      const inicio = new Date(Date.now() - dias * 24 * 60 * 60 * 1000).toISOString();
      setDados(await api.dashboard(projetoId, inicio));
    } catch (erroCarga) {
      setErro(mensagemDeErro(erroCarga));
    } finally {
      setCarregando(false);
    }
  }, [projetoId, dias]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  return (
    <AppShell projetoId={projetoId} projetoNome={projeto?.nome}>
      <div className="page-header">
        <h1>Dashboard — {projeto?.nome ?? '…'}</h1>
        <select
          aria-label="Filtro de período"
          value={dias}
          onChange={(evento) => setDias(Number(evento.target.value))}
        >
          {JANELAS.map((janela) => (
            <option key={janela.dias} value={janela.dias}>
              {janela.rotulo}
            </option>
          ))}
        </select>
      </div>

      {carregando && <Skeleton linhas={3} />}
      {!carregando && erro && <EstadoErro mensagem={erro} aoTentar={() => void carregar()} />}
      {!carregando && !erro && dados && <Dashboard dados={dados} />}
    </AppShell>
  );
}
