/** Estado de loading padrao das telas (secao 6 do brief). */
export function Skeleton({ linhas = 3 }: { linhas?: number }) {
  return (
    <div aria-busy="true" aria-live="polite">
      {Array.from({ length: linhas }).map((_, indice) => (
        // eslint-disable-next-line react/no-array-index-key
        <div key={indice} className="skeleton" />
      ))}
    </div>
  );
}

/** Estado vazio padrao. */
export function EstadoVazio({ children }: { children: React.ReactNode }) {
  return <div className="empty-state">{children}</div>;
}

/** Estado de erro padrao, com acao de nova tentativa. */
export function EstadoErro({ mensagem, aoTentar }: { mensagem: string; aoTentar?: () => void }) {
  return (
    <div className="toast toast-error" role="alert">
      {mensagem}
      {aoTentar && (
        <button className="btn btn-outline" type="button" onClick={aoTentar}>
          Tentar novamente
        </button>
      )}
    </div>
  );
}
