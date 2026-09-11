/**
 * Configuração de ambiente.
 *
 * Há dois endereços para cada dependência e a distinção não é decorativa: o
 * navegador da pessoa alcança o provedor por `localhost:8180`, e o servidor do
 * Next, quando roda em contêiner, o alcança por `keycloak:8080`. Usar um só
 * quebra num dos dois lados, e o lado que quebra depende de onde a stack sobe.
 *
 * Só o que precisa chegar ao navegador leva `NEXT_PUBLIC_`. O endereço interno
 * do serviço REST não leva, e por isso nunca é serializado para o cliente.
 */
export const config = {
  /** Alcançado pelo navegador — vai para a barra de endereços. */
  provedorPublico: process.env.NEXT_PUBLIC_IDENTIDADE_URL ?? 'http://localhost:8180',
  /** Alcançado pelo servidor do Next — troca de código por token. */
  provedorInterno:
    process.env.IDENTIDADE_URL_INTERNO ??
    process.env.NEXT_PUBLIC_IDENTIDADE_URL ??
    'http://localhost:8180',
  realm: process.env.NEXT_PUBLIC_IDENTIDADE_REALM ?? 'idsd',
  clientId: process.env.NEXT_PUBLIC_IDENTIDADE_CLIENT_ID ?? 'idsd-web',
  /** Endereço público desta aplicação — compõe o `redirect_uri`. */
  aplicacao: process.env.NEXT_PUBLIC_APP_URL ?? 'http://localhost:3000',
  /** Serviço REST, alcançado apenas pelo servidor do Next. */
  api: process.env.API_URL_INTERNO ?? 'http://localhost:8080',
} as const

export const ROTA_RETORNO = '/entrar/retorno'
