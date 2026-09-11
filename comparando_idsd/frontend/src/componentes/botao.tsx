import Link from 'next/link'
import type { ComponentProps, ReactNode } from 'react'

type Variante = 'primario' | 'secundario'

/**
 * Altura mínima de 44px na ação primária, que é o alvo de toque prescrito pelos
 * tokens. O foco não é estilizado aqui: `globals.css` o garante para tudo, e
 * repeti-lo por componente é como se perde um.
 */
const base =
  'inline-flex h-controle items-center justify-center gap-2 rounded-lg border px-4 text-base font-medium disabled:opacity-60 disabled:cursor-not-allowed'

const variante: Record<Variante, string> = {
  primario:
    'bg-primario text-primario-contraste border-primario hover:bg-[var(--cor-primario-hover)]',
  secundario: 'bg-secundario text-texto border-borda hover:bg-[var(--cor-secundario-hover)]',
}

export function Botao({
  tipo = 'primario',
  className = '',
  ...resto
}: ComponentProps<'button'> & { tipo?: Variante }) {
  return <button className={`${base} ${variante[tipo]} ${className}`} {...resto} />
}

export function BotaoLink({
  tipo = 'primario',
  href,
  className = '',
  children,
}: {
  tipo?: Variante
  href: string
  className?: string
  children: ReactNode
}) {
  return (
    <Link href={href} className={`${base} ${variante[tipo]} ${className}`}>
      {children}
    </Link>
  )
}
