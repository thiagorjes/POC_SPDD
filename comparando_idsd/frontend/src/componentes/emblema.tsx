import type { ReactNode } from 'react'

type Papel = 'neutro' | 'leitura' | 'impedimento' | 'alcance'

const estilo: Record<Papel, string> = {
  neutro: 'bg-[var(--cor-superficie-baixa)] text-texto border-borda',
  leitura: 'bg-[var(--cor-info)] text-[var(--cor-info-texto)] border-[var(--cor-info-borda)]',
  impedimento:
    'bg-impedimento text-[var(--cor-impedimento-texto)] border-[var(--cor-impedimento-borda)]',
  alcance: 'bg-[var(--cor-info)] text-[var(--cor-info-texto)] border-[var(--cor-info-borda)]',
}

/**
 * Emblema de estado.
 *
 * O papel `impedimento` é próprio e nunca o destrutivo (DDR-007, QD-01):
 * impedimento é condição legítima do trabalho, e pintá-lo de erro ensinaria que
 * o estado normal de uma tarefa travada é uma falha do sistema.
 *
 * O texto carrega sempre o significado. Nenhum emblema depende da cor para ser
 * lido — é o critério de aceite 6 e a norma de DDR-005.
 */
export function Emblema({
  papel = 'neutro',
  icone,
  children,
}: {
  papel?: Papel
  icone?: ReactNode
  children: ReactNode
}) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-xl border px-2.5 py-1 text-xs font-medium ${estilo[papel]}`}
    >
      {icone}
      {children}
    </span>
  )
}
