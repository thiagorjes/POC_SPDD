export type TipoTarefa = 'FEATURE' | 'BUG' | 'TAREFA' | 'MELHORIA';
export type PrioridadeTarefa = 'BAIXA' | 'MEDIA' | 'ALTA' | 'CRITICA';
export type StatusProjeto = 'ATIVO' | 'FINALIZADO';

export interface ErrorResponse {
  errorCode: string;
  message: string;
  timestamp: string;
  path: string;
  campos?: { campo: string; mensagem: string }[];
}

export interface Projeto {
  id: string;
  nome: string;
  descricao?: string;
  status: StatusProjeto;
  finalizadoEm?: string;
  criadoEm: string;
  versao: number;
}

export interface Etapa {
  id: string;
  nome: string;
  ordem: number;
  etapaFinal: boolean;
}

export interface Raia {
  id: string;
  nome: string;
  ordem: number;
  padrao: boolean;
}

export interface Transicao {
  id: string;
  etapaOrigemId: string;
  etapaDestinoId: string;
}

export interface Workflow {
  id: string;
  projetoId: string;
  nome: string;
  ativo: boolean;
}

export interface TarefaResumo {
  id: string;
  titulo: string;
  tipo: TipoTarefa;
  prioridade: PrioridadeTarefa;
  etapaId: string;
  raiaId: string;
  responsavelId?: string;
  responsavelNome?: string;
  iniciada: boolean;
  impedida: boolean;
  versao: number;
  /** Ja filtrado por grafo e permissao pelo backend (DDR-002). */
  destinosPermitidos: string[];
}

export interface LeadTimeEtapa {
  etapaId: string;
  etapaNome: string;
  permanenciaSegundos: number;
  impedimentoSegundos: number;
}

export interface RegistroAuditoria {
  id: string;
  autorId: string;
  autorNome?: string;
  campo: 'RESPONSAVEL' | 'TITULO' | 'ETAPA' | 'IMPEDIMENTO';
  valorAnterior?: string;
  valorNovo?: string;
  ocorridoEm: string;
}

export interface TarefaDetalhe extends TarefaResumo {
  projetoId: string;
  workflowId: string;
  descricao?: string;
  criadorId: string;
  motivoImpedimento?: string;
  criadaEm: string;
  leadTimePorEtapa: LeadTimeEtapa[];
  impedimentoTotalSegundos: number;
  historico: RegistroAuditoria[];
  acoesPermitidas: string[];
  observando: boolean;
}

export interface BoardSnapshot {
  projetoId: string;
  workflowId: string;
  seq: number;
  somenteLeitura: boolean;
  etapas: Etapa[];
  raias: Raia[];
  tarefas: TarefaResumo[];
  permissoes: string[];
}

export interface EventoBoard {
  projetoId: string;
  seq: number;
  tipo:
    | 'TAREFA_CRIADA'
    | 'TAREFA_MOVIDA'
    | 'TAREFA_ATUALIZADA'
    | 'TAREFA_EXCLUIDA'
    | 'TAREFA_IMPEDIDA'
    | 'TAREFA_DESIMPEDIDA'
    | 'BOARD_RECONFIGURADO';
  tarefaId?: string;
  etapaId?: string;
  raiaId?: string;
  ocorridoEm: string;
}

export interface PermissoesEfetivas {
  projetoId: string;
  adminGlobal: boolean;
  projetoAtivo: boolean;
  permissoes: string[];
  papeis: string[];
  toggles: Record<string, boolean>;
}

export interface Dashboard {
  projetoId: string;
  inicio: string;
  fim: string;
  etapas: {
    etapaId: string;
    etapaNome: string;
    ordem: number;
    amostras: number;
    mediaPermanenciaSegundos: number;
    mediaImpedimentoSegundos: number;
  }[];
  impedimentoMedioTotalSegundos: number;
}

export interface Notificacao {
  id: string;
  projetoId: string;
  tarefaId: string;
  tipo: string;
  mensagem: string;
  criadaEm: string;
  lidaEm?: string;
}

export interface UsuarioAtual {
  id: string;
  nome: string;
  email: string;
  adminGlobal: boolean;
}

export interface MembroProjeto {
  usuarioId: string;
  nome: string;
  email: string;
  papeis: string[];
}

export interface Papel {
  codigo: string;
  nome: string;
  protegido: boolean;
  permissoes: string[];
}
