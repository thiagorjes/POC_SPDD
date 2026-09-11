'use client'

import * as Menu from '@radix-ui/react-dropdown-menu'

/**
 * Barra superior com o menu da pessoa.
 *
 * É componente de cliente porque menu é interação de teclado — setas, `Escape`,
 * retorno do foco ao gatilho —, e Radix entrega isso pronto e conforme.
 * Reimplementar `role="menu"` à mão é o caminho curto que costuma sair sem
 * navegação por teclado, e DDR-005 a torna obrigatória.
 *
 * O nome vem de `GET /v1/sessao`, isto é, do provedor. Não há cadastro local a
 * completar, e a ausência de tela pedindo dados é parte do que SCN-001.2
 * verifica.
 */
export function BarraSuperior({ nome, adminGlobal }: { nome: string; adminGlobal: boolean }) {
  const iniciais = nome
    .split(/\s+/)
    .slice(0, 2)
    .map((parte) => parte[0]?.toUpperCase() ?? '')
    .join('')

  return (
    <header className="flex h-20 items-center gap-4 border-b border-borda bg-superficie px-8">
      <span className="text-lg font-semibold">Fluxo de Tarefas</span>
      <span className="flex-1" />

      <Menu.Root>
        <Menu.Trigger className="flex h-controle items-center gap-3 rounded-lg border border-borda px-3 text-left hover:bg-[var(--cor-secundario-hover)]">
          <span
            aria-hidden="true"
            className="flex h-8 w-8 items-center justify-center rounded-full bg-primario text-xs font-semibold text-primario-contraste"
          >
            {iniciais}
          </span>
          <span className="flex flex-col leading-tight">
            <span className="text-sm font-medium">{nome}</span>
            <span className="text-xs text-texto-secundario">
              {adminGlobal ? 'Administração global' : 'Conta do sistema'}
            </span>
          </span>
        </Menu.Trigger>

        <Menu.Portal>
          <Menu.Content
            align="end"
            sideOffset={8}
            className="min-w-52 rounded-xl border border-borda bg-superficie p-1 shadow-lg"
          >
            <Menu.Item asChild>
              <a
                href="/sair"
                className="flex cursor-pointer items-center rounded-lg px-3 py-2 text-sm outline-none data-[highlighted]:bg-[var(--cor-secundario-hover)]"
              >
                Sair
              </a>
            </Menu.Item>
          </Menu.Content>
        </Menu.Portal>
      </Menu.Root>
    </header>
  )
}
