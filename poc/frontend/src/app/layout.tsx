import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import { FeedbackProvider } from '@/components/Feedback';
import { CHAVE_CONFIG_GLOBAL, lerConfigDoAmbiente } from '@/lib/config';
import '@/styles/globals.css';

export const metadata: Metadata = {
  title: 'Kanban de Tarefas',
  description: 'Board configuravel com impedimentos, lead-time e RBAC por projeto',
};

/** Sem isto o Next pre-renderiza o script na build e congela a configuracao na imagem. */
export const dynamic = 'force-dynamic';

export default function RootLayout({ children }: { children: ReactNode }) {
  const config = lerConfigDoAmbiente();

  return (
    <html lang="pt-BR">
      <head>
        {/*
          Configuracao publica resolvida no servidor a cada request (ADR-008). JSON.stringify
          duplo produz um literal de string valido; `<` e escapado para impedir que um valor
          feche a tag <script> prematuramente.
        */}
        <script
          dangerouslySetInnerHTML={{
            __html: `window.${CHAVE_CONFIG_GLOBAL}=JSON.parse(${JSON.stringify(
              JSON.stringify(config),
            ).replace(/</g, '\\u003c')});`,
          }}
        />
      </head>
      <body>
        <FeedbackProvider>{children}</FeedbackProvider>
      </body>
    </html>
  );
}
