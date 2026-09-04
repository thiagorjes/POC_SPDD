import { type ComponentProps, type FC, type ReactElement } from 'react';

import { cn } from '@/lib/utils';

type LabelValueProps = {
  variant?: 'inline' | 'block';
  label: string | number | ReactElement;
  value?: string | number | ReactElement | null | undefined;
  className?: ComponentProps<'div'>['className'];
};

export const LabelValue: FC<LabelValueProps> = ({
  label,
  value,
  className,
  variant = 'inline',
}) => (
  <div
    className={cn(
      variant === 'block' && 'space-y-2',
      variant === 'inline' &&
        'flex justify-between items-center gap-8 [&>div+div]:text-right',
      className,
    )}
  >
    <div className="text-sm text-gray-600 whitespace-nowrap">{label}</div>

    <div className="text-sm font-semibold break-all">{value || '–'} </div>
  </div>
);
