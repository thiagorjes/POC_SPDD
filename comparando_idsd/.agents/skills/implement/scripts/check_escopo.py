"""
Verifica, contra o git, que a implementacao respeitou o escopo da task.

A independencia da verificacao nao e sustentada por boa vontade: o /tests
escreveu sem ver a implementacao, e o /implement nao pode tocar no que o /tests
produziu. Este script checa o segundo lado.

Confere que:
  - nenhum .feature ou step definition foi alterado;
  - todo arquivo alterado esta na tabela "estrutura de arquivos" da task;
  - nenhum caminho listado como proibido foi tocado;
  - o historico da task registra ao menos uma tentativa.

Uso:
  python check_escopo.py --task docs/tasks/x/TASK-01.1-slug.md [--base HEAD~1]
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "scripts"))
from md_tables import celula, corpo_util, preenchido, secao, tabela_da_secao  # noqa: E402

PROTEGIDO = re.compile(
    r"\.feature$|(^|/)(features|steps|step_definitions|step_defs)(/|$)",
    re.IGNORECASE,
)


def alterados(base: str) -> list[str] | None:
    try:
        saida = subprocess.run(
            ["git", "diff", "--name-only", base],
            capture_output=True, text=True, check=True,
        ).stdout
    except (subprocess.CalledProcessError, FileNotFoundError) as exc:
        print(f"ERRO: nao foi possivel consultar o git: {exc}", file=sys.stderr)
        return None
    return [l.strip() for l in saida.splitlines() if l.strip()]


def caminhos_da_task(texto: str) -> tuple[set[str], set[str]]:
    permitidos = set()
    for linha in tabela_da_secao(texto, "Guia técnico — estrutura de arquivos", nivel=4):
        alvo = celula(linha, 0).strip("` ")
        if preenchido(alvo):
            permitidos.add(alvo)

    proibidos = set()
    m = re.search(r"\*\*Proibido tocar:\*\*(.*)", texto)
    if m:
        proibidos = {p.strip().strip("`") for p in m.group(1).split(",") if p.strip()}
    return permitidos, proibidos


def coberto(arquivo: str, permitidos: set[str]) -> bool:
    return any(arquivo == p or arquivo.startswith(p.rstrip("/") + "/")
               for p in permitidos)


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--task", required=True)
    p.add_argument("--base", default="HEAD~1")
    args = p.parse_args()

    caminho = Path(args.task)
    if not caminho.exists():
        print(f"ERRO: task nao encontrada: {caminho}", file=sys.stderr)
        return 2

    texto = corpo_util(caminho.read_text(encoding="utf-8"))
    permitidos, proibidos = caminhos_da_task(texto)
    erros: list[str] = []

    if not permitidos:
        erros.append("ERRO: task sem escopo de arquivo declarado — nada a comparar.")
    if not proibidos:
        erros.append(
            "ERRO: task sem escopo proibido — os .feature e step definitions "
            "precisam estar explicitamente fora do alcance."
        )

    mudancas = alterados(args.base)
    if mudancas is None:
        return 2
    if not mudancas:
        erros.append(f"ERRO: nenhuma alteracao entre {args.base} e a arvore atual.")

    for arquivo in mudancas:
        if PROTEGIDO.search(arquivo):
            erros.append(
                f"ERRO: {arquivo} foi alterado — cenario e step definition sao "
                f"escopo do /tests; alteracao aqui destroi a verificacao "
                f"independente."
            )
            continue
        if any(coberto(arquivo, {pr}) for pr in proibidos):
            erros.append(f"ERRO: {arquivo} esta no escopo proibido da task.")
            continue
        if permitidos and not coberto(arquivo, permitidos) \
                and not arquivo.startswith("docs/tasks/"):
            erros.append(
                f"ERRO: {arquivo} alterado fora da estrutura de arquivos "
                f"declarada na task."
            )

    historico = [l for l in tabela_da_secao(texto, "Histórico", nivel=4)
                 if preenchido(celula(l, 1))]
    if not historico:
        erros.append(
            "ERRO: historico da task vazio — tentativa, custo e intervencao "
            "humana sao registro incremental; o /evidence nao aceita "
            "reconstrucao."
        )

    if not preenchido(secao(texto, "Contexto", nivel=4).strip()):
        erros.append("ERRO: task sem contexto.")

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
