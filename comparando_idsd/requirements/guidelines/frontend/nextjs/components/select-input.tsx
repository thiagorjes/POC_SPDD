import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui';
import { useWidthObserver } from '@/hooks/use-width-observer';
import { cn } from '@/lib/utils';

type BaseOption<V extends string> = {
  value: V;
  label: string;
};

type SelectInputProps<
  Value extends string,
  Option extends BaseOption<Value>,
> = {
  options: Option[];
  value: Value | undefined;
  onChangeValue(
    newValue: Value | undefined,
    selectedItem: Option | undefined,
  ): void;
  disabled?: boolean;
  placeholder?: string;
  error?: boolean;
  className?: string;
  id?: string;
};

export function SelectInput<
  Value extends string,
  Option extends BaseOption<Value>,
>(props: Readonly<SelectInputProps<Value, Option>>) {
  const {
    options,
    value,
    onChangeValue,
    disabled,
    placeholder,
    error,
    className,
    ...rest
  } = props;

  const { ref: triggerRef, width: triggerWidth } =
    useWidthObserver<HTMLButtonElement>(300);

  const handleValueChange = (newValue: string) => {
    const selectedItem = options.find(
      (option) => String(option.value) === newValue,
    );

    onChangeValue(selectedItem?.value, selectedItem);
  };

  return (
    /** @ts-ignore precisa aceitar undefined para mostrar o placeholder corretamente */
    <Select
      disabled={!!disabled}
      value={value}
      onValueChange={handleValueChange}
    >
      <SelectTrigger
        ref={triggerRef}
        className={cn('h-[2.75rem]', className, !!error && 'border-red-500')}
        {...rest}
      >
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>

      <SelectContent style={{ width: triggerWidth }}>
        {options.map((option) => (
          <SelectItem key={`op-${option.value}`} value={String(option.value)}>
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
