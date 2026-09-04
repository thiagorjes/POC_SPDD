---
description: "Captura a intenção do demandante nas palavras dele — resultado esperado, motivação, limites e fora de escopo — e valida com ele antes de qualquer descoberta. Primeira etapa da camada de Controle. Use na abertura de qualquer demanda, antes de /classify."
applyTo: "**"
---

<!-- GERADO por .agents/scripts/generate_platform.py — nao editar. -->
<!-- Fonte: .agents/skills/intent/SKILL.md -->

# /intent

## Objetivo

Registrar o que o demandante quer, **nas palavras dele**, e obter a validação
dele sobre esse registro. A intenção pertence a quem demanda (IDSD 4.2): você
transcreve e organiza, não traduz para linguagem técnica, não completa o
raciocínio e não melhora a formulação.

Esta etapa existe porque, sem ela, a intenção fica diluída dentro do PRD e
ninguém consegue mais distinguir o que foi pedido do que foi inferido.

## Argumentos

- (sem argumento) — abre captura para uma nova demanda
- `"nome-da-feature"` — captura para uma feature nomeada
- `revisar` — reabre a Intent existente para nova validação do demandante

## Pré-condições

Nenhuma. Esta é a porta de entrada.

## Workflow

### Fase 1 — Captura

Uma pergunta de cada vez. Ordem fixa, porque cada resposta enquadra a seguinte:

1. "O que precisa estar diferente depois que isto existir?"
2. "Por que agora? O que dói hoje, ou que oportunidade se perde se esperarmos?"
3. "Que restrições já existem? Prazo, orçamento, decisão já tomada, sistema que
   não pode parar."
4. "O que você **não** quer que aconteça junto com isso?"
5. "Isso tem tela, ou é comportamento sem interface?"

Registre a resposta como ela veio. Quando a formulação for ambígua, **não
desambigue** — pergunte de novo, e registre a resposta final entre aspas.

Se o demandante propuser uma solução ("quero um botão que…"), registre em
**Limites declarados** com origem "proposta do demandante". Nunca em Resultado
esperado, e nunca como decisão.

### Fase 2 — Devolução

Apresente o rascunho ao demandante e pergunte, literalmente: "Isto está dito do
jeito que você diria?"

Ajuste até ele confirmar. Preencha **Validado pelo demandante em** só depois da
confirmação explícita — o gate reprova sem esse campo.

### Fase 3 — Saída

Salve em `docs/intent/[feature]-intent.md` a partir de
`.agents/templates/intent-template.md`.

Valide:

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/intent/validate-rules.json \
  --artifact docs/intent/[feature]-intent.md
```

Registre em `memory/state.md`: feature, data, status "Intent validada".

## Fronteiras

Você responde **o que foi pedido**. Não responde por que o problema existe
(`/discovery`), qual direção tomar (`/shape`), como se comporta (`/solution`),
o que precisa ser verdade (`/prd`) nem com o quê (`/techspec`).

As regras negativas completas estão no template, na seção "Fora deste
artefato" — leia antes de escrever.

## Handoff

Próximo comando: `/classify`.
