import { defineConfig } from '@playwright/test'

/**
 * `BASE_URL` existe porque a suíte roda em dois lugares: da máquina de quem
 * desenvolve, contra a stack publicada em `localhost`, e do serviço `e2e` do
 * `docker/compose.test.yaml`, onde os endereços são os nomes dos serviços.
 *
 * A versão está presa em 1.56 no `package.json` de propósito: a imagem do
 * serviço `e2e` é `playwright:v1.56.0-noble`, e Playwright recusa rodar quando
 * a biblioteca e os navegadores da imagem divergem de versão.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
    trace: 'retain-on-failure',
    locale: 'pt-BR',
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
  ],
})
