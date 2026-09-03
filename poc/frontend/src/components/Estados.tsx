'use client';

import type { ReactNode } from 'react';

/** Os seis estados obrigatorios de tela: loading, vazio, erro, sem permissao, somente leitura, ok. */

export function Skeleton({ linhas = 3 }: { linhas?: number }) {
  return (
    <div aria-busy="true" aria-label="Carregando" style={{ display: 'grid', gap: 8 }}>
      {Array.from({ length: linhas }).map((_, i) => (
        <div className="skeleton" key={i} />
      ))}
    </div>
  );
}

export function Vazio({ titulo, acao }: { titulo: string; acao?: ReactNode }) {
  return (
    <div className="vazio">
      <p>{titulo}</p>
      {acao}
    </div>
  );
}

export function Erro({ mensagem, aoTentarNovamente }: { mensagem: string; aoTentarNovamente?: () => void }) {
  return (
    <div className="vazio" role="alert">
      <p className="erro-inline">{mensagem}</p>
      {aoTentarNovamente && <button onClick={aoTentarNovamente}>Tentar novamente</button>}
    </div>
  );
}

export function SemPermissao() {
  return (
    <div className="vazio" role="alert">
      <p>Voce nao tem permissao para visualizar este conteudo.</p>
    </div>
  );
}

export function AvisoSomenteLeitura() {
  return (
    <div className="cartao" role="status" style={{ marginBottom: 16 }}>
      Projeto finalizado: o conteudo esta em modo somente leitura para todos os papeis.
    </div>
  );
}
