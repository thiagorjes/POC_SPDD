import type { Metadata } from 'next';
import { AuthProvider } from '@/components/AuthProvider';
import { FeedbackProvider } from '@/components/FeedbackProvider';
import { lerEnvDoServidor } from '@/lib/config';
import '@/styles/tokens.css';
import '@/styles/app.css';

export const metadata: Metadata = {
  title: 'Kanban de Tarefas',
  description: 'Board kanban configurável com lead time e RBAC por projeto.',
};

/**
 * Sem isto o Next pre-renderiza o script de configuracao no build e congela os valores; a imagem
 * deixaria de servir qualquer ambiente (ADR-008).
 */
export const dynamic = 'force-dynamic';

export default function RootLayout({ children }: { children: React.ReactNode }) {
  const env = lerEnvDoServidor();

  return (
    <html lang="pt-BR">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        {/*
          A regra no-page-custom-font mira o Pages Router (_document.js); no App Router a fonte e
          carregada no layout raiz, portanto vale para todas as paginas. Inter e a familia definida
          em design-tokens.json.
        */}
        {/* eslint-disable-next-line @next/next/no-page-custom-font */}
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap"
        />
        {/*
          Configuracao publica lida a cada request e entregue ao navegador. Somente URLs, realm e
          clientId — valores publicos por natureza. Segredo nunca e injetado no HTML.
        */}
        <script
          dangerouslySetInnerHTML={{
            __html: `window.__KANBAN_ENV__=${JSON.stringify(env)};`,
          }}
        />
      </head>
      <body>
        <FeedbackProvider>
          <AuthProvider>{children}</AuthProvider>
        </FeedbackProvider>
      </body>
    </html>
  );
}
