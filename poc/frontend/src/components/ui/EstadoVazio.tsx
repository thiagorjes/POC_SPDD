import type { ReactNode } from 'react';

export function EstadoVazio({
  mensagem,
  acao,
  compacto = false,
}: {
  mensagem: ReactNode;
  acao?: ReactNode;
  compacto?: boolean;
}) {
  return (
    <div className="empty-state" style={compacto ? { padding: 'var(--espaco-scale-md)' } : undefined}>
      <p>{mensagem}</p>
      {acao}
    </div>
  );
}
