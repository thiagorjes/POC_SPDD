'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { useFeedback } from '@/components/FeedbackProvider';
import { EstadoVazio, Skeleton } from '@/components/Skeleton';
import { usePermissoes } from '@/hooks/usePermissoes';
import { api } from '@/lib/api';
import {
  PERMISSAO,
  type EtapaResponse,
  type ProjetoResponse,
  type RaiaResponse,
  type TransicaoResponse,
  type WorkflowResponse,
} from '@/lib/types';

type Aba = 'colunas' | 'transicoes' | 'raias';

/**
 * TL-08 — Admin do projeto (RF-009, RF-010, RF-002, RF-011).
 *
 * Reordenar colunas nunca altera o grafo de transicoes; toda mutacao de etapa ou transicao e
 * revalidada pelo backend contra RN-003 (etapa nao-final precisa de saida) e RN-005 (recurso com
 * tarefa ativa nao e excluido). A mensagem de erro exibida vem do backend.
 */
export default function AdminProjetoPage() {
  const projetoId = String(useParams().id);
  const feedback = useFeedback();
  const permissoes = usePermissoes(projetoId);

  const [aba, setAba] = useState<Aba>('colunas');
  const [projeto, setProjeto] = useState<ProjetoResponse | null>(null);
  const [workflows, setWorkflows] = useState<WorkflowResponse[]>([]);
  const [etapas, setEtapas] = useState<EtapaResponse[]>([]);
  const [transicoes, setTransicoes] = useState<TransicaoResponse[]>([]);
  const [raias, setRaias] = useState<RaiaResponse[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [nomeNovaEtapa, setNomeNovaEtapa] = useState('');
  const [nomeNovaRaia, setNomeNovaRaia] = useState('');
  const [origem, setOrigem] = useState('');
  const [destino, setDestino] = useState('');

  const ativo = workflows.find((workflow) => workflow.ativo) ?? null;

  const carregar = useCallback(async () => {
    setCarregando(true);
    try {
      const [projetoCarregado, workflowsCarregados, raiasCarregadas] = await Promise.all([
        api.projeto(projetoId),
        api.workflows(projetoId),
        api.raias(projetoId),
      ]);
      setProjeto(projetoCarregado);
      setWorkflows(workflowsCarregados);
      setRaias(raiasCarregadas);

      const workflowAtivo = workflowsCarregados.find((workflow) => workflow.ativo);
      if (workflowAtivo) {
        const [etapasCarregadas, transicoesCarregadas] = await Promise.all([
          api.etapas(workflowAtivo.id),
          api.transicoes(workflowAtivo.id),
        ]);
        setEtapas(etapasCarregadas);
        setTransicoes(transicoesCarregadas);
      } else {
        setEtapas([]);
        setTransicoes([]);
      }
    } catch (erro) {
      feedback.reportar(erro);
    } finally {
      setCarregando(false);
    }
    // feedback e estavel; a carga depende apenas do projeto.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projetoId]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  async function executar(acao: () => Promise<unknown>, mensagem: string) {
    try {
      await acao();
      feedback.sucesso(mensagem);
      await carregar();
    } catch (erro) {
      feedback.reportar(erro);
    }
  }

  function saidasDe(etapaId: string): string {
    const nomes = transicoes
      .filter((transicao) => transicao.etapaOrigemId === etapaId)
      .map(
        (transicao) => etapas.find((etapa) => etapa.id === transicao.etapaDestinoId)?.nome ?? '—',
      );
    return nomes.length === 0 ? '— (permite desfinalizar)' : nomes.join(', ');
  }

  async function reordenar(etapaId: string, direcao: -1 | 1) {
    const ordenadas = [...etapas].sort((a, b) => a.ordem - b.ordem);
    const indice = ordenadas.findIndex((etapa) => etapa.id === etapaId);
    const alvo = indice + direcao;
    if (!ativo || indice < 0 || alvo < 0 || alvo >= ordenadas.length) {
      return;
    }
    const trocada = [...ordenadas];
    const item = trocada[indice];
    const vizinho = trocada[alvo];
    if (!item || !vizinho) {
      return;
    }
    trocada[indice] = vizinho;
    trocada[alvo] = item;
    await executar(
      () => api.reordenarEtapas(ativo.id, { ordemIds: trocada.map((etapa) => etapa.id) }),
      'Configuração de workflow salva com sucesso.',
    );
  }

  const podeGerenciarWorkflow = permissoes.possui(PERMISSAO.WORKFLOW_GERENCIAR);
  const podeGerenciarRaia = permissoes.possui(PERMISSAO.RAIA_GERENCIAR);
  const podeAdministrar = permissoes.possui(PERMISSAO.PROJETO_ADMINISTRAR);
  const finalizado = projeto?.status === 'FINALIZADO';

  return (
    <AppShell projetoId={projetoId} projetoNome={projeto?.nome}>
      <div className="page-header">
        <h1>Admin de Projeto</h1>
        <div className="page-header__actions">
          <Link href={`/projetos/${projetoId}/admin/papeis`} className="btn btn-outline">
            Papéis/Permissões
          </Link>
          <Link href={`/projetos/${projetoId}/admin/usuarios`} className="btn btn-outline">
            Usuários
          </Link>
          {finalizado ? (
            <button
              className="btn btn-primary"
              type="button"
              disabled={!podeAdministrar}
              onClick={() =>
                void executar(() => api.reabrirProjeto(projetoId), 'Projeto reaberto.')
              }
            >
              Reabrir projeto
            </button>
          ) : (
            <button
              className="btn btn-danger"
              type="button"
              disabled={!podeAdministrar}
              onClick={() =>
                void executar(() => api.finalizarProjeto(projetoId), 'Projeto finalizado.')
              }
            >
              Finalizar projeto
            </button>
          )}
        </div>
      </div>

      <div className="tabs" role="tablist" aria-label="Configuração de workflow">
        <button role="tab" aria-selected={aba === 'colunas'} onClick={() => setAba('colunas')}>
          Colunas
        </button>
        <button
          role="tab"
          aria-selected={aba === 'transicoes'}
          onClick={() => setAba('transicoes')}
        >
          Transições
        </button>
        <button role="tab" aria-selected={aba === 'raias'} onClick={() => setAba('raias')}>
          Raias
        </button>
      </div>

      {carregando && <Skeleton linhas={3} />}

      {!carregando && !ativo && (
        <EstadoVazio>
          Este projeto ainda não possui workflow configurado.
          <br />
          <button
            className="btn btn-primary"
            type="button"
            disabled={!podeGerenciarWorkflow}
            onClick={() =>
              void executar(
                () => api.criarWorkflow(projetoId, { nome: 'Workflow principal' }),
                'Workflow criado.',
              )
            }
          >
            Criar workflow
          </button>
        </EstadoVazio>
      )}

      {!carregando && ativo && aba === 'colunas' && (
        <section aria-label="Colunas do workflow">
          <table>
            <thead>
              <tr>
                <th>Ordem</th>
                <th>Coluna</th>
                <th>Transições de saída</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {[...etapas]
                .sort((a, b) => a.ordem - b.ordem)
                .map((etapa) => (
                  <tr key={etapa.id}>
                    <td>{etapa.ordem + 1}</td>
                    <td>
                      {etapa.nome}
                      {etapa.etapaFinal ? ' (etapa final)' : ''}
                    </td>
                    <td>{saidasDe(etapa.id)}</td>
                    <td>
                      <button
                        className="btn btn-text"
                        type="button"
                        disabled={!podeGerenciarWorkflow}
                        onClick={() => void reordenar(etapa.id, -1)}
                      >
                        ↑
                      </button>
                      <button
                        className="btn btn-text"
                        type="button"
                        disabled={!podeGerenciarWorkflow}
                        onClick={() => void reordenar(etapa.id, 1)}
                      >
                        ↓
                      </button>
                      <button
                        className="btn btn-text"
                        type="button"
                        disabled={!podeGerenciarWorkflow}
                        onClick={() =>
                          void executar(
                            () => api.excluirEtapa(ativo.id, etapa.id),
                            'Configuração de workflow salva com sucesso.',
                          )
                        }
                      >
                        Excluir
                      </button>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>

          <div className="form-field">
            <label htmlFor="nova-etapa">Nova coluna</label>
            <input
              id="nova-etapa"
              type="text"
              maxLength={120}
              value={nomeNovaEtapa}
              onChange={(evento) => setNomeNovaEtapa(evento.target.value)}
            />
          </div>
          <button
            className="btn btn-outline"
            type="button"
            disabled={!podeGerenciarWorkflow || nomeNovaEtapa.trim() === ''}
            onClick={() =>
              void executar(async () => {
                await api.criarEtapa(ativo.id, { nome: nomeNovaEtapa.trim(), etapaFinal: false });
                setNomeNovaEtapa('');
              }, 'Configuração de workflow salva com sucesso.')
            }
          >
            + Nova coluna
          </button>
        </section>
      )}

      {!carregando && ativo && aba === 'transicoes' && (
        <section aria-label="Transições do workflow">
          <table>
            <thead>
              <tr>
                <th>Origem</th>
                <th>Destino</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {transicoes.map((transicao) => (
                <tr key={transicao.id}>
                  <td>{etapas.find((e) => e.id === transicao.etapaOrigemId)?.nome ?? '—'}</td>
                  <td>{etapas.find((e) => e.id === transicao.etapaDestinoId)?.nome ?? '—'}</td>
                  <td>
                    <button
                      className="btn btn-text"
                      type="button"
                      disabled={!podeGerenciarWorkflow}
                      onClick={() =>
                        void executar(
                          () => api.excluirTransicao(ativo.id, transicao.id),
                          'Configuração de workflow salva com sucesso.',
                        )
                      }
                    >
                      Excluir
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="form-field">
            <label htmlFor="transicao-origem">Origem</label>
            <select
              id="transicao-origem"
              value={origem}
              onChange={(evento) => setOrigem(evento.target.value)}
            >
              <option value="">Selecione</option>
              {etapas.map((etapa) => (
                <option key={etapa.id} value={etapa.id}>
                  {etapa.nome}
                </option>
              ))}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="transicao-destino">Destino</label>
            <select
              id="transicao-destino"
              value={destino}
              onChange={(evento) => setDestino(evento.target.value)}
            >
              <option value="">Selecione</option>
              {etapas.map((etapa) => (
                <option key={etapa.id} value={etapa.id}>
                  {etapa.nome}
                </option>
              ))}
            </select>
          </div>
          <button
            className="btn btn-outline"
            type="button"
            disabled={!podeGerenciarWorkflow || origem === '' || destino === ''}
            onClick={() =>
              void executar(async () => {
                await api.criarTransicao(ativo.id, {
                  etapaOrigemId: origem,
                  etapaDestinoId: destino,
                });
                setOrigem('');
                setDestino('');
              }, 'Configuração de workflow salva com sucesso.')
            }
          >
            + Nova transição
          </button>
        </section>
      )}

      {!carregando && aba === 'raias' && (
        <section aria-label="Raias do projeto">
          <table>
            <thead>
              <tr>
                <th>Ordem</th>
                <th>Raia</th>
                <th>Padrão</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {[...raias]
                .sort((a, b) => a.ordem - b.ordem)
                .map((raia) => (
                  <tr key={raia.id}>
                    <td>{raia.ordem + 1}</td>
                    <td>{raia.nome}</td>
                    <td>
                      {raia.padrao ? (
                        <span className="badge badge-success">Padrão</span>
                      ) : (
                        <button
                          className="btn btn-text"
                          type="button"
                          disabled={!podeGerenciarRaia}
                          onClick={() =>
                            void executar(
                              () => api.definirRaiaPadrao(projetoId, raia.id),
                              'Raia padrão atualizada.',
                            )
                          }
                        >
                          Definir como padrão
                        </button>
                      )}
                    </td>
                    <td>
                      <button
                        className="btn btn-text"
                        type="button"
                        disabled={!podeGerenciarRaia || raia.padrao}
                        onClick={() =>
                          void executar(
                            () => api.excluirRaia(projetoId, raia.id),
                            'Raia excluída.',
                          )
                        }
                      >
                        Excluir
                      </button>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>

          <div className="form-field">
            <label htmlFor="nova-raia">Nova raia</label>
            <input
              id="nova-raia"
              type="text"
              maxLength={120}
              value={nomeNovaRaia}
              onChange={(evento) => setNomeNovaRaia(evento.target.value)}
            />
          </div>
          <button
            className="btn btn-outline"
            type="button"
            disabled={!podeGerenciarRaia || nomeNovaRaia.trim() === ''}
            onClick={() =>
              void executar(async () => {
                await api.criarRaia(projetoId, { nome: nomeNovaRaia.trim() });
                setNomeNovaRaia('');
              }, 'Raia criada.')
            }
          >
            + Nova raia
          </button>
        </section>
      )}
    </AppShell>
  );
}
