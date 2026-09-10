# TASK-01.4 — Sessão autenticada, autoprovisionamento e admin global

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.3
- **Cenários cobertos:** SCN-001.3, SCN-021.1
- **Origem:** RF-001, RF-021, RN-035, ADR-003, ADR-006, ADR-010

#### Contexto

A entrada no sistema autoprovisiona a pessoa a partir do token, porque não há
cadastro local. No mesmo caminho vive a promoção do primeiro administrador
global — o mecanismo de autorização mais poderoso do sistema, e por isso o que
mais precisa de forma estrita: promoção por identificador verificado, uma única
vez, com registro auditável.

#### O que deve ser feito

- [ ] Implementar `GET /v1/sessao` como Resource Server, exigindo
      `Authorization: Bearer <jwt>` emitido pelo realm confiável.
- [ ] Autoprovisionar `usuario` na primeira entrada a partir do identificador de
      sujeito, do nome e do e-mail do token; espelhar nome e e-mail a cada
      entrada.
- [ ] Implementar a promoção a administrador global casando o **identificador
      de sujeito** do token com a property configurada.
- [ ] Recusar a promoção se já existir qualquer administrador global.
- [ ] Registrar toda promoção em nível `WARN`, com quem foi promovido e quando.
- [ ] Devolver `401` para token ausente, inválido ou expirado.
- [ ] Devolver `503` com cabeçalho `Retry-After` quando o provedor de identidade
      está indisponível e o conjunto de chaves não pode ser validado.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/internal/acesso/SessaoController.java` | criar | rota `GET /v1/sessao` |
| `backend/src/main/java/<pkg>/internal/acesso/SessaoService.java` | criar | autoprovisionamento e promoção |
| `backend/src/main/java/<pkg>/internal/acesso/SessaoResposta.java` | criar | registro de saída |
| `backend/src/main/java/<pkg>/config/SegurancaConfig.java` | criar | Resource Server, negação por padrão |
| `backend/src/main/resources/application.yml` | alterar | property do sujeito a promover |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Contrato literal da rota:

- **Entrada:** apenas o token.
- **Saída `200`:** `{ id, nome, email, adminGlobal }`.
- **`401`** token ausente, inválido ou expirado.
- **`503`** com `Retry-After` quando o provedor está indisponível. O código
  distingue indisponibilidade de credencial ruim, e **nenhum caminho
  alternativo de autenticação é oferecido** — não existe fallback local.

Erro sempre em `application/problem+json`. Rota não mapeada nega por padrão.

Promoção do administrador global:

- A property de configuração guarda o **identificador de sujeito** do token,
  nunca o e-mail.
- A promoção só ocorre se ainda não existir nenhum administrador global. A
  segunda tentativa pelo mesmo caminho não promove ninguém e não altera nada.
- O registro em `WARN` é o histórico auditável exigido, e precisa nomear quem
  foi promovido e o instante.

#### Guia técnico — pontos de atenção

- **Nunca promova por e-mail.** O e-mail é mutável no provedor e, em realm com
  autocadastro ou federação, atribuível por quem se registra: usá-lo como chave
  de promoção poria o bypass universal de autorização a uma requisição de
  distância. Foi exatamente o defeito que a decisão anterior de bootstrap
  carregava.
- **A promoção é única.** "Só ocorre se não existir nenhum administrador global"
  precisa ser verificado dentro da mesma transação da promoção, ou duas entradas
  simultâneas promovem duas pessoas.
- **Indisponibilidade do provedor não abre porta.** Devolver `200` com sessão
  degradada, ou aceitar token sem validar assinatura, contraria a decisão de não
  haver fallback.
- O autoconfigure do OAuth2 resolve o issuer de forma *eager* na subida do
  contexto: sem a ordem de subida do compose a aplicação falha no arranque com
  mensagem que não aponta para a causa.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Primeira entrada cria o usuário a partir do token | duas chamadas seguidas produzem um único registro em `usuario` |
| 2 | Nome e e-mail são espelhados a cada entrada | alterar o nome no token e reentrar atualiza a coluna |
| 3 | Provedor indisponível devolve `503` com `Retry-After` e nenhum caminho alternativo | derrubar o provedor e chamar a rota |
| 4 | A promoção casa o identificador de sujeito, e não o e-mail | token com o e-mail configurado mas sujeito diferente **não** promove |
| 5 | A promoção é única | segunda tentativa pelo mesmo caminho não promove ninguém |
| 6 | Toda promoção fica registrada em `WARN` com quem e quando | inspeção do log da execução que promoveu |
| 7 | Token ausente, inválido ou expirado devolve `401` em problem+json | três requisições, uma por caso |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
