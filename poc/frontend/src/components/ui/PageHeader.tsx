import type { ReactNode } from 'react';

export function PageHeader({
  titulo,
  subtitulo,
  acoes,
}: {
  titulo: ReactNode;
  subtitulo?: ReactNode;
  acoes?: ReactNode;
}) {
  return (
    <div className="page-header">
      <div>
        <h1>{titulo}</h1>
        {subtitulo && <p className="text-secondary">{subtitulo}</p>}
      </div>
      <div>{acoes}</div>
    </div>
  );
}
