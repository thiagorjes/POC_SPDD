import {
  type ReactElement,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';

import { type UseQueryOptions, useQuery } from '@tanstack/react-query';

import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui';
import { parseError } from '@/errors/parser';
import { cn } from '@/lib/utils';

import { BanestesLottieAnimation } from './banestes-lottie-animation';
import { ErrorView } from './error-view';

type StringKeyOf<T> = {
  [K in keyof T]: T[K] extends string | number ? K : never;
}[keyof T];

type AsyncSelectProps<Item, ValueKey extends StringKeyOf<Item>> = {
  queryOptions: UseQueryOptions<Item[]>;
  valueKey: ValueKey | ((item: Item) => Item[ValueKey]);
  value: Item[ValueKey] | undefined;
  itemKeywords: (item: Item) => string[];
  onSelect: (
    data: { item: Item; itemValue: Item[ValueKey] } | undefined,
  ) => void;
  renderItem: (data: {
    item: Item;
    itemValue: Item[ValueKey];
    isSelected: boolean;
  }) => ReactElement;
  renderTrigger: (data: {
    selectedItem: Item | undefined;
    isPopoverOpen: boolean;
  }) => ReactElement;
};

export function AsyncSelect<Item, ValueKey extends StringKeyOf<Item>>({
  queryOptions,
  valueKey,
  value,
  onSelect,
  renderTrigger,
  renderItem,
  itemKeywords,
}: Readonly<AsyncSelectProps<Item, ValueKey>>) {
  const [isOpen, setIsOpen] = useState(false);
  const popoverRef = useRef<HTMLButtonElement>(null);

  const query = useQuery(queryOptions);

  const onSelectRef = useRef(onSelect);

  useEffect(() => {
    onSelectRef.current = onSelect;
  });

  const items: Item[] = useMemo(() => query.data ?? [], [query.data]);

  const getItemValue = useCallback(
    (item: Item): Item[ValueKey] =>
      typeof valueKey === 'function' ? valueKey(item) : item[valueKey],
    [valueKey],
  );

  const selectedItem = useMemo(
    () =>
      value !== undefined
        ? items.find((item) => getItemValue(item) === value)
        : undefined,
    [getItemValue, items, value],
  );

  useEffect(() => {
    if (!query.isSuccess || value === undefined || !selectedItem) return;

    onSelectRef.current({ item: selectedItem, itemValue: value });
  }, [query.isSuccess, selectedItem, value]);

  const handleOpenChange = useCallback(
    (opened: boolean) => {
      if (opened && query.isError) void query.refetch();
      setIsOpen(opened);
    },
    [query],
  );

  const popoverWidth = popoverRef.current?.offsetWidth
    ? `${popoverRef.current.offsetWidth}px`
    : 'auto';

  return (
    <Popover open={isOpen} onOpenChange={handleOpenChange}>
      <PopoverTrigger ref={popoverRef} asChild>
        {renderTrigger({ selectedItem, isPopoverOpen: isOpen })}
      </PopoverTrigger>

      <PopoverContent
        className="p-0"
        style={{ width: popoverWidth }}
        align="start"
        side="bottom"
      >
        <Command>
          <CommandInput
            className="h-10"
            placeholder="Digite para pesquisar..."
            disabled={!query.isSuccess}
          />

          {!items.length && query.isSuccess && (
            <CommandEmpty className="text-center py-3">
              <span>Nenhum resultado encontrado para a pesquisa</span>
            </CommandEmpty>
          )}

          {!items.length && query.isError && !query.isFetching && (
            <ErrorView
              appError={parseError(query.error)}
              className="px-4 py-6"
              retryButton={{
                text: 'Tentar novamente',
                onClick: () => void query.refetch(),
              }}
            />
          )}

          {!items.length && query.isFetching && (
            <div className="flex flex-col items-center px-4 py-6">
              <BanestesLottieAnimation className="size-[50%] max-w-[60px] max-h-[60px]" />
            </div>
          )}

          <CommandList className={cn({ hidden: !items.length })}>
            <CommandGroup>
              {items.map((item) => {
                const itemValue = getItemValue(item);
                const isSelected = itemValue === value;
                return (
                  <CommandItem
                    key={String(itemValue)}
                    keywords={itemKeywords(item)}
                    value={String(itemValue)}
                    onSelect={() => {
                      onSelect({ itemValue, item });
                      setIsOpen(false);
                    }}
                    className="p-0"
                  >
                    {renderItem({ item, isSelected, itemValue })}
                  </CommandItem>
                );
              })}
            </CommandGroup>
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
