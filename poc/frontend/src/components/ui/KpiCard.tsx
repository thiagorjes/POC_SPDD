export function KpiCard({ valor, rotulo }: { valor: string; rotulo: string }) {
  return (
    <div className="card kpi-card">
      <div className="kpi-value">{valor}</div>
      <div className="kpi-label">{rotulo}</div>
    </div>
  );
}
