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
          Inter e a fonte normativa do design (DDR-001); sem ela toda a tipografia caía no fallback
          do sistema. Carregada por <link> e nao por next/font/google de proposito: next/font baixa
          a fonte durante o `next build`, o que quebra o build da imagem em ambiente sem rede
          (ADR-008). O fallback declarado em --fonte-font-family-base cobre o caso offline.
        */}
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap"
        />
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
