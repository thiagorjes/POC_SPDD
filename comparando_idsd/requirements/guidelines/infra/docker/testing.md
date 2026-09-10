# Testes — Infra Docker

> O que esta coleção governa é o **ambiente** em que a suíte roda, não a suíte.
> Estratégia de teste por camada é da coleção da stack — ver
> `../../backend/java/testing.md` e `../../frontend/nextjs/testing.md`.
> Topologia e ordem de subida: [`architecture.md`](architecture.md).

## 1. Duas coisas diferentes

| O quê | Onde vive | Quem verifica |
|---|---|---|
| Teste **do sistema**, que precisa de infraestrutura para rodar | `compose.test.yaml` | Esta coleção define o ambiente |
| Teste **da infraestrutura** — a imagem sobe, fica saudável, a ordem funciona | Ver §5 | Esta coleção inteira |

Confundir os dois produz o sintoma clássico: a suíte passa e o `compose` de
desenvolvimento não sobe, ou o contrário.

## 2. `compose.test.yaml` é arquivo próprio

Não é o `compose.yaml` com override informal. Regras:

- Nenhum serviço de teste publica porta no host. Verificador: `docker compose -f
  docker/compose.test.yaml config` não lista `ports`.
- Rede própria, distinta da de desenvolvimento, para a suíte não alcançar um banco
  de desenvolvimento por acidente. Verificador: `docker compose config` mostra rede
  dedicada.
- Serviços de teste rodam com `restart: "no"` e saem com o código de saída da
  suíte. Verificador: `docker compose run --rm <servico>` propaga o código.
- Dado de teste em volume descartável ou em `tmpfs`. Verificador: revisor humano —
  nenhum volume de teste sobrevive ao `down -v`.

## 3. Biblioteca que sobe contêiner de dentro do contêiner

Suíte que cria contêineres em tempo de execução (padrão Testcontainers) precisa
falar com o daemon do host. A decisão do workspace é **montar o socket do daemon
no serviço de teste**; DinD está descartado. Razão, custo e alternativa:
[ADR-012](../../../../docs/decisions/ADR-012-testcontainers-por-socket-do-host.md).

Consequências que são regra, não detalhe:

| Regra | Razão | Verificador |
|---|---|---|
| Os contêineres criados pela suíte são **irmãos** do serviço de teste, não filhos | Eles nascem no daemon do host | Revisor humano |
| O serviço de teste e os contêineres da suíte ficam na **mesma rede** | Sendo irmãos, o nome de serviço do `compose` não os resolve | A suíte conecta sem `localhost` |
| A montagem do socket só existe em `compose.test.yaml` | Ela concede o equivalente a root no host | `grep -l 'docker.sock' docker/compose.yaml` sem saída |
| A imagem do banco usada pela suíte vem da **mesma variável** do `compose` | Ver [`stack.md`](stack.md) | `docker compose config` e a configuração da suíte exibem o mesmo valor |

Aceito apenas em desenvolvimento e CI. Ambiente compartilhado ou de produção nunca
recebe serviço com socket montado.

## 4. Cache

Ciclo de teste em contêiner sem cache de dependência fica caro o bastante para as
pessoas o contornarem, e suíte contornada não verifica nada. Cache de dependência
(Maven, npm) vai em **volume nomeado** dedicado à suíte, descartável e nunca
compartilhado com o volume de dados.

Verificador: revisor humano — o segundo `docker compose run` da suíte não rebaixa
dependência.

## 5. Verificação da própria infraestrutura

Antes de considerar o ambiente pronto, e a cada mudança em `docker/`:

| Verificação | Comando |
|---|---|
| Compose válido e resolvido | `docker compose -f docker/compose.yaml config -q` |
| Dockerfile conforme | `docker run --rm -i hadolint/hadolint:2.12.0-alpine < docker/<servico>/Dockerfile` |
| Subida limpa a partir do zero | `docker compose down -v && docker compose up --build` |
| Todo serviço fica `healthy` | `docker compose ps` exibe `healthy` na coluna de status de todos os serviços |
| Runtime não é root | `docker compose run --rm <servico> id -u` ≠ `0` |
| Sem segredo na imagem | `trivy image --scanners secret,vuln <ref>` |
| Suíte roda de ponta a ponta | `docker compose -f docker/compose.test.yaml run --rm <servico-de-teste>` |

**A subida a partir do zero é a que encontra o defeito real.** Ambiente que só sobe
com volume preexistente esconde falta de migração, falta de import de dados e
ordem errada — e o primeiro a descobrir é quem acabou de entrar no time.

## 6. O que não roda em contêiner

Teste unitário puro, sem infraestrutura, roda fora de contêiner. É o que mantém
curto o ciclo de quem implementa, e forçá-lo para dentro é o caminho mais rápido
para a suíte deixar de ser executada localmente.

Verificador: revisor humano — existe um alvo de teste unitário que não depende do
daemon Docker.

## Checklist

1. `compose.test.yaml` é arquivo próprio, com rede própria e sem porta publicada.
2. Socket do daemon só em serviço de teste, só em `compose.test.yaml`.
3. Imagem de banco vinda da variável única, também na suíte.
4. Cache de dependência em volume nomeado descartável.
5. `down -v` seguido de `up --build` sobe tudo e todos ficam `healthy`.
6. `trivy` sem achado de segredo.
7. Existe alvo de teste unitário que não precisa de Docker.
