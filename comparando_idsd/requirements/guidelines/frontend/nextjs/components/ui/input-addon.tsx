import {
  forwardRef,
  type HTMLAttributes,
  type InputHTMLAttributes,
  type ReactElement,
} from 'react';

import { cn } from '@/lib/utils';

export type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  inputClassName?: HTMLAttributes<HTMLInputElement>['className'];
  leftElement?: ReactElement;
  rightElement?: ReactElement;
  onChangeValue?(newValue: string): void;
};

const InputAddon = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      className,
      type,
      id,
      disabled,
      leftElement,
      rightElement,
      inputClassName,
      onChangeValue,
      onChange,
      ...rest
    },
    ref,
  ) => {
    return (
      <div
        className={cn(
          'flex h-9 w-auto min-w-[220px] border border-gray-300 rounded-md bg-transparent overflow-hidden focus-within:ring-1 focus-within:ring-ring',
          className,
        )}
      >
        {!!leftElement && leftElement}

        <input
          ref={ref}
          id={id}
          disabled={disabled}
          className={cn(
            'h-full w-full flex px-3 py-1 text-sm transition-colors placeholder:text-gray-500 disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-none',
            leftElement && 'border-l-0 rounded-tl-none rounded-bl-none',
            inputClassName,
          )}
          onChange={(e) => {
            onChange?.(e);
            onChangeValue?.(e.target.value);
          }}
          {...rest}
        />

        {!!rightElement && rightElement}
      </div>
    );
  },
);
InputAddon.displayName = 'InputAddon';

export { InputAddon };
