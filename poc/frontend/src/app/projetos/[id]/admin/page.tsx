'use client';

import { use, useCallback, useEffect, useState } from 'react';
import { ConfirmarExclusaoModal } from '@/components/ConfirmarExclusaoModal';
import { AvisoSomenteLeitura, Erro, SemPermissao, Skeleton } from '@/components/Estados';
import { useFeedback } from '@/components/Feedback';
import { Badge } from '@/components/ui/Badge';
import { Botao } from '@/components/ui/Botao';
import { EstadoVazio } from '@/components/ui/EstadoVazio';
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
      setErro(e instanceof Error ? e.message : 'Falha ao carregar a configuração.');
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
        <div
          className="toast toast-error"
          role="alert"
          style={{ marginBottom: 'var(--espaco-scale-md)' }}
        >
          {avisoInline}
        </div>
      )}
      {semSaida.length > 0 && (
        <div
          className="toast toast-error"
          role="status"
          style={{ marginBottom: 'var(--espaco-scale-md)' }}
        >
          Etapas não finais sem transição de saída: {semSaida.map((e) => e.nome).join(', ')}. O
          workflow só pode ser ativado quando toda etapa não final tiver ao menos uma saída (RN-003).
        </div>
      )}

      <section className="secao">
        <h2>Workflows</h2>
        {workflows.length === 0 ? (
          <EstadoVazio mensagem="Este projeto ainda não possui workflow configurado." />
        ) : (
          <table>
            <thead>
              <tr>
                <th>Nome</th>
                <th>Situação</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {workflows.map((workflow) => (
                <tr key={workflow.id} aria-selected={workflow.id === selecionado}>
                  <td>
                    <Botao variante="text" onClick={() => setSelecionado(workflow.id)}>
                      {workflow.nome}
                    </Botao>
                  </td>
                  <td>
                    {workflow.ativo ? <Badge variante="success">Ativo</Badge> : 'Inativo'}
                  </td>
                  <td>
                    <span className="linha">
                      {!workflow.ativo && (
                        <Botao
                          variante="text"
                          disabled={somenteLeitura}
                          onClick={() =>
                            executar(() => api.ativarWorkflow(id, workflow.id), 'Workflow ativado.')
                          }
                        >
                          Ativar
                        </Botao>
                      )}
                      <Botao
                        variante="text"
                        disabled={somenteLeitura}
                        onClick={() =>
                          setAlvo({ tipo: 'workflow', id: workflow.id, titulo: workflow.nome })
                        }
                      >
                        Excluir
                      </Botao>
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <form
          className="form-row"
          style={{ marginTop: 'var(--espaco-scale-md)' }}
          onSubmit={(evento) => {
            evento.preventDefault();
            void executar(() => api.criarWorkflow(id, nomeWorkflow), 'Workflow criado.').then(() =>
              setNomeWorkflow(''),
            );
          }}
        >
          <div className="form-field">
            <label htmlFor="wf-nome">Nome do workflow</label>
            <input
              id="wf-nome"
              value={nomeWorkflow}
              onChange={(e) => setNomeWorkflow(e.target.value)}
              required
              disabled={somenteLeitura}
            />
          </div>
          <Botao variante="primary" type="submit" disabled={somenteLeitura}>
            Adicionar
          </Botao>
        </form>
      </section>

      {selecionado && (
        <>
          <section className="secao">
            <h2>Colunas</h2>
            <table>
              <thead>
                <tr>
                  <th>Ordem</th>
                  <th>Nome</th>
                  <th>Final</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {etapas.map((etapa, indice) => (
                  <tr key={etapa.id}>
                    <td>{etapa.ordem}</td>
                    <td>{etapa.nome}</td>
                    <td>{etapa.etapaFinal ? 'Sim' : 'Não'}</td>
                    <td>
                      <span className="linha">
                        <Botao
                          variante="text"
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
                        </Botao>
                        <Botao
                          variante="text"
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
                        </Botao>
                        <Botao
                          variante="text"
                          disabled={somenteLeitura}
                          onClick={() => setAlvo({ tipo: 'etapa', id: etapa.id, titulo: etapa.nome })}
                        >
                          Excluir
                        </Botao>
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <form
              className="form-row"
              style={{ marginTop: 'var(--espaco-scale-md)' }}
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
              <div className="form-field">
                <label htmlFor="etapa-nome">Nome da coluna</label>
                <input
                  id="etapa-nome"
                  value={nomeEtapa}
                  onChange={(e) => setNomeEtapa(e.target.value)}
                  required
                  disabled={somenteLeitura}
                />
              </div>
              <div className="form-field toggle">
                <label htmlFor="etapa-final">
                  <input
                    id="etapa-final"
                    type="checkbox"
                    checked={etapaFinal}
                    onChange={(e) => setEtapaFinal(e.target.checked)}
                    disabled={somenteLeitura}
                  />
                  Etapa final
                </label>
              </div>
              <Botao variante="primary" type="submit" disabled={somenteLeitura}>
                Adicionar
              </Botao>
            </form>
          </section>

          <section className="secao">
            <h2>Transições</h2>
            {transicoes.length === 0 ? (
              <EstadoVazio mensagem="Nenhuma transição configurada." />
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Origem → Destino</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {transicoes.map((transicao) => (
                    <tr key={transicao.id}>
                      <td>
                        {nomeEtapaDe(transicao.etapaOrigemId)} →{' '}
                        {nomeEtapaDe(transicao.etapaDestinoId)}
                      </td>
                      <td>
                        <Botao
                          variante="text"
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
                        </Botao>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            <form
              className="form-row"
              style={{ marginTop: 'var(--espaco-scale-md)' }}
              onSubmit={(evento) => {
                evento.preventDefault();
                void executar(
                  () => api.criarTransicao(selecionado, origem, destino),
                  'Transição criada.',
                );
              }}
            >
              <div className="form-field">
                <label htmlFor="tr-origem">Etapa de origem</label>
                <select
                  id="tr-origem"
                  value={origem}
                  onChange={(e) => setOrigem(e.target.value)}
                  required
                  disabled={somenteLeitura}
                >
                  <option value="">Selecione…</option>
                  {etapas
                    .filter((e) => !e.etapaFinal)
                    .map((etapa) => (
                      <option key={etapa.id} value={etapa.id}>
                        {etapa.nome}
                      </option>
                    ))}
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="tr-destino">Etapa de destino</label>
                <select
                  id="tr-destino"
                  value={destino}
                  onChange={(e) => setDestino(e.target.value)}
                  required
                  disabled={somenteLeitura}
                >
                  <option value="">Selecione…</option>
                  {etapas
                    .filter((e) => e.id !== origem)
                    .map((etapa) => (
                      <option key={etapa.id} value={etapa.id}>
                        {etapa.nome}
                      </option>
                    ))}
                </select>
              </div>
              <Botao variante="primary" type="submit" disabled={somenteLeitura}>
                Adicionar
              </Botao>
            </form>
          </section>
        </>
      )}

      <section className="secao">
        <h2>Raias</h2>
        <table>
          <thead>
            <tr>
              <th>Ordem</th>
              <th>Nome</th>
              <th>Padrão</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {raias.map((raia) => (
              <tr key={raia.id}>
                <td>{raia.ordem}</td>
                <td>{raia.nome}</td>
                <td>{raia.padrao ? 'Sim' : 'Não'}</td>
                <td>
                  <span className="linha">
                    {!raia.padrao && (
                      <Botao
                        variante="text"
                        disabled={somenteLeitura}
                        onClick={() =>
                          executar(() => api.definirRaiaPadrao(id, raia.id), 'Raia padrão definida.')
                        }
                      >
                        Tornar padrão
                      </Botao>
                    )}
                    <Botao
                      variante="text"
                      disabled={somenteLeitura}
                      onClick={() => setAlvo({ tipo: 'raia', id: raia.id, titulo: raia.nome })}
                    >
                      Excluir
                    </Botao>
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <form
          className="form-row"
          style={{ marginTop: 'var(--espaco-scale-md)' }}
          onSubmit={(evento) => {
            evento.preventDefault();
            void executar(() => api.criarRaia(id, nomeRaia), 'Raia criada.').then(() =>
              setNomeRaia(''),
            );
          }}
        >
          <div className="form-field">
            <label htmlFor="raia-nome">Nome da raia</label>
            <input
              id="raia-nome"
              value={nomeRaia}
              onChange={(e) => setNomeRaia(e.target.value)}
              required
              disabled={somenteLeitura}
            />
          </div>
          <Botao variante="primary" type="submit" disabled={somenteLeitura}>
            Adicionar
          </Botao>
        </form>
      </section>

      {alvo && (
        <ConfirmarExclusaoModal
          titulo={alvo.titulo}
          impacto="A exclusão é recusada pelo servidor se houver tarefa ativa vinculada ou se o grafo do workflow ficar inválido."
          aoConfirmar={async () => {
            const acoes: Record<Alvo['tipo'], () => Promise<void>> = {
              workflow: () => api.excluirWorkflow(id, alvo.id),
              etapa: () => api.excluirEtapa(selecionado!, alvo.id),
              transicao: () => api.excluirTransicao(selecionado!, alvo.id),
              raia: () => api.excluirRaia(id, alvo.id),
            };
            await acoes[alvo.tipo]();
            setAlvo(null);
            informar('Exclusão concluída.');
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
