import { type ComponentProps, type FC } from 'react';

import { cn } from '@/lib/utils';

type DividerProps = {
  className?: ComponentProps<'div'>['className'];
  type?: 'solid' | 'dashed';
};

export const Divider: FC<DividerProps> = ({ className, type = 'solid' }) => {
  const dividerStyle =
    type === 'solid'
      ? 'w-full h-[1px] min-h-[1px] bg-gray-200'
      : 'w-full h-0 border-t border-dashed border-gray-300';

  return <div className={cn(dividerStyle, className)} />;
};
