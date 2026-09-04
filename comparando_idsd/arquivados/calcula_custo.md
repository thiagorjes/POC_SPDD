Consumo de tokens — consolidado por data/modelo

Analise C:\Users\User\.claude\projects\**\*.jsonl e gere apenas o consolidado por data e modelo do dia 03/09/2026 (fuso UTC-3, corte 00:00:00–23:59:59).

Regras:
- Atribua cada mensagem ao dia do seu timestamp, não ao dia de abertura da sessão.
- Considere toda entrada com message.usage, deduplicada por (requestId, message.id); inclua transcripts de subagents.
- Não segregue por workspace/cwd, não liste sessões, não faça inventário de pastas, não faça ranking.
- Preços (USD/MTok): opus-5 5/25, sonnet-5 2/10, sonnet-4-6 3/15, haiku-4-5 1/5. Cache write = 1,25× input; cache read = 0,10× input.

Saída: uma única tabela markdown no chat, uma linha por modelo + linha de total do dia, colunas Modelo | Input | Cache write | Cache read | Output | Custo (USD). Sem arquivo, sem comentário adicional.