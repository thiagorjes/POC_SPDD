// RNF-009 — as consultas respondem dentro do p95 no envelope de 12 meses.
//
// Envelope declarado: 12 meses de historia e 5.000 tarefas. O numero nao veio
// de medicao — nao existe linha de base, e a propria RNF registra isso —, e por
// isso ele e revisto ao fim do primeiro periodo de uso. Ate la e o unico
// tamanho contra o qual a promessa foi feita.
//
// O alvo sao as leituras que agregam, e nao o board: `tempo-por-etapa` percorre
// tres series de intervalos sobre toda a historia, e e a unica consulta cujo
// custo cresce com o tempo em vez de crescer com o que esta na tela. Medir so o
// board daria verde sobre a consulta que nao e o risco.

import http from 'k6/http'
import { check } from 'k6'

const BASE = __ENV.BASE_URL || 'http://localhost:8080'
const PROJETO = __ENV.PROJETO_ID
const TOKEN = __ENV.TOKEN

const cabecalhos = {
  headers: { Authorization: `Bearer ${TOKEN}` },
}

export const options = {
  // 120 leituras por minuto por sujeito e o limite de RNF-010. A carga fica
  // logo abaixo dele de proposito: acima, estariamos medindo o comportamento
  // do proprio limitador.
  scenarios: {
    leituras: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1m',
      duration: '5m',
      preAllocatedVUs: 20,
    },
  },
  thresholds: {
    'http_req_duration{consulta:tempo-por-etapa}': ['p(95)<2000'],
    'http_req_duration{consulta:andamento}': ['p(95)<2000'],
    'http_req_duration{consulta:board}': ['p(95)<2000'],
    'http_req_duration{consulta:fila}': ['p(95)<2000'],
    'http_req_failed': ['rate==0'],
  },
}

export default function () {
  const respostas = http.batch([
    ['GET', `${BASE}/v1/projetos/${PROJETO}/tempo-por-etapa`,
      null, { ...cabecalhos, tags: { consulta: 'tempo-por-etapa' } }],
    ['GET', `${BASE}/v1/projetos/${PROJETO}/andamento`,
      null, { ...cabecalhos, tags: { consulta: 'andamento' } }],
    ['GET', `${BASE}/v1/projetos/${PROJETO}/board`,
      null, { ...cabecalhos, tags: { consulta: 'board' } }],
    ['GET', `${BASE}/v1/fila`,
      null, { ...cabecalhos, tags: { consulta: 'fila' } }],
  ])

  respostas.forEach((resposta) => {
    check(resposta, { 'consulta respondida': (r) => r.status === 200 })
  })
}
