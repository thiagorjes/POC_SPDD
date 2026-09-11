'use client'

import { useRouter } from 'next/navigation'
import { useEffect, useRef, type ReactNode } from 'react'

/**
 * Painel lateral modal (ACH-09 da revisão de TASK-01.7).
 *
 * O diálogo se declarava modal e três coisas o desmentiam: o foco não vinha
 * para ele na abertura, `Escape` não o fechava, e o que ficava fora da região
 * inerte continuava alcançável pelo teclado enquanto a camada de escurecimento
 * o cobria — tecnologia assistiva ignorava justamente o que o teclado ainda
 * alcançava. Nada disso é detectável por auditoria automatizada, e é por isso
 * que o critério 5 estava verde com o defeito em pé.
 *
 * O confinamento do foco não é reimplementado aqui: `inert` em tudo o que fica
 * atrás — barra superior inclusive — já retira aquilo da ordem de tabulação e
 * da árvore de acessibilidade. Escrever um segundo mecanismo ao lado seria a
 * segunda fonte da mesma regra.
 *
 * O foco vai para o contêiner, e não para o primeiro campo: começar no campo
 * salta o título e a explicação do painel, que é o que diz a quem chegou ali o
 * que aquilo é.
 */
export function PainelModal({
  id,
  rotuladoPor,
  aoFechar,
  children,
}: {
  id?: string
  rotuladoPor: string
  aoFechar: string
  children: ReactNode
}) {
  const painel = useRef<HTMLDivElement>(null)
  const router = useRouter()

  useEffect(() => {
    painel.current?.focus()
  }, [])

  useEffect(() => {
    function aoTeclar(evento: KeyboardEvent) {
      if (evento.key === 'Escape') router.push(aoFechar)
    }
    document.addEventListener('keydown', aoTeclar)
    return () => document.removeEventListener('keydown', aoTeclar)
  }, [router, aoFechar])

  return (
    <div
      ref={painel}
      id={id}
      role="dialog"
      aria-modal="true"
      aria-labelledby={rotuladoPor}
      // `-1` e não `0`: o painel recebe foco por programa na abertura, e não
      // deve virar mais uma parada da tabulação depois disso.
      tabIndex={-1}
      className="fixed inset-y-0 right-0 z-10 w-painel-lateral max-w-full overflow-y-auto border-l border-borda bg-superficie p-6 shadow-2xl"
    >
      {children}
    </div>
  )
}
