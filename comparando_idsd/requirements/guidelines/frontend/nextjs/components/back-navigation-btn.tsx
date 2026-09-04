import { type ComponentProps, type FC } from 'react';

import { useRouter } from 'next/navigation';

import { ArrowLeftIcon } from '@phosphor-icons/react';

import { Button } from '@/components/ui';

type BackNavigationBtnProps = {
  className?: ComponentProps<'button'>['className'];
};

export const BackNavigationBtn: FC<BackNavigationBtnProps> = ({
  className,
}) => {
  const { back } = useRouter();

  return (
    <Button
      variant="ghost"
      aria-label="Voltar"
      className={className}
      onClick={back}
    >
      <ArrowLeftIcon className="size-5 text-brand-blueDark-500" />
      Voltar
    </Button>
  );
};
