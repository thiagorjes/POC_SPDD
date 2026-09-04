/** Circulo com as iniciais do responsavel. Presente em TL-03, TL-04 e TL-10. */
export function iniciaisDe(nome?: string): string {
  const particulas = new Set(['de', 'da', 'do', 'dos', 'das', 'e']);
  const partes = (nome ?? '')
    .trim()
    .split(/\s+/)
    .filter((p) => p.length > 0 && !particulas.has(p.toLowerCase()));

  if (partes.length === 0) {
    return '?';
  }
  if (partes.length === 1) {
    return partes[0].slice(0, 2).toUpperCase();
  }
  return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase();
}

/**
 * Decorativo: o nome sempre acompanha o avatar em texto adjacente, entao marcamos aria-hidden
 * para o leitor de tela nao anunciar as iniciais duas vezes.
 */
export function Avatar({ nome, tamanho = 28 }: { nome?: string; tamanho?: number }) {
  return (
    <span
      className="avatar"
      style={{ width: tamanho, height: tamanho, fontSize: Math.round(tamanho * 0.43) }}
      aria-hidden="true"
      title={nome}
    >
      {iniciaisDe(nome)}
    </span>
  );
}
