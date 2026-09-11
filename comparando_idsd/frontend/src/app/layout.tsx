import type { Metadata } from 'next'
import type { ReactNode } from 'react'

import './globals.css'

export const metadata: Metadata = {
  title: 'Fluxo de Tarefas',
  description: 'Acompanhamento de tarefas por etapa, sem medição por pessoa.',
}

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="pt-BR">
      <body className="min-h-screen antialiased">
        <a
          href="#principal"
          className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-primario focus:px-4 focus:py-2 focus:text-primario-contraste"
        >
          Pular para o conteúdo principal
        </a>
        {children}
      </body>
    </html>
  )
}
