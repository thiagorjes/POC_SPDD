import { type ComponentProps, type FC } from 'react';

import { LockIcon } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';

import { Badge } from './ui';

const MODULO_SEGURANCA_STATUS = {
  ACTIVE: 'ativo',
  INACTIVE: 'inativo',
};

type ModuloSegurancaBadgeProps = {
  status: keyof typeof MODULO_SEGURANCA_STATUS;
  className?: ComponentProps<'div'>['className'];
};

export const ModuloSegurancaBadge: FC<ModuloSegurancaBadgeProps> = ({
  status,
  className,
}) => {
  return (
    <Badge
      className={cn(
        'w-fit flex justify-center items-center gap-2 px-3 py-[4.8px] bg-emerald-50 [&>*]:text-emerald-700',
        status === 'INACTIVE' && 'bg-red-50 [&>*]:text-red-600',
        className,
      )}
    >
      <LockIcon size={16} />

      <span className="font-regular max-xl:text-sm">
        Módulo de segurança {MODULO_SEGURANCA_STATUS[status]}
      </span>
    </Badge>
  );
};
