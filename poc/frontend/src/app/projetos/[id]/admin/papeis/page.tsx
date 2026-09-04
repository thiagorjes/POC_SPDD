'use client';

import { use, useCallback, useEffect, useState } from 'react';
import { AvisoSomenteLeitura, Erro, SemPermissao, Skeleton } from '@/components/Estados';
import { useFeedback } from '@/components/Feedback';
import { Badge } from '@/components/ui/Badge';
import { usePermissoes } from '@/hooks/usePermissoes';
import { api } from '@/lib/api';
import type { Papel } from '@/lib/types';

const ROTULO_TOGGLE: Record<string, string> = {
  DEV_PODE_EXCLUIR_TAREFA: 'Dev pode excluir tarefa',
  DEV_PODE_FINALIZAR_TAREFA: 'Dev pode mover tarefa para a etapa final',
  DEV_PODE_EDITAR_TAREFA_INICIADA: 'Dev pode editar tarefa já iniciada',
  GESTOR_PODE_VER_BOARD: 'Gestor pode ver o board',
};

/**
 * TL-09. O catalogo papel × permissao e fechado para os papeis do projeto, inclusive para o admin
 * do projeto. Alterar o catalogo e privilegio do admin global, mas nao existe endpoint de escrita
 * (AdminPapelController expoe apenas GET /papeis) — por isso a matriz segue somente leitura aqui.
 * O que esta tela realmente ajusta sao os toggles do projeto.
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
  const [salvo, setSalvo] = useState(false);

  const carregar = useCallback(async () => {
    setErro(null);
    try {
      const [listaPapeis, valores] = await Promise.all([api.papeis(id), api.toggles(id)]);
      setPapeis(listaPapeis);
      setToggles(valores);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar papéis e toggles.');
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
    setSalvo(false);
    try {
      setToggles(await api.atualizarToggles(id, novos));
      setSalvo(true);
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

      <section className="secao">
        <h2>Papéis e permissões</h2>
        <p className="text-secondary">
          O catálogo de papéis e permissões é fixo para os papéis do projeto, inclusive para o
          administrador do projeto. Alterações no catálogo são privilégio do administrador global e
          ainda não estão disponíveis nesta tela.
        </p>
        <table>
          <thead>
            <tr>
              <th>Permissão</th>
              {papeis.map((papel) => (
                <th key={papel.codigo}>
                  <span className="linha">
                    {papel.nome}
                    {papel.protegido && <Badge variante="neutro">protegido</Badge>}
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {permissoes.map((permissao) => (
              <tr key={permissao}>
                <td>{permissao}</td>
                {papeis.map((papel) => {
                  const tem = papel.permissoes.includes(permissao);
                  return (
                    <td key={papel.codigo} aria-label={tem ? 'sim' : 'não'}>
                      {tem ? <Badge variante="success">●</Badge> : '—'}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="secao">
        <h2>Toggles específicos do projeto</h2>
        {salvo && (
          <div
            className="toast toast-success"
            role="status"
            aria-live="polite"
            style={{ marginBottom: 'var(--espaco-scale-md)' }}
          >
            Permissões atualizadas com sucesso.
          </div>
        )}
        {Object.keys(toggles)
          .sort()
          .map((chave) => (
            <div className="form-field toggle" key={chave}>
              <label htmlFor={`toggle-${chave}`}>
                <input
                  id={`toggle-${chave}`}
                  type="checkbox"
                  checked={toggles[chave]}
                  disabled={salvando || !projetoAtivo}
                  aria-describedby={`toggle-${chave}-desc`}
                  onChange={(e) => salvar(chave, e.target.checked)}
                />
                {ROTULO_TOGGLE[chave] ?? chave}
              </label>
              <span className="text-secondary" id={`toggle-${chave}-desc`}>
                Aplica-se somente a este projeto e é revalidado pelo backend a cada ação.
              </span>
            </div>
          ))}
      </section>
    </>
  );
}
