import { render, screen, within } from '@testing-library/react'
import { axe } from 'jest-axe'
import { CartaoDeTarefa } from '../CartaoDeTarefa'

/**
 * SCN-003.2 e SCN-003.3 — o cartao exibe as tres dimensoes.
 *
 * Os dois cenarios ja sao verificados no backend contra o contrato. O que o
 * teste de integracao nao alcanca e a parte que DDR-007 decidiu: a marca de
 * impedimento e uma dimensao visual ortogonal, e nao um estado que substitui os
 * outros dois. Um payload correto renderizado como "cartao impedido" — sem
 * etapa, sem condicao, sem contagem — passaria em todo teste de contrato e
 * entregaria exatamente o colapso das dimensoes que a emenda do PRD desfez.
 *
 * A verificacao de acessibilidade nao e acessorio: DDR-005 tornou WCAG 2.1 AA
 * obrigatorio, e a decisao que fechou Q-006 exige que o estado nao dependa de
 * cor. Um teste que so olhasse texto deixaria isso passar.
 */

const tarefaEmCurso = {
  id: '11111111-1111-1111-1111-111111111111',
  titulo: 'Ajustar o relatorio',
  etapa: { id: 'e2', nome: 'Desenvolvimento' },
  condicao: 'EM_CURSO',
  responsavel: { nome: 'Bruno' },
  permanencia: { decorrido: 'PT26H' },
  esperaTomada: null,
  impedimento: null,
}

describe('SCN-003.2 — o cartao carrega as tres dimensoes e as contagens', () => {
  it('mostra etapa, condicao e responsavel sem colapsar uma na outra', () => {
    render(<CartaoDeTarefa tarefa={tarefaEmCurso} />)

    const cartao = screen.getByRole('article', { name: /Ajustar o relatorio/i })
    expect(within(cartao).getByTestId('etapa')).toHaveTextContent('Desenvolvimento')
    expect(within(cartao).getByTestId('condicao')).toHaveTextContent(/em curso/i)
    expect(within(cartao).getByTestId('responsavel')).toHaveTextContent('Bruno')
  })

  it('exibe a permanencia em curso e nao exibe espera quando ja houve tomada', () => {
    render(<CartaoDeTarefa tarefa={tarefaEmCurso} />)

    expect(screen.getByTestId('permanencia')).toBeVisible()
    expect(screen.queryByTestId('espera-tomada')).not.toBeInTheDocument()
  })

  it('exibe a espera correndo enquanto ninguem assumiu', () => {
    render(
      <CartaoDeTarefa
        tarefa={{
          ...tarefaEmCurso,
          condicao: 'AGUARDANDO_TOMADA',
          responsavel: null,
          esperaTomada: { decorrido: 'PT3H' },
        }}
      />,
    )

    // DDR-006 promoveu a espera a estado de primeira classe: ela precisa estar
    // na tela, e nao inferida da ausencia de responsavel.
    expect(screen.getByTestId('espera-tomada')).toBeVisible()
    expect(screen.queryByTestId('responsavel')).not.toBeInTheDocument()
  })

  it('nao apresenta soma das series', () => {
    render(<CartaoDeTarefa tarefa={tarefaEmCurso} />)

    // RN-008: somar contaria o mesmo minuto ate tres vezes. QD-02 registra que
    // dois contadores no mesmo cartao convidam a leitura somada — a interface
    // nao pode oferecer essa soma pronta.
    expect(screen.queryByTestId('tempo-total')).not.toBeInTheDocument()
    expect(screen.queryByText(/total/i)).not.toBeInTheDocument()
  })
})

describe('SCN-003.3 — o cartao impedido mantem as outras duas dimensoes', () => {
  const impedida = {
    ...tarefaEmCurso,
    impedimento: { motivo: 'aguardando o fornecedor', decorrido: 'PT10H' },
  }

  it('acende a marca sem apagar etapa nem condicao', () => {
    render(<CartaoDeTarefa tarefa={impedida} />)

    expect(screen.getByTestId('impedimento')).toHaveTextContent('aguardando o fornecedor')
    // O modo de falha e este: a marca virar o estado do cartao.
    expect(screen.getByTestId('etapa')).toHaveTextContent('Desenvolvimento')
    expect(screen.getByTestId('condicao')).toHaveTextContent(/em curso/i)
    expect(screen.queryByText(/impedida/i)).not.toBeInTheDocument()
  })

  it('sinaliza o impedimento por meio que nao depende de cor', async () => {
    const { container } = render(<CartaoDeTarefa tarefa={impedida} />)

    // Q-006 fechou assim: os dois azuis eram indistinguiveis, e a norma que
    // DDR-005 tornou obrigatoria nao aceita cor como unico portador de estado.
    const marca = screen.getByTestId('impedimento')
    expect(marca).toHaveAccessibleName(/impedimento/i)
    expect(within(marca).getByRole('img', { hidden: true })).toBeInTheDocument()

    expect(await axe(container)).toHaveNoViolations()
  })

  it('mantem as tres contagens correndo em paralelo', () => {
    render(<CartaoDeTarefa tarefa={{ ...impedida, esperaTomada: { decorrido: 'PT1H' } }} />)

    // As series correm juntas por decisao de Q-01: descrever uma delas como
    // travada seria a soma proibida dita de outro jeito.
    expect(screen.getByTestId('permanencia')).toBeVisible()
    expect(screen.getByTestId('espera-tomada')).toBeVisible()
    expect(screen.getByTestId('impedimento')).toBeVisible()
  })
})
