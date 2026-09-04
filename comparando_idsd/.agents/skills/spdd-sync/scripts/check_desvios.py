"""
Verificacao do registro de desvios — skill /spdd-sync.

O registro so tem valor se a decisao for atribuivel e justificada. Este script
reprova o que transforma o arquivo em formalidade:

  1. desvio sem os dois lados escritos (o que a spec diz / o que o codigo faz)
  2. direcao invalida ou ainda com as opcoes do template na celula
  3. 'spec corrigida' cuja justificativa apenas descreve o codigo
  4. 'aceito com prazo' sem data limite e responsavel
  5. sumario que nao bate com os blocos DEV

Uso:
    python check_desvios.py --artifact docs/spdd/<feature>-deviations.md
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "scripts"))
from md_tables import campo, celula, corpo_util, preenchido, secao, tabela  # noqa: E402

DIRECOES = {"spec corrigida", "código corrigido", "codigo corrigido", "aceito com prazo"}
STATUS = {"resolvido", "pendente"}

# Justificativa que so narra a implementacao nao explica por que a spec estava
# errada — e exatamente a racionalizacao que esta skill existe para barrar.
SO_DESCREVE_CODIGO = re.compile(
    r"^(porque\s+)?(o\s+)?c[óo]digo\s+(j[áa]\s+)?(faz|ficou|foi|est[áa])\b",
    re.IGNORECASE,
)


def opcao(valor: str) -> str:
    """Celula ainda com as alternativas do template nao e resposta."""
    v = valor.strip()
    return "" if "|" in v or v.startswith("{{") else v.lower()


def verificar(texto: str) -> list[str]:
    util = corpo_util(texto)
    erros: list[str] = []

    ids_sumario = {
        celula(l, 0) for l in tabela(secao(util, "Sumário")) if celula(l, 0)
    }
    blocos = re.findall(r"^##\s+(DEV-\d{2})\s*[—-]\s*(.+)$", util, re.MULTILINE)
    ids_bloco = {d for d, _ in blocos}

    if not blocos:
        erros.append("nenhum desvio registrado — se nao ha desvio, o arquivo nao existe.")

    for so_no_sumario in sorted(ids_sumario - ids_bloco):
        erros.append(f"{so_no_sumario}: no sumario sem bloco correspondente.")
    for so_no_bloco in sorted(ids_bloco - ids_sumario):
        erros.append(f"{so_no_bloco}: bloco sem linha no sumario.")

    for dev, titulo in blocos:
        bloco = secao(util, f"{dev} — {titulo}")
        obrig = [
            "Artefato de spec", "Local no código", "O que a spec diz",
            "O que o código faz", "Quem decidiu", "Por quê",
        ]
        for rotulo in obrig:
            if not preenchido(campo(bloco, rotulo)):
                erros.append(f"{dev}: campo '{rotulo}' vazio.")

        direcao = opcao(campo(bloco, "Direção"))
        if direcao not in DIRECOES:
            erros.append(f"{dev}: direcao invalida ou indecisa: '{direcao}'.")

        status = opcao(campo(bloco, "Status"))
        if status not in STATUS:
            erros.append(f"{dev}: status invalido ou indeciso: '{status}'.")

        porque = campo(bloco, "Por quê")
        if direcao == "spec corrigida" and SO_DESCREVE_CODIGO.match(porque.strip()):
            erros.append(
                f"{dev}: justificativa descreve o codigo em vez de explicar por "
                f"que a spec estava errada."
            )

        if direcao == "aceito com prazo":
            prazo = campo(bloco, "Prazo e dono")
            if not preenchido(prazo):
                erros.append(f"{dev}: aceite sem prazo e responsavel nomeado.")

    if "{{" in util:
        erros.append("placeholder do template nao substituido.")

    return erros


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--artifact", required=True)
    args = p.parse_args()

    caminho = Path(args.artifact)
    if not caminho.exists():
        print(f"ERRO: artefato nao encontrado: {caminho}", file=sys.stderr)
        return 1

    erros = verificar(caminho.read_text(encoding="utf-8"))
    if erros:
        for e in erros:
            print(f"FALHA {e}", file=sys.stderr)
        print(f"\n{len(erros)} falha(s).", file=sys.stderr)
        return 1
    print("OK - registro de desvios conforme.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
