import { type ComponentProps } from 'react';

import { Button, type ButtonProps } from '@/components/ui';
import { type AppError } from '@/errors/app-error';
import { cn } from '@/lib/utils';

type ErrorViewProps = {
  appError: AppError;
  retryButton?: {
    text?: string;
    disabled?: boolean;
    onClick: ButtonProps['onClick'];
  };
  className?: ComponentProps<'div'>['className'];
};

export const ErrorView = ({
  appError,
  retryButton,
  className,
}: ErrorViewProps) => (
  <div
    className={cn(
      'flex h-full flex-col items-center justify-center text-center',
      className,
    )}
  >
    <h3 className="font-semibold">{appError.title}</h3>

    <p className="text-gray-600 mt-1">{appError.message}</p>

    {!!retryButton && (
      <Button
        variant="secondary"
        aria-label={retryButton.text ?? 'Tente novamente'}
        onClick={retryButton.onClick}
        disabled={retryButton.disabled}
        className="mt-5"
      >
        {retryButton.text || 'Tente novamente'}
      </Button>
    )}
  </div>
);
