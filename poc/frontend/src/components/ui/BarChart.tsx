export interface SerieBarra {
  rotulo: string;
  valor: number;
  textoValor: string;
}

/**
 * Grafico de barras verticais do TL-07. Serie com valor zero ainda aparece com 2px: omitir a barra
 * esconderia do eixo uma etapa que foi medida.
 */
export function BarChart({
  series,
  descricaoAcessivel,
  alturaMax = 160,
}: {
  series: SerieBarra[];
  descricaoAcessivel: string;
  alturaMax?: number;
}) {
  const maior = Math.max(1, ...series.map((s) => s.valor));

  return (
    <div className="bar-chart" role="img" aria-label={descricaoAcessivel}>
      {series.map((serie) => (
        <div className="bar-wrap" key={serie.rotulo}>
          <span className="rotulo">{serie.textoValor}</span>
          <div
            className="bar"
            style={{ height: serie.valor > 0 ? Math.max(2, Math.round((serie.valor / maior) * alturaMax)) : 2 }}
          />
          <span className="rotulo">{serie.rotulo}</span>
        </div>
      ))}
    </div>
  );
}
