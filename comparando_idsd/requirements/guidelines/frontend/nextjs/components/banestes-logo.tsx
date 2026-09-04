import { type FC, type SVGProps } from 'react';

import { BanestesFgBlueLogo, BanestesFgWhiteLogo } from '@/assets/logos';

type BanestesLogoProps = {
  fg: 'blue' | 'white';
  w: number;
  h: number;
  className?: string;
};

const LOGOS: Record<BanestesLogoProps['fg'], FC<SVGProps<SVGElement>>> = {
  blue: BanestesFgBlueLogo,
  white: BanestesFgWhiteLogo,
};

export const BanestesLogo: FC<BanestesLogoProps> = ({
  fg,
  w,
  h,
  className,
}) => {
  const Logo = LOGOS[fg];

  return <Logo width={w} height={h} className={className} />;
};
