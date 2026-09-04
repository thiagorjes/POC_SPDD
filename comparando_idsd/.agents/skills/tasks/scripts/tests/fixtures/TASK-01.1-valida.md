# TASK-01.1 — Registrar recusa de pedido

- **Status:** pendente
- **Sistema:** pedidos
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-001.1
- **Origem:** RF-001, RN-002

#### Contexto

A recusa hoje não deixa rastro, e o atendimento não consegue explicar ao cliente
por que o pedido não avançou.

#### O que deve ser feito

- [ ] Persistir o motivo da recusa junto ao pedido.
- [ ] Expor o motivo na consulta do pedido.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `src/pedidos/recusa.py` | criar | registra motivo e instante |

**Proibido tocar:** `features/`, `tests/steps/`

#### Guia técnico — padrão a seguir

Análogo: `src/pedidos/cancelamento.py`.
Assinatura: `registrar_recusa(pedido_id: str, motivo: MotivoRecusa) -> Recusa`.
Mensagem literal em motivo desconhecido: `"motivo de recusa não reconhecido"`.

#### Guia técnico — pontos de atenção

- O pedido já recusado não pode ser recusado de novo.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Recusa registrada com motivo | consulta do pedido devolve o motivo |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-03 | criação | task gerada pela /tasks |
