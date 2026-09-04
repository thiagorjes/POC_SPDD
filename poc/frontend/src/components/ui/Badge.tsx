import type { ReactNode } from 'react';

type VarianteBadge = 'tipo' | 'warning' | 'error' | 'success' | 'neutro';

const CLASSE: Record<VarianteBadge, string> = {
  tipo: 'badge badge-tipo',
  warning: 'badge badge-warning',
  error: 'badge badge-error',
  success: 'badge badge-success',
  neutro: 'badge',
};

export function Badge({
  variante = 'neutro',
  children,
  title,
}: {
  variante?: VarianteBadge;
  children: ReactNode;
  title?: string;
}) {
  return (
    <span className={CLASSE[variante]} title={title}>
      {children}
    </span>
  );
}
