import { useCallback, useMemo, useState, type FC } from 'react';
import { type Matcher } from 'react-day-picker';

import { Divider } from '@/components/divider';
import {
  Calendar,
  CalendarFooter,
  Popover,
  PopoverContent,
  PopoverTrigger,
  type PopoverContentProps,
} from '@/components/ui';
import { dayjs } from '@/lib/dayjs';
import { type CallbackFunction, type Children } from '@/types';

type DatePickerProps = Children & {
  opened?: boolean;
  minDate?: Date | undefined;
  maxDate?: Date | undefined;
  value: Date | undefined;
  onSelectDate: CallbackFunction<Date | undefined>;
  onOpenChange?: CallbackFunction<boolean>;
  onOpenAutoFocus?: PopoverContentProps['onOpenAutoFocus'];
};

const MIN_DATE = dayjs(new Date(1900, 0, 1)) // 01/01/1900
  .startOf('d')
  .toDate();
const MAX_DATE = dayjs(new Date(2099, 11, 31)) // 01/01/1900
  .startOf('d')
  .toDate();

export const DatePicker: FC<DatePickerProps> = ({
  opened,
  onOpenChange,
  children,
  value,
  minDate,
  maxDate,
  onSelectDate,
  onOpenAutoFocus,
}) => {
  const [isOpened, setIsOpened] = useState(false);

  const dataHoje = new Date();

  const disabled = useMemo(() => {
    if (!minDate && !maxDate) return undefined;

    const matcher: Matcher[] = [];
    if (minDate) matcher.push({ before: minDate });
    if (maxDate) matcher.push({ after: maxDate });

    return matcher;
  }, [minDate, maxDate]);

  const onSelect = useCallback(
    (date: Date | undefined) => {
      onSelectDate(date);
      setIsOpened(false);
      onOpenChange?.(false);
    },
    [onSelectDate, onOpenChange],
  );

  return (
    <Popover
      open={opened ?? isOpened}
      onOpenChange={onOpenChange ?? setIsOpened}
    >
      {!!children && <PopoverTrigger asChild>{children}</PopoverTrigger>}

      <PopoverContent
        className="w-auto p-0 border-0 shadow-xl"
        align="start"
        onOpenAutoFocus={onOpenAutoFocus}
      >
        <div className="bg-white rounded-2xl overflow-hidden">
          <Calendar
            autoFocus
            showOutsideDays
            fixedWeeks
            mode="single"
            captionLayout="dropdown"
            disabled={disabled}
            startMonth={minDate || MIN_DATE}
            endMonth={maxDate || MAX_DATE}
            defaultMonth={value || dataHoje}
            selected={value}
            onSelect={onSelect}
          />

          <Divider />

          <CalendarFooter onTodayClick={() => onSelect(dataHoje)} />
        </div>
      </PopoverContent>
    </Popover>
  );
};
