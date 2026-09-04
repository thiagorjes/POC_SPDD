"""
Verifica o plano de verificacao (GATE-VERIFICACAO-INDEPENDENTE).

Confere que:
  - a declaracao de independencia esta preenchida e coerente;
  - todo cenario listado tem arquivo de teste e situacao 'coberto';
  - a execucao inicial e Red — nenhuma linha com zero falhando, salvo audit;
  - dependencia simulada tem motivo;
  - alteracao registrada na tabela de congelamento tem motivo e aprovador.

Modo audit e reconhecido pela declaracao de independencia e afrouxa apenas a
exigencia de Red — nao a de cobertura.

Uso: python check_independencia.py --artifact docs/tests/x-verificacao.md
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

NEGATIVO = {"nao", "não", "nenhum", "nenhuma"}


def negativo(valor: str) -> bool:
    return valor.strip().lower().split(" ")[0].strip(".") in NEGATIVO


def respondido(valor: str) -> bool:
    """Aqui 'nenhum' e a resposta desejada, e nao ausencia de resposta."""
    v = valor.strip()
    return bool(v) and not v.startswith("{{") and v.lower() not in {"-", "—", "?"}


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

    # Declaracao de independencia
    dec = secao(texto, "Declaração de independência")
    existente = campo(dec, "Implementação existente no momento da escrita")
    audit = preenchido(existente) and not negativo(existente)

    if not preenchido(existente):
        erros.append("ERRO: declaracao de independencia sem resposta sobre "
                     "implementacao existente.")
    elif audit and len(existente.split()) < 4:
        erros.append(
            "ERRO: implementacao ja existente exige justificativa na declaracao "
            "de independencia — modo audit nao se declara com uma palavra."
        )

    lidos = campo(dec, "Arquivos de produção lidos")
    if not respondido(lidos):
        erros.append("ERRO: declaracao sem 'Arquivos de produção lidos'.")
    elif not negativo(lidos) and not audit:
        erros.append(
            f"ERRO: suite escreveu apos ler producao ('{lidos}') — reprova o "
            f"GATE-VERIFICACAO-INDEPENDENTE."
        )

    alterados = campo(dec, "Arquivos de produção alterados nesse commit")
    if not respondido(alterados):
        erros.append("ERRO: declaracao sem os arquivos de producao alterados no "
                     "commit da suite.")
    elif not negativo(alterados):
        erros.append(
            f"ERRO: o commit da suite altera producao ('{alterados}') — a suite "
            f"nao pode carregar implementacao."
        )

    if not preenchido(campo(dec, "Commit da suíte")):
        erros.append("ERRO: declaracao sem SHA do commit da suite.")

    # Cenarios congelados
    cen = secao(texto, "Cenários congelados")
    mudados = campo(cen, "Alterados desde o gate de spec")
    if not respondido(mudados):
        erros.append("ERRO: nao consta se algum cenario foi alterado desde o "
                     "gate de spec.")
    elif not negativo(mudados):
        erros.append(
            f"ERRO: cenario alterado apos o gate de spec ('{mudados}') sem "
            f"emenda — reprova o GATE-GHERKIN-CONGELADO."
        )

    linhas = [l for l in tabela_da_secao(texto, "Cenários congelados")
              if preenchido(celula(l, 0))]
    if not linhas:
        erros.append("ERRO: nenhum cenario listado — nada verificado.")
    for l in linhas:
        sid = celula(l, 0)
        if not preenchido(celula(l, 3)):
            erros.append(f"ERRO: cenario {sid} sem tipo de teste.")
        if not preenchido(celula(l, 4)):
            erros.append(f"ERRO: cenario {sid} sem arquivo de teste.")
        situacao = celula(l, 5).lower()
        if not situacao.startswith("cobert"):
            erros.append(
                f"ERRO: cenario {sid} com situacao '{situacao}' — cenario "
                f"congelado sem teste reprova o gate."
            )

    # Estrategia
    est = secao(texto, "Estratégia")
    for rotulo in ("Framework", "Runner", "Cobertura mínima exigida"):
        if not preenchido(campo(est, rotulo)):
            erros.append(f"ERRO: estrategia sem '{rotulo}'.")
    for l in tabela_da_secao(texto, "Dependências simuladas", nivel=3):
        if preenchido(celula(l, 0)) and not preenchido(celula(l, 1)):
            erros.append(
                f"ERRO: dependencia simulada '{celula(l, 0)}' sem motivo — mock "
                f"sem motivo vira teste que verifica o mock."
            )

    # Execucao inicial
    exec_linhas = [l for l in tabela_da_secao(texto, "Execução inicial (Red)")
                   if preenchido(celula(l, 0))]
    if not exec_linhas:
        erros.append("ERRO: execucao inicial nao registrada.")
    for l in exec_linhas:
        falhando = celula(l, 2)
        if not preenchido(falhando):
            erros.append(f"ERRO: {celula(l, 0)} sem contagem de falhas.")
        elif not audit and re.fullmatch(r"0+", falhando.strip()):
            erros.append(
                f"ERRO: {celula(l, 0)} com 0 testes falhando antes da "
                f"implementacao — teste que ja passa verifica outra coisa."
            )
        if not preenchido(celula(l, 3)):
            erros.append(f"ERRO: {celula(l, 0)} sem motivo da falha.")

    # Congelamento
    for l in tabela_da_secao(texto, "Congelamento da suíte"):
        if not preenchido(celula(l, 1)):
            continue
        if not preenchido(celula(l, 3)) or not preenchido(celula(l, 4)):
            erros.append(
                f"ERRO: alteracao em '{celula(l, 1)}' apos o congelamento sem "
                f"motivo ou sem aprovador humano."
            )

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
