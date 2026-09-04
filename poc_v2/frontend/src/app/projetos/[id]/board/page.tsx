'use client';

import { useParams } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { useSessao } from '@/components/AuthProvider';
import { Board } from '@/components/Board';
import { ConfirmarExclusaoModal } from '@/components/ConfirmarExclusaoModal';
import { useFeedback } from '@/components/FeedbackProvider';
import { NovaTarefaModal } from '@/components/NovaTarefaModal';
import { EstadoErro, EstadoVazio, Skeleton } from '@/components/Skeleton';
import { TarefaDrawer } from '@/components/TarefaDrawer';
import type { Densidade } from '@/components/TaskCard';
import { useBoardStream } from '@/hooks/useBoardStream';
import { usePermissoes } from '@/hooks/usePermissoes';
import { api } from '@/lib/api';
import { PERMISSAO, type MembroProjetoResponse, type ProjetoResponse, type TarefaResumoResponse } from '@/lib/types';

/** TL-03 / TL-03b — Board do projeto (RF-001, RF-002, RF-011, RF-012). */
export default function BoardPage() {
  const projetoId = String(useParams().id);
  const { usuario } = useSessao();
  const feedback = useFeedback();
  const permissoes = usePermissoes(projetoId);
  const { snapshot, carregando, erro, recarregar } = useBoardStream(projetoId, usuario?.id ?? null);

  const [projeto, setProjeto] = useState<ProjetoResponse | null>(null);
  const [membros, setMembros] = useState<MembroProjetoResponse[]>([]);
  const [densidade, setDensidade] = useState<Densidade>('compacto');
  const [tarefaAberta, setTarefaAberta] = useState<string | null>(null);
  const [criandoNaRaia, setCriandoNaRaia] = useState<string | null>(null);
  const [excluindo, setExcluindo] = useState<TarefaResumoResponse | null>(null);

  useEffect(() => {
    void api
      .projeto(projetoId)
      .then(setProjeto)
      .catch(() => setProjeto(null));
    void api
      .membros(projetoId)
      .then(setMembros)
      .catch(() => setMembros([]));
  }, [projetoId]);

  const nomePorUsuario = Object.fromEntries(membros.map((m) => [m.usuarioId, m.nome]));

  const mover = useCallback(
    async (tarefa: TarefaResumoResponse, etapaDestinoId: string, raiaDestinoId: string) => {
      try {
        await api.moverTarefa(tarefa.id, {
          etapaDestinoId,
          raiaDestinoId,
          versaoEsperada: tarefa.versao,
        });
        await recarregar();
      } catch (erroMovimento) {
        feedback.reportar(erroMovimento);
        await recarregar();
      }
    },
    [recarregar, feedback],
  );

  const podeCriar = permissoes.possui(PERMISSAO.TAREFA_GERENCIAR);
  const podeExcluir = permissoes.possui(PERMISSAO.TAREFA_GERENCIAR);
  const somenteLeitura = snapshot?.somenteLeitura === true;

  return (
    <AppShell projetoId={projetoId} projetoNome={projeto?.nome}>
      <div className="page-header">
        <h1>
          Board — {projeto?.nome ?? '…'}{' '}
          {somenteLeitura && <span className="badge badge-neutral">Somente leitura</span>}
        </h1>
        <div className="page-header__actions">
          <button
            className="btn btn-outline"
            type="button"
            onClick={() =>
              setDensidade((atual) => (atual === 'compacto' ? 'expandido' : 'compacto'))
            }
          >
            {densidade === 'compacto' ? 'Ver cards expandidos' : 'Ver cards compactos'}
          </button>
          <button
            className="btn btn-primary"
            type="button"
            disabled={!podeCriar || somenteLeitura}
            onClick={() => setCriandoNaRaia('')}
          >
            + Novo card
          </button>
        </div>
      </div>

      {carregando && <Skeleton linhas={4} />}

      {!carregando && erro && <EstadoErro mensagem={erro} aoTentar={() => void recarregar()} />}

      {!carregando && !erro && snapshot && snapshot.etapas.length === 0 && (
        <EstadoVazio>Este projeto ainda não possui workflow configurado.</EstadoVazio>
      )}

      {!carregando && !erro && snapshot && snapshot.etapas.length > 0 && (
        <Board
          snapshot={snapshot}
          densidade={densidade}
          podeCriar={podeCriar}
          podeExcluir={podeExcluir}
          nomePorUsuario={nomePorUsuario}
          aoAbrirTarefa={setTarefaAberta}
          aoMover={(tarefa, etapaDestinoId, raiaDestinoId) =>
            void mover(tarefa, etapaDestinoId, raiaDestinoId)
          }
          aoExcluirTarefa={setExcluindo}
          aoNovoCard={setCriandoNaRaia}
          aoDropInvalido={() =>
            feedback.falha(
              'Transição não permitida a partir desta etapa. O card retornou à posição original.',
            )
          }
        />
      )}

      {criandoNaRaia !== null && snapshot && (
        <NovaTarefaModal
          projetoId={projetoId}
          raias={snapshot.raias}
          membros={membros}
          raiaPreSelecionada={criandoNaRaia === '' ? null : criandoNaRaia}
          aoFechar={() => setCriandoNaRaia(null)}
          aoCriar={() => {
            setCriandoNaRaia(null);
            void recarregar();
          }}
        />
      )}

      {tarefaAberta && snapshot && (
        <TarefaDrawer
          tarefaId={tarefaAberta}
          etapas={snapshot.etapas}
          raias={snapshot.raias}
          membros={membros}
          somenteLeitura={somenteLeitura}
          podeEditar={permissoes.possui(PERMISSAO.TAREFA_GERENCIAR)}
          podeImpedir={permissoes.possui(PERMISSAO.TAREFA_IMPEDIR)}
          podeAtribuir={
            permissoes.possui(PERMISSAO.TAREFA_ATRIBUIR) || permissoes.possui(PERMISSAO.TAREFA_MOVER)
          }
          aoFechar={() => setTarefaAberta(null)}
          aoAlterar={() => void recarregar()}
        />
      )}

      {excluindo && (
        <ConfirmarExclusaoModal
          tarefaId={excluindo.id}
          titulo={excluindo.titulo}
          permitido={podeExcluir && !somenteLeitura}
          aoFechar={() => setExcluindo(null)}
          aoExcluir={() => {
            setExcluindo(null);
            void recarregar();
          }}
        />
      )}
    </AppShell>
  );
}
