import { type LegacyRef } from 'react';
import { type IMaskInputProps, IMaskMixin } from 'react-imask';

import { type InputMaskElement } from 'imask';

import { Input, type InputProps } from '@/components/ui';

export type MaskInputProps = IMaskInputProps<InputMaskElement> & InputProps;

export const MaskInput = IMaskMixin<InputMaskElement, MaskInputProps>(
  ({ inputRef, ...props }) => (
    <Input {...props} ref={inputRef as LegacyRef<HTMLInputElement>} />
  ),
);
