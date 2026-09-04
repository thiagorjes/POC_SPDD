'use client';

import { type FC } from 'react';

import dynamic from 'next/dynamic';

import { type LottieComponentProps } from 'lottie-react';

import banestesIconLottie from '@/assets/lottie/banestes-icon-lottie.json';
import { cn } from '@/lib/utils';

const Lottie = dynamic(() => import('lottie-react'), { ssr: false });

type BanestesLottieAnimationProps = Omit<LottieComponentProps, 'animationData'>;

export const BanestesLottieAnimation: FC<BanestesLottieAnimationProps> = ({
  className,
  ...rest
}) => (
  <Lottie
    animationData={banestesIconLottie}
    className={cn('size-[80px]', className)}
    {...rest}
  />
);
