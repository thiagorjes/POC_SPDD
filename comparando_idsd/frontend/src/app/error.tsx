'use client'

import { useEffect } from 'react'

import { Alerta } from '@/componentes/alerta'
import { Botao, BotaoLink } from '@/componentes/botao'

/**
 * Limite de erro da árvore de rotas (ACH-10 da revisão de TASK-01.7).
 *
 * Sem ele qualquer falha do serviço — inclusive a recusa por limite de
 * requisições que TASK-01.6 instituiu — sobe até a página genérica do
 * framework, que é escrita em inglês dentro de um documento declarado em
 * português e não oferece caminho de volta.
 *
 * A mensagem é deliberadamente genérica: o `detail` do serviço pode nomear
 * recurso que a pessoa não alcança, e repeti-lo aqui entregaria na tela de erro
 * o que a recusa existe para proteger. O identificador de correlação, quando
 * existe, é o que liga a tela ao log (RNF-010, TASK-01.6).
 */
export default function Erro({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  useEffect(() => {
    // Registro no servidor de rendering, e não `console.log` de depuração: é o
    // único ponto em que a falha do cliente deixa rastro.
    console.error(error)
  }, [error])

  return (
    <main id="principal" className="mx-auto flex min-h-screen max-w-xl flex-col justify-center p-8">
      <Alerta tom="recusa" titulo="Não foi possível completar a operação">
        <p>
          O serviço recusou ou não respondeu. Tentar de novo costuma resolver quando a causa é
          momentânea; se persistir, procure quem administra o sistema.
        </p>
        {error.digest ? (
          <p className="mt-2">
            Referência para o suporte: <code>{error.digest}</code>
          </p>
        ) : null}
      </Alerta>

      <div className="mt-8 flex flex-wrap gap-3">
        <Botao onClick={reset}>Tentar de novo</Botao>
        <BotaoLink href="/projetos" tipo="secundario">
          Voltar aos projetos
        </BotaoLink>
      </div>
    </main>
  )
}
