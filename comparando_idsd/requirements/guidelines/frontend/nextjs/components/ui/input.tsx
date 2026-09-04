import {
  type ComponentProps,
  forwardRef,
  type InputHTMLAttributes,
  type ReactElement,
  useState,
} from 'react';

import { EyeIcon, EyeSlashIcon } from '@phosphor-icons/react';

import { cn } from '@/lib/utils';

import { Button } from './button';
import { Label } from './label';

export type InputProps = Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'className'
> & {
  label?: string;
  error?: boolean | undefined;
  errorMessage?: string | undefined;
  showPasswordEye?: boolean;
  containerClass?: ComponentProps<'div'>['className'];
  inputClass?: ComponentProps<'input'>['className'];
  leftElement?: ReactElement;
  rightElement?: ReactElement;
  onChangeValue?: (newValue: string) => void;
};

const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      type,
      id,
      disabled,
      label,
      error,
      errorMessage,
      showPasswordEye,
      leftElement,
      rightElement,
      containerClass,
      inputClass,
      onChangeValue,
      onChange,
      ...rest
    },
    ref,
  ) => {
    const [showPassword, setShowPassword] = useState(false);

    const isPasswordType = type === 'password';
    const inputType = isPasswordType && showPassword ? 'alpha' : type;
    const hasError = Boolean(error || errorMessage);
    const hasLeftElement = Boolean(leftElement);
    const hasRightElement = Boolean(rightElement);
    const showPasswordToggle = isPasswordType && showPasswordEye;

    const isControlled = 'value' in rest;

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      onChange?.(e);
      onChangeValue?.(e.target.value);
    };

    const togglePassword = () => setShowPassword((prev) => !prev);

    return (
      <div className={cn('space-y-2 flex flex-col', containerClass)}>
        {label && <Label htmlFor={id}>{label}</Label>}

        <div className="w-full h-11 flex items-center relative">
          {hasLeftElement && (
            <div
              className={cn(
                'h-full flex items-center px-2 rounded-tl-md rounded-bl-md border border-gray-300 group-focus-within:ring-1 focus-visible:ring-ring',
                hasError && 'border-red-500',
                disabled && 'border-gray-200 [&>*]:text-gray-400',
              )}
            >
              {leftElement}
            </div>
          )}

          <input
            ref={ref}
            id={id}
            type={inputType}
            disabled={disabled}
            onChange={handleChange}
            className={cn(
              'h-full min-w-0 flex-1 px-3 py-1 rounded-md border border-gray-300 bg-transparent text-sm shadow-sm transition-colors placeholder:text-gray-500 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50',
              hasError && 'border-red-500',
              hasLeftElement && 'border-l-0 rounded-tl-none rounded-bl-none',
              hasRightElement && 'border-r-0 rounded-tr-none rounded-br-none',
              showPasswordToggle && 'pr-10',
              inputClass,
            )}
            {...rest}
            {...(isControlled && { value: rest.value ?? '' })}
          />

          {showPasswordToggle && (
            <Button
              variant="ghost"
              onClick={togglePassword}
              disabled={disabled}
              className="absolute right-0.5"
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              {showPassword ? (
                <EyeIcon size={18} className="text-gray-500" />
              ) : (
                <EyeSlashIcon size={18} className="text-gray-500" />
              )}
            </Button>
          )}

          {!showPasswordToggle && hasRightElement && (
            <div
              className={cn(
                'h-full flex items-center px-2 rounded-tr-md rounded-br-md border border-gray-300 group-focus-within:ring-1 focus-visible:ring-ring',
                hasError && 'border-red-500',
                disabled && 'border-gray-200 [&>*]:text-gray-400',
              )}
            >
              {rightElement}
            </div>
          )}
        </div>

        {hasError && <small className="text-red-500">{errorMessage}</small>}
      </div>
    );
  },
);

Input.displayName = 'Input';

export { Input };
