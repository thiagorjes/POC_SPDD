"""
Verifica o relatorio de analise cruzada (GATE-CONSISTENCIA).

Confere que:
  - todos os artefatos da cadeia foram considerados ou tem ausencia justificada;
  - nenhum artefato esta desatualizado em relacao a montante;
  - todos os elos da cobertura tem contagem e situacao decididas;
  - elo com orfaos declarados nao esta marcado como ok;
  - todo achado tem tipo, severidade, local e dono validos;
  - as contagens de procedencia e hipoteses sao zero;
  - o veredicto e coerente com os achados.

Uso: python check_consistencia.py --artifact docs/analyze/x-analysis.md
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

TIPOS = {"contradição", "contradicao", "lacuna", "ambiguidade", "duplicação",
         "duplicacao", "escopo órfão", "escopo orfao"}
SEVERIDADES = {"bloqueante", "relevante", "menor"}
DONOS = {"/prd", "/techspec", "/solution", "/shape", "/design", "/intent",
         "/context", "/discovery"}
NEGATIVO = {"nao", "não", "nenhum", "nenhuma"}


def negativo(valor: str) -> bool:
    return valor.strip().lower().split(" ")[0].strip(".") in NEGATIVO


def respondido(valor: str) -> bool:
    v = valor.strip()
    return bool(v) and not v.startswith("{{") and v not in {"-", "—", "?"}


def opcao(valor: str) -> str:
    v = valor.strip().lower()
    return "" if "|" in v else v


def contagem(valor: str) -> int | None:
    v = valor.strip()
    return int(v) if re.fullmatch(r"\d+", v) else None


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

    # Artefatos
    linhas = [l for l in tabela_da_secao(texto, "Artefatos e versões")
              if preenchido(celula(l, 0))]
    if not linhas:
        erros.append("ERRO: nenhum artefato listado — nada foi confrontado.")
    for l in linhas:
        nome = celula(l, 0)
        considerado = opcao(celula(l, 3))
        if not considerado:
            erros.append(f"ERRO: artefato {nome} sem situacao decidida.")
        elif considerado == "ausente":
            erros.append(
                f"ERRO: artefato {nome} ausente — a cadeia nao pode ser "
                f"verificada com um elo faltando."
            )
        elif considerado == "sim" and not preenchido(celula(l, 2)):
            erros.append(f"ERRO: artefato {nome} sem referencia de versao.")

    desatualizado = campo(texto, "Artefato desatualizado em relação a montante")
    if not respondido(desatualizado):
        erros.append("ERRO: nao consta se ha artefato desatualizado.")
    elif not negativo(desatualizado):
        erros.append(
            f"ERRO: artefato desatualizado em relacao a montante "
            f"('{desatualizado}') — alguem mudou a spec e nao propagou."
        )

    # Cobertura
    elos = [l for l in tabela_da_secao(texto, "Cobertura da cadeia")
            if preenchido(celula(l, 0))]
    if len(elos) < 5:
        erros.append("ERRO: cobertura da cadeia incompleta — os cinco elos sao "
                     "obrigatorios.")
    for l in elos:
        elo = celula(l, 0)
        situacao = opcao(celula(l, 3))
        montante, jusante = contagem(celula(l, 1)), contagem(celula(l, 2))
        if situacao == "n/a":
            continue
        if montante is None or jusante is None:
            erros.append(f"ERRO: elo '{elo}' sem contagem de orfaos.")
            continue
        if situacao not in {"ok", "achado"}:
            erros.append(f"ERRO: elo '{elo}' sem situacao decidida.")
        elif situacao == "ok" and (montante or jusante):
            erros.append(
                f"ERRO: elo '{elo}' marcado como ok com {montante + jusante} "
                f"orfao(s) declarado(s)."
            )

    # Achados
    bloqueantes: list[str] = []
    for l in tabela_da_secao(texto, "Achados"):
        aid = celula(l, 0)
        if not preenchido(aid):
            continue
        tipo, sev = opcao(celula(l, 1)), opcao(celula(l, 2))
        if tipo not in TIPOS:
            erros.append(f"ERRO: {aid} com tipo invalido: '{tipo}'.")
        if sev not in SEVERIDADES:
            erros.append(f"ERRO: {aid} sem severidade decidida.")
        elif sev == "bloqueante":
            bloqueantes.append(aid)
        if not preenchido(celula(l, 3)):
            erros.append(f"ERRO: {aid} sem localizacao.")
        if not preenchido(celula(l, 4)):
            erros.append(f"ERRO: {aid} sem descricao.")
        dono = celula(l, 5).strip()
        if dono not in DONOS:
            erros.append(
                f"ERRO: {aid} sem dono valido ('{dono}') — a analise localiza, "
                f"o artefato dono corrige."
            )

    # Procedencia
    verificacoes = [l for l in tabela_da_secao(texto, "Procedência e hipóteses")
                    if preenchido(celula(l, 0))]
    if len(verificacoes) < 3:
        erros.append("ERRO: verificacoes de procedencia incompletas.")
    for l in verificacoes:
        n = contagem(celula(l, 1))
        if n is None:
            erros.append(f"ERRO: '{celula(l, 0)}' sem contagem.")
        elif n > 0:
            erros.append(
                f"ERRO: {celula(l, 0).lower()}: {n} — a spec esta apoiada em "
                f"algo que ninguem confirmou; bloqueia o GATE-CONSISTENCIA."
            )

    # Veredicto
    ver = secao(texto, "Veredicto")
    veredicto = opcao(campo(ver, "GATE-CONSISTENCIA"))
    if veredicto not in {"aprovado", "reprovado"}:
        erros.append("ERRO: GATE-CONSISTENCIA sem veredicto decidido.")
    elif veredicto == "aprovado" and bloqueantes:
        erros.append(
            f"ERRO: GATE-CONSISTENCIA aprovado com bloqueante em aberto "
            f"({', '.join(bloqueantes)}) — resolva no artefato dono e reexecute."
        )
    if not preenchido(campo(ver, "Aprovado por")):
        erros.append("ERRO: veredicto sem aprovador nomeado.")

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
