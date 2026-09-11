import type { ReactNode } from 'react'

type Tom = 'info' | 'sucesso' | 'recusa'

const estilo: Record<Tom, string> = {
  info: 'bg-[var(--cor-info)] text-[var(--cor-info-texto)] border-[var(--cor-info-borda)]',
  sucesso:
    'bg-[var(--cor-sucesso)] text-[var(--cor-sucesso-texto)] border-[var(--cor-sucesso-borda)]',
  recusa: 'bg-[var(--cor-superficie-baixa)] text-destrutivo border-destrutivo',
}

/**
 * `role="status"` no sucesso e `role="alert"` na recusa: sem eles a mudança de
 * estado acontece em silêncio para quem usa leitor de tela, e a informação
 * passaria a ser transmitida apenas pelo que apareceu na tela.
 */
export function Alerta({
  tom = 'info',
  titulo,
  children,
}: {
  tom?: Tom
  titulo: string
  children?: ReactNode
}) {
  return (
    <div
      role={tom === 'recusa' ? 'alert' : 'status'}
      className={`flex flex-col gap-1 rounded-xl border p-4 text-sm ${estilo[tom]}`}
    >
      <strong className="font-semibold">{titulo}</strong>
      {children}
    </div>
  )
}
