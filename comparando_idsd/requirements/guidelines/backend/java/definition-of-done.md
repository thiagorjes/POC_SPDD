# Definition of Done — Backend Java

Uma task está **concluída** quando todos os critérios obrigatórios forem atendidos.

## Obrigatórios

### Qualidade — SonarQube (`Banestes_way`)
- [ ] Quality Gate verde
- [ ] Sem issue **Blocker** ou **Critical**
- [ ] Complexidade cognitiva < 15 por método
- [ ] Sem `System.out.println`, `@SuppressWarnings("squid:...")` ou `// NOSONAR`
- [ ] Sem `catch` vazio ou que só imprime stack trace

### Dependências
- [ ] SCA sem CVE **Critical/High** sem correção ou waiver aprovado
      (ver [`_shared/vulnerable-and-proprietary-libs.md`](../../_shared/vulnerable-and-proprietary-libs.md))
- [ ] Libs internas só do Nexus corporativo, versão fixa; release sem `SNAPSHOT`

### Documentação de API — Swagger
- [ ] Novo endpoint com `@Tag`, `@Operation`, `@ApiResponses`
- [ ] Records de request/response com `@Schema` (descrição + exemplo)
- [ ] Alteração de contrato (campo, status, URL) refletida nas anotações

### Contrato HTTP
- [ ] Status codes conforme [`_shared/api-standards.md`](../../_shared/api-standards.md) (400 formato / 422 negócio / 403 permissão)
- [ ] Erro em `application/problem+json` sem vazar interno
- [ ] Rota versionada `/v1`; breaking change ⇒ nova major

### Segurança
- [ ] Endpoint novo com permissão explícita (deny-by-default)
- [ ] Downstream chamado com token transcodificado, não repassado
- [ ] Sem secret hardcoded — `@ConfigurationProperties` + secret externo

## Recomendados

- [ ] Testes cobrindo fluxo principal e casos de erro
- [ ] Cobertura geral > 80% (JaCoCo)
- [ ] Migration Flyway criada se houve mudança de schema (`V<ANO><MES><DIA><HORA>__descricao.sql`)
- [ ] README atualizado se houve novo endpoint ou mudança de execução

## Checklist de code review

- [ ] Nenhuma `entity` JPA retornada pelo controller
- [ ] Nenhuma regra de negócio no controller
- [ ] Constructor injection com `final` — sem `@Autowired` em campo
- [ ] Records para todos os DTOs
- [ ] `@Slf4j` — sem `System.out`; níveis corretos; sem dado sensível em log
- [ ] Exceções de negócio nomeadas (não `RuntimeException` genérica)
- [ ] Timeout configurado em toda integração (`WebClient`)
- [ ] DTOs de integração isolados em `integrations/<api>/models/`

## Critério não medido se declara

_Acrescentado em 2026-09-11, a partir dos guardrails extraídos das revisões do
sistema IDSD._

- [ ] Todo critério de aceite cuja única prova depende de trabalho de uma
      entrega posterior está **declarado como não medido**, com o motivo e o
      destino, em vez de deixado em silêncio na tabela de aceite.

O silêncio é o problema, não a dependência. Tabela de aceite que afirma um
verificador que nunca roda produz entrega marcada como concluída sobre critério
que ninguém conferiu, e o defeito só aparece quando alguém tenta rodá-lo — em
geral na revisão do épico, quando já custa reabrir. Declarar transfere o custo
para o momento em que ele é barato.

- [ ] Tabela de arquivos de instrução escreve o **caminho real**, e nunca
      marcador de pacote. Verificador de escopo compara texto com a saída do
      `git diff`: com marcador, ele reprova os próprios arquivos que a instrução
      manda criar, e o sinal deixa de distinguir desvio real de notação.

**Verificador:** o verificador de escopo do sistema, sobre a entrega fechada.

> **Dívida nomeada.** O segundo item não é específico de Java — vale para
> qualquer stack cujo processo tenha verificador de escopo. Promovê-lo a
> `_shared/` pertence ao `/guidelines`.
