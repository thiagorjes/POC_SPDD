# Realm de desenvolvimento

`realm.json` é material de bootstrap: versionado, montado somente leitura e
importado na subida do provedor. Ele **não aceita comentário** — a importação
recusa qualquer campo desconhecido e o servidor entra em laço de reinício com a
mensagem `Unrecognized field`. Por isso as razões estão aqui.

## Papéis — o realm não tem nenhum, de propósito

**A autorização é modelada na aplicação**, por par usuário↔projeto (ADR-003,
BDR-001). Por isso `realm.json` não declara `roles` e nenhuma conta carrega
`realmRoles`: o provedor autentica e diz *quem é*, nunca *o que pode*.

A versão anterior deste arquivo declarava `project_admin`, `product_owner`,
`dev` e `gestor` como papéis de realm e os atribuía globalmente às contas, para
"dar o vocabulário do produto ao ambiente" (ACH-02 da revisão de TASK-01.2). O
efeito não era decorativo: o token passava a afirmar que `ana` é
`project_admin` **em toda parte**, e é esse token que TASK-01.4 e TASK-01.5 leem
para montar autorização. Derivar papel de `realm_access` era o caminho mais
curto a partir do arquivo, e ele concede acesso a projeto de que a pessoa nem
participa — exatamente o que BDR-001 existe para impedir.

Papel de teste, portanto, é **concedido no banco**, junto com a participação.
A suíte congelada já faz assim: nenhum teste lê `realm_access` ou
`resource_access` do token.

## Clients

| Client | Fluxo | Por quê |
| --- | --- | --- |
| `idsd-web` | authorization code com PKCE (S256), client público | Não há client secret no backend — ele é Resource Server puro |
| `idsd-e2e` | direct access grant | Só o suporte da suíte usa, para montar massa pela API. Os cenários autenticam pela tela de verdade; injetar token tiraria de SCN-001.1 exatamente o que ele verifica |

## Contas

Senha única, `senha-de-teste`, igual à que o suporte da suíte usa. Este realm
nunca sai de desenvolvimento e não há nada a proteger nele.

Os `id` são fixos para que o bootstrap da TASK-01.4 e a concessão de
participação não dependam de valor sorteado a cada importação. O papel que cada
conta exerce é intenção de uso, não conteúdo do token — quem o concede é a
aplicação:

| Conta | `subject_id` | Papel pretendido no ambiente |
| --- | --- | --- |
| `ana` | `…0001` | `project_admin` e `dev` no projeto de demonstração |
| `bruno` | `…0002` | `dev` |
| `carla` | `…0003` | `product_owner` |
| `denis` | `…0004` | `gestor` (somente leitura) |
| `admin` | `…0005` | administração global |

`admin` (`6f9d4c2a-0000-4000-8000-000000000005`) é a conta promovida a
administradora global por `subject_id` (ADR-010). Ela **não participa de
projeto nenhum**: o alcance dela é de escopo, não de participação, e SCN-021.2
existe para provar isso.

## Sem conta administrativa do provedor

Não há `KC_BOOTSTRAP_ADMIN_*`. O realm inteiro vem deste arquivo e nenhum
cenário precisa do console. Criar a conta exigiria mais uma credencial em
disco, e é a que menos se justifica: o sufixo `_FILE` que a manteria fora de
variável de ambiente não é suportado nessa opção, e a alternativa seria
credencial por `ENV` — que `infra/docker/architecture.md` §7 recusa.
