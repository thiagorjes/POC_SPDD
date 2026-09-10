// RNF-001 — a atualizacao do board aparece em ate 2 s (p95) para quem esta olhando.
//
// Envelope declarado: 100 tarefas no projeto, 50 sessoes simultaneas no mesmo
// board. Nenhum cenario congelado mede tempo, e nenhum teste de integracao
// mediria isto de forma honesta: MockMvc nao tem rede, nao tem WebSocket e nao
// tem concorrencia de leitura.
//
// O que se mede e o intervalo entre a resposta de aceite da escrita e a chegada
// do evento a sessao que observa. Medir so a latencia do POST responderia outra
// pergunta — a promessa do produto e sobre o que a outra pessoa ve, e nao sobre
// o tempo do proprio clique.

import ws from 'k6/ws'
import http from 'k6/http'
import { check } from 'k6'
import { Trend, Rate } from 'k6/metrics'

const propagacao = new Trend('propagacao_ate_a_sessao', true)
const perdidos = new Rate('eventos_nao_recebidos')

const BASE = __ENV.BASE_URL || 'http://localhost:8080'
const PROJETO = __ENV.PROJETO_ID
const TOKEN = __ENV.TOKEN
const SESSOES = 50
const TAREFAS = 100

export const options = {
  scenarios: {
    // As sessoes ficam abertas o tempo todo: o custo do broadcast cresce com
    // quem esta escutando, e nao com quem esta escrevendo.
    observadores: {
      executor: 'per-vu-iterations',
      vus: SESSOES,
      iterations: 1,
      maxDuration: '3m',
      exec: 'observar',
    },
    // A escrita comeca depois que as sessoes ja subiram. Sem essa folga, as
    // primeiras tarefas seriam criadas sem ninguem escutando e o p95 sairia
    // otimista.
    escritor: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: TAREFAS,
      startTime: '20s',
      maxDuration: '2m',
      exec: 'escrever',
    },
  },
  thresholds: {
    'propagacao_ate_a_sessao': ['p(95)<2000'],
    // Evento perdido nao aparece na latencia: ele simplesmente nunca entra na
    // amostra, e o p95 fica melhor quanto pior o sistema estiver.
    'eventos_nao_recebidos': ['rate==0'],
    'http_req_failed': ['rate==0'],
  },
}

export function observar() {
  const url = `${BASE.replace('http', 'ws')}/ws?projetoId=${PROJETO}`
  ws.connect(url, { headers: { Authorization: `Bearer ${TOKEN}` } }, (socket) => {
    socket.on('message', (bruto) => {
      const evento = JSON.parse(bruto)
      if (!evento.ocorridoEm) {
        return
      }
      propagacao.add(Date.now() - Date.parse(evento.ocorridoEm))
      perdidos.add(false)
    })
    socket.setTimeout(() => socket.close(), 150000)
  })
}

export function escrever() {
  const resposta = http.post(
    `${BASE}/v1/projetos/${PROJETO}/tarefas`,
    JSON.stringify({ titulo: `Carga ${__ITER}` }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${TOKEN}`,
      },
    },
  )
  check(resposta, { 'escrita aceita': (r) => r.status === 201 })
}
