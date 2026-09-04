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
