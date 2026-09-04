'use client';

import { Toaster as Sonner } from 'sonner';

import { ErrorIcon, SuccessIcon, WarningIcon } from '@/assets/icons';

type ToasterProps = React.ComponentProps<typeof Sonner>;

const Toaster = ({ ...props }: ToasterProps) => {
  return (
    <Sonner
      closeButton
      theme="light"
      className="toaster group"
      position="top-right"
      icons={{
        error: <ErrorIcon width={32} height={32} />,
        success: <SuccessIcon width={32} height={32} />,
        warning: <WarningIcon width={32} height={32} />,
      }}
      toastOptions={{
        style: {
          height: '64px',
        },
        classNames: {
          icon: 'size-[32px]',
          title: 'text-[14px] text-gray-700 ml-3',
          error: '!border-b-[4px] !border-b-red-500',
          success: '!border-b-[4px] !border-b-emerald-500',
          warning: '!border-b-[4px] !border-b-yellow-500',
        },
      }}
      {...props}
    />
  );
};

export { Toaster };
