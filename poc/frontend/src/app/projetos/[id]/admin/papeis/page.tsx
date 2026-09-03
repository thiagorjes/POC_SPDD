'use client';

import { use, useCallback, useEffect, useState } from 'react';
import { AvisoSomenteLeitura, Erro, SemPermissao, Skeleton } from '@/components/Estados';
import { useFeedback } from '@/components/Feedback';
import { usePermissoes } from '@/hooks/usePermissoes';
import { api } from '@/lib/api';
import type { Papel } from '@/lib/types';

const ROTULO_TOGGLE: Record<string, string> = {
  DEV_PODE_EXCLUIR_TAREFA: 'Dev pode excluir tarefa',
  DEV_PODE_FINALIZAR_TAREFA: 'Dev pode mover tarefa para a etapa final',
  DEV_PODE_EDITAR_TAREFA_INICIADA: 'Dev pode editar tarefa ja iniciada',
  GESTOR_PODE_VER_BOARD: 'Gestor pode ver o board',
};

/**
 * TL-09. A matriz papel × permissao e catalogo fechado (BDR-001) e por isso e somente leitura;
 * o que o administrador ajusta sao os toggles do projeto.
 */
export default function PapeisPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { informar, reportar } = useFeedback();
  const { carregando: carregandoPermissoes, pode, projetoAtivo } = usePermissoes(id);

  const [papeis, setPapeis] = useState<Papel[]>([]);
  const [toggles, setToggles] = useState<Record<string, boolean>>({});
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  const carregar = useCallback(async () => {
    setErro(null);
    try {
      const [listaPapeis, valores] = await Promise.all([api.papeis(id), api.toggles(id)]);
      setPapeis(listaPapeis);
      setToggles(valores);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar papeis e toggles.');
    } finally {
      setCarregando(false);
    }
  }, [id]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  if (carregando || carregandoPermissoes) {
    return <Skeleton linhas={6} />;
  }
  if (!pode('projeto:administrar')) {
    return <SemPermissao />;
  }
  if (erro) {
    return <Erro mensagem={erro} aoTentarNovamente={carregar} />;
  }

  const permissoes = Array.from(new Set(papeis.flatMap((p) => p.permissoes))).sort();

  const salvar = async (chave: string, valor: boolean) => {
    const anteriores = toggles;
    const novos = { ...toggles, [chave]: valor };
    setToggles(novos);
    setSalvando(true);
    try {
      setToggles(await api.atualizarToggles(id, novos));
      informar('Toggles atualizados.');
    } catch (e) {
      setToggles(anteriores);
      reportar(e);
    } finally {
      setSalvando(false);
    }
  };

  return (
    <>
      {!projetoAtivo && <AvisoSomenteLeitura />}

      <section className="cartao">
        <h2>Papeis e permissoes</h2>
        <p>Catalogo fechado: papeis e permissoes sao definidos pelo sistema e nao sao editaveis.</p>
        <table className="tabela">
          <thead>
            <tr>
              <th>Permissao</th>
              {papeis.map((papel) => (
                <th key={papel.codigo}>
                  {papel.nome}
                  {papel.protegido && <span className="badge">protegido</span>}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {permissoes.map((permissao) => (
              <tr key={permissao}>
                <td>{permissao}</td>
                {papeis.map((papel) => (
                  <td key={papel.codigo} aria-label={papel.permissoes.includes(permissao) ? 'sim' : 'nao'}>
                    {papel.permissoes.includes(permissao) ? '●' : '—'}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="cartao">
        <h2>Toggles do projeto</h2>
        <ul>
          {Object.keys(toggles)
            .sort()
            .map((chave) => (
              <li key={chave}>
                <label>
                  <input
                    type="checkbox"
                    checked={toggles[chave]}
                    disabled={salvando || !projetoAtivo}
                    onChange={(e) => salvar(chave, e.target.checked)}
                  />
                  {ROTULO_TOGGLE[chave] ?? chave}
                </label>
              </li>
            ))}
        </ul>
      </section>
    </>
  );
}
