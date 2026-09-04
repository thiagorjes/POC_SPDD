import { type FC, type ReactElement } from 'react';
import { Stepper as FormStepper } from 'react-form-stepper';
import { type ConnectorStyleProps as FormStepConnectorStyleProps } from 'react-form-stepper/dist/components/Connector/ConnectorTypes';
import {
  type StepDTO as FormStep,
  type StepStyleDTO as FormStepStyleProps,
} from 'react-form-stepper/dist/components/Step/StepTypes';

import { cn } from '@/lib/utils';

type Step = {
  label: string | ReactElement;
  Element: ReactElement;
};

type StepperProps = {
  steps: Array<Step>;
  activeStep: number;
  className?: string;
};

const formStepperStyles: {
  root: Partial<FormStepStyleProps>;
  connector: Partial<FormStepConnectorStyleProps>;
} = {
  root: {
    activeBgColor: '#004B8D',
    activeTextColor: '#004B8D',
    completedBgColor: '#004B8D',
    completedTextColor: '#004B8D',
    inactiveTextColor: '#111827',
  },
  connector: {
    activeColor: '#004B8D',
    completedColor: '#004B8D',
  },
};

export const Stepper: FC<StepperProps> = ({ className, activeStep, steps }) => (
  <FormStepper
    activeStep={activeStep}
    steps={steps as Array<FormStep>}
    styleConfig={formStepperStyles.root as FormStepStyleProps}
    stepClassName={cn('pointer-events-none [&>span]:hidden', className)}
    connectorStyleConfig={
      formStepperStyles.connector as FormStepConnectorStyleProps
    }
  />
);
