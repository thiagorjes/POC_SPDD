import {
  ArrowRightIcon,
  CopyIcon,
  DotsThreeVerticalIcon,
} from '@phosphor-icons/react';
import { type Meta, type StoryObj } from '@storybook/nextjs';

import { Button } from '@/components/ui/button';

const meta = {
  title: 'UI/Button',
  component: Button,
  tags: ['autodocs'],
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    variant: {
      control: 'select',
      options: ['primary', 'secondary', 'ghost', 'destructive'],
      description: 'Nível de hierarquia da ação.',
    },
    size: {
      control: 'inline-radio',
      options: ['sm', 'md'],
      description: 'sm: 36px (padrão, interfaces densas). md: 48px (CTAs).',
    },
    iconOnly: {
      control: 'boolean',
      description: 'Botão quadrado sem rótulo. Exige `aria-label`.',
    },
    isLoading: { control: 'boolean' },
    loadingLabel: { control: 'text' },
    disabled: { control: 'boolean' },
    asChild: { table: { disable: true } },
  },
  args: {
    children: 'Continuar',
    variant: 'primary',
    size: 'sm',
    disabled: false,
    isLoading: false,
  },
} satisfies Meta<typeof Button>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Primary: Story = {};

export const Secondary: Story = {
  args: { variant: 'secondary' },
};

export const Ghost: Story = {
  args: { variant: 'ghost' },
};

export const Destructive: Story = {
  args: { variant: 'destructive', children: 'Cancelar programação' },
};

export const ComIcones: Story = {
  parameters: { controls: { disable: true } },
  render: () => (
    <div className="flex flex-wrap items-center gap-4">
      <Button variant="secondary">
        <CopyIcon />
        Copiar
      </Button>

      <Button>
        Continuar
        <ArrowRightIcon />
      </Button>

      <Button variant="secondary" iconOnly aria-label="Mais ações">
        <DotsThreeVerticalIcon />
      </Button>
    </div>
  ),
};

export const Tamanhos: Story = {
  parameters: { controls: { disable: true } },
  render: () => (
    <div className="flex flex-wrap items-center gap-4">
      <Button size="md">
        <CopyIcon />
        Medium — 48px
      </Button>

      <Button size="sm">
        <CopyIcon />
        Small — 36px
      </Button>

      <Button size="md" iconOnly aria-label="Mais ações">
        <DotsThreeVerticalIcon />
      </Button>

      <Button size="sm" iconOnly aria-label="Mais ações">
        <DotsThreeVerticalIcon />
      </Button>
    </div>
  ),
};

export const Estados: Story = {
  parameters: { controls: { disable: true } },
  render: () => (
    <div className="flex flex-col gap-4">
      {(['primary', 'secondary', 'ghost', 'destructive'] as const).map(
        (variant) => (
          <div key={variant} className="flex flex-wrap items-center gap-4">
            <Button variant={variant}>Padrão</Button>

            <Button variant={variant} disabled>
              Desabilitado
            </Button>

            <Button variant={variant} isLoading>
              Enviando
            </Button>

            <Button variant={variant} isLoading loadingLabel="Pagando...">
              Pagar
            </Button>
          </div>
        ),
      )}
    </div>
  ),
};

export const ComoLink: Story = {
  parameters: { controls: { disable: true } },
  render: () => (
    <Button asChild variant="ghost">
      <a href="#extrato">
        Ver extrato
        <ArrowRightIcon />
      </a>
    </Button>
  ),
};
