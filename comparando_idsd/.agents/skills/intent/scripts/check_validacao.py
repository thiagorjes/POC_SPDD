"""
Verifica que a Intent foi validada pelo demandante antes do gate.

O GATE-INTENT nao depende de o documento estar bonito — depende de existir
confirmacao explicita de quem demandou. Campo em branco reprova.

Uso: python check_validacao.py --artifact docs/intent/x-intent.md
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

PLACEHOLDERS = {"", "{{data}}", "{{nome}}", "-", "—", "a definir", "tbd"}


def valor_do_campo(texto: str, rotulo: str) -> str | None:
    m = re.search(rf"^\s*[-*]\s*\*\*{re.escape(rotulo)}:?\*\*\s*(.*)$", texto,
                  re.IGNORECASE | re.MULTILINE)
    if not m:
        return None
    return m.group(1).split("<!--")[0].strip()


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

    for rotulo in ("Demandante", "Validado pelo demandante em"):
        valor = valor_do_campo(texto, rotulo)
        if valor is None:
            erros.append(f"ERRO: campo obrigatorio ausente: '{rotulo}'")
        elif valor.lower() in PLACEHOLDERS:
            erros.append(f"ERRO: campo '{rotulo}' nao preenchido (GATE-INTENT).")

    interface = valor_do_campo(texto, "Tem interface visual")
    if interface and interface.lower() not in {"sim", "nao", "não", "a definir"}:
        erros.append(
            "ERRO: 'Tem interface visual' deve ser sim | nao | a definir "
            f"(encontrado: '{interface}')."
        )

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
