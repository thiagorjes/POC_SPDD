"""
Verifica a cobertura minima de bordas e estados do artefato de solucao.

A omissao e o modo de falha desta etapa: o agente descreve o caminho feliz e
pula o resto. Este script torna a omissao visivel.

Confere que:
  - as sete situacoes de borda minimas foram respondidas (inclusive com
    'nao se aplica', que e valido, mas precisa estar escrito);
  - ha estado inicial, estados terminais e transicoes proibidas declarados;
  - ha ao menos um caminho alternativo em algum fluxo;
  - ha ao menos uma questao em aberto na secao de opcoes.

Uso: python check_bordas.py --artifact docs/solution/x-solution.md
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

BORDAS = {
    "vazio": r"vazi[oa]|nenhum registro|sem resultado",
    "duplicado": r"duplicad[oa]|repetid[oa]|idempot",
    "concorrente": r"concorren|simult[âa]ne|corrida",
    "fora de ordem": r"fora de ordem|desordenad|atrasad",
    "parcial": r"parcial",
    "expirado": r"expirad|vencid|timeout|prazo esgotado",
    "sem permissão": r"sem permiss|n[ãa]o autorizad|negad[oa]",
}

VAZIOS = {"", "-", "—", "tbd", "a definir"}


def secao(texto: str, titulo: str) -> str:
    m = re.search(rf"^##\s+{titulo}\s*$(.*?)(?=^##\s|\Z)", texto,
                  re.MULTILINE | re.DOTALL)
    return m.group(1) if m else ""


def campo(bloco: str, rotulo: str) -> str:
    m = re.search(rf"[-*]\s*\*\*{re.escape(rotulo)}:?\*\*\s*(.*)", bloco)
    return m.group(1).split("<!--")[0].strip() if m else ""


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--artifact", required=True)
    args = p.parse_args()

    caminho = Path(args.artifact)
    if not caminho.exists():
        print(f"ERRO: artefato nao encontrado: {caminho}", file=sys.stderr)
        return 2

    texto = caminho.read_text(encoding="utf-8")
    # Ignora o bloco de regras negativas, que cita termos de proposito.
    corte = texto.find("## Fora deste artefato")
    corpo = texto[:corte] if corte != -1 else texto
    erros: list[str] = []

    # Descarta linhas de citacao: a nota do template lista as sete bordas de
    # proposito e satisfaria o casamento sem que nada tenha sido respondido.
    bordas = "\n".join(
        linha for linha in secao(corpo, "Regras de borda").splitlines()
        if not linha.lstrip().startswith(">")
    )
    for nome, padrao in BORDAS.items():
        if not re.search(padrao, bordas, re.IGNORECASE):
            erros.append(
                f"ERRO: borda minima nao respondida: '{nome}'. "
                f"'Nao se aplica' e valido, mas precisa estar escrito."
            )

    estados = secao(corpo, "Estados e transições")
    for rotulo in ("Estado inicial", "Estados terminais", "Transições proibidas"):
        if campo(estados, rotulo).lower() in VAZIOS:
            erros.append(f"ERRO: '{rotulo}' nao declarado.")

    fluxos = secao(corpo, "Fluxos de operação")
    if not re.search(r"Caminhos alternativos", fluxos, re.IGNORECASE):
        erros.append("ERRO: nenhum fluxo declara caminhos alternativos.")

    opcoes = secao(corpo, "Opções de comportamento consideradas")
    linhas = [
        linha for linha in opcoes.splitlines()
        if linha.strip().startswith("|")
        and not all(set(c.strip()) <= set("- :") for c in linha.strip("| ").split("|"))
    ]
    # cabecalho + ao menos uma linha de dados
    if len(linhas) < 2:
        erros.append(
            "ERRO: nenhuma questao em aberto registrada — divergencia de D2 sem "
            "alternativa costuma significar que o agente decidiu sozinho."
        )

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
