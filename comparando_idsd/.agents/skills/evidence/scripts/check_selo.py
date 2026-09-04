"""
Verifica as condicoes de selo do pacote de evidencias (IDSD 4.10).

Confere que:
  - a secao 'Registros ausentes' esta vazia;
  - nenhum gate bloqueante consta como reprovado;
  - a ordem de verificacao esta correta em todos os cenarios (4.9.1);
  - a varredura de dados sensiveis acusou zero ocorrencias (4.10.1);
  - ha hash de pacote quando o selo e reivindicado.

Uso: python check_selo.py --artifact docs/evidence/x/evidence.md
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

VAZIOS = {"", "-", "—", "tbd", "a definir", "n/a", "nenhum"}


def secao(texto: str, titulo: str) -> str:
    m = re.search(rf"^##\s+{re.escape(titulo)}\s*$(.*?)(?=^##\s|\Z)", texto,
                  re.MULTILINE | re.DOTALL)
    return m.group(1) if m else ""


def linhas(bloco: str) -> list[list[str]]:
    fora, cabecalho = [], False
    for linha in bloco.splitlines():
        linha = linha.strip()
        if not linha.startswith("|"):
            continue
        celulas = [c.strip() for c in linha.strip("|").split("|")]
        if all(set(c) <= set("- :") for c in celulas if c):
            continue
        if not cabecalho:
            cabecalho = True
            continue
        if any(celulas):
            fora.append(celulas)
    return fora


def campo(texto: str, rotulo: str) -> str:
    m = re.search(rf"[-*]\s*\*\*{re.escape(rotulo)}:?\*\*\s*(.*)", texto)
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
    erros: list[str] = []

    ausentes = linhas(secao(texto, "Registros ausentes"))
    for linha in ausentes:
        erros.append(
            f"ERRO: registro ausente impede o selo: '{linha[0][:60]}' "
            f"(deveria ter sido escrito por: {linha[1] if len(linha) > 1 else '?'})."
        )

    for linha in linhas(secao(texto, "Conformidade de gates")):
        resultado = (linha[-1] if linha else "").lower()
        if resultado.startswith(("reprov", "falh", "nao")) or "❌" in resultado:
            erros.append(f"ERRO: gate reprovado impede o selo: {linha[0]}")

    for linha in linhas(secao(texto, "Ordem de verificação")):
        ok = (linha[-1] if linha else "").lower()
        if ok.startswith(("nao", "não")) or "❌" in ok:
            erros.append(
                f"ERRO: cenario '{linha[0]}' com teste escrito depois da "
                f"implementacao — violacao da IDSD 4.9.1."
            )

    ocorrencias = campo(texto, "Ocorrências de dado real de cliente")
    if not ocorrencias:
        erros.append("ERRO: varredura de dados sensiveis nao executada.")
    else:
        m = re.match(r"(\d+)", ocorrencias)
        if not m:
            erros.append(
                f"ERRO: contador de dados sensiveis ilegivel: '{ocorrencias}'."
            )
        elif int(m.group(1)) > 0:
            erros.append(
                f"ERRO: {m.group(1)} ocorrencia(s) de dado real de cliente — "
                f"GATE-DADOS reprovado e nao admite waiver (IDSD 4.10.1)."
            )

    hash_pacote = campo(texto, "Hash do pacote")
    if hash_pacote.lower() in VAZIOS or hash_pacote.startswith("{{"):
        erros.append("ERRO: pacote sem hash — selo nao pode ser reivindicado.")

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
