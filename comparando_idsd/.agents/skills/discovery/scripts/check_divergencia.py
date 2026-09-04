"""
Verifica que o discovery realmente divergiu.

O modo de falha desta etapa e apresentar uma unica leitura do problema, o que
transforma o /shape em carimbo. Confere que:
  - ha ao menos dois enquadramentos alternativos, cada um dizendo o que ficaria
    sem resposta;
  - toda dor tem evidencia (ou esta marcada como suposicao a validar);
  - ha ao menos uma persona.

Uso: python check_divergencia.py --artifact docs/discovery/x-discovery.md
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "scripts"))
from md_tables import celula, corpo_util, preenchido, tabela_da_secao  # noqa: E402


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--artifact", required=True)
    args = p.parse_args()

    caminho = Path(args.artifact)
    if not caminho.exists():
        print(f"ERRO: artefato nao encontrado: {caminho}", file=sys.stderr)
        return 2

    texto = corpo_util(caminho.read_text(encoding="utf-8"))
    erros: list[str] = []

    enquadramentos = tabela_da_secao(texto, "Enquadramentos alternativos do problema")
    validos = [e for e in enquadramentos if preenchido(celula(e, 1))]
    if len(validos) < 2:
        erros.append(
            f"ERRO: divergencia exige ao menos 2 enquadramentos alternativos "
            f"(encontrados {len(validos)}) — com um so, o /shape nao tem o que "
            f"convergir."
        )
    for e in validos:
        if not preenchido(celula(e, 3)):
            erros.append(
                f"ERRO: enquadramento '{celula(e, 1)[:40]}' nao diz o que ficaria "
                f"sem resposta."
            )

    dores = tabela_da_secao(texto, "Dores observadas")
    if not dores:
        erros.append("ERRO: nenhuma dor observada registrada.")
    for d in dores:
        if not preenchido(celula(d, 1)):
            continue
        evidencia = celula(d, 4)
        if not preenchido(evidencia):
            erros.append(
                f"ERRO: dor '{celula(d, 1)[:40]}' sem evidencia — registre a fonte "
                f"ou marque como suposicao a validar."
            )
        elif "sabido" in evidencia.lower() or "todos sabem" in evidencia.lower():
            erros.append(
                f"ERRO: dor '{celula(d, 1)[:40]}' com evidencia nao verificavel: "
                f"'{evidencia[:40]}'."
            )

    if not tabela_da_secao(texto, "Personas"):
        erros.append("ERRO: nenhuma persona registrada.")

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
