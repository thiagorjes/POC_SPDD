import type { ButtonHTMLAttributes } from 'react';

type VarianteBotao = 'primary' | 'secondary' | 'outline' | 'danger' | 'text';

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: VarianteBotao;
  carregando?: boolean;
}

/** Traduz as variantes de botao do prototipo (_shared.css) para props tipadas. */
export function Botao({
  variante = 'outline',
  carregando = false,
  className,
  disabled,
  children,
  ...resto
}: Props) {
  return (
    <button
      {...resto}
      className={['btn', `btn-${variante}`, className].filter(Boolean).join(' ')}
      disabled={disabled || carregando}
      aria-busy={carregando || undefined}
    >
      {children}
    </button>
  );
}
