'use client';

import { use, useCallback, useEffect, useState } from 'react';
import { ConfirmarExclusaoModal } from '@/components/ConfirmarExclusaoModal';
import { AvisoSomenteLeitura, Erro, SemPermissao, Skeleton, Vazio } from '@/components/Estados';
import { useFeedback } from '@/components/Feedback';
import { usePermissoes } from '@/hooks/usePermissoes';
import { ApiError, api } from '@/lib/api';
import type { Etapa, Raia, Transicao, Workflow } from '@/lib/types';

type Alvo = { tipo: 'workflow' | 'etapa' | 'transicao' | 'raia'; id: string; titulo: string };

/**
 * TL-08. A recusa de RN-003 (grafo incompleto) e RN-005 (recurso com tarefa ativa) chega do
 * backend e e exibida <b>inline</b> nesta tela, com o texto que o servidor devolveu.
 */
export default function AdminWorkflowPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { informar, reportar } = useFeedback();
  const { carregando: carregandoPermissoes, pode, projetoAtivo } = usePermissoes(id);

  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [selecionado, setSelecionado] = useState<string | null>(null);
  const [etapas, setEtapas] = useState<Etapa[]>([]);
  const [transicoes, setTransicoes] = useState<Transicao[]>([]);
  const [raias, setRaias] = useState<Raia[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [avisoInline, setAvisoInline] = useState<string | null>(null);
  const [alvo, setAlvo] = useState<Alvo | null>(null);

  const [nomeWorkflow, setNomeWorkflow] = useState('');
  const [nomeEtapa, setNomeEtapa] = useState('');
  const [etapaFinal, setEtapaFinal] = useState(false);
  const [origem, setOrigem] = useState('');
  const [destino, setDestino] = useState('');
  const [nomeRaia, setNomeRaia] = useState('');

  const somenteLeitura = !projetoAtivo;

  const carregar = useCallback(async () => {
    setErro(null);
    try {
      const [lista, listaRaias] = await Promise.all([api.workflows(id), api.raias(id)]);
      setWorkflows(lista);
      setRaias(listaRaias);
      const ativo = lista.find((w) => w.ativo) ?? lista[0];
      setSelecionado(ativo?.id ?? null);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar a configuracao.');
    } finally {
      setCarregando(false);
    }
  }, [id]);

  const carregarGrafo = useCallback(async (workflowId: string) => {
    const [listaEtapas, listaTransicoes] = await Promise.all([
      api.etapas(workflowId),
      api.transicoes(workflowId),
    ]);
    setEtapas(listaEtapas);
    setTransicoes(listaTransicoes);
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  useEffect(() => {
    if (selecionado) {
      carregarGrafo(selecionado).catch(reportar);
    }
  }, [selecionado, carregarGrafo, reportar]);

  /** Recusa de regra de configuracao fica inline; o resto segue o fluxo de toast/modal. */
  const executar = async (acao: () => Promise<unknown>, sucesso: string) => {
    setAvisoInline(null);
    try {
      await acao();
      informar(sucesso);
      if (selecionado) {
        await carregarGrafo(selecionado);
      }
      await carregar();
    } catch (e) {
      if (
        e instanceof ApiError &&
        ['WORKFLOW_INVALIDO', 'RECURSO_POSSUI_TAREFAS_ATIVAS'].includes(e.errorCode)
      ) {
        setAvisoInline(e.message);
        return;
      }
      reportar(e);
    }
  };

  if (carregando || carregandoPermissoes) {
    return <Skeleton linhas={6} />;
  }
  if (!pode('workflow:gerenciar')) {
    return <SemPermissao />;
  }
  if (erro) {
    return <Erro mensagem={erro} aoTentarNovamente={carregar} />;
  }

  const nomeEtapaDe = (etapaId: string) => etapas.find((e) => e.id === etapaId)?.nome ?? etapaId;
  const semSaida = etapas.filter(
    (e) => !e.etapaFinal && !transicoes.some((t) => t.etapaOrigemId === e.id),
  );

  return (
    <>
      {somenteLeitura && <AvisoSomenteLeitura />}
      {avisoInline && (
        <p className="erro-inline" role="alert">
          {avisoInline}
        </p>
      )}
      {semSaida.length > 0 && (
        <p className="erro-inline" role="status">
          Etapas nao finais sem transicao de saida: {semSaida.map((e) => e.nome).join(', ')}. O
          workflow so pode ser ativado quando toda etapa nao final tiver ao menos uma saida (RN-003).
        </p>
      )}

      <section className="cartao">
        <h2>Workflows</h2>
        {workflows.length === 0 ? (
          <Vazio titulo="Nenhum workflow configurado." />
        ) : (
          <table className="tabela">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Situacao</th>
                <th>Acoes</th>
              </tr>
            </thead>
            <tbody>
              {workflows.map((workflow) => (
                <tr key={workflow.id} aria-selected={workflow.id === selecionado}>
                  <td>
                    <button onClick={() => setSelecionado(workflow.id)}>{workflow.nome}</button>
                  </td>
                  <td>{workflow.ativo ? <span className="badge">Ativo</span> : 'Inativo'}</td>
                  <td style={{ display: 'flex', gap: 8 }}>
                    {!workflow.ativo && (
                      <button
                        disabled={somenteLeitura}
                        onClick={() =>
                          executar(() => api.ativarWorkflow(id, workflow.id), 'Workflow ativado.')
                        }
                      >
                        Ativar
                      </button>
                    )}
                    <button
                      disabled={somenteLeitura}
                      onClick={() =>
                        setAlvo({ tipo: 'workflow', id: workflow.id, titulo: workflow.nome })
                      }
                    >
                      Excluir
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <form
          style={{ display: 'flex', gap: 8, marginTop: 12 }}
          onSubmit={(evento) => {
            evento.preventDefault();
            void executar(() => api.criarWorkflow(id, nomeWorkflow), 'Workflow criado.').then(() =>
              setNomeWorkflow(''),
            );
          }}
        >
          <input
            value={nomeWorkflow}
            onChange={(e) => setNomeWorkflow(e.target.value)}
            placeholder="Nome do workflow"
            required
            disabled={somenteLeitura}
          />
          <button className="primario" type="submit" disabled={somenteLeitura}>
            Adicionar
          </button>
        </form>
      </section>

      {selecionado && (
        <>
          <section className="cartao">
            <h2>Colunas</h2>
            <table className="tabela">
              <thead>
                <tr>
                  <th>Ordem</th>
                  <th>Nome</th>
                  <th>Final</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {etapas.map((etapa, indice) => (
                  <tr key={etapa.id}>
                    <td>{etapa.ordem}</td>
                    <td>{etapa.nome}</td>
                    <td>{etapa.etapaFinal ? 'Sim' : 'Nao'}</td>
                    <td style={{ display: 'flex', gap: 8 }}>
                      <button
                        disabled={somenteLeitura || indice === 0}
                        aria-label={`Subir ${etapa.nome}`}
                        onClick={() => {
                          const ordem = etapas.map((e) => e.id);
                          [ordem[indice - 1], ordem[indice]] = [ordem[indice], ordem[indice - 1]];
                          void executar(
                            () => api.reordenarEtapas(selecionado, ordem),
                            'Ordem atualizada.',
                          );
                        }}
                      >
                        ↑
                      </button>
                      <button
                        disabled={somenteLeitura || indice === etapas.length - 1}
                        aria-label={`Descer ${etapa.nome}`}
                        onClick={() => {
                          const ordem = etapas.map((e) => e.id);
                          [ordem[indice], ordem[indice + 1]] = [ordem[indice + 1], ordem[indice]];
                          void executar(
                            () => api.reordenarEtapas(selecionado, ordem),
                            'Ordem atualizada.',
                          );
                        }}
                      >
                        ↓
                      </button>
                      <button
                        disabled={somenteLeitura}
                        onClick={() => setAlvo({ tipo: 'etapa', id: etapa.id, titulo: etapa.nome })}
                      >
                        Excluir
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <form
              style={{ display: 'flex', gap: 8, marginTop: 12, alignItems: 'center' }}
              onSubmit={(evento) => {
                evento.preventDefault();
                void executar(
                  () => api.criarEtapa(selecionado, { nome: nomeEtapa, etapaFinal }),
                  'Coluna criada.',
                ).then(() => {
                  setNomeEtapa('');
                  setEtapaFinal(false);
                });
              }}
            >
              <input
                value={nomeEtapa}
                onChange={(e) => setNomeEtapa(e.target.value)}
                placeholder="Nome da coluna"
                required
                disabled={somenteLeitura}
              />
              <label>
                <input
                  type="checkbox"
                  checked={etapaFinal}
                  onChange={(e) => setEtapaFinal(e.target.checked)}
                  disabled={somenteLeitura}
                />
                Etapa final
              </label>
              <button className="primario" type="submit" disabled={somenteLeitura}>
                Adicionar
              </button>
            </form>
          </section>

          <section className="cartao">
            <h2>Transicoes</h2>
            {transicoes.length === 0 ? (
              <Vazio titulo="Nenhuma transicao configurada." />
            ) : (
              <ul>
                {transicoes.map((transicao) => (
                  <li key={transicao.id} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <span>
                      {nomeEtapaDe(transicao.etapaOrigemId)} → {nomeEtapaDe(transicao.etapaDestinoId)}
                    </span>
                    <button
                      disabled={somenteLeitura}
                      onClick={() =>
                        setAlvo({
                          tipo: 'transicao',
                          id: transicao.id,
                          titulo: `${nomeEtapaDe(transicao.etapaOrigemId)} → ${nomeEtapaDe(transicao.etapaDestinoId)}`,
                        })
                      }
                    >
                      Excluir
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <form
              style={{ display: 'flex', gap: 8, marginTop: 12 }}
              onSubmit={(evento) => {
                evento.preventDefault();
                void executar(
                  () => api.criarTransicao(selecionado, origem, destino),
                  'Transicao criada.',
                );
              }}
            >
              <select
                value={origem}
                onChange={(e) => setOrigem(e.target.value)}
                required
                disabled={somenteLeitura}
                aria-label="Etapa de origem"
              >
                <option value="">Origem…</option>
                {etapas
                  .filter((e) => !e.etapaFinal)
                  .map((etapa) => (
                    <option key={etapa.id} value={etapa.id}>
                      {etapa.nome}
                    </option>
                  ))}
              </select>
              <select
                value={destino}
                onChange={(e) => setDestino(e.target.value)}
                required
                disabled={somenteLeitura}
                aria-label="Etapa de destino"
              >
                <option value="">Destino…</option>
                {etapas
                  .filter((e) => e.id !== origem)
                  .map((etapa) => (
                    <option key={etapa.id} value={etapa.id}>
                      {etapa.nome}
                    </option>
                  ))}
              </select>
              <button className="primario" type="submit" disabled={somenteLeitura}>
                Adicionar
              </button>
            </form>
          </section>
        </>
      )}

      <section className="cartao">
        <h2>Raias</h2>
        <table className="tabela">
          <thead>
            <tr>
              <th>Ordem</th>
              <th>Nome</th>
              <th>Padrao</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {raias.map((raia) => (
              <tr key={raia.id}>
                <td>{raia.ordem}</td>
                <td>{raia.nome}</td>
                <td>{raia.padrao ? 'Sim' : 'Nao'}</td>
                <td style={{ display: 'flex', gap: 8 }}>
                  {!raia.padrao && (
                    <button
                      disabled={somenteLeitura}
                      onClick={() =>
                        executar(() => api.definirRaiaPadrao(id, raia.id), 'Raia padrao definida.')
                      }
                    >
                      Tornar padrao
                    </button>
                  )}
                  <button
                    disabled={somenteLeitura}
                    onClick={() => setAlvo({ tipo: 'raia', id: raia.id, titulo: raia.nome })}
                  >
                    Excluir
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <form
          style={{ display: 'flex', gap: 8, marginTop: 12 }}
          onSubmit={(evento) => {
            evento.preventDefault();
            void executar(() => api.criarRaia(id, nomeRaia), 'Raia criada.').then(() =>
              setNomeRaia(''),
            );
          }}
        >
          <input
            value={nomeRaia}
            onChange={(e) => setNomeRaia(e.target.value)}
            placeholder="Nome da raia"
            required
            disabled={somenteLeitura}
          />
          <button className="primario" type="submit" disabled={somenteLeitura}>
            Adicionar
          </button>
        </form>
      </section>

      {alvo && (
        <ConfirmarExclusaoModal
          titulo={alvo.titulo}
          impacto="A exclusao e recusada pelo servidor se houver tarefa ativa vinculada ou se o grafo do workflow ficar invalido."
          aoConfirmar={async () => {
            const acoes: Record<Alvo['tipo'], () => Promise<void>> = {
              workflow: () => api.excluirWorkflow(id, alvo.id),
              etapa: () => api.excluirEtapa(selecionado!, alvo.id),
              transicao: () => api.excluirTransicao(selecionado!, alvo.id),
              raia: () => api.excluirRaia(id, alvo.id),
            };
            await acoes[alvo.tipo]();
            setAlvo(null);
            informar('Exclusao concluida.');
            if (selecionado) {
              await carregarGrafo(selecionado);
            }
            await carregar();
          }}
          aoFechar={() => setAlvo(null)}
        />
      )}
    </>
  );
}
