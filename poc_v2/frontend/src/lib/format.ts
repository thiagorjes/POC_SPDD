import type { PrioridadeTarefa, TipoTarefa } from './types';

const ROTULO_TIPO: Record<TipoTarefa, string> = {
  FEATURE: 'Feature',
  BUG: 'Bug',
  TAREFA: 'Tarefa',
  MELHORIA: 'Melhoria',
};

const ROTULO_PRIORIDADE: Record<PrioridadeTarefa, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
  CRITICA: 'Crítica',
};

export function rotuloTipo(tipo: TipoTarefa): string {
  return ROTULO_TIPO[tipo];
}

export function rotuloPrioridade(prioridade: PrioridadeTarefa): string {
  return ROTULO_PRIORIDADE[prioridade];
}

/** Duracao em segundos formatada como no protótipo ("2d 4h", "6h 40m", "—" quando zero). */
export function duracao(segundos: number): string {
  if (!segundos || segundos <= 0) {
    return '—';
  }
  const dias = Math.floor(segundos / 86400);
  const horas = Math.floor((segundos % 86400) / 3600);
  const minutos = Math.floor((segundos % 3600) / 60);
  if (dias > 0) {
    return horas > 0 ? `${dias}d ${horas}h` : `${dias}d`;
  }
  if (horas > 0) {
    return minutos > 0 ? `${horas}h ${minutos}m` : `${horas}h`;
  }
  return `${Math.max(minutos, 1)}m`;
}

/** Iniciais para o avatar (duas letras). */
export function iniciais(nome: string | null | undefined): string {
  if (!nome) {
    return '—';
  }
  const partes = nome.trim().split(/\s+/);
  const primeira = partes[0]?.[0] ?? '';
  const ultima = partes.length > 1 ? (partes[partes.length - 1]?.[0] ?? '') : '';
  return (primeira + ultima).toUpperCase();
}

/** Data/hora curta no formato do protótipo (24/08 14:20). */
export function dataHora(iso: string): string {
  const data = new Date(iso);
  const dia = String(data.getDate()).padStart(2, '0');
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const hora = String(data.getHours()).padStart(2, '0');
  const minuto = String(data.getMinutes()).padStart(2, '0');
  return `${dia}/${mes} ${hora}:${minuto}`;
}
