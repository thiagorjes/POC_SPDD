# Princípios de Arquitetura (transversal)

> Agnóstico de linguagem. Cada stack materializa estes princípios em `backend/<stack>/architecture.md`
> ou `frontend/<stack>/architecture.md`, com estrutura de pastas e ferramentas concretas.

## 1. Screaming Architecture (domain-driven)

A intenção do negócio domina a estrutura de diretórios. Ao abrir a árvore de pastas do
projeto deve ser imediatamente óbvio **o que a aplicação faz**, não qual framework ela usa.

- Cada domínio de negócio é isolado num diretório próprio (`<raiz-de-domínios>/<dominio>/`).
- Componentes compartilhados entre domínios ficam num `core/` explícito — nunca espalhados.
- Código genérico e sem dependência de framework fica num `util/` reutilizável.
- Integrações com sistemas externos ficam isoladas por sistema de destino, fora dos domínios.

## 2. Camadas e direção de dependência

O fluxo de dependência é **sempre unidirecional**, da borda para o núcleo:

```
entrada (controller/handler)  →  serviço (regra de negócio)  →  persistência/integração
```

| Camada | Pode depender de | Nunca depende de |
|---|---|---|
| Entrada (REST/UI) | Serviço, modelos de transporte | Persistência ou clientes externos diretamente |
| Serviço | Persistência, integrações, `core` | Camada de entrada |
| Persistência | Entidades/modelo de dados | Serviço, entrada |
| Entidade / modelo de dados | Nada interno | Qualquer outra camada |
| Integrações | Seus próprios modelos | Domínios internos (sem acoplamento bidirecional) |
| Configuração | Qualquer camada | — |

**Regra crítica:** um objeto de persistência (entidade de banco) **jamais** é retornado pela
camada de entrada. A conversão modelo-de-dados → modelo-de-transporte ocorre **no serviço**.

## 3. Regra do controller magro

A camada de entrada recebe a request, valida input básico (formato/obrigatoriedade) e delega.
**Zero regra de negócio** nessa camada — orquestração e decisão vivem no serviço.

## 4. Modelos de transporte imutáveis

Todo objeto que cruza fronteira (request, response, projeção, DTO de integração) é imutável.
Cada stack usa seu recurso nativo (records em Java, tipos/`readonly` em TS, records em C#).
Sem getters/setters mutáveis em objetos de transporte.

## 5. Injeção de dependência por construtor

Dependências são declaradas no construtor e mantidas imutáveis. Injeção em campo / service
locator são proibidos — prejudicam testabilidade e escondem o grafo de dependências.

## 6. Configuração tipada

Parâmetros de ambiente (URLs, timeouts, flags) são agrupados em objetos de configuração
tipados e validados no startup. Nunca ler variável de ambiente solta no meio da regra de negócio.
Segredos nunca hardcoded — sempre via configuração externa.

## 7. Tratamento de erros

- Erros de negócio são tipos de exceção/erro **nomeados**, nunca genéricos.
- Um handler global único traduz erro → resposta padronizada (ver [`api-standards.md`](api-standards.md), formato Problem Details / RFC 7807).
- `catch` vazio ou que apenas imprime stack trace é proibido: registre com contexto e relance ou trate.

## 8. Nomes que descrevem negócio

Métodos de serviço têm nomes de ação de negócio (`finalizarCompra`, não `updateStatus`).
Comentários explicam **por quê**, nunca **o quê**.

## 9. Fronteira com sistemas externos

Cada integração externa tem seus próprios modelos, isolados dos domínios internos.
Timeout explícito obrigatório em toda chamada de rede. Detalhes por stack em `integrations.md`.

---

## Checklist de revisão arquitetural

- [ ] Estrutura de pastas revela os domínios de negócio, não o framework
- [ ] Nenhuma dependência da borda para o núcleo na direção errada
- [ ] Nenhuma entidade de persistência exposta pela camada de entrada
- [ ] Nenhuma regra de negócio na camada de entrada
- [ ] Injeção por construtor; sem service locator / injeção em campo
- [ ] Configuração tipada e validada no startup; sem segredo hardcoded
- [ ] Erros de negócio nomeados + handler global padronizado
- [ ] Integrações externas isoladas por sistema, com timeout explícito
