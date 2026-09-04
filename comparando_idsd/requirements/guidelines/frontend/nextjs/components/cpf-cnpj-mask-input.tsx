import { type FC } from 'react';

import { MaskInput, type MaskInputProps } from '@/components/mask-input';
import { Cnpj, Cpf } from '@/constants';

type CpfCnpjMaskInputProps = Omit<MaskInputProps, 'mask' | 'maxLength'> & {
  accept: 'CPF' | 'CNPJ' | 'CPF,CNPJ';
};

export const CpfCnpjMaskInput: FC<CpfCnpjMaskInputProps> = ({
  accept,
  ...props
}) => (
  <MaskInput
    autoComplete="off"
    placeholder={`Informe o ${accept.replace(',', ' ou ')}`}
    maxLength={accept === 'CPF' ? Cpf.length.formatted : Cnpj.length.formatted}
    mask={[
      ...(accept === 'CPF' || accept === 'CPF,CNPJ'
        ? [{ mask: '000.000.000-00' }]
        : []),
      ...(accept === 'CNPJ' || accept === 'CPF,CNPJ'
        ? [{ mask: 'XX.XXX.XXX/XXXX-XX', definitions: { X: /[A-Za-z0-9]/ } }]
        : []),
    ]}
    {...props}
  />
);
