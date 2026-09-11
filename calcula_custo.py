#!/usr/bin/env python3
"""Consolida uso de tokens dos transcripts do Claude Code por data e modelo.

Uso:
    python3.exe calcula_custo.py <data-inicio|data-unica> [data-fim]

Datas em DD/MM/AAAA ou AAAA-MM-DD. Fuso UTC-3, corte 00:00:00-23:59:59.
"""
import collections
import datetime
import glob
import json
import os
import sys

PRECOS = {  # USD por MTok: (input, output)
    "opus-5": (5, 25),
    "sonnet-5": (2, 10),
    "sonnet-4-6": (3, 15),
    "haiku-4-5": (1, 5),
}
TZ = datetime.timezone(datetime.timedelta(hours=-3))
BASE = os.path.join(os.path.expanduser("~"), ".claude", "projects")


def parse_data(s):
    for fmt in ("%d/%m/%Y", "%Y-%m-%d", "%d-%m-%Y"):
        try:
            return datetime.datetime.strptime(s, fmt).date()
        except ValueError:
            pass
    sys.exit(f"Data invalida: {s}")


def norm_modelo(m):
    m = (m or "unknown").lower()
    if "opus" in m and "5" in m:
        return "opus-5"
    if "haiku" in m:
        return "haiku-4-5"
    if "sonnet" in m:
        return "sonnet-4-6" if ("4-6" in m or "4.6" in m) else "sonnet-5"
    return m


def custo(a, modelo):
    ip, op = PRECOS.get(modelo, (0, 0))
    return (a[0] * ip + a[1] * ip * 1.25 + a[2] * ip * 0.10 + a[3] * op) / 1e6


def fmt(n):
    return f"{n:,}".replace(",", ".")


def main():
    if len(sys.argv) not in (2, 3):
        sys.exit(__doc__)
    d_ini = parse_data(sys.argv[1])
    d_fim = parse_data(sys.argv[2]) if len(sys.argv) == 3 else d_ini
    if d_fim < d_ini:
        d_ini, d_fim = d_fim, d_ini

    seen = set()
    agg = collections.defaultdict(lambda: [0, 0, 0, 0])  # (data, modelo) -> tokens

    for arq in glob.glob(os.path.join(BASE, "**", "*.jsonl"), recursive=True):
        with open(arq, encoding="utf-8", errors="replace") as fh:
            for linha in fh:
                linha = linha.strip()
                if not linha:
                    continue
                try:
                    d = json.loads(linha)
                except ValueError:
                    continue
                msg = d.get("message")
                if not isinstance(msg, dict):
                    continue
                u = msg.get("usage")
                ts = d.get("timestamp")
                if not isinstance(u, dict) or not ts:
                    continue
                try:
                    dia = datetime.datetime.fromisoformat(
                        ts.replace("Z", "+00:00")
                    ).astimezone(TZ).date()
                except ValueError:
                    continue
                if not (d_ini <= dia <= d_fim):
                    continue
                chave = (d.get("requestId"), msg.get("id"))
                if chave in seen:
                    continue
                seen.add(chave)
                a = agg[(dia, norm_modelo(msg.get("model")))]
                a[0] += u.get("input_tokens") or 0
                a[1] += u.get("cache_creation_input_tokens") or 0
                a[2] += u.get("cache_read_input_tokens") or 0
                a[3] += u.get("output_tokens") or 0

    varios_dias = d_ini != d_fim
    cab = (["Data"] if varios_dias else []) + [
        "Modelo", "Input", "Cache write", "Cache read", "Output", "Custo (USD)"
    ]
    print("| " + " | ".join(cab) + " |")
    print("|" + "---|" * len(cab))

    total = [0, 0, 0, 0]
    custo_total = 0.0
    for (dia, modelo), a in sorted(agg.items(), key=lambda kv: (kv[0][0], kv[0][1])):
        if not any(a):  # entradas sem tokens (ex.: <synthetic>)
            continue
        c = custo(a, modelo)
        custo_total += c
        total = [t + x for t, x in zip(total, a)]
        pref = [dia.strftime("%d/%m/%Y")] if varios_dias else []
        print("| " + " | ".join(pref + [modelo] + [fmt(x) for x in a]
                                + [f"{c:,.2f}".replace(",", "@").replace(".", ",").replace("@", ".")]) + " |")

    rotulo = (["**Total**", ""] if varios_dias else ["**Total**"])
    ct = f"{custo_total:,.2f}".replace(",", "@").replace(".", ",").replace("@", ".")
    print("| " + " | ".join(rotulo + [f"**{fmt(x)}**" for x in total] + [f"**{ct}**"]) + " |")


if __name__ == "__main__":
    main()
