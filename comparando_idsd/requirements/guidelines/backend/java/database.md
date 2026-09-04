# Performance & Banco de Dados — Backend Java

## 1. Migrations: o único caminho

- Nunca `spring.jpa.hibernate.ddl-auto=update` fora de teste. Use `validate`.
- **Flyway** (dialeto Oracle). Nomenclatura: `V<ANO><MES><DIA><HORA>__descricao_clara.sql`
  (ex.: `V202411221030__create_table_clientes.sql`).
- **Imutabilidade:** migration aplicada nunca é alterada — crie uma nova para corrigir.

## 2. Combate ao N+1

- **Join Fetch:** `JOIN FETCH` em JPQL quando os dados relacionados serão usados.
- **EntityGraph:** `@EntityGraph` no repository para carregar atributos específicos.

```java
@Query("SELECT p FROM Pedido p JOIN FETCH p.itens WHERE p.cliente.id = :clienteId")
List<Pedido> findAllByClienteWithItens(Long clienteId);
```

## 3. Projeções com Records

Não busque entity completa (`SELECT *`) para usar dois campos.

```java
public record ClienteMinDTO(String nome, String email) {}

@Query("SELECT new com.exemplo.model.ClienteMinDTO(c.nome, c.email) FROM Cliente c WHERE c.ativo = true")
List<ClienteMinDTO> findAllAtivosProjected();
```

Projeção evita o Persistence Context e reduz payload do banco.

## 4. Índices

- Sempre indexar colunas de **foreign key**.
- Indexar colunas frequentes em `WHERE`, `ORDER BY`, `JOIN`.
- **Índice composto** quando a busca combina colunas (ex.: `status` + `data_criacao`).
- Migration de criação de tabela vem acompanhada da análise de índices.

## 5. Transacionalidade

- `@Transactional(readOnly = true)` em métodos de consulta.
- Transação **curta**. Proibido chamada a API externa (`WebClient`) dentro de método
  `@Transactional` — não segure conexão de banco esperando I/O de rede.

## 6. Batch

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 25
        order_inserts: true
        order_updates: true
```

## Resumo

- **Entity JPA:** só escrita (save/update/delete).
- **Records/projeções:** 100% das leituras expostas pela API.
- **Lazy loading** é o padrão absoluto; `FetchType.EAGER` é proibido.
