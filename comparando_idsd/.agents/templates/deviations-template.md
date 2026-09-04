# Desvios — {{FEATURE}}
_Atualizado em: {{DATE}}_

> Registro dos pontos em que o código entregue diverge da especificação, e da
> decisão sobre qual dos dois lados estava errado. Um desvio resolvido nunca é
> apagado: ele é a única memória de por que a spec mudou depois do gate.

---

## Sumário

| DEV | Artefato de spec | Direção | Status |
| --- | --- | --- | --- |
| DEV-01 | {{ARTEFATO}} | {{DIRECAO}} | {{STATUS}} |

Direções possíveis: `spec corrigida` · `código corrigido` · `aceito com prazo`
Status possíveis: `resolvido` · `pendente`

---

## DEV-01 — {{TITULO_CURTO}}

- **Detectado em:** {{DATE}}
- **Artefato de spec:** {{CAMINHO_E_SECAO}}
- **Local no código:** {{CAMINHO_E_LINHA}}
- **Cenário afetado:** {{ID_CENARIO_OU_NENHUM}}
- **O que a spec diz:** {{AFIRMACAO_DA_SPEC}}
- **O que o código faz:** {{COMPORTAMENTO_REAL}}
- **Quem decidiu:** {{PESSOA_OU_PAPEL}}
- **Direção:** {{DIRECAO}}
- **Por quê:** {{JUSTIFICATIVA}}
- **Status:** {{STATUS}}

> Se a direção for `aceito com prazo`, a linha abaixo é obrigatória.

- **Prazo e dono:** {{DATA_LIMITE_E_RESPONSAVEL}}

---

## Fora deste artefato — regras negativas

- **Não** corrige a spec nem o código; registra a decisão e aponta onde ela foi
  aplicada.
- **Não** aceita desvio sem responsável humano nomeado.
- **Não** registra divergência de estilo ou de organização de arquivo — isso é
  `/code-review`.
- **Não** substitui o pacote de evidências — desvio aceito continua tendo de
  aparecer em `/evidence`.
