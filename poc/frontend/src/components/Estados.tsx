'use client';

import type { ReactNode } from 'react';
import { Botao } from './ui/Botao';
import { EstadoVazio } from './ui/EstadoVazio';

/** Os seis estados obrigatorios de tela: loading, vazio, erro, sem permissao, somente leitura, ok. */

export function Skeleton({ linhas = 3 }: { linhas?: number }) {
  return (
    <div aria-busy="true" aria-label="Carregando" className="pilha">
      {Array.from({ length: linhas }).map((_, i) => (
        <div className="skeleton" key={i} />
      ))}
    </div>
  );
}

export function Vazio({ titulo, acao }: { titulo: string; acao?: ReactNode }) {
  return <EstadoVazio mensagem={titulo} acao={acao} />;
}

export function Erro({ mensagem, aoTentarNovamente }: { mensagem: string; aoTentarNovamente?: () => void }) {
  return (
    <div className="toast toast-error linha" role="alert">
      <span>{mensagem}</span>
      {aoTentarNovamente && (
        <Botao variante="outline" onClick={aoTentarNovamente}>
          Tentar novamente
        </Botao>
      )}
    </div>
  );
}

export function SemPermissao() {
  return (
    <div className="empty-state" role="alert">
      <p>Você não tem permissão para visualizar este conteúdo.</p>
    </div>
  );
}

export function AvisoSomenteLeitura() {
  return (
    <div className="toast" role="status" style={{ marginBottom: 'var(--espaco-scale-md)' }}>
      Projeto finalizado: o conteúdo está em modo somente leitura para todos os papéis.
    </div>
  );
}
