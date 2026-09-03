'use client';

import { useEffect, useState } from 'react';
import { Erro, Skeleton, Vazio } from './Estados';
import { api } from '@/lib/api';
import type { Dashboard as DashboardDados } from '@/lib/types';

function horas(segundos: number): string {
  return (segundos / 3600).toFixed(1);
}

/** TL-07. Etapa com {@code amostras === 0} nao vira barra: media zero sem medicao seria mentira. */
export function Dashboard({ projetoId }: { projetoId: string }) {
  const [dados, setDados] = useState<DashboardDados | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [inicio, setInicio] = useState('');
  const [fim, setFim] = useState('');

  const carregar = async () => {
    setCarregando(true);
    setErro(null);
    try {
      setDados(
        await api.dashboard(
          projetoId,
          inicio ? new Date(inicio).toISOString() : undefined,
          fim ? new Date(fim).toISOString() : undefined,
        ),
      );
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar o dashboard.');
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => {
    void carregar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projetoId]);

  if (carregando) {
    return <Skeleton linhas={4} />;
  }
  if (erro) {
    return <Erro mensagem={erro} aoTentarNovamente={carregar} />;
  }
  if (!dados) {
    return null;
  }

  const comAmostras = dados.etapas.filter((e) => e.amostras > 0);
  const maior = Math.max(1, ...comAmostras.map((e) => e.mediaPermanenciaSegundos));

  return (
    <>
      <form
        style={{ display: 'flex', gap: 8, alignItems: 'end', marginBottom: 24 }}
        onSubmit={(evento) => {
          evento.preventDefault();
          void carregar();
        }}
      >
        <label>
          Inicio
          <input type="datetime-local" value={inicio} onChange={(e) => setInicio(e.target.value)} />
        </label>
        <label>
          Fim
          <input type="datetime-local" value={fim} onChange={(e) => setFim(e.target.value)} />
        </label>
        <button className="primario" type="submit">
          Aplicar
        </button>
      </form>

      <div style={{ display: 'flex', gap: 16, marginBottom: 24 }}>
        <div className="cartao">
          <p>Impedimento medio por etapa</p>
          <strong>{horas(dados.impedimentoMedioTotalSegundos)} h</strong>
        </div>
        <div className="cartao">
          <p>Etapas com medicao</p>
          <strong>
            {comAmostras.length} / {dados.etapas.length}
          </strong>
        </div>
      </div>

      {comAmostras.length === 0 ? (
        <Vazio titulo="Ainda nao ha historico suficiente no periodo selecionado." />
      ) : (
        <table className="tabela">
          <caption>Lead-time medio por etapa</caption>
          <thead>
            <tr>
              <th>Etapa</th>
              <th>Amostras</th>
              <th>Permanencia media (h)</th>
              <th>Impedimento medio (h)</th>
              <th>Distribuicao</th>
            </tr>
          </thead>
          <tbody>
            {dados.etapas.map((etapa) => (
              <tr key={etapa.etapaId}>
                <td>{etapa.etapaNome}</td>
                <td>{etapa.amostras}</td>
                <td>{horas(etapa.mediaPermanenciaSegundos)}</td>
                <td>{horas(etapa.mediaImpedimentoSegundos)}</td>
                <td>
                  {etapa.amostras === 0 ? (
                    <span className="badge">sem dados</span>
                  ) : (
                    <div
                      className="barra"
                      style={{ width: `${(etapa.mediaPermanenciaSegundos / maior) * 100}%` }}
                      aria-label={`${horas(etapa.mediaPermanenciaSegundos)} horas`}
                    />
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
