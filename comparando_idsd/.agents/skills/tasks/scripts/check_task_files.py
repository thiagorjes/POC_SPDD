"""
Verifica os arquivos individuais de task.

O documento consolidado e o plano; o arquivo individual e o que o /implement
consome. Se ele nao basta sozinho, a task nao esta pronta.

Confere que:
  - todo TASK-xx.y do consolidado tem arquivo em docs/tasks/<feature>/;
  - o arquivo declara os campos de execucao obrigatorios;
  - tem contexto, acoes, guia tecnico, criterios de aceite e historico;
  - declara escopo de arquivo, incluindo o que e proibido tocar;
  - nao remete a outro documento no lugar da instrucao;
  - executor humano traz esforco.

Uso:
  python check_task_files.py --tasks docs/tasks/x-tasks.md
  python check_task_files.py --task docs/tasks/x/TASK-01.1-slug.md
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "scripts"))
from md_tables import campo, corpo_util, preenchido, secao  # noqa: E402

CAMPOS = ["Status", "Sistema", "Executor", "Depende de", "Cenários cobertos", "Origem"]

SECOES = [
    "Contexto",
    "O que deve ser feito",
    "Guia técnico — estrutura de arquivos",
    "Critérios de aceite",
    "Histórico",
]

ADIAMENTO = re.compile(
    r"\bconforme (a |o )?(TechSpec|PRD|especifica[çc][ãa]o)\b|"
    r"\b(ver|veja|consulte) (a |o )?(TechSpec|PRD)\b",
    re.IGNORECASE,
)

EXECUTORES = {"agente", "humano", "misto"}


def verificar(caminho: Path, rotulo: str) -> list[str]:
    erros: list[str] = []
    texto = corpo_util(caminho.read_text(encoding="utf-8"))

    for c in CAMPOS:
        if not preenchido(campo(texto, c)):
            erros.append(f"ERRO: {rotulo} sem campo '{c}'.")

    for s in SECOES:
        if not preenchido(secao(texto, s, nivel=4).strip()) and \
           not preenchido(secao(texto, s, nivel=3).strip()):
            erros.append(f"ERRO: {rotulo} sem secao '{s}'.")

    executor = campo(texto, "Executor").lower()
    if executor and executor not in EXECUTORES:
        erros.append(
            f"ERRO: {rotulo} com executor invalido: '{executor}'. "
            f"Validos: agente, humano, misto."
        )
    if executor == "humano" and not preenchido(campo(texto, "Esforço")):
        erros.append(
            f"ERRO: {rotulo} com executor humano precisa de esforco — "
            f"'tentativas' nao se aplica a execucao humana."
        )
    if executor in {"agente", "misto"} and not preenchido(campo(texto, "Tentativas")):
        erros.append(f"ERRO: {rotulo} sem orcamento de tentativas.")

    if not re.search(r"\*\*Proibido tocar:\*\*", texto):
        erros.append(
            f"ERRO: {rotulo} sem escopo proibido — os .feature e os step "
            f"definitions precisam estar fora do alcance da implementacao."
        )

    if not re.findall(r"SCN-\d{3}\.\d{1,2}", campo(texto, "Cenários cobertos")):
        erros.append(f"ERRO: {rotulo} sem cenario coberto.")

    if not re.search(r"^\s*-\s*\[ \]", texto, re.MULTILINE):
        erros.append(f"ERRO: {rotulo} sem checklist de acoes.")

    achado = ADIAMENTO.search(texto)
    if achado:
        erros.append(
            f"ERRO: {rotulo} remete a outro documento ('{achado.group(0)}') no "
            f"lugar da instrucao — a task deve bastar sozinha."
        )

    return erros


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--tasks")
    p.add_argument("--task")
    args = p.parse_args()

    if not args.tasks and not args.task:
        print("ERRO: informe --tasks ou --task.", file=sys.stderr)
        return 2

    erros: list[str] = []

    if args.task:
        caminho = Path(args.task)
        if not caminho.exists():
            print(f"ERRO: task nao encontrada: {caminho}", file=sys.stderr)
            return 2
        erros += verificar(caminho, caminho.name)

    if args.tasks:
        consolidado = Path(args.tasks)
        if not consolidado.exists():
            print(f"ERRO: documento de tasks nao encontrado: {consolidado}",
                  file=sys.stderr)
            return 2
        pasta = consolidado.parent / consolidado.stem.replace("-tasks", "")
        ids = sorted(set(re.findall(
            r"^###\s+(TASK-\d{2}\.\d{1,2})\b",
            corpo_util(consolidado.read_text(encoding="utf-8")), re.MULTILINE,
        )))
        if not ids:
            erros.append("ERRO: nenhuma task no documento consolidado.")
        for tid in ids:
            achados = sorted(pasta.glob(f"{tid}-*.md")) + sorted(pasta.glob(f"{tid}.md"))
            if not achados:
                erros.append(
                    f"ERRO: {tid} sem arquivo individual em {pasta}/ — e o "
                    f"arquivo que o /implement consome."
                )
                continue
            erros += verificar(achados[0], achados[0].name)

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
