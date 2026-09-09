import { token } from './auth';
import { config } from './config';
import type {
  AssociarPapelRequest,
  AtribuirResponsavelRequest,
  AtualizarEtapaRequest,
  AtualizarProjetoRequest,
  AtualizarTarefaRequest,
  AtualizarTogglesRequest,
  AuditoriaResponse,
  BoardSnapshotResponse,
  CriarEtapaRequest,
  CriarProjetoRequest,
  CriarRaiaRequest,
  CriarTarefaRequest,
  CriarTransicaoRequest,
  CriarWorkflowRequest,
  DashboardResponse,
  ErrorResponse,
  EtapaResponse,
  MarcarImpedimentoRequest,
  MembroProjetoResponse,
  MoverTarefaRequest,
  NotificacaoResponse,
  Page,
  PermissoesEfetivasResponse,
  ProjetoResponse,
  RaiaResponse,
  ReordenarEtapasRequest,
  TarefaDetalheResponse,
  TarefaResumoResponse,
  TransicaoResponse,
  UsuarioResponse,
  UsuarioResumoResponse,
  WorkflowResponse,
} from './types';

/** Erro de negocio devolvido pelo backend, ja com o `errorCode` do contrato. */
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

/**
 * Erros que exigem atencao explicita do usuario sao apresentados em modal; os demais em toast
 * (DDR-003). A escolha e feita pelo `errorCode`, nunca pela mensagem.
 */
const CODIGOS_MODAL = new Set([
  'PERMISSAO_NEGADA',
  'PROJETO_FINALIZADO',
  'CONFLITO_CONCORRENCIA',
  'RECURSO_POSSUI_TAREFAS_ATIVAS',
  'WORKFLOW_INVALIDO',
  'VIOLACAO_INTEGRIDADE',
  'PAPEL_PROTEGIDO',
  'TOGGLE_DESCONHECIDO',
  'ERRO_INTERNO',
]);

export function exigeModal(erro: unknown): boolean {
  return erro instanceof ApiError && CODIGOS_MODAL.has(erro.errorCode);
}

export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof ApiError) {
    return erro.message;
  }
  return 'Nao foi possivel completar a operacao. Tente novamente.';
}

async function requisitar<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
  const jwt = await token();
  const resposta = await fetch(`${config().apiUrl}${caminho}`, {
    method: metodo,
    headers: {
      ...(corpo === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...(jwt ? { Authorization: `Bearer ${jwt}` } : {}),
    },
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  });

  if (!resposta.ok) {
    let payload: ErrorResponse;
    try {
      payload = (await resposta.json()) as ErrorResponse;
    } catch {
      payload = {
        errorCode: 'ERRO_INTERNO',
        message: 'Nao foi possivel completar a operacao. Tente novamente.',
        timestamp: new Date().toISOString(),
        path: caminho,
        campos: [],
      };
    }
    throw new ApiError(resposta.status, payload);
  }

  if (resposta.status === 204) {
    return undefined as T;
  }
  return (await resposta.json()) as T;
}

/** Cliente REST tipado. Nenhuma tela monta URL de API fora daqui. */
export const api = {
  eu: () => requisitar<UsuarioResponse>('GET', '/api/usuarios/me'),

  projetos: () => requisitar<ProjetoResponse[]>('GET', '/api/projetos'),
  projeto: (id: string) => requisitar<ProjetoResponse>('GET', `/api/projetos/${id}`),
  criarProjeto: (r: CriarProjetoRequest) =>
    requisitar<ProjetoResponse>('POST', '/api/projetos', r),
  atualizarProjeto: (id: string, r: AtualizarProjetoRequest) =>
    requisitar<ProjetoResponse>('PUT', `/api/projetos/${id}`, r),
  finalizarProjeto: (id: string) =>
    requisitar<ProjetoResponse>('POST', `/api/projetos/${id}/finalizar`),
  reabrirProjeto: (id: string) =>
    requisitar<ProjetoResponse>('POST', `/api/projetos/${id}/reabrir`),
  excluirProjeto: (id: string) => requisitar<void>('DELETE', `/api/projetos/${id}`),
  permissoes: (id: string) =>
    requisitar<PermissoesEfetivasResponse>('GET', `/api/projetos/${id}/permissoes`),
  toggles: (id: string) => requisitar<Record<string, boolean>>('GET', `/api/projetos/${id}/toggles`),
  atualizarToggles: (id: string, r: AtualizarTogglesRequest) =>
    requisitar<Record<string, boolean>>('PUT', `/api/projetos/${id}/toggles`, r),

  board: (projetoId: string) =>
    requisitar<BoardSnapshotResponse>('GET', `/api/projetos/${projetoId}/board`),
  dashboard: (projetoId: string, inicio?: string, fim?: string) => {
    const query = new URLSearchParams();
    if (inicio) {
      query.set('inicio', inicio);
    }
    if (fim) {
      query.set('fim', fim);
    }
    const sufixo = query.toString() ? `?${query}` : '';
    return requisitar<DashboardResponse>('GET', `/api/projetos/${projetoId}/dashboard${sufixo}`);
  },

  workflows: (projetoId: string) =>
    requisitar<WorkflowResponse[]>('GET', `/api/projetos/${projetoId}/workflows`),
  criarWorkflow: (projetoId: string, r: CriarWorkflowRequest) =>
    requisitar<WorkflowResponse>('POST', `/api/projetos/${projetoId}/workflows`, r),
  ativarWorkflow: (projetoId: string, workflowId: string) =>
    requisitar<WorkflowResponse>(
      'PUT',
      `/api/projetos/${projetoId}/workflows/${workflowId}/ativar`,
    ),
  excluirWorkflow: (projetoId: string, workflowId: string) =>
    requisitar<void>('DELETE', `/api/projetos/${projetoId}/workflows/${workflowId}`),

  etapas: (workflowId: string) =>
    requisitar<EtapaResponse[]>('GET', `/api/workflows/${workflowId}/etapas`),
  criarEtapa: (workflowId: string, r: CriarEtapaRequest) =>
    requisitar<EtapaResponse>('POST', `/api/workflows/${workflowId}/etapas`, r),
  atualizarEtapa: (workflowId: string, etapaId: string, r: AtualizarEtapaRequest) =>
    requisitar<EtapaResponse>('PUT', `/api/workflows/${workflowId}/etapas/${etapaId}`, r),
  reordenarEtapas: (workflowId: string, r: ReordenarEtapasRequest) =>
    requisitar<EtapaResponse[]>('PATCH', `/api/workflows/${workflowId}/etapas/ordem`, r),
  excluirEtapa: (workflowId: string, etapaId: string) =>
    requisitar<void>('DELETE', `/api/workflows/${workflowId}/etapas/${etapaId}`),

  transicoes: (workflowId: string) =>
    requisitar<TransicaoResponse[]>('GET', `/api/workflows/${workflowId}/transicoes`),
  criarTransicao: (workflowId: string, r: CriarTransicaoRequest) =>
    requisitar<TransicaoResponse>('POST', `/api/workflows/${workflowId}/transicoes`, r),
  excluirTransicao: (workflowId: string, transicaoId: string) =>
    requisitar<void>('DELETE', `/api/workflows/${workflowId}/transicoes/${transicaoId}`),

  raias: (projetoId: string) =>
    requisitar<RaiaResponse[]>('GET', `/api/projetos/${projetoId}/raias`),
  criarRaia: (projetoId: string, r: CriarRaiaRequest) =>
    requisitar<RaiaResponse>('POST', `/api/projetos/${projetoId}/raias`, r),
  atualizarRaia: (projetoId: string, raiaId: string, r: CriarRaiaRequest) =>
    requisitar<RaiaResponse>('PUT', `/api/projetos/${projetoId}/raias/${raiaId}`, r),
  definirRaiaPadrao: (projetoId: string, raiaId: string) =>
    requisitar<RaiaResponse>('PUT', `/api/projetos/${projetoId}/raias/${raiaId}/padrao`),
  excluirRaia: (projetoId: string, raiaId: string) =>
    requisitar<void>('DELETE', `/api/projetos/${projetoId}/raias/${raiaId}`),

  criarTarefa: (projetoId: string, r: CriarTarefaRequest) =>
    requisitar<TarefaResumoResponse>('POST', `/api/projetos/${projetoId}/tarefas`, r),
  tarefa: (id: string) => requisitar<TarefaDetalheResponse>('GET', `/api/tarefas/${id}`),
  atualizarTarefa: (id: string, r: AtualizarTarefaRequest) =>
    requisitar<TarefaResumoResponse>('PUT', `/api/tarefas/${id}`, r),
  excluirTarefa: (id: string) => requisitar<void>('DELETE', `/api/tarefas/${id}`),
  moverTarefa: (id: string, r: MoverTarefaRequest) =>
    requisitar<TarefaResumoResponse>('PATCH', `/api/tarefas/${id}/mover`, r),
  atribuirResponsavel: (id: string, r: AtribuirResponsavelRequest) =>
    requisitar<TarefaResumoResponse>('PATCH', `/api/tarefas/${id}/responsavel`, r),
  marcarImpedimento: (id: string, r: MarcarImpedimentoRequest) =>
    requisitar<TarefaResumoResponse>('POST', `/api/tarefas/${id}/impedimento`, r),
  desmarcarImpedimento: (id: string) =>
    requisitar<TarefaResumoResponse>('DELETE', `/api/tarefas/${id}/impedimento`),
  observar: (id: string) => requisitar<void>('POST', `/api/tarefas/${id}/observadores/me`),
  desobservar: (id: string) => requisitar<void>('DELETE', `/api/tarefas/${id}/observadores/me`),
  historico: (id: string, pagina = 0, tamanho = 20) =>
    requisitar<Page<AuditoriaResponse>>(
      'GET',
      `/api/tarefas/${id}/historico?page=${pagina}&size=${tamanho}`,
    ),

  membros: (projetoId: string) =>
    requisitar<MembroProjetoResponse[]>('GET', `/api/projetos/${projetoId}/usuarios`),
  usuariosDisponiveis: (projetoId: string) =>
    requisitar<UsuarioResumoResponse[]>(
      'GET',
      `/api/projetos/${projetoId}/usuarios/disponiveis`,
    ),
  associarPapel: (projetoId: string, r: AssociarPapelRequest) =>
    requisitar<void>('POST', `/api/projetos/${projetoId}/usuarios`, r),
  desassociarPapel: (projetoId: string, usuarioId: string, codigoPapel: string) =>
    requisitar<void>(
      'DELETE',
      `/api/projetos/${projetoId}/usuarios/${usuarioId}?codigoPapel=${encodeURIComponent(codigoPapel)}`,
    ),

  notificacoes: (apenasNaoLidas = false) =>
    requisitar<Page<NotificacaoResponse>>(
      'GET',
      `/api/notificacoes?apenasNaoLidas=${apenasNaoLidas}&size=20`,
    ),
  marcarNotificacaoLida: (id: string) => requisitar<void>('PATCH', `/api/notificacoes/${id}/lida`),
};
