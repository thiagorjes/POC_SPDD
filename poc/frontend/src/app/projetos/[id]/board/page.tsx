'use client';

import { use, useEffect, useState } from 'react';
import { Board } from '@/components/Board';
import { ConfirmarExclusaoModal } from '@/components/ConfirmarExclusaoModal';
import { AvisoSomenteLeitura, Erro, SemPermissao, Skeleton } from '@/components/Estados';
import { useFeedback } from '@/components/Feedback';
import { NovaTarefaModal } from '@/components/NovaTarefaModal';
import { TarefaDrawer } from '@/components/TarefaDrawer';
import { Badge } from '@/components/ui/Badge';
import { Botao } from '@/components/ui/Botao';
import { PageHeader } from '@/components/ui/PageHeader';
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
  const [erroTransicao, setErroTransicao] = useState<string | null>(null);

  useEffect(() => {
    if (!erroTransicao) {
      return;
    }
    const timer = setTimeout(() => setErroTransicao(null), 7000);
    return () => clearTimeout(timer);
  }, [erroTransicao]);

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
      <PageHeader
        titulo="Board"
        acoes={
          <>
            <Badge variante={conectado ? 'success' : 'warning'}>
              <span aria-live="polite">{conectado ? 'Tempo real ativo' : 'Reconectando…'}</span>
            </Badge>
            {pode('tarefa:gerenciar') && !snapshot.somenteLeitura && (
              <Botao variante="primary" onClick={() => setCriando(true)}>
                + Novo card
              </Botao>
            )}
          </>
        }
      />

      {(snapshot.somenteLeitura || !projetoAtivo) && <AvisoSomenteLeitura />}

      {/*
        Recusa de transicao fica inline acima do board, com aria-live assertivo: um toast no canto
        fixo passa despercebido enquanto o usuario esta com o foco no scroll horizontal das colunas.
      */}
      {erroTransicao && (
        <div
          className="toast toast-error"
          role="alert"
          aria-live="assertive"
          style={{ marginBottom: 'var(--espaco-scale-md)' }}
        >
          {erroTransicao}
        </div>
      )}

      <Board
        snapshot={snapshot}
        podeMover={pode('tarefa:mover')}
        aoMover={mover}
        aoAbrir={setTarefaAberta}
        aoNovaTarefa={() => setCriando(true)}
        aoDropInvalido={() =>
          setErroTransicao(
            'Transição não permitida a partir desta etapa. O card retornou à posição original.',
          )
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
          impacto="A tarefa, seus períodos de etapa e suas notificações serão removidos. O histórico de auditoria é preservado."
          aoConfirmar={async () => {
            await api.excluirTarefa(paraExcluir.id);
            setParaExcluir(null);
            setTarefaAberta(null);
            informar('Tarefa excluída.');
            await resync();
          }}
          aoFechar={() => setParaExcluir(null)}
        />
      )}
    </>
  );
}
