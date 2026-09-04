import { type ComponentProps, type FC } from 'react';

import { type Icon as IconType } from '@phosphor-icons/react';

import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui';

type TooltipIconProps = {
  message: string;
  icon: IconType;
  side?: ComponentProps<typeof TooltipContent>['side'];
  triggerBtnClass?: ComponentProps<'button'>['className'];
  contentClass?: ComponentProps<'div'>['className'];
};

export const TooltipIcon: FC<TooltipIconProps> = ({
  message,
  triggerBtnClass,
  contentClass,
  side = 'top',
  icon: Icon,
}) => (
  <TooltipProvider>
    <Tooltip>
      <TooltipTrigger
        type="button"
        aria-label="Ajuda"
        className={triggerBtnClass}
      >
        <Icon size={20} className="text-gray-500" />
      </TooltipTrigger>

      <TooltipContent side={side} className={contentClass}>
        {message}
      </TooltipContent>
    </Tooltip>
  </TooltipProvider>
);
