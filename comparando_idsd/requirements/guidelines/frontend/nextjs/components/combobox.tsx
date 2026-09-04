'use client';

import { type ComponentProps, type FC, useMemo, useState } from 'react';

import { CaretDownIcon, CheckIcon } from '@phosphor-icons/react';

import { Button } from '@/components/ui/button';
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { cn } from '@/lib/utils';

export type ComboboxOption = {
  value: string;
  label: string;
  disabled?: boolean;
};

export type ComboboxProps = {
  id?: string;
  options: ComboboxOption[];
  value?: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  placeholder?: string;
  searchPlaceholder?: string;
  emptyText?: string;
  className?: ComponentProps<'button'>['className'];
  contentClassName?: ComponentProps<'div'>['className'];
};

export const Combobox: FC<ComboboxProps> = ({
  id,
  options,
  value,
  onChange,
  disabled = false,
  placeholder = 'Selecione uma opção',
  searchPlaceholder = 'Digite para pesquisar...',
  emptyText = 'Nenhum resultado encontrado.',
  className,
  contentClassName,
}) => {
  const [isOpened, setIsOpened] = useState(false);

  const selectedOption = useMemo(
    () => options.find((item) => item.value === String(value)),
    [options, value],
  );

  return (
    <Popover open={isOpened} onOpenChange={setIsOpened}>
      <PopoverTrigger asChild>
        <Button
          id={id}
          disabled={disabled}
          aria-expanded={isOpened}
          variant="secondary"
          role="combobox"
          className={cn(
            'w-full justify-between px-3 py-2 text-sm border-gray-300 hover:bg-white',
            !selectedOption && 'text-gray-500',
            className,
          )}
        >
          {selectedOption ? selectedOption.label : placeholder}

          <CaretDownIcon className="size-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>

      <PopoverContent
        className={cn(
          'w-[--radix-popover-trigger-width] p-0',
          contentClassName,
        )}
        align="start"
      >
        <Command>
          <CommandInput placeholder={searchPlaceholder} />

          <CommandList>
            <CommandEmpty>{emptyText}</CommandEmpty>

            <CommandGroup>
              {options.map((option) => (
                <CommandItem
                  key={option.value}
                  value={option.label}
                  disabled={!!option.disabled}
                  onSelect={() => {
                    onChange(option.value);
                    setIsOpened(false);
                  }}
                  className="cursor-default"
                >
                  <span>{option.label}</span>

                  {String(option.value) === value && (
                    <CheckIcon className="size-4 ml-auto text-brand-blueDark-500" />
                  )}
                </CommandItem>
              ))}
            </CommandGroup>
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
};
