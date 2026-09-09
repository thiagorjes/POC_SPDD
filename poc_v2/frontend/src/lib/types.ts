/** Espelho tipado dos DTOs do backend. Renomear campo aqui e breaking change de contrato. */

export type TipoTarefa = 'FEATURE' | 'BUG' | 'TAREFA' | 'MELHORIA';
export type PrioridadeTarefa = 'BAIXA' | 'MEDIA' | 'ALTA' | 'CRITICA';
export type StatusProjeto = 'ATIVO' | 'FINALIZADO';
export type CampoAuditado = 'RESPONSAVEL' | 'TITULO' | 'ETAPA' | 'IMPEDIMENTO';
export type TipoNotificacao = 'ETAPA_ALTERADA' | 'IMPEDIMENTO_MARCADO' | 'IMPEDIMENTO_DESMARCADO';

export type ChaveToggle =
  | 'DEV_PODE_EXCLUIR_TAREFA'
  | 'DEV_PODE_FINALIZAR_TAREFA'
  | 'DEV_PODE_EDITAR_TAREFA_INICIADA'
  | 'GESTOR_PODE_VER_BOARD';

export type TipoEventoBoard =
  | 'TAREFA_CRIADA'
  | 'TAREFA_MOVIDA'
  | 'TAREFA_ATUALIZADA'
  | 'TAREFA_EXCLUIDA'
  | 'TAREFA_IMPEDIDA'
  | 'TAREFA_DESIMPEDIDA'
  | 'BOARD_RECONFIGURADO'
  | 'PROJETO_STATUS_ALTERADO';

/** Catalogo fechado de permissoes (BDR-001). */
export const PERMISSAO = {
  PROJETO_ADMINISTRAR: 'projeto:administrar',
  PROJETO_VISUALIZAR: 'projeto:visualizar',
  WORKFLOW_GERENCIAR: 'workflow:gerenciar',
  RAIA_GERENCIAR: 'raia:gerenciar',
  USUARIO_ASSOCIAR: 'usuario:associar',
  TAREFA_GERENCIAR: 'tarefa:gerenciar',
  TAREFA_MOVER: 'tarefa:mover',
  TAREFA_FINALIZAR: 'tarefa:finalizar',
  TAREFA_IMPEDIR: 'tarefa:impedir',
  TAREFA_ATRIBUIR: 'tarefa:atribuir',
  DASHBOARD_VISUALIZAR: 'dashboard:visualizar',
} as const;

export interface CampoErro {
  campo: string;
  mensagem: string;
}

export interface ErrorResponse {
  errorCode: string;
  message: string;
  timestamp: string;
  path: string;
  campos: CampoErro[];
}

export interface UsuarioResponse {
  id: string;
  nome: string;
  email: string;
  adminGlobal: boolean;
}

export interface ProjetoResponse {
  id: string;
  nome: string;
  descricao: string | null;
  status: StatusProjeto;
  finalizadoEm: string | null;
  criadoEm: string;
  versao: number;
}

export interface PermissoesEfetivasResponse {
  projetoId: string;
  adminGlobal: boolean;
  projetoAtivo: boolean;
  permissoes: string[];
  toggles: Record<string, boolean>;
}

export interface EtapaResponse {
  id: string;
  workflowId: string;
  nome: string;
  ordem: number;
  etapaFinal: boolean;
}

export interface RaiaResponse {
  id: string;
  projetoId: string;
  nome: string;
  ordem: number;
  padrao: boolean;
}

export interface WorkflowResponse {
  id: string;
  projetoId: string;
  nome: string;
  ativo: boolean;
}

export interface TransicaoResponse {
  id: string;
  workflowId: string;
  etapaOrigemId: string;
  etapaDestinoId: string;
}

export interface TarefaResumoResponse {
  id: string;
  titulo: string;
  tipo: TipoTarefa;
  prioridade: PrioridadeTarefa;
  etapaId: string;
  raiaId: string;
  responsavelId: string | null;
  iniciada: boolean;
  impedida: boolean;
  versao: number;
  destinosPermitidos: string[];
}

export interface LeadTimeEtapaResponse {
  etapaId: string;
  etapaNome: string;
  ordem: number;
  duracaoSegundos: number;
  impedimentoSegundos: number;
}

export interface AuditoriaResponse {
  id: string;
  autorId: string | null;
  campo: CampoAuditado;
  valorAnterior: string | null;
  valorNovo: string | null;
  ocorridoEm: string;
}

export interface TarefaDetalheResponse {
  id: string;
  projetoId: string;
  workflowId: string;
  etapaId: string;
  raiaId: string;
  responsavelId: string | null;
  criadorId: string;
  titulo: string;
  descricao: string | null;
  tipo: TipoTarefa;
  prioridade: PrioridadeTarefa;
  iniciada: boolean;
  impedida: boolean;
  criadaEm: string;
  versao: number;
  leadTimePorEtapa: LeadTimeEtapaResponse[];
  impedimentoTotalSegundos: number;
  historico: AuditoriaResponse[];
  acoesPermitidas: string[];
}

export interface BoardSnapshotResponse {
  projetoId: string;
  workflowId: string;
  seq: number;
  somenteLeitura: boolean;
  etapas: EtapaResponse[];
  raias: RaiaResponse[];
  tarefas: TarefaResumoResponse[];
}

export interface LeadTimeMedioEtapaResponse {
  etapaId: string;
  nome: string;
  ordem: number;
  mediaSegundos: number;
  amostras: number;
  impedimentoMedioSegundos: number;
}

export interface DashboardResponse {
  inicio: string;
  fim: string;
  etapas: LeadTimeMedioEtapaResponse[];
  impedimentoMedioTotalSegundos: number;
  totalTarefas: number;
}

export interface NotificacaoResponse {
  id: string;
  tarefaId: string | null;
  projetoId: string;
  tipo: TipoNotificacao;
  mensagem: string;
  criadaEm: string;
  lidaEm: string | null;
}

/** Usuario provisionado disponivel para associacao ao projeto (RF-015). */
export interface UsuarioResumoResponse {
  id: string;
  nome: string;
  email: string;
}

export interface MembroProjetoResponse {
  usuarioId: string;
  nome: string;
  email: string;
  papeis: string[];
}

export interface EventoBoard {
  projetoId: string;
  seq: number;
  tipo: TipoEventoBoard;
  tarefaId: string | null;
  etapaId: string | null;
  raiaId: string | null;
  ocorridoEm: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/* ----- requests ----- */

export interface CriarProjetoRequest {
  nome: string;
  descricao?: string | null;
}

export interface AtualizarProjetoRequest {
  nome: string;
  descricao?: string | null;
}

export interface CriarTarefaRequest {
  titulo: string;
  descricao?: string | null;
  tipo: TipoTarefa;
  prioridade?: PrioridadeTarefa | null;
  raiaId?: string | null;
  responsavelId?: string | null;
}

export interface AtualizarTarefaRequest {
  titulo: string;
  descricao?: string | null;
  tipo: TipoTarefa;
  prioridade: PrioridadeTarefa;
  raiaId?: string | null;
}

export interface MoverTarefaRequest {
  etapaDestinoId: string;
  raiaDestinoId?: string | null;
  versaoEsperada: number;
}

export interface AtribuirResponsavelRequest {
  responsavelId: string | null;
}

export interface MarcarImpedimentoRequest {
  motivo?: string | null;
}

export interface CriarWorkflowRequest {
  nome: string;
}

export interface CriarEtapaRequest {
  nome: string;
  etapaFinal: boolean;
}

export interface AtualizarEtapaRequest {
  nome: string;
}

export interface ReordenarEtapasRequest {
  ordemIds: string[];
}

export interface CriarTransicaoRequest {
  etapaOrigemId: string;
  etapaDestinoId: string;
}

export interface CriarRaiaRequest {
  nome: string;
}

export interface AssociarPapelRequest {
  usuarioId: string;
  codigoPapel: string;
}

export interface AtualizarTogglesRequest {
  toggles: Record<string, boolean>;
}
