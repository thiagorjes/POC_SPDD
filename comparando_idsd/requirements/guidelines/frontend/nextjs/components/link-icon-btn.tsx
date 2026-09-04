import {
  type ComponentProps,
  type FC,
  type MouseEventHandler,
  type ReactElement,
} from 'react';

import Link, { type LinkProps } from 'next/link';

import { cn } from '@/lib/utils';
import { type Children } from '@/types';

type LinkIconBtnProps = LinkProps &
  Children & {
    icon: ReactElement;
    disabled?: boolean;
    onDisabledClick?: VoidFunction | undefined;
    onClick?: VoidFunction | undefined;
    className?: ComponentProps<'a'>['className'];
  };

export const LinkIconBtn: FC<LinkIconBtnProps> = ({
  icon,
  href,
  disabled,
  onClick,
  onDisabledClick,
  className,
  children,
  ...props
}) => {
  const handleClick: MouseEventHandler<HTMLAnchorElement> = (e) => {
    if (disabled) {
      e.preventDefault();
      onDisabledClick?.();
      return;
    }

    onClick?.(e);
  };

  return (
    <Link
      href={disabled ? '#' : href}
      aria-disabled={disabled}
      onClick={handleClick}
      className={cn(
        'h-[4.5rem] min-w-[9rem] flex-1 flex justify-start items-center gap-3.5 p-3 font-medium rounded-xl border border-gray-200 bg-white hover:bg-gray-50',
        disabled &&
          'cursor-not-allowed border-gray-200 bg-gray-50 text-gray-400 hover:bg-gray-50',
        className,
      )}
      {...props}
    >
      <div className="ml-1 p-2 rounded-full bg-gray-100">{icon}</div>

      <span>{children}</span>
    </Link>
  );
};
