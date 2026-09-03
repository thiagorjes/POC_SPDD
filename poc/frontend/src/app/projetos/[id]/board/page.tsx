'use client';

import { use, useEffect, useState } from 'react';
import { Board } from '@/components/Board';
import { ConfirmarExclusaoModal } from '@/components/ConfirmarExclusaoModal';
import { AvisoSomenteLeitura, Erro, SemPermissao, Skeleton } from '@/components/Estados';
import { useFeedback } from '@/components/Feedback';
import { NovaTarefaModal } from '@/components/NovaTarefaModal';
import { TarefaDrawer } from '@/components/TarefaDrawer';
import { useBoardStream } from '@/hooks/useBoardStream';
import { usePermissoes } from '@/hooks/usePermissoes';
import { api } from '@/lib/api';
import type { Raia, TarefaDetalhe, TarefaResumo, UsuarioAtual } from '@/lib/types';

/**
 * TL-03. O board vive do snapshot REST e usa o stream apenas como gatilho de resync (ADR-004);
 * o modal de exclusao (TL-06) so e aberto pelo drawer, que conhece o titulo da tarefa.
 */
export default function BoardPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { informar, reportar } = useFeedback();
  const { snapshot, carregando, erro, conectado, resync } = useBoardStream(id);
  const { carregando: carregandoPermissoes, pode, projetoAtivo } = usePermissoes(id);

  const [tarefaAberta, setTarefaAberta] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);
  const [paraExcluir, setParaExcluir] = useState<TarefaDetalhe | null>(null);
  const [raias, setRaias] = useState<Raia[]>([]);
  const [usuarios, setUsuarios] = useState<UsuarioAtual[]>([]);

  useEffect(() => {
    if (!criando) {
      return;
    }
    Promise.all([api.raias(id), api.usuarios()])
      .then(([listaRaias, listaUsuarios]) => {
        setRaias(listaRaias);
        setUsuarios(listaUsuarios);
      })
      .catch(reportar);
  }, [criando, id, reportar]);

  const mover = async (tarefa: TarefaResumo, etapaDestinoId: string, raiaDestinoId?: string) => {
    try {
      await api.moverTarefa(tarefa.id, {
        etapaDestinoId,
        raiaDestinoId,
        versaoEsperada: tarefa.versao,
      });
      await resync();
    } catch (e) {
      await resync();
      reportar(e);
    }
  };

  if (carregando || carregandoPermissoes) {
    return <Skeleton linhas={6} />;
  }
  if (erro) {
    return <Erro mensagem={erro} aoTentarNovamente={resync} />;
  }
  if (!snapshot) {
    return <SemPermissao />;
  }

  return (
    <>
      <header style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h1>Board</h1>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <span className="badge" aria-live="polite">
            {conectado ? 'Tempo real ativo' : 'Reconectando…'}
          </span>
          {pode('tarefa:gerenciar') && !snapshot.somenteLeitura && (
            <button className="primario" onClick={() => setCriando(true)}>
              Nova tarefa
            </button>
          )}
        </div>
      </header>

      {(snapshot.somenteLeitura || !projetoAtivo) && <AvisoSomenteLeitura />}

      <Board
        snapshot={snapshot}
        podeMover={pode('tarefa:mover')}
        aoMover={mover}
        aoAbrir={setTarefaAberta}
        aoNovaTarefa={() => setCriando(true)}
        aoDropInvalido={() =>
          informar('Esta transicao nao esta configurada no workflow ou voce nao tem permissao.')
        }
      />

      {tarefaAberta && (
        <TarefaDrawer
          tarefaId={tarefaAberta}
          somenteLeitura={snapshot.somenteLeitura}
          aoFechar={() => setTarefaAberta(null)}
          aoAlterar={resync}
          aoExcluir={setParaExcluir}
        />
      )}

      {criando && (
        <NovaTarefaModal
          projetoId={id}
          raias={raias}
          usuarios={usuarios}
          aoFechar={() => setCriando(false)}
          aoCriar={() => {
            setCriando(false);
            void resync();
          }}
        />
      )}

      {paraExcluir && (
        <ConfirmarExclusaoModal
          titulo={paraExcluir.titulo}
          impacto="A tarefa, seus periodos de etapa e suas notificacoes serao removidos. O historico de auditoria e preservado."
          aoConfirmar={async () => {
            await api.excluirTarefa(paraExcluir.id);
            setParaExcluir(null);
            setTarefaAberta(null);
            informar('Tarefa excluida.');
            await resync();
          }}
          aoFechar={() => setParaExcluir(null)}
        />
      )}
    </>
  );
}
