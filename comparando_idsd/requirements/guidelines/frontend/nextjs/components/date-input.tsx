import {
  type ComponentProps,
  type FC,
  useCallback,
  useEffect,
  useState,
} from 'react';

import { CalendarBlankIcon } from '@phosphor-icons/react';
import { type Dayjs } from 'dayjs';

import { MaskInput, type MaskInputProps } from '@/components/mask-input';
import { dayjs } from '@/lib/dayjs';
import { cn } from '@/lib/utils';
import { type CallbackFunction } from '@/types';

import { DatePicker } from './date-picker';

type DateInputProps = {
  value: Date | undefined;
  minDate?: Date | undefined;
  maxDate?: Date | undefined;
  inputClass?: ComponentProps<'input'>['className'];
  containerClass?: ComponentProps<'div'>['className'];
  disabled?: MaskInputProps['disabled'];
  error?: MaskInputProps['error'];
  errorMessage?: MaskInputProps['errorMessage'];
  id?: MaskInputProps['id'];
  onBlur?: MaskInputProps['onBlur'];
  onChangeValue: CallbackFunction<Date | undefined>;
};

const DATE_FORMAT = 'DD/MM/YYYY';

export const DateInput: FC<DateInputProps> = ({
  id,
  disabled,
  error,
  errorMessage,
  minDate,
  maxDate,
  value,
  containerClass,
  inputClass,
  onBlur,
  onChangeValue,
}) => {
  const [dateInputValue, setDateInputValue] = useState(() =>
    value ? dayjs(value).format(DATE_FORMAT) : '',
  );

  const isValidDate = useCallback(
    (date: Dayjs) => {
      if (!date.isValid()) return false;

      if (minDate && date.isBefore(dayjs(minDate).startOf('day'))) return false;

      if (maxDate && date.isAfter(dayjs(maxDate).endOf('day'))) return false;

      return true;
    },
    [minDate, maxDate],
  );

  const handleSelecionarData = (dataSelecionada: Date | undefined) => {
    if (!dataSelecionada) return;

    setDateInputValue(dayjs(dataSelecionada).format(DATE_FORMAT));
    onChangeValue(dataSelecionada);
  };

  const handleChangeDateInputValue = useCallback(
    (maskedValue: string) => {
      setDateInputValue(maskedValue);

      const parsedDate = dayjs(maskedValue, DATE_FORMAT, true);
      onChangeValue(isValidDate(parsedDate) ? parsedDate.toDate() : undefined);
    },
    [isValidDate, onChangeValue],
  );

  const handleBlurDateInput = useCallback(() => {
    if (!dateInputValue || dateInputValue.length < DATE_FORMAT.length) {
      setDateInputValue('');
      onChangeValue(undefined);
      return;
    }

    const parsedDate = dayjs(dateInputValue, DATE_FORMAT, true);
    if (!isValidDate(parsedDate)) {
      setDateInputValue('');
      onChangeValue(undefined);
    }
  }, [dateInputValue, isValidDate, onChangeValue]);

  useEffect(() => {
    if (!value) return;

    const formattedDate = dayjs(value).format(DATE_FORMAT);
    setDateInputValue((prevState) => {
      if (prevState === formattedDate) return prevState;
      return formattedDate;
    });
  }, [value]);

  return (
    <DatePicker
      value={value}
      onSelectDate={handleSelecionarData}
      minDate={minDate}
      maxDate={maxDate}
    >
      <div className={cn('relative', containerClass)}>
        <MaskInput
          overwrite
          mask="DD{/}MM{/}AAAA"
          placeholder="dd/mm/aaaa"
          id={id}
          error={!!error}
          errorMessage={errorMessage}
          disabled={disabled}
          value={dateInputValue}
          onAccept={(_, mask) => handleChangeDateInputValue(mask.value)}
          onBlur={(e) => {
            handleBlurDateInput();
            onBlur?.(e);
          }}
          blocks={{
            DD: {
              mask: '00',
              maxLength: 2,
              placeholderChar: 'd',
            },
            MM: {
              mask: '00',
              maxLength: 2,
              placeholderChar: 'm',
            },
            AAAA: {
              mask: '0000',
              maxLength: 4,
              placeholderChar: 'a',
            },
          }}
          inputClass={cn('pr-10', inputClass)}
        />

        <CalendarBlankIcon
          size={18}
          className="absolute right-3 top-[22px] -translate-y-1/2 text-gray-500 pointer-events-none"
        />
      </div>
    </DatePicker>
  );
};
