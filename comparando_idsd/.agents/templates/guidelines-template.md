# {{TITULO}} — {{SISTEMA}}
_Atualizado em: {{DATE}}_

> Norma vigente do sistema. É lida como contexto obrigatório por `/techspec`,
> `/implement` e `/code-review`, e alimenta a dimensão N do REASONS Canvas.
> Norma que ninguém consegue verificar não é norma: é preferência.

---

## Escopo

{{O_QUE_ESTE_ARQUIVO_GOVERNA_E_O_QUE_NAO}}

---

## Regras

> Uma regra por linha, no imperativo, com o verificador ao lado. `verificação:
> revisor humano` é aceitável e honesto; deixar a coluna vazia não é.

| # | Regra | Verificação | Origem |
| --- | --- | --- | --- |
| 01 | {{REGRA}} | {{COMANDO_LINTER_OU_REVISOR_HUMANO}} | {{DR_OU_DECISAO_DA_ENTREVISTA}} |

---

## Exceções permitidas

> Exceção prevista é governança; exceção improvisada é dívida. Se não houver,
> escreva "Nenhuma" — não deixe em branco.

| Regra | Quando a exceção vale | Quem autoriza | Como fica registrada |
| --- | --- | --- | --- |
| {{NN}} | {{CONDICAO}} | {{PAPEL}} | {{ONDE_FICA_O_REGISTRO}} |

---

## Decisões que sustentam estas regras

| DR | Título | Regras afetadas |
| --- | --- | --- |
| {{TIPO}}-{{NNN}} | {{TITULO_DR}} | {{NN}} |

---

## Fora deste artefato — regras negativas

- **Não** descreve requisito de produto — isso é `/prd`.
- **Não** decide arquitetura de uma feature específica — isso é `/techspec`.
- **Não** contém código de aplicação; exemplo curto só quando a regra é
  ambígua sem ele.
- **Não** registra estado de execução (o que já foi implementado) — isso é
  `memory/state.md`.
