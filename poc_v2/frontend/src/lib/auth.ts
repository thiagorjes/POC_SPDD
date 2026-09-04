import Keycloak from 'keycloak-js';
import { config } from './config';

/**
 * Autenticacao exclusivamente via Keycloak (ADR-006: sem fallback local). O client e publico e
 * usa PKCE S256 — nao existe segredo no navegador.
 */
let instancia: Keycloak | null = null;
let inicializacao: Promise<boolean> | null = null;

function keycloak(): Keycloak {
  if (!instancia) {
    const env = config();
    instancia = new Keycloak({
      url: env.keycloakUrl,
      realm: env.keycloakRealm,
      clientId: env.keycloakClientId,
    });
  }
  return instancia;
}

/** Inicializa o adaptador uma unica vez por sessao de navegador. */
export function inicializarAuth(): Promise<boolean> {
  if (!inicializacao) {
    inicializacao = keycloak().init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });
  }
  return inicializacao;
}

export function autenticado(): boolean {
  return instancia?.authenticated === true;
}

export function entrar(): Promise<void> {
  return keycloak().login({ redirectUri: window.location.href });
}

export function sair(): Promise<void> {
  return keycloak().logout({ redirectUri: window.location.origin });
}

/** Nome de exibicao vindo das claims do token. */
export function nomeUsuario(): string {
  const perfil = keycloak().tokenParsed as { name?: string; preferred_username?: string } | undefined;
  return perfil?.name ?? perfil?.preferred_username ?? '';
}

/**
 * Token de acesso valido. Renova quando falta menos de 30s de validade — sem isto, requisicoes
 * em abas antigas voltam 401 apos a expiracao.
 */
export async function token(): Promise<string | null> {
  if (!autenticado()) {
    return null;
  }
  await keycloak().updateToken(30);
  return keycloak().token ?? null;
}
