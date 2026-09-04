"""
Verifica a disciplina de procedencia do PRD (GATE-PROVENIENCIA).

Confere que:
  - toda regra de negocio tem procedencia de um tipo valido;
  - todo RF declara procedencia;
  - todo RNF tem envelope e condicao de medicao;
  - hipoteses tem experimento, criterio de descarte e prazo;
  - inferencias do agente estao confirmadas por humano;
  - nao ha duvida material em aberto.

As tres ultimas bloqueiam o gate de spec.

Uso: python check_proveniencia.py --artifact docs/prd/x-prd.md
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "scripts"))
from md_tables import (  # noqa: E402
    campo, celula, corpo_util, preenchido, secao, tabela_da_secao,
)

TIPOS = {
    "informada",
    "derivada",
    "extraída de legado",
    "extraida de legado",
    "hipótese a validar",
    "hipotese a validar",
    "inferida pelo agente",
}


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--artifact", required=True)
    args = p.parse_args()

    caminho = Path(args.artifact)
    if not caminho.exists():
        print(f"ERRO: artefato nao encontrado: {caminho}", file=sys.stderr)
        return 2

    bruto = caminho.read_text(encoding="utf-8")
    texto = corpo_util(bruto)
    erros: list[str] = []

    # Regras de negocio
    regras = tabela_da_secao(texto, "Regras de negócio")
    if not regras:
        erros.append("ERRO: nenhuma regra de negocio registrada.")
    for r in regras:
        rid = celula(r, 0)
        if not preenchido(rid):
            continue
        proc = celula(r, 2).lower()
        if not preenchido(proc):
            erros.append(f"ERRO: {rid} sem procedencia.")
        elif proc not in TIPOS:
            erros.append(
                f"ERRO: {rid} com procedencia invalida: '{proc}'. "
                f"Validos: informada, derivada, extraida de legado, "
                f"hipotese a validar, inferida pelo agente."
            )
        if not preenchido(celula(r, 3)):
            erros.append(f"ERRO: {rid} sem fonte.")

    # RFs: procedencia declarada em bloco, nao em tabela
    for m in re.finditer(r"^###\s+(RF-\d{3})\b(.*?)(?=^###\s|^##\s|\Z)",
                         texto, re.MULTILINE | re.DOTALL):
        rid, bloco = m.group(1), m.group(2)
        proc = campo(bloco, "Procedência").lower()
        if not preenchido(proc):
            erros.append(f"ERRO: {rid} sem procedencia.")
        elif proc not in TIPOS:
            erros.append(f"ERRO: {rid} com procedencia invalida: '{proc}'.")
        if not preenchido(campo(bloco, "Fonte")):
            erros.append(f"ERRO: {rid} sem fonte.")

    # RNFs: envelope e condicao de medicao
    for r in tabela_da_secao(texto, "Requisitos não-funcionais"):
        rid = celula(r, 0)
        if not preenchido(rid):
            continue
        if not preenchido(celula(r, 2)):
            erros.append(
                f"ERRO: {rid} sem envelope — RNF sem limite verificavel reprova "
                f"o GATE-NFR mais adiante."
            )
        if not preenchido(celula(r, 3)):
            erros.append(f"ERRO: {rid} sem condicao de medicao.")
        proc = celula(r, 4).lower()
        if not preenchido(proc):
            erros.append(f"ERRO: {rid} sem procedencia.")

    # Hipoteses
    for h in tabela_da_secao(texto, "Hipóteses a validar", nivel=3):
        hid = celula(h, 0)
        if not preenchido(hid):
            continue
        for i, nome in ((1, "experimento"), (2, "criterio de descarte"), (3, "prazo")):
            if not preenchido(celula(h, i)):
                erros.append(f"ERRO: hipotese {hid} sem {nome}.")

    # Inferencias pendentes
    for linha in tabela_da_secao(
        texto, "Inferências do agente pendentes de confirmação", nivel=3
    ):
        iid = celula(linha, 0)
        if preenchido(iid) and not preenchido(celula(linha, 3)):
            erros.append(
                f"ERRO: inferencia do agente em {iid} nao confirmada por humano "
                f"— bloqueia o GATE-SPEC."
            )

    # Duvidas materiais
    for d in tabela_da_secao(texto, "Dúvidas materiais em aberto"):
        status = celula(d, 4).lower()
        if preenchido(celula(d, 1)) and not status.startswith(("resolv", "fechad")):
            erros.append(
                f"ERRO: duvida material em aberto bloqueia o GATE-SPEC: "
                f"'{celula(d, 1)[:50]}'."
            )

    # Aprovacao
    aprovacao = secao(bruto, "Aprovação — gate de spec")
    if not preenchido(campo(aprovacao, "Aprovado por")):
        erros.append("ERRO: GATE-SPEC sem aprovador humano.")
    if not preenchido(campo(aprovacao, "Data")):
        erros.append("ERRO: GATE-SPEC sem data de aprovacao.")

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
