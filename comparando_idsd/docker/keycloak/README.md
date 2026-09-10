# Realm de desenvolvimento

`realm.json` é material de bootstrap: versionado, montado somente leitura e
importado na subida do provedor. Ele **não aceita comentário** — a importação
recusa qualquer campo desconhecido e o servidor entra em laço de reinício com a
mensagem `Unrecognized field`. Por isso as razões estão aqui.

## Papéis

Os quatro papéis existem no realm apenas para dar ao ambiente o vocabulário do
produto. **A autorização efetiva é modelada na aplicação**, por par
usuário↔projeto (ADR-003, BDR-001): nenhuma decisão de permissão depende deste
arquivo, e mudar um papel aqui não muda o que alguém pode fazer.

## Clients

| Client | Fluxo | Por quê |
| --- | --- | --- |
| `idsd-web` | authorization code com PKCE (S256), client público | Não há client secret no backend — ele é Resource Server puro |
| `idsd-e2e` | direct access grant | Só o suporte da suíte usa, para montar massa pela API. Os cenários autenticam pela tela de verdade; injetar token tiraria de SCN-001.1 exatamente o que ele verifica |

## Contas

Senha única, `senha-de-teste`, igual à que o suporte da suíte usa. Este realm
nunca sai de desenvolvimento e não há nada a proteger nele.

`admin` (`6f9d4c2a-0000-4000-8000-000000000005`) é a conta promovida a
administradora global por `subject_id` (ADR-010). Ela **não participa de
projeto nenhum** e não carrega papel de realm: o alcance dela é de escopo, não
de participação, e SCN-021.2 existe para provar isso. O `id` é fixo para que o
bootstrap da TASK-01.4 não dependa de um valor sorteado a cada importação.

## Sem conta administrativa do provedor

Não há `KC_BOOTSTRAP_ADMIN_*`. O realm inteiro vem deste arquivo e nenhum
cenário precisa do console. Criar a conta exigiria mais uma credencial em
disco, e é a que menos se justifica: o sufixo `_FILE` que a manteria fora de
variável de ambiente não é suportado nessa opção, e a alternativa seria
credencial por `ENV` — que `infra/docker/architecture.md` §7 recusa.
