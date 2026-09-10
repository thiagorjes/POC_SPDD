# Arquitetura — Infra Docker

> Materializa [`../../_shared/architecture-principles.md`](../../_shared/architecture-principles.md):
> as fronteiras que lá são de código, aqui são de contêiner, rede e ordem de subida.
> Versões e ferramental: [`stack.md`](stack.md).

## 1. Topologia

Um processo por contêiner. Um serviço do `compose` por processo. Serviço que
executa duas coisas — aplicação e migração, aplicação e proxy — não é
otimização: é o que impede reiniciar uma sem a outra.

| Papel | Regra | Verificador |
|---|---|---|
| Aplicação | Uma imagem por serviço, com um único `CMD`/`ENTRYPOINT` de processo longo | Revisor humano no `Dockerfile` |
| Banco e infraestrutura | Imagem oficial, sem camada própria; customização por script de init montado | `docker compose config -q` + revisor |
| Tarefa de ciclo de vida (migração, seed) | Serviço próprio com `restart: "no"`, que roda e sai | Revisor humano |
| Teste | Serviço próprio, nunca o mesmo do runtime | Ver [`testing.md`](testing.md) |

## 2. Redes e exposição

- Toda comunicação entre serviços acontece por **rede interna nomeada** do
  `compose`, resolvida por nome de serviço. Verificador: revisor humano — nenhum
  serviço referencia `localhost` nem IP para falar com outro.
- **Publicar porta no host é exceção**, permitida só para o que uma pessoa acessa
  do navegador ou do terminal. Banco e provedor de identidade não publicam porta
  em `compose.test.yaml`. Verificador: `docker compose -f docker/compose.test.yaml config`
  não lista `ports` em serviço de infraestrutura.
- Porta de gerenciamento (métricas, health administrativo) nunca é publicada no
  host. Verificador: revisor humano.

Exceções a esta seção vão na tabela do fim do arquivo, com quem autorizou.

## 3. Volumes

| Tipo de dado | Onde vive | Regra |
|---|---|---|
| Dado do banco | Volume nomeado | Nunca `bind mount` de diretório do repositório — permissão de host difere entre sistemas operacionais e o defeito só aparece na máquina de outra pessoa |
| Material de bootstrap (realm, seed, script de init) | `bind mount` **somente leitura** (`:ro`) | Versionado em `docker/` |
| Artefato de build | Não é volume | Sai em camada de imagem ou em `docker compose cp`; volume para isso mascara build quebrado |
| Cache de dependência em teste | Volume nomeado, descartável | Ver [`testing.md`](testing.md) |

Verificador: `docker compose config -q` + revisor humano sobre a tabela acima.

## 4. Ordem de subida

`depends_on` sem condição declara ordem de **início**, não de prontidão, e é a
causa mais comum de arranque que falha com mensagem que não aponta para a causa.

- Todo `depends_on` declara `condition:`. Verificador: `docker compose config`
  não deve conter `depends_on` em forma de lista simples.
- Dependência sobre serviço de longa duração usa `condition: service_healthy`.
- Dependência sobre tarefa de ciclo de vida usa
  `condition: service_completed_successfully`.
- `service_started` é permitido apenas quando o consumidor tolera indisponibilidade
  do dependente e **reconecta sozinho**. Quem usa declara por comentário no
  `compose` qual é o mecanismo de reconexão. Verificador: revisor humano.

**Cliente OIDC é o caso que não tolera.** Framework que resolve o `issuer-uri` na
subida do contexto falha no arranque se o provedor não estiver no ar com o realm
já importado — e "processo no ar" e "realm importado" não são a mesma condição. O
`start_period` do healthcheck do provedor é dimensionado pela importação, não pelo
boot.

## 5. Healthcheck

Todo serviço de longa duração declara `healthcheck`. Sem ele, `service_healthy` é
inutilizável e a topologia inteira degrada para ordem de início.

| Regra | Razão | Verificador |
|---|---|---|
| O healthcheck sonda **prontidão**, não vivacidade | Quem espera pelo serviço quer saber se pode usá-lo | Revisor humano |
| Serviço com porta de gerenciamento separada é sondado **nessa** porta | Sondar a porta da aplicação responde antes de o serviço estar utilizável | Revisor humano |
| Imagem sem ferramenta HTTP no runtime não recebe healthcheck com `curl` | O healthcheck passa a falhar sempre ou é silenciosamente desligado | `docker inspect <servico>` reporta `State.Health.Status: healthy` |
| `start_period` cobre o pior caso de inicialização, incluindo import de dados | Ver §4 | Revisor humano |
| Endpoint de saúde que precisa ser habilitado por flag é habilitado explicitamente | Healthcheck que aponta para endpoint desligado é `service_started` disfarçado | Revisor humano |

**Liveness e readiness são probes distintos e não se colapsam.** Liveness observa
só o processo. Readiness inclui as dependências de que o serviço precisa para
atender — banco, conexão de escuta, fila. O `healthcheck` do `compose` usa
readiness.

Colapsar os dois é o erro caro: instabilidade momentânea do banco derruba **todas**
as réplicas ao mesmo tempo, exatamente durante a janela de backoff em que elas se
recuperariam sozinhas. Verificador: revisor humano, com os dois endpoints
declarados no `compose`.

## 6. Migração de schema

Migração de banco **não roda no boot da aplicação** quando o sistema pode ter mais
de uma instância. Ela é um serviço dedicado, que roda até o fim e sai; as
instâncias sobem com validação de schema e sem permissão de migrar, dependendo da
migração por `condition: service_completed_successfully`.

Razão e alternativa descartada: [ADR-011](../../../../docs/decisions/ADR-011-migracao-de-schema-em-servico-dedicado.md).
Verificador: revisor humano — o `compose` tem o serviço de migração e nenhuma
instância de aplicação com migração habilitada.

## 7. Configuração e segredo

| Natureza | Como entra | Proibido |
|---|---|---|
| Configuração não sensível | Variável de ambiente, com valor padrão declarado | Valor embutido em imagem |
| Credencial, chave, token | **Arquivo montado** (`secrets:` do Compose), lido pela aplicação a partir do caminho | `ENV`/`ARG` com credencial, valor em `application.yml`/`.env` versionado, `--build-arg` com segredo |

Variável de ambiente vaza em `docker inspect`, em log de crash e em qualquer dump
de processo; arquivo montado não. Em desenvolvimento, valor descartável no
`compose` é aceitável **desde que entre pelo mesmo mecanismo** — se o caminho de
desenvolvimento difere do de produção, o de produção nunca é exercitado.

`ARG` de build jamais recebe segredo: ele fica na história de camadas da imagem
mesmo que a camada seguinte o apague.

Materializa [`../../_shared/api-security.md`](../../_shared/api-security.md).
Verificadores: `docker history <imagem>` não exibe credencial; `trivy image
--scanners secret` sem achado; revisor humano no `compose`.

## 8. Log

Contêiner escreve log em `stdout`/`stderr`, sempre. Nada de arquivo de log dentro
da imagem, nada de rotação própria, nada de driver de log configurado por serviço.
Níveis e dados proibidos em log seguem
[`../../_shared/logging-and-levels.md`](../../_shared/logging-and-levels.md), que
esta coleção não reescreve.

Verificador: revisor humano — nenhum `Dockerfile` cria diretório de log, nenhum
serviço monta volume para log.

## 9. Protocolo de uso pela IA

Ao gerar ou alterar arquivo de infra:

1. Leia [`stack.md`](stack.md) primeiro — é ele que fixa versões e imagens.
2. Nunca introduza imagem base nova sem digest e sem linha na tabela do `stack.md`.
3. Nunca troque `service_healthy` por `service_started` para "destravar" a subida.
   Se o healthcheck não fica pronto, o defeito é o healthcheck.
4. Nunca mova migração para o boot para simplificar o `compose`.
5. Não invente serviço de proxy, cache ou fila que nenhum DR do sistema decidiu.

## 10. Exceções

| Exceção | Onde vale | Quem autoriza |
|---|---|---|
| Publicar porta de infraestrutura no host | Ambiente de desenvolvimento, para inspeção manual | Responsável técnico do sistema, com comentário no `compose` |
| `service_started` em dependência | Consumidor com reconexão declarada | Responsável técnico do sistema |
| Montar socket do daemon em um serviço | Somente serviço de teste, somente desenvolvimento e CI — ver [ADR-012](../../../../docs/decisions/ADR-012-testcontainers-por-socket-do-host.md) | Responsável técnico do sistema |

## Checklist

1. Um processo por contêiner; migração em serviço próprio.
2. Todo `depends_on` tem `condition:`.
3. Todo serviço de longa duração tem `healthcheck` que sonda prontidão.
4. Liveness e readiness separados; o `compose` usa readiness.
5. Nenhum segredo em variável de ambiente, `ARG` ou arquivo versionado.
6. Dado de banco em volume nomeado; bootstrap montado como `:ro`.
7. Log só em `stdout`/`stderr`.
8. Toda exceção está na tabela §10, com autorizador nomeado.
