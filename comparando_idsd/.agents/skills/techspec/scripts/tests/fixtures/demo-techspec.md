# TechSpec — demo

## 1. Visão Geral Técnica

Serviço HTTP de cobrança, coleção `backend/demo`.

## 2. Decisões Arquiteturais

Camada de aplicação isolada do adaptador HTTP (ADR-001).

## 3. Modelo de Dados

Resumo; fonte em `demo/data-model.md`. Entidade `Cobranca`.

## 4. Contratos de API / Interface

Índice; fonte em `demo/contracts/cobranca.md`.

## 5. Arquitetura e Fluxo

Adaptador HTTP → caso de uso → repositório.

## 6. Dependências Inter-Sistemas

Nenhuma.

## 7. Estratégia de Testes

Conforme `testing.md` da coleção declarada.

## 8. Segurança e Observabilidade

Conforme `_shared/api-security.md` e `_shared/logging-and-levels.md`.

## 9. Matriz de Rastreabilidade

| Requisito | Decisão | Verificação |
|---|---|---|
| RF-001 | ADR-001 | teste de integração |
| RF-002 | ADR-001 | teste de integração |

## 10. Questões em Aberto

Nenhuma.

## 11. Histórico de Revisões

| Versão | Data | Autor | Alteração |
|---|---|---|---|
| 1.0 | 2026-09-03 | fixture | Versão inicial |

## Fora deste artefato — regras negativas

Cenário Gherkin pertence ao /prd. TASK-01.1 e EPIC-01 pertencem ao /tasks.
