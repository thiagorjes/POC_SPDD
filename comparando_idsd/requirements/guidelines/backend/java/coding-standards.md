# Padrões de Código — Backend Java

> Convenções de commit: ver [`_shared/git-workflow.md`](../../_shared/git-workflow.md).
> Níveis de log: ver [`_shared/logging-and-levels.md`](../../_shared/logging-and-levels.md).

## Nomenclatura

| Artefato | Convenção | Exemplo |
|---|---|---|
| Classes | PascalCase | `ExtratoService`, `ClienteRequest` |
| Métodos / variáveis | camelCase | `buscarExtratoPorConta`, `valorTotal` |
| Constantes | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Packages | lowercase (underscore só se necessário) | `extrato`, `gud_api` |
| Arquivos Java | PascalCase | `ExtratoController.java` |
| Migrations Flyway | `V<ANO><MES><DIA><HORA>__descricao_clara.sql` | `V202411221030__create_table_clientes.sql` |
| Mocks WireMock | `<metodo>-<recurso>-<status>.json` | `get-address-200.json` |

Proibido campo cujo nome tenha duas maiúsculas seguidas logo após o prefixo
(`eFinal`, `xATivo`). A introspecção JavaBeans — usada por Jackson, MapStruct e
Hibernate — só decapitaliza a primeira letra quando a seguinte é minúscula:
`isEFinal()` expõe a propriedade `EFinal`, e a serialização quebra em silêncio,
sem erro de compilação. Renomeie para uma forma onde a letra após o prefixo já
seja minúscula (`etapaFinal`).
**Verificador:** revisor humano na tabela de nomenclatura; teste de contrato com
`jsonPath` exaustivo ([`testing.md`](testing.md) §3) detecta a quebra.

## Injeção de dependência

Exclusivamente **constructor injection** com campos `final`. `@Autowired` em campo é proibido.
Com um único construtor, o `@Autowired` no construtor é opcional (Spring 6+). Garante
imutabilidade e expõe dependências circulares.

## DTOs e imutabilidade

**Java Records** para todo objeto de transporte (DTO, request, response, projeção).
Proibido classe com getter/setter para DTO.

```java
public record ExtratoRequest(
    @NotBlank String numeroConta,
    @NotNull LocalDate dataInicio,
    @NotNull LocalDate dataFim
) {}
```

## Tratamento de erros

- Exceção de negócio = classe customizada nomeada; nunca `throw new RuntimeException("...")`.
- `catch` vazio ou com só `printStackTrace()` é proibido.
- `@RestControllerAdvice` global em `config/` retorna `ProblemDetail` (RFC 7807 — mapa
  exceção → status em [`_shared/api-standards.md`](../../_shared/api-standards.md) §2).

```java
throw new RemessaNotFoundException("Remessa " + id + " não encontrada");
```

## Logging

`@Slf4j` do Lombok. `System.out`/`System.err` proibidos (Sonar S106). Mensagem parametrizada,
sem concatenar valores. Níveis e dados proibidos em log: [`_shared/logging-and-levels.md`](../../_shared/logging-and-levels.md).

## Complexidade cognitiva

Máximo **15** por método (Sonar S3776). Acima disso, extrair métodos privados com nome de
ação de negócio.

## Comentários

Zero comentário explicando **o quê**. Comente só o **porquê** quando não for óbvio.

## Supressão de alertas Sonar

Proibido `@SuppressWarnings("squid:...")` e `// NOSONAR`. Corrigir estruturalmente.

## Checklist

- [ ] Nomenclatura conforme tabela
- [ ] Constructor injection com `final`; sem `@Autowired` em campo
- [ ] Records para todos os DTOs
- [ ] Exceções de negócio nomeadas; sem `catch` vazio; handler global + `ProblemDetail`
- [ ] `@Slf4j`; sem `System.out`; mensagem parametrizada
- [ ] Complexidade cognitiva < 15
- [ ] Sem `// NOSONAR` / `@SuppressWarnings`
