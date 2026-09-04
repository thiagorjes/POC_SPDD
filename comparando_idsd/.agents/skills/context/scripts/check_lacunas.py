"""
Verifica a disciplina de lacunas do artefato de contexto (IDSD 4.6).

Confere que:
  - toda linha de 'Contexto resolvido' tem fonte preenchida;
  - confianca 'baixa' foi tratada como lacuna, nao como item resolvido;
  - toda suposicao operacional tem dono e prazo de confirmacao;
  - o contador de lacunas bloqueantes bate com a tabela.

Uso: python check_lacunas.py --artifact docs/context/x-context.md
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

VAZIOS = {"", "-", "—", "tbd", "a definir", "n/a"}


def linhas_da_tabela(texto: str, secao: str) -> list[list[str]]:
    """Retorna as linhas de dados da primeira tabela sob a secao dada."""
    m = re.search(rf"^##\s+{secao}\s*$(.*?)(?=^##\s|\Z)", texto,
                  re.MULTILINE | re.DOTALL)
    if not m:
        return []
    linhas = []
    for linha in m.group(1).splitlines():
        linha = linha.strip()
        if not linha.startswith("|"):
            continue
        celulas = [c.strip() for c in linha.strip("|").split("|")]
        if not celulas or all(set(c) <= set("- :") for c in celulas):
            continue  # separador
        if celulas and celulas[0].lower() in {"item", "#", "sistema"}:
            continue  # cabecalho
        if any(c for c in celulas):
            linhas.append(celulas)
    return linhas


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--artifact", required=True)
    args = p.parse_args()

    caminho = Path(args.artifact)
    if not caminho.exists():
        print(f"ERRO: artefato nao encontrado: {caminho}", file=sys.stderr)
        return 2

    texto = caminho.read_text(encoding="utf-8")
    erros: list[str] = []

    # Contexto resolvido: | Item | Fonte | Trecho | Confianca |
    for celulas in linhas_da_tabela(texto, "Contexto resolvido"):
        item = celulas[0]
        fonte = celulas[1] if len(celulas) > 1 else ""
        conf = (celulas[3] if len(celulas) > 3 else "").lower()
        if fonte.lower() in VAZIOS:
            erros.append(f"ERRO: item '{item}' sem fonte — deve ir para Lacunas.")
        if conf.startswith("baix"):
            erros.append(
                f"ERRO: item '{item}' com confianca baixa listado como resolvido "
                f"— confianca baixa e lacuna."
            )

    # Suposicoes: | # | Suposicao | Se errada | Dono | Confirmar ate |
    for celulas in linhas_da_tabela(texto, "Suposições operacionais"):
        sup = celulas[1] if len(celulas) > 1 else celulas[0]
        dono = celulas[3] if len(celulas) > 3 else ""
        prazo = celulas[4] if len(celulas) > 4 else ""
        if dono.lower() in VAZIOS or prazo.lower() in VAZIOS:
            erros.append(
                f"ERRO: suposicao '{sup[:50]}' sem dono ou sem prazo — "
                f"vira lacuna bloqueante."
            )

    # Contador de bloqueantes
    m = re.search(r"Lacunas bloqueantes em aberto:\*{0,2}\s*(\d+)", texto)
    if not m:
        erros.append("ERRO: contador 'Lacunas bloqueantes em aberto' ausente.")
    else:
        declarado = int(m.group(1))
        abertas = 0
        for celulas in linhas_da_tabela(texto, "Lacunas"):
            status = (celulas[-1] if celulas else "").lower()
            bloqueia = (celulas[3] if len(celulas) > 3 else "").lower()
            if bloqueia not in VAZIOS and not status.startswith(("resolv", "fechad")):
                abertas += 1
        if declarado != abertas:
            erros.append(
                f"ERRO: contador de lacunas bloqueantes ({declarado}) nao bate "
                f"com a tabela ({abertas})."
            )

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
