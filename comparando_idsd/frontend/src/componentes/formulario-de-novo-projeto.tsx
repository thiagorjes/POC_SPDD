'use client'

import Link from 'next/link'
import { useActionState } from 'react'
import { useFormStatus } from 'react-dom'

import { Alerta } from '@/componentes/alerta'
import { Botao } from '@/componentes/botao'
import { criarProjetoAcao, type EstadoDoFormulario } from '@/app/projetos/novo/acoes'

const rotulo = 'block text-sm font-medium'
const campo =
  'mt-1 w-full rounded-md border border-borda bg-superficie px-3 py-2.5 text-sm text-texto'

function Erro({ id, mensagem }: { id: string; mensagem?: string }) {
  if (!mensagem) return null
  // O erro é texto ao lado do campo e referenciado por `aria-describedby`, e
  // não uma borda vermelha: borda é cor, e cor sozinha não transmite nada
  // (DDR-005, critério de aceite 6).
  return (
    <p id={id} className="mt-1.5 text-sm text-destrutivo">
      {mensagem}
    </p>
  )
}

function BotaoDeEnvio() {
  const { pending } = useFormStatus()
  return (
    <Botao type="submit" disabled={pending} aria-busy={pending}>
      {pending ? 'Criando o projeto…' : 'Criar projeto'}
    </Botao>
  )
}

/**
 * TL-11 — novo projeto.
 *
 * A criação nomeia a primeira administradora do projeto **na mesma operação**
 * (RN-036, RN-037): sem isso o projeto nasceria sem ninguém dentro e sem
 * ninguém que pudesse conceder a primeira participação. Quem cria não vira
 * participante — o alcance global já lhe dá acesso, e virar participante
 * confundiria escopo com participação, apagando a marca que a lista exibe.
 */
export function FormularioDeNovoProjeto() {
  const [estado, acao] = useActionState<EstadoDoFormulario, FormData>(criarProjetoAcao, {})

  if (estado.criado) {
    return (
      <div className="flex flex-col gap-6">
        <Alerta
          tom="sucesso"
          titulo={`${estado.criado.nome} criado · a administradora nomeada já pode configurá-lo`}
        >
          <p>
            O projeto <strong>ainda não tem fluxo</strong> e por isso não aceita tarefa.
            Configurar as etapas é o segundo passo, e ele é obrigatório.
          </p>
        </Alerta>

        <div className="flex flex-wrap gap-3">
          <Link
            href={`/projetos/${estado.criado.id}/fluxo`}
            className="inline-flex h-controle items-center rounded-lg border border-primario bg-primario px-4 text-base font-medium text-primario-contraste"
          >
            Configurar o fluxo agora
          </Link>
          <Link
            href="/projetos"
            className="inline-flex h-controle items-center rounded-lg border border-borda px-4 text-base font-medium"
          >
            Depois
          </Link>
        </div>

        <p className="text-sm text-texto-secundario">
          Escolher “depois” deixa o projeto utilizável apenas para configuração: a criação de
          tarefa é recusada com essa razão até haver etapa. A pendência fica marcada no cartão do
          projeto na lista, porque nada mais no caminho lembraria dela.
        </p>
      </div>
    )
  }

  return (
    <form action={acao} className="flex flex-col gap-6" noValidate>
      {estado.recusa ? <Alerta tom="recusa" titulo={estado.recusa} /> : null}

      <div>
        <label className={rotulo} htmlFor="np-nome">
          Nome do projeto
        </label>
        <input
          className={campo}
          id="np-nome"
          name="nome"
          type="text"
          aria-invalid={Boolean(estado.erros?.nome)}
          aria-describedby={estado.erros?.nome ? 'np-nome-msg' : 'np-nome-ajuda'}
        />
        <p id="np-nome-ajuda" className="mt-1.5 text-xs text-texto-secundario">
          É como o projeto aparece na lista, no board e no andamento.
        </p>
        <Erro id="np-nome-msg" mensagem={estado.erros?.nome} />
      </div>

      <div>
        <label className={rotulo} htmlFor="np-desc">
          Descrição
        </label>
        <textarea className={campo} id="np-desc" name="descricao" rows={3} aria-describedby="np-desc-ajuda" />
        <p id="np-desc-ajuda" className="mt-1.5 text-xs text-texto-secundario">
          Opcional.
        </p>
      </div>

      <div>
        <label className={rotulo} htmlFor="np-admin">
          Primeira administradora do projeto
        </label>
        <input
          className={campo}
          id="np-admin"
          name="primeiroAdministradorId"
          type="text"
          inputMode="text"
          autoComplete="off"
          aria-invalid={Boolean(estado.erros?.primeiroAdministradorId)}
          aria-describedby={
            estado.erros?.primeiroAdministradorId ? 'np-admin-msg' : 'np-admin-ajuda'
          }
        />
        <p id="np-admin-ajuda" className="mt-1.5 text-xs text-texto-secundario">
          Obrigatória. Ela recebe o papel de administradora do projeto na mesma operação e passa a
          ser quem configura o fluxo e concede as demais participações. Informe o identificador da
          conta — só existem contas de quem já entrou no sistema ao menos uma vez, porque é a
          primeira entrada que cria a conta a partir do provedor corporativo.
        </p>
        <Erro id="np-admin-msg" mensagem={estado.erros?.primeiroAdministradorId} />
      </div>

      <Alerta tom="info" titulo="O projeto não aparecerá como participação sua">
        <p>
          Você o alcança pela administração global, e não por participação. A lista marca esse
          acesso como alcance global justamente para que ele não se confunda com estar dentro do
          projeto.
        </p>
      </Alerta>

      <div className="flex flex-wrap gap-3">
        <BotaoDeEnvio />
        <Link
          href="/projetos"
          className="inline-flex h-controle items-center rounded-lg border border-borda px-4 text-base font-medium"
        >
          Cancelar
        </Link>
      </div>
    </form>
  )
}
