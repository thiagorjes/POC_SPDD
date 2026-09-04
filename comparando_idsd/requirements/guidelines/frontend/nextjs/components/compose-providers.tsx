import { type ComponentType, type FC } from 'react';

import { type Children } from '@/types';

type Provider = ComponentType<Children>;

type ComposeProvidersProps = Children & {
  providers: Array<Provider>;
};

export const ComposeProviders: FC<ComposeProvidersProps> = ({
  providers,
  children,
}) => (
  <>
    {providers.reduceRight(
      (acc, Provider) => (
        <Provider>{acc}</Provider>
      ),
      children,
    )}
  </>
);
