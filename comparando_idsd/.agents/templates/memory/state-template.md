# Estado Operacional — {{PROJECT_NAME}}
_Atualizado em: {{DATE}}_

> Estado atual do sistema e das demandas em andamento.
> Para princípios estáveis e Decision Records, veja [constitution.md](constitution.md).

---

## Sistema

| Campo | Valor |
| --- | --- |
| Nome | {{PROJECT_NAME}} |
| Cenário | Novo (greenfield) |
| Guidelines | pendente — rode o flow `setup` (`/guidelines`) |
| Inicializado em | {{DATE}} |

Os flows disponíveis não são listados aqui: eles vivem em `governance/flows/`,
e essa é a única fonte. Copiar a cadeia de etapas para cá cria uma segunda
versão que envelhece em silêncio — foi o que aconteceu na stack anterior.

Regenerar os derivados: `python .agents/scripts/init.py`.
Verificar sincronia: `python .agents/scripts/check_drift.py`.

---

## Demandas ativas

| Demanda | Flow | Classe | Etapa atual | Estado |
| --- | --- | --- | --- | --- |

---

## Gates atravessados

| Demanda | Gate | Data | Quem aprovou |
| --- | --- | --- | --- |

---

## Devoluções

> Contadores do anti-ping-pong. Ver o orçamento vigente na policy.

| Demanda | Por par | Por diamante | Total |
| --- | --- | --- | --- |

---

## Histórico

| Data | Mudança |
| --- | --- |
| {{DATE}} | Sistema inicializado via `init.py` |
