/**
 * Origem unica de configuracao do cliente (bloco 18 das Operations).
 *
 * O Next inlina `process.env.NEXT_PUBLIC_*` durante o build, que roda sem `.env`; ler essas
 * variaveis no navegador entregaria string vazia. O layout raiz injeta os valores a cada request
 * em `window.__KANBAN_ENV__` e este modulo e a unica porta de leitura para auth, api e stomp.
 * Uma imagem serve qualquer ambiente (ADR-008).
 */
export interface KanbanEnv {
  apiUrl: string;
  keycloakUrl: string;
  keycloakRealm: string;
  keycloakClientId: string;
}

declare global {
  interface Window {
    __KANBAN_ENV__?: KanbanEnv;
  }
}

/** Le a configuracao publica do processo. Usado apenas no servidor, na renderizacao do layout. */
export function lerEnvDoServidor(): KanbanEnv {
  return {
    apiUrl: process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8081',
    keycloakUrl: process.env.NEXT_PUBLIC_KEYCLOAK_URL ?? 'http://localhost:8080',
    keycloakRealm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM ?? 'kanban',
    keycloakClientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID ?? 'kanban-frontend',
  };
}

/** Configuracao disponivel no navegador. Lanca se o layout nao tiver injetado o script. */
export function config(): KanbanEnv {
  if (typeof window === 'undefined') {
    return lerEnvDoServidor();
  }
  const env = window.__KANBAN_ENV__;
  if (!env) {
    throw new Error('Configuracao do cliente indisponivel: __KANBAN_ENV__ nao foi injetado.');
  }
  return env;
}

/** URL do endpoint STOMP derivada da URL da API (http -> ws, https -> wss). */
export function urlWebSocket(): string {
  const api = config().apiUrl;
  return `${api.replace(/^http/, 'ws')}/ws`;
}
