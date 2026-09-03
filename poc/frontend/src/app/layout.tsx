import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import { FeedbackProvider } from '@/components/Feedback';
import '@/styles/globals.css';

export const metadata: Metadata = {
  title: 'Kanban de Tarefas',
  description: 'Board configuravel com impedimentos, lead-time e RBAC por projeto',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="pt-BR">
      <body>
        <FeedbackProvider>{children}</FeedbackProvider>
      </body>
    </html>
  );
}
