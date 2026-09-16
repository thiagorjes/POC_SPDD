# TASK-01.9 — Claim ausente no autoprovisionamento

- **Status:** pendente — dívida declarada, **fora do gate de EPIC-01**
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 2
- **Depende de:** TASK-01.4
- **Cenários cobertos:** nenhum — task de correção de borda de autenticação.
  Nenhum cenário congelado exercita realm sem as claims; o que ela evita é `500`
  na única rota que a pessoa alcança
- **Prazo:** antes de o produto encostar em realm federado
- **Origem:** Q-012 da TechSpec, decidida na v1.8; RF-001; ADR-006; ADR-010

#### Contexto

Q-012 está aberta desde 2026-09-10 com dono `/tasks` e sem task — é a última
questão da TechSpec nessa condição. A decisão já foi tomada na v1.8; o que
faltava era distribuí-la, e é o que esta task faz.

Token válido **sem a claim `name` ou sem a claim `email`** é estado alcançável em
realm federado. Com as duas colunas `NOT NULL`, a primeira entrada dessa pessoa
viola restrição e o autoprovisionamento de RF-001 responde `500` — falha de
**instalação** disfarçada de defeito, e na única rota que ela alcança, porque
ADR-006 não deixa caminho alternativo para quem for recusado.

**Por que ela não bloqueia EPIC-01 e é dívida com prazo próprio:** enquanto o
realm for o do `compose`, as duas claims chegam sempre. O épico está fechado e
esta task **não reabre o gate dele** — ela vence quando o primeiro realm externo
entrar, e é por isso que carrega prazo em vez de posição na fila.

Recusar a entrada foi descartado na decisão: o token é legítimo, o que falta é
configuração do realm.

#### O que deve ser feito

- [ ] Migration nova tornando `usuario.email` anulável (`DROP NOT NULL`).
- [ ] Recuo de `nome` para `preferred_username` e, na falta dele, para o `sub`.
- [ ] Preservar o valor já gravado quando a claim chega ausente, em vez de
      sobrescrevê-lo com nulo.
- [ ] Refletir a nulidade de `email` no mapeamento da entidade.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/resources/db/migration/V2026091619__email_anulavel.sql` | criar | `ALTER TABLE usuario ALTER COLUMN email DROP NOT NULL`. **Migration nova, nunca editar a `V2026091009`** — ela já foi aplicada, e alterá-la quebra o checksum do Flyway |
| `backend/src/main/java/br/com/idsd/kanban/internal/acesso/Usuario.java` | alterar | `@Column(name = "email", nullable = false)` → `nullable = true`; `espelharDoToken` deixa de sobrescrever com nulo |
| `backend/src/main/java/br/com/idsd/kanban/internal/acesso/SessaoController.java` | alterar | o recuo de `nome` — hoje lê `name` e `email` direto das claims |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Ordem de recuo de `nome`, literal:

1. claim `name`;
2. claim `preferred_username`;
3. o `sub`.

`email` **não recua** — fica nulo. A assimetria é deliberada e está decidida:
`nome` é exibido no cartão e na fila, e nulo ali produz interface sem quem;
`email` é campo de exibição e ADR-010 já proíbe que qualquer autorização o
consulte, de modo que nulo nele não afeta decisão nenhuma.

`espelharDoToken` passa a **preservar** o valor gravado quando a claim chega
ausente. Sem isso, uma mudança de configuração no provedor esvaziaria em massa o
e-mail de todo mundo que já entrou — e o espelho existe para acompanhar o token,
não para apagar o que ele deixou de dizer.

#### Guia técnico — pontos de atenção

- **A migration é de ordem nova.** A `V2026091009` está aplicada em todo ambiente
  e no serviço dedicado de ADR-011; editá-la faz a migração falhar com o backend
  parado esperando `service_completed_successfully`.
- **`sub` como nome é feio e é o ponto.** O recuo final existe para que a pessoa
  entre e a interface diga alguma coisa, não para ficar bonito: quem vir um UUID
  no cartão descobre que o realm está mal configurado, que é exatamente a
  informação útil.
- **Não introduza validação que recuse o token.** ADR-006 não tem fallback de
  autenticação local; recusar aqui deixa a pessoa sem caminho nenhum.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Token sem claim `email` entra e é provisionado, com `email` nulo | primeira entrada com token forjado sem a claim |
| 2 | Token sem `name` e com `preferred_username` grava o `preferred_username` como nome | entrada com token forjado |
| 3 | Token sem `name` e sem `preferred_username` grava o `sub` como nome | entrada com token forjado |
| 4 | Segunda entrada com a claim ausente **preserva** o valor já gravado | entrar com a claim, depois sem ela, e ler o registro |
| 5 | A migration aplica sobre banco já migrado, sem `repair` | subida contra banco com as migrations anteriores aplicadas |
| 6 | Nenhuma entrada devolve `500` por claim ausente | as três combinações do critério 1 a 3 |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-16 | criação | Task nascida de **Q-012**, aberta na TechSpec v1.8 com dono `/tasks` e sem distribuição desde 2026-09-10 — a última questão da TechSpec nessa condição. Criada como **TASK-01.9, fora do gate de EPIC-01**: o épico está fechado e a dívida vence por evento externo (o primeiro realm federado), não por posição na fila. Numerada em EPIC-01 e não em EPIC-02 porque o código que ela toca é o do autoprovisionamento de RF-001, entregue por TASK-01.4, e task pertence ao épico do comportamento que altera. A migration recebe ordem nova — a de ordem 7 que a v1.8 previa já foi consumida pelo fechamento de ACH-02 de TASK-02.3 |
