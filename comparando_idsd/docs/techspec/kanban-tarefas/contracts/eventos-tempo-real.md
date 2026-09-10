# Contrato — eventos em tempo real

_Cobre RF-020, RNF-001, RNF-002 | Base: WebSocket/STOMP_
_Normas: ADR-001, ADR-002, ADR-004, pesquisa I-05_

---

## Handshake

- Endpoint: `/ws`, STOMP sobre WebSocket.
- Autenticação no `CONNECT`, com o mesmo JWT das rotas REST. Sessão sem token
  válido é recusada no handshake — não existe canal anônimo.
- A inscrição em um tópico de projeto é autorizada contra a participação da
  pessoa, no servidor. Inscrição em projeto do qual não participa é recusada;
  não basta não oferecer o tópico na interface (RNF-004).

### A autorização não vale para sempre

A conexão é de vida longa e a autorização do `SUBSCRIBE` é um instantâneo. Sem as
três regras abaixo, quem foi removido do projeto continua recebendo títulos de
tarefa, responsáveis e motivos de impedimento enquanto mantiver o socket aberto —
o que contradiz literalmente RN-015 e RNF-004.

1. **A sessão expira com o token.** O servidor encerra a sessão no `exp` do JWT e
   exige reconexão. Não há renovação silenciosa de autorização.
2. **Mudança de participação invalida a inscrição na hora.** `PUT` e `DELETE` de
   participação removem as inscrições da pessoa naquele projeto, na mesma
   transação em que já operam (`contracts/sessao-e-projetos.md`). Não se espera o
   `exp`.
3. **`/user/queue/fila` é endereçada pelo `sub` do principal do servidor**, nunca
   por identificador vindo do cliente.

O registro de sessões vive em memória de cada instância, e é por isso que a regra
2 precisa ser um efeito declarado da escrita, e não uma conferência preguiçosa no
momento de retransmitir.

## Tópicos

| Tópico | Quem recebe | Serve |
| --- | --- | --- |
| `/topic/board/{projetoId}` | participantes inscritos no board | SCN-020.1, SCN-020.3 |
| `/user/queue/fila` | a própria pessoa | SCN-020.2 |

## Envelope do evento

```
{ "projetoId", "tarefaId", "tipo", "seq", "ocorridoEm" }
```

Enxuto por obrigação: o payload do `NOTIFY` do PostgreSQL tem limite de 8 KB
(ADR-004). O cliente recebe o aviso e busca o detalhe por REST quando precisa —
o evento diz **que** algo mudou, não o estado inteiro.

## Ordem, lacuna e resincronização

- `seq` é sequencial por projeto, **gerado no banco dentro da transação de
  escrita** e único por `(projeto_id, seq)` — SDR-004. Contador por instância
  produziria duplicata, e duplicata torna a lacuna indetectável.
- O cliente guarda o último `seq` recebido por board.
- **Lacuna transitória é tolerada.** Ordem de atribuição não é ordem de commit,
  então ver `n+1` antes de `n` é legítimo. O cliente só dispara
  `GET /v1/projetos/{projetoId}/board` quando a lacuna **persiste** após uma
  janela curta, e com jitter. Resync imediato, com 50 sessões no projeto, seria
  rajada sincronizada contra o mesmo banco que atende a escrita — pressão sobre
  RNF-001 causada pelo mecanismo que existe para protegê-lo.
- Reconexão sempre resincroniza, sem esperar janela.
- **O servidor também cobre a janela de perda.** Ao assumir ou reassumir o
  `LISTEN`, a instância varre `evento_tarefa` por `seq` acima do último publicado
  conhecido e retransmite o que faltou (SDR-004). Publicar em `afterCommit` abre
  a possibilidade de a instância morrer entre o commit e o envio; sem esta
  varredura, o evento sumiria e só a reconexão de algum cliente o revelaria.

## Entrega em múltiplas instâncias

Conforme a pesquisa I-05: canal `LISTEN/NOTIFY` **único** (`board_events`). Toda
instância recebe todo evento e decide, a partir das sessões que mantém, o que
retransmite — inclusive para `/user/queue/fila`, que de outro modo não sairia da
instância que processou a escrita.

O `NOTIFY` é publicado em `afterCommit`, nunca dentro da transação de escrita
(ADR-004): evento anunciado e depois revertido é pior que evento atrasado.

## Envelope de tempo

RNF-001: p95 ≤ 2 s entre o aceite da escrita e a atualização visível nas demais
sessões, com 100 tarefas e 50 sessões no mesmo projeto. Medido do aceite ao
repintar da sessão observadora — não do `NOTIFY` ao `send`, que mediria o trecho
fácil.
