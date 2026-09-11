import 'server-only'

import { headers } from 'next/headers'
import { redirect } from 'next/navigation'

import { tokenDeAcesso } from '@/lib/auth/cookies'
import { destinoInterno } from '@/lib/auth/destino'
import { config } from '@/lib/config'
import { CABECALHO_CAMINHO } from '@/proxy'

/**
 * Cliente do serviço REST.
 *
 * Ele roda **sempre no servidor** — o `server-only` acima faz disso erro de
 * compilação e não convenção. São duas razões que se somam: a coleção
 * `frontend/nextjs` proíbe que componente de tela alcance o gateway
 * diretamente, e o token de acesso vive em cookie `httpOnly`, de modo que o
 * navegador não teria como anexá-lo nem se quisesse.
 */

/** Corpo `application/problem+json` (RFC 9457) como o serviço o emite. */
export type Problema = {
  status: number
  title?: string
  detail?: string
  traceId?: string
  errors?: { campo?: string; mensagem?: string }[]
}

export class FalhaDaApi extends Error {
  constructor(readonly problema: Problema) {
    super(problema.detail ?? problema.title ?? `Falha ${problema.status}`)
    this.name = 'FalhaDaApi'
  }
}

type Opcoes = {
  metodo?: 'GET' | 'POST'
  corpo?: unknown
}

/**
 * Para onde voltar depois de autenticar: a tela pedida, e nunca o caminho da
 * API que falhou — `/v1/sessao` não é lugar para devolver ninguém.
 */
async function destinoDeRetorno(): Promise<string> {
  return destinoInterno((await headers()).get(CABECALHO_CAMINHO))
}

export async function chamar<T>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  const token = await tokenDeAcesso()
  if (!token) redirect(`/entrar/iniciar?destino=${encodeURIComponent(await destinoDeRetorno())}`)

  const resposta = await fetch(`${config.api}${caminho}`, {
    method: opcoes.metodo ?? 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json, application/problem+json',
      ...(opcoes.corpo === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    body: opcoes.corpo === undefined ? undefined : JSON.stringify(opcoes.corpo),
    cache: 'no-store',
  })

  // Token expirado é o caso comum de `401`, e recomeçar o authorization code
  // resolve sem que ninguém digite nada: a sessão de SSO do provedor continua
  // de pé. Tratar `401` como erro de tela mostraria uma recusa para quem tem
  // todo o direito de estar ali.
  if (resposta.status === 401) {
    redirect(`/entrar/iniciar?destino=${encodeURIComponent(await destinoDeRetorno())}`)
  }

  if (resposta.status === 204) return undefined as T

  const texto = await resposta.text()
  const corpo = texto ? (JSON.parse(texto) as unknown) : {}

  if (!resposta.ok) {
    throw new FalhaDaApi({ status: resposta.status, ...(corpo as object) })
  }
  return corpo as T
}
