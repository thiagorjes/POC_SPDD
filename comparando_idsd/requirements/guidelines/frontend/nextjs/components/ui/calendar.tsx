'use client';

import * as React from 'react';
import {
  type DayButton,
  DayPicker,
  getDefaultClassNames,
} from 'react-day-picker';
import { ptBR as dayPickerPtBR } from 'react-day-picker/locale';

import {
  CaretDownIcon,
  CaretLeftIcon,
  CaretRightIcon,
} from '@phosphor-icons/react';

import { Button, buttonVariants } from '@/components/ui/button';
import { cn } from '@/lib/utils';

function Calendar({
  className,
  classNames,
  formatters,
  components,
  month,
  onMonthChange,
  ...props
}: React.ComponentProps<typeof DayPicker> & {
  buttonVariant?: React.ComponentProps<typeof Button>['variant'];
}) {
  const defaultClassNames = getDefaultClassNames();

  return (
    <DayPicker
      locale={dayPickerPtBR}
      className={cn(
        'bg-white rounded-t-2xl p-2.5 [--cell-size:2rem] lg:[--cell-size:2.25rem]',
        className,
      )}
      formatters={{
        formatCaption: (date) => {
          const monthYear = date.toLocaleString('pt-BR', {
            month: 'long',
            year: 'numeric',
          });
          return monthYear.charAt(0).toUpperCase() + monthYear.slice(1);
        },
        ...formatters,
      }}
      classNames={{
        root: cn('w-full w-[240px]', defaultClassNames.root),
        months: cn('relative flex flex-col gap-2', defaultClassNames.months),
        month: cn('flex w-full flex-col gap-2', defaultClassNames.month),
        nav: cn(
          'absolute inset-x-0 top-0 flex w-full items-center justify-between gap-1',
          defaultClassNames.nav,
        ),
        button_previous: cn(
          buttonVariants({ variant: 'ghost' }),
          'h-6 w-6 p-0 hover:bg-gray-100',
          defaultClassNames.button_previous,
        ),
        button_next: cn(
          buttonVariants({ variant: 'ghost' }),
          'h-6 w-6 p-0 hover:bg-gray-100',
          defaultClassNames.button_next,
        ),
        month_caption: cn(
          'flex h-6 w-full items-center justify-center px-8',
          defaultClassNames.month_caption,
        ),
        dropdowns: cn(
          'flex items-center justify-center gap-2 text-xs font-semibold',
          defaultClassNames.dropdowns,
        ),
        dropdown_root: cn('relative', defaultClassNames.dropdown_root),
        dropdown: cn('absolute inset-0 opacity-0', defaultClassNames.dropdown),
        caption_label: cn(
          'flex items-center gap-1 text-sm font-semibold',
          defaultClassNames.caption_label,
        ),
        month_grid: 'w-full border-collapse',
        weekdays: cn('flex mb-1', defaultClassNames.weekdays),
        weekday: cn(
          'flex-1 text-center text-[10px] font-medium text-gray-700',
          defaultClassNames.weekday,
        ),
        week: cn('flex w-full', defaultClassNames.week),
        day: cn(
          'relative aspect-square h-full w-full p-0 text-center',
          defaultClassNames.day,
        ),
        range_start: cn('rounded-l-md', defaultClassNames.range_start),
        range_middle: cn('rounded-none', defaultClassNames.range_middle),
        range_end: cn('rounded-r-md', defaultClassNames.range_end),
        today: cn('font-semibold', defaultClassNames.today),
        outside: cn('text-gray-400', defaultClassNames.outside),
        disabled: cn('text-gray-300', defaultClassNames.disabled),
        hidden: cn('invisible', defaultClassNames.hidden),
        ...classNames,
      }}
      components={{
        Root: CalendarRoot,
        Chevron: CalendarChevron,
        DayButton: CalendarDayButton,
        WeekNumber: CalendarWeekNumber,
        ...components,
      }}
      {...props}
    />
  );
}

function CalendarDayButton({
  className,
  day,
  modifiers,
  ...props
}: React.ComponentProps<typeof DayButton>) {
  const defaultClassNames = getDefaultClassNames();

  const ref = React.useRef<HTMLButtonElement>(null);
  React.useEffect(() => {
    if (modifiers.focused) ref.current?.focus();
  }, [modifiers.focused]);

  return (
    <Button
      ref={ref}
      variant="ghost"
      data-day={day.date.toLocaleDateString()}
      data-selected={modifiers.selected}
      data-today={modifiers.today}
      data-outside={modifiers.outside}
      className={cn(
        'h-7 w-7 p-0 font-regular text-sm hover:bg-gray-100',
        'data-[selected=true]:bg-brand-blueDark-500 data-[selected=true]:text-white data-[selected=true]:hover:bg-brand-blueDark-400',
        'data-[today=true]:font-semibold',
        'data-[outside=true]:text-gray-400',
        'rounded-md transition-colors',
        defaultClassNames.day,
        className,
      )}
      {...props}
    />
  );
}

type CalendarComponents = NonNullable<
  React.ComponentProps<typeof DayPicker>['components']
>;

type CalendarComponentProps<K extends keyof CalendarComponents> =
  React.ComponentProps<NonNullable<CalendarComponents[K]>>;

function CalendarRoot({
  className,
  rootRef,
  ...props
}: CalendarComponentProps<'Root'>) {
  return (
    <div
      data-slot="calendar"
      ref={rootRef}
      className={cn(className)}
      {...props}
    />
  );
}

function CalendarChevron({
  className,
  orientation,
  ...props
}: CalendarComponentProps<'Chevron'>) {
  if (orientation === 'left') {
    return (
      <CaretLeftIcon
        className={cn('size-4 text-gray-600', className)}
        {...props}
      />
    );
  }
  if (orientation === 'right') {
    return (
      <CaretRightIcon
        className={cn('size-4 text-gray-600', className)}
        {...props}
      />
    );
  }
  return (
    <CaretDownIcon
      className={cn('size-3 text-gray-600', className)}
      {...props}
    />
  );
}

function CalendarWeekNumber({
  children,
  ...props
}: CalendarComponentProps<'WeekNumber'>) {
  return (
    <td {...props}>
      <div className="flex size-[--cell-size] items-center justify-center text-center">
        {children}
      </div>
    </td>
  );
}

function CalendarFooter({
  onTodayClick,
}: Readonly<{ onTodayClick?: VoidFunction }>) {
  return (
    <div className="p-2">
      <Button
        size="sm"
        aria-label="Hoje"
        className="w-full py-2 text-xs"
        onClick={onTodayClick}
      >
        Hoje
      </Button>
    </div>
  );
}

export { Calendar, CalendarFooter };
