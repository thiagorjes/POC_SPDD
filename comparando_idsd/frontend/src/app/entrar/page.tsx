import { Alerta } from '@/componentes/alerta'
import { BotaoLink } from '@/componentes/botao'

/**
 * TL-01 — entrada autenticada.
 *
 * Esta tela **não** tem campo de senha, e a ausência é parte do produto: quem
 * autentica é o provedor (ADR-003), e um formulário aqui seria um segundo lugar
 * onde credencial corporativa é digitada.
 *
 * Ela também não oferece caminho alternativo de entrada em nenhum estado de
 * falha (ADR-006). Provedor fora do ar mostra a recusa e o convite a tentar de
 * novo; uma segunda forma de entrar seria, por definição, uma que não passa
 * pelo controle de acesso da organização.
 */

const RAZAO: Record<string, { titulo: string; texto: string }> = {
  recusado: {
    titulo: 'A entrada foi recusada pelo provedor de identidade',
    texto:
      'A credencial não foi aceita ou a autorização foi cancelada. Tente novamente; se persistir, procure quem administra o acesso corporativo.',
  },
  expirado: {
    titulo: 'A tentativa de entrada expirou',
    texto: 'A ida ao provedor demorou demais para voltar. Comece de novo.',
  },
  estado: {
    titulo: 'A volta do provedor não corresponde à ida',
    texto:
      'Por segurança, a entrada foi descartada. Comece de novo a partir desta tela, e não por um link recebido.',
  },
  provedor: {
    titulo: 'O provedor de identidade não respondeu',
    texto:
      'Não é possível entrar enquanto ele estiver indisponível — não há segunda forma de autenticação neste sistema. Tente novamente em instantes.',
  },
}

export default async function Entrar({
  searchParams,
}: {
  searchParams: Promise<{ erro?: string }>
}) {
  const { erro } = await searchParams
  // `Object.hasOwn` e não índice direto (ACH-19 da revisão de TASK-01.7):
  // `?erro=constructor` nomeia membro herdado do protótipo, escapa do valor
  // padrão e renderizaria um alerta sem título. O valor bruto nunca chega ao
  // documento — o framework escapa —, então é robustez e não injeção.
  const recusa = erro ? (Object.hasOwn(RAZAO, erro) ? RAZAO[erro] : RAZAO.recusado) : null

  return (
    <main id="principal" className="mx-auto flex min-h-screen max-w-xl flex-col justify-center p-8">
      <h1 className="text-2xl font-semibold">Fluxo de Tarefas</h1>
      <p className="mt-2 text-sm text-texto-secundario">
        A entrada usa a sua conta corporativa. Nenhuma senha é digitada nesta tela.
      </p>

      {recusa ? (
        <div className="mt-6">
          <Alerta tom="recusa" titulo={recusa.titulo}>
            <p>{recusa.texto}</p>
          </Alerta>
        </div>
      ) : null}

      <div className="mt-8">
        <BotaoLink href="/entrar/iniciar?destino=%2Fprojetos">
          {recusa ? 'Tentar entrar novamente' : 'Entrar com a conta corporativa'}
        </BotaoLink>
      </div>
    </main>
  )
}
