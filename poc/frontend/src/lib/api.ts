'use client';

import { obterToken } from './auth';
import type {
  BoardSnapshot,
  Dashboard,
  Etapa,
  MembroProjeto,
  Notificacao,
  Papel,
  PermissoesEfetivas,
  Projeto,
  Raia,
  RegistroAuditoria,
  TarefaDetalhe,
  Transicao,
  UsuarioAtual,
  Workflow,
  ErrorResponse,
} from './types';

const BASE = process.env.NEXT_PUBLIC_API_URL ?? '';

/**
 * Erro de API preservando o {@link ErrorResponse.errorCode} — o codigo e contrato estavel e e o
 * que decide entre toast (informativo) e modal (exige atencao), conforme DDR-003.
 */
export class ApiError extends Error {
  readonly errorCode: string;
  readonly status: number;
  readonly campos: { campo: string; mensagem: string }[];

  constructor(status: number, corpo: ErrorResponse) {
    super(corpo.message);
    this.name = 'ApiError';
    this.status = status;
    this.errorCode = corpo.errorCode;
    this.campos = corpo.campos ?? [];
  }
}

/** Codigos tratados como bloqueio que exige decisao do usuario; o resto vira toast. */
const CODIGOS_MODAIS = new Set([
  'PERMISSAO_NEGADA',
  'PROJETO_FINALIZADO',
  'CONFLITO_CONCORRENCIA',
  'RECURSO_POSSUI_TAREFAS_ATIVAS',
  'ERRO_INTERNO',
]);

export function exigeModal(erro: unknown): boolean {
  return erro instanceof ApiError && CODIGOS_MODAIS.has(erro.errorCode);
}

async function requisitar<T>(rota: string, init: RequestInit = {}): Promise<T> {
  const token = await obterToken();
  const resposta = await fetch(`${BASE}${rota}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init.headers ?? {}),
    },
  });

  if (resposta.status === 204) {
    return undefined as T;
  }
  if (!resposta.ok) {
    const corpo = (await resposta.json().catch(() => null)) as ErrorResponse | null;
    throw new ApiError(
      resposta.status,
      corpo ?? {
        errorCode: 'ERRO_REDE',
        message: 'Nao foi possivel comunicar com o servidor.',
        timestamp: new Date().toISOString(),
        path: rota,
      },
    );
  }
  return (await resposta.json()) as T;
}

const get = <T,>(rota: string) => requisitar<T>(rota);
const post = <T,>(rota: string, corpo?: unknown) =>
  requisitar<T>(rota, { method: 'POST', body: corpo ? JSON.stringify(corpo) : undefined });
const put = <T,>(rota: string, corpo?: unknown) =>
  requisitar<T>(rota, { method: 'PUT', body: corpo ? JSON.stringify(corpo) : undefined });
const patch = <T,>(rota: string, corpo?: unknown) =>
  requisitar<T>(rota, { method: 'PATCH', body: corpo ? JSON.stringify(corpo) : undefined });
const del = <T,>(rota: string) => requisitar<T>(rota, { method: 'DELETE' });

export const api = {
  eu: () => get<UsuarioAtual>('/api/usuarios/me'),
  usuarios: () => get<UsuarioAtual[]>('/api/usuarios'),

  projetos: () => get<Projeto[]>('/api/projetos'),
  criarProjeto: (corpo: { nome: string; descricao?: string }) =>
    post<Projeto>('/api/projetos', corpo),
  projeto: (id: string) => get<Projeto>(`/api/projetos/${id}`),
  atualizarProjeto: (id: string, corpo: { nome: string; descricao?: string }) =>
    put<Projeto>(`/api/projetos/${id}`, corpo),
  finalizarProjeto: (id: string) => post<Projeto>(`/api/projetos/${id}/finalizar`),
  reabrirProjeto: (id: string) => post<Projeto>(`/api/projetos/${id}/reabrir`),
  permissoes: (id: string) => get<PermissoesEfetivas>(`/api/projetos/${id}/permissoes`),

  board: (id: string) => get<BoardSnapshot>(`/api/projetos/${id}/board`),
  dashboard: (id: string, inicio?: string, fim?: string) => {
    const q = new URLSearchParams();
    if (inicio) q.set('inicio', inicio);
    if (fim) q.set('fim', fim);
    const sufixo = q.size ? `?${q}` : '';
    return get<Dashboard>(`/api/projetos/${id}/dashboard${sufixo}`);
  },

  workflows: (projetoId: string) => get<Workflow[]>(`/api/projetos/${projetoId}/workflows`),
  criarWorkflow: (projetoId: string, nome: string) =>
    post<Workflow>(`/api/projetos/${projetoId}/workflows`, { nome }),
  ativarWorkflow: (projetoId: string, workflowId: string) =>
    put<Workflow>(`/api/projetos/${projetoId}/workflows/${workflowId}/ativar`),
  excluirWorkflow: (projetoId: string, workflowId: string) =>
    del<void>(`/api/projetos/${projetoId}/workflows/${workflowId}`),

  etapas: (workflowId: string) => get<Etapa[]>(`/api/workflows/${workflowId}/etapas`),
  criarEtapa: (workflowId: string, corpo: { nome: string; etapaFinal: boolean }) =>
    post<Etapa>(`/api/workflows/${workflowId}/etapas`, corpo),
  renomearEtapa: (workflowId: string, etapaId: string, corpo: { nome: string; etapaFinal: boolean }) =>
    put<Etapa>(`/api/workflows/${workflowId}/etapas/${etapaId}`, corpo),
  reordenarEtapas: (workflowId: string, ordemIds: string[]) =>
    patch<Etapa[]>(`/api/workflows/${workflowId}/etapas/ordem`, { ordemIds }),
  excluirEtapa: (workflowId: string, etapaId: string) =>
    del<void>(`/api/workflows/${workflowId}/etapas/${etapaId}`),

  transicoes: (workflowId: string) => get<Transicao[]>(`/api/workflows/${workflowId}/transicoes`),
  criarTransicao: (workflowId: string, etapaOrigemId: string, etapaDestinoId: string) =>
    post<Transicao>(`/api/workflows/${workflowId}/transicoes`, { etapaOrigemId, etapaDestinoId }),
  excluirTransicao: (workflowId: string, transicaoId: string) =>
    del<void>(`/api/workflows/${workflowId}/transicoes/${transicaoId}`),

  raias: (projetoId: string) => get<Raia[]>(`/api/projetos/${projetoId}/raias`),
  criarRaia: (projetoId: string, nome: string) =>
    post<Raia>(`/api/projetos/${projetoId}/raias`, { nome }),
  renomearRaia: (projetoId: string, raiaId: string, nome: string) =>
    put<Raia>(`/api/projetos/${projetoId}/raias/${raiaId}`, { nome }),
  definirRaiaPadrao: (projetoId: string, raiaId: string) =>
    put<Raia>(`/api/projetos/${projetoId}/raias/${raiaId}/padrao`),
  excluirRaia: (projetoId: string, raiaId: string) =>
    del<void>(`/api/projetos/${projetoId}/raias/${raiaId}`),

  criarTarefa: (
    projetoId: string,
    corpo: {
      titulo: string;
      descricao?: string;
      tipo: string;
      prioridade?: string;
      raiaId?: string;
      responsavelId?: string;
    },
  ) => post<TarefaDetalhe>(`/api/projetos/${projetoId}/tarefas`, corpo),
  tarefa: (id: string) => get<TarefaDetalhe>(`/api/tarefas/${id}`),
  atualizarTarefa: (
    id: string,
    corpo: {
      titulo: string;
      descricao?: string;
      tipo: string;
      prioridade: string;
      raiaId?: string;
      versaoEsperada: number;
    },
  ) => put<TarefaDetalhe>(`/api/tarefas/${id}`, corpo),
  excluirTarefa: (id: string) => del<void>(`/api/tarefas/${id}`),
  moverTarefa: (
    id: string,
    corpo: { etapaDestinoId: string; raiaDestinoId?: string; versaoEsperada: number },
  ) => patch<TarefaDetalhe>(`/api/tarefas/${id}/mover`, corpo),
  atribuirResponsavel: (id: string, responsavelId: string | null) =>
    patch<TarefaDetalhe>(`/api/tarefas/${id}/responsavel`, { responsavelId }),
  marcarImpedimento: (id: string, motivo: string) =>
    post<TarefaDetalhe>(`/api/tarefas/${id}/impedimento`, { motivo }),
  desmarcarImpedimento: (id: string) => del<TarefaDetalhe>(`/api/tarefas/${id}/impedimento`),
  observar: (id: string) => post<void>(`/api/tarefas/${id}/observadores/me`),
  desobservar: (id: string) => del<void>(`/api/tarefas/${id}/observadores/me`),
  historico: (id: string, pagina = 0) =>
    get<RegistroAuditoria[]>(`/api/tarefas/${id}/historico?page=${pagina}&size=20`),

  membros: (projetoId: string) => get<MembroProjeto[]>(`/api/projetos/${projetoId}/usuarios`),
  papeis: (projetoId: string) => get<Papel[]>(`/api/projetos/${projetoId}/papeis`),
  associar: (projetoId: string, usuarioId: string, codigoPapel: string) =>
    post<void>(`/api/projetos/${projetoId}/usuarios`, { usuarioId, codigoPapel }),
  desassociar: (projetoId: string, usuarioId: string, codigoPapel?: string) =>
    del<void>(
      `/api/projetos/${projetoId}/usuarios/${usuarioId}` +
        (codigoPapel ? `?codigoPapel=${codigoPapel}` : ''),
    ),

  toggles: (projetoId: string) => get<Record<string, boolean>>(`/api/projetos/${projetoId}/toggles`),
  atualizarToggles: (projetoId: string, valores: Record<string, boolean>) =>
    put<Record<string, boolean>>(`/api/projetos/${projetoId}/toggles`, { valores }),

  notificacoes: (apenasNaoLidas = false) =>
    get<Notificacao[]>(`/api/notificacoes?apenasNaoLidas=${apenasNaoLidas}&size=20`),
  marcarNotificacaoLida: (id: string) => patch<void>(`/api/notificacoes/${id}/lida`),
};
