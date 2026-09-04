# Pacote de Evidências — {{FEATURE}}

> Selado no release com hash. IDSD 4.10.
> **Gates:** GATE-EVIDENCIA, GATE-DADOS
>
> Este artefato é **cartório, não narrador**. Ele consolida registros que já
> foram escritos incrementalmente por cada etapa. Se um registro está ausente,
> a etapa correta é **falhar** — nunca reconstruir por memória ou inferência.

- **Release:** {{TAG}}
- **Selado em:** {{DATA}}
- **Hash do pacote:** {{SHA256}}
- **Flow executado:** `governance/flows/{{FLOW}}.yaml`
- **Classe:** {{CLASSE}}

---

## Conformidade de gates

| Gate | Exigido por | Evidência | Verificador | Resultado |
| --- | --- | --- | --- | --- |

- **Gates reprovados:** {{N}}
- **Waivers aplicados:** <!-- caminho em governance/waivers/, ou "nenhum" -->

## Versões

| Item | Versão | Hash |
| --- | --- | --- |
| Skills | | |
| Policies | | |
| Catálogo de gates | | |
| Modelo(s) de agente | | |

> Versão de skill e de policy entram aqui porque a mesma spec produz resultado
> diferente sob skills diferentes. Sem isto a evidência não é reproduzível.

## Classificação e overrides

| Classificação final | Overrides | Reclassificações |
| --- | --- | --- |

## Rastreabilidade

| RF | Cenário | Épico | Task | Commit | PR |
| --- | --- | --- | --- | --- | --- |

## Ordem de verificação

> Prova do GATE-VERIFICACAO-INDEPENDENTE: para cada cenário, o commit do teste
> precede o commit da implementação. Ordem invertida é violação da 4.9.1.

| Cenário | Commit do teste | Commit da implementação | Ordem correta |
| --- | --- | --- | --- |

## Resultados de cenário

| Cenário | Resultado | Execução | Data |
| --- | --- | --- | --- |

## Envelopes de NFR

| RNF | Envelope declarado | Condição de medição | Medido | Dentro |
| --- | --- | --- | --- | --- |

## Achados de revisão

| # | Severidade | Onde | Status | Resolvido em |
| --- | --- | --- | --- | --- |

## Tentativas e custo

| Task | Tentativas | Resultado final | Tokens | Tempo |
| --- | --- | --- | --- | --- |

## Intervenções humanas

> Toda vez que um humano decidiu, aprovou, corrigiu ou destravou.

| Data | Quem | Etapa | O que decidiu |
| --- | --- | --- | --- |

## Verificação de dados sensíveis

- **Varredura executada:** {{DATA}}
- **Ocorrências de dado real de cliente:** {{N}} <!-- diferente de 0 reprova -->

## Registros ausentes

> Se esta seção não está vazia, o pacote **não sela**.

| Registro esperado | Etapa que deveria ter escrito | Por que falta |
| --- | --- | --- |

---

## Fora deste artefato — regras negativas

- **Reconstruir um registro que não existe.** É a proibição central. Ausência
  de registro é falha do pacote, não lacuna a preencher.
- **Resumir ou interpretar resultado.** Evidência transcreve; análise é de
  `/code-review` e `/analyze`.
- **Dado real de cliente**, em qualquer campo (IDSD 4.10.1). Referencie o
  identificador do registro, nunca o conteúdo.
- **Alterar qualquer artefato de origem.** O selo é somente leitura sobre o que
  já existe.
- **Dispensar gate.** Waiver é decisão humana registrada em governança; aqui
  apenas se cita o waiver aplicado.
