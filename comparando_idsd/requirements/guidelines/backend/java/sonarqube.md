# Qualidade de Código — SonarQube (Backend Java)

Perfil: `Banestes_way` (`sonarqube.apps.ioc.bcloud.sfb`). Regras a seguir rigorosamente.

## Segurança e criptografia

- **Algoritmos fortes:** proibido MD5, SHA-1, DES. Usar AES-256, SHA-256/512. (S4426)
- **Cripto dinâmica:** Salt e IV dinâmicos em toda cifragem. (S5542)
- **Dependências:** proibido lib com vulnerabilidade conhecida (ex.: Log4j 1.x / 2.x vulnerável).
  Ver [`_shared/vulnerable-and-proprietary-libs.md`](../../_shared/vulnerable-and-proprietary-libs.md).

## Manutenibilidade e complexidade

- **Complexidade cognitiva ≤ 15** por método. (S3776)
- Métodos curtos, responsabilidade única.
- Sem código duplicado — extrair para método/serviço comum.

## Higiene e logging

- Proibido `System.out.println` / `System.err.println`. Usar SLF4J (`@Slf4j`). (S106)
- Nunca ignorar exceção — logar com contexto e tratar ou relançar como exceção de negócio.
- CamelCase para variáveis/métodos, PascalCase para classes.

## Configuração (YAML / XML)

- YAML validado quanto a indentação e chave duplicada.
- Serviços internos seguem o padrão de host corporativo (ex.: `demopolis.banestes.sfb`).

## Regras de ouro para a IA

1. **Nunca** usar `@SuppressWarnings("squid:...")` nem `// NOSONAR`. Resolver estruturalmente.
2. Se prever complexidade > 15, **quebrar** em métodos privados durante a geração.
3. Assumir `@Slf4j`; `log.info/warn/error` no lugar de qualquer console.
4. `catch` vazio ou só com stack trace é proibido — logar e relançar/tratar conforme o domínio.

> Atualizar este arquivo sempre que o Quality Profile mudar.
