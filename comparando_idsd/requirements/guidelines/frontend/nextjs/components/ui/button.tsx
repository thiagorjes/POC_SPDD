import { forwardRef, type ButtonHTMLAttributes } from 'react';

import { SpinnerIcon } from '@phosphor-icons/react';
import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';

import { cn } from '@/lib/utils';

type IconOnlyProps =
  { iconOnly: true; 'aria-label': string } | { iconOnly?: false };

type CompositionProps =
  | { asChild: true; isLoading?: never; loadingLabel?: never }
  | { asChild?: false; isLoading?: boolean; loadingLabel?: string };

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> &
  Pick<VariantProps<typeof buttonVariants>, 'variant' | 'size'> &
  IconOnlyProps &
  CompositionProps;

const DEFAULT_LOADING_LABEL = 'Carregando';

const ICON_SIZE_MD = "[&_svg:not([class*='size-'])]:size-6";
const ICON_SIZE_SM = "[&_svg:not([class*='size-'])]:size-5";

const buttonVariants = cva(
  cn(
    'inline-flex shrink-0 items-center justify-center gap-2 whitespace-nowrap',
    'rounded-lg font-medium transition-colors',
    'outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-background',
    '[&_svg]:shrink-0 [&_svg]:pointer-events-none',
    'disabled:pointer-events-none disabled:opacity-50',
    'aria-disabled:pointer-events-none aria-disabled:opacity-50',
  ),
  {
    variants: {
      variant: {
        primary: cn(
          'bg-primary text-primary-foreground',
          'hover:bg-primary-hover active:bg-primary-active',
          'focus-visible:ring-ring',
        ),
        secondary: cn(
          'border border-secondary-border bg-secondary text-secondary-foreground',
          'hover:bg-secondary-hover active:bg-secondary-active',
          'focus-visible:ring-ring',
        ),
        ghost: cn(
          'text-ghost-foreground',
          'hover:bg-ghost-hover active:bg-ghost-active',
          'focus-visible:ring-ring',
        ),
        destructive: cn(
          'border border-destructive-border bg-destructive text-destructive-foreground',
          'hover:bg-destructive-hover active:bg-destructive-active',
          'focus-visible:ring-ring-destructive',
        ),
      },
      size: {
        md: cn('h-11 px-4 text-base', ICON_SIZE_MD),
        sm: cn('h-9 px-3 text-sm', ICON_SIZE_SM),
      },
      iconOnly: {
        true: 'px-0',
      },
    },
    compoundVariants: [
      { iconOnly: true, size: 'md', class: 'w-12' },
      { iconOnly: true, size: 'sm', class: 'w-9' },
    ],
    defaultVariants: {
      variant: 'primary',
      size: 'sm',
      iconOnly: false,
    },
  },
);

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant,
      children,
      iconOnly,
      className,
      size = 'md',
      type = 'button',
      asChild = false,
      disabled = false,
      isLoading = false,
      loadingLabel = DEFAULT_LOADING_LABEL,
      onClick,
      ...props
    },
    ref,
  ) => {
    const classes = cn(buttonVariants({ variant, size, iconOnly, className }));

    if (asChild) {
      return (
        <Slot
          ref={ref}
          className={classes}
          onClick={onClick}
          aria-disabled={disabled || undefined}
          {...props}
        >
          {children}
        </Slot>
      );
    }

    return (
      <button
        ref={ref}
        type={type}
        className={classes}
        disabled={disabled}
        aria-disabled={isLoading || disabled || undefined}
        aria-busy={isLoading || undefined}
        onClick={
          isLoading
            ? (event) => {
                event.preventDefault();
                event.stopPropagation();
              }
            : onClick
        }
        {...props}
      >
        {isLoading ? (
          <>
            <SpinnerIcon aria-hidden="true" className="animate-spin" />

            {!iconOnly && loadingLabel}
          </>
        ) : (
          children
        )}
      </button>
    );
  },
);
Button.displayName = 'Button';

export { Button, buttonVariants };
