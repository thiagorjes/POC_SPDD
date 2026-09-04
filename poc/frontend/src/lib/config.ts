/**
 * Configuracao publica do cliente, lida em runtime.
 *
 * O Next inlina `process.env.NEXT_PUBLIC_*` durante o build, que roda sem `.env` na imagem
 * Docker — ler direto no navegador entregaria string vazia. O layout raiz injeta os valores em
 * `window.__KANBAN_ENV__` a cada request, de modo que uma unica imagem serve qualquer ambiente
 * (ADR-008). O fallback para `process.env` mantem `npm run dev` funcionando.
 *
 * Apenas valores publicos por natureza (URLs, realm, clientId) trafegam aqui: em client publico
 * nao existe segredo a proteger, e nada sensivel pode ser injetado no HTML.
 */
export interface ConfigPublica {
  apiUrl: string;
  wsUrl: string;
  keycloakUrl: string;
  keycloakRealm: string;
  keycloakClientId: string;
}

declare global {
  interface Window {
    __KANBAN_ENV__?: ConfigPublica;
  }
}

export const CHAVE_CONFIG_GLOBAL = '__KANBAN_ENV__';

/** Lido no servidor a cada request; nunca durante o build. */
export function lerConfigDoAmbiente(): ConfigPublica {
  return {
    apiUrl: process.env.NEXT_PUBLIC_API_URL ?? '',
    wsUrl: process.env.NEXT_PUBLIC_WS_URL ?? '',
    keycloakUrl: process.env.NEXT_PUBLIC_KEYCLOAK_URL ?? '',
    keycloakRealm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM ?? '',
    keycloakClientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID ?? '',
  };
}

export function obterConfig(): ConfigPublica {
  if (typeof window !== 'undefined' && window.__KANBAN_ENV__) {
    return window.__KANBAN_ENV__;
  }
  return lerConfigDoAmbiente();
}
