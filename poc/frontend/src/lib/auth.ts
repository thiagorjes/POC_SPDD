'use client';

import Keycloak from 'keycloak-js';

/**
 * Autenticacao exclusivamente via Keycloak. Nao existe fallback local: se o IdP estiver
 * indisponivel o login falha com erro explicito (ADR-006).
 */
let keycloak: Keycloak | null = null;
let inicializando: Promise<boolean> | null = null;

export function obterKeycloak(): Keycloak {
  if (!keycloak) {
    keycloak = new Keycloak({
      url: process.env.NEXT_PUBLIC_KEYCLOAK_URL ?? '',
      realm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM ?? '',
      clientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID ?? '',
    });
  }
  return keycloak;
}

export function inicializarAuth(): Promise<boolean> {
  if (!inicializando) {
    inicializando = obterKeycloak()
      .init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false })
      .catch(() => {
        throw new Error(
          'Nao foi possivel contatar o provedor de autenticacao. Tente novamente em instantes.',
        );
      });
  }
  return inicializando;
}

export async function obterToken(): Promise<string | null> {
  const kc = obterKeycloak();
  if (!kc.authenticated) {
    return null;
  }
  // Renova quando faltam menos de 30s: evita 401 no meio de um drag.
  await kc.updateToken(30).catch(() => kc.login());
  return kc.token ?? null;
}

export function entrar(): void {
  void obterKeycloak().login();
}

export function sair(): void {
  void obterKeycloak().logout({ redirectUri: window.location.origin + '/login' });
}
