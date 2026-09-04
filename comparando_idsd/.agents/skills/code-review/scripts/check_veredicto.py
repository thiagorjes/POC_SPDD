"""
Verifica o relatorio de revisao (GATE-REVISAO-TECNICA e GATE-NFR).

Confere que:
  - a integridade da verificacao foi checada (cenario e step defs intactos);
  - todo cenario do escopo tem resultado e conformidade declarados;
  - todo RNF tem medicao — 'nao medido' e achado, nao ausencia;
  - todo achado tem severidade, local e destino validos;
  - achado de seguranca rebaixado tem justificativa e aprovador;
  - o veredicto e coerente com os achados: bloqueante em aberto reprova;
  - ha revisor humano nomeado.

Uso: python check_veredicto.py --artifact docs/review/x/EPIC-01-review.md
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

SEVERIDADES = {"bloqueante", "relevante", "menor"}
DESTINOS = {"/implement", "/prd", "/techspec", "/tasks", "/tests"}
NEGATIVO = {"nao", "não", "nenhum", "nenhuma"}


def negativo(valor: str) -> bool:
    return valor.strip().lower().split(" ")[0].strip(".") in NEGATIVO


def respondido(valor: str) -> bool:
    v = valor.strip()
    return bool(v) and not v.startswith("{{") and v not in {"-", "—", "?"}


def opcao(valor: str) -> str:
    """Valor de uma celula de escolha.

    Celula que ainda traz as alternativas separadas por barra ('dentro \\| fora')
    e devolvida como indecidida: sem isso, 'dentro \\| fora \\| nao medido'
    passaria como 'dentro'.
    """
    v = valor.strip().lower()
    return "" if "|" in v else v


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

    # Integridade da verificacao
    escopo = secao(texto, "Escopo revisado")
    for rotulo in ("Cenários congelados alterados no período",
                   "Step definitions alterados no período"):
        valor = campo(escopo, rotulo)
        if not respondido(valor):
            erros.append(f"ERRO: escopo revisado sem '{rotulo}'.")
        elif not negativo(valor):
            erros.append(
                f"ERRO: {rotulo.lower()}: '{valor}' — sem emenda registrada, a "
                f"suite nao e mais a acordada e nao ha o que revisar."
            )

    # Conformidade
    conf = [l for l in tabela_da_secao(texto, "Conformidade com a especificação")
            if preenchido(celula(l, 0))]
    if not conf:
        erros.append("ERRO: nenhum cenario avaliado.")
    for l in conf:
        sid = celula(l, 0)
        passa = opcao(celula(l, 1))
        conforme = opcao(celula(l, 2))
        if passa not in {"sim", "não", "nao"}:
            erros.append(f"ERRO: cenario {sid} sem resultado decidido.")
        elif passa.startswith("n"):
            erros.append(f"ERRO: cenario {sid} nao passa — o epico nao fecha.")
        if conforme not in {"sim", "não", "nao", "parcial"}:
            erros.append(
                f"ERRO: cenario {sid} sem avaliacao de conformidade com a task "
                f"— passar e ter sido implementado como especificado sao "
                f"perguntas distintas."
            )

    excesso = campo(texto, "Escopo além do especificado")
    if not respondido(excesso):
        erros.append("ERRO: nao consta se ha escopo alem do especificado.")

    # RNF
    for l in tabela_da_secao(texto, "Envelopes de RNF"):
        rid = celula(l, 0)
        if not preenchido(rid):
            continue
        situacao = opcao(celula(l, 4))
        if not preenchido(celula(l, 1)):
            erros.append(f"ERRO: {rid} sem envelope.")
        if situacao.startswith("não medido") or situacao.startswith("nao medido"):
            erros.append(
                f"ERRO: {rid} nao medido — ausencia de medicao reprova o "
                f"GATE-NFR; nao passa por omissao."
            )
        elif situacao.startswith("fora"):
            erros.append(f"ERRO: {rid} fora do envelope.")
        elif not situacao.startswith("dentro"):
            erros.append(f"ERRO: {rid} sem situacao decidida.")
        if situacao.startswith("dentro") and not preenchido(celula(l, 3)):
            erros.append(f"ERRO: {rid} medido sem instrumento declarado.")

    # Achados
    bloqueantes: list[str] = []
    for l in tabela_da_secao(texto, "Achados"):
        aid = celula(l, 0)
        if not preenchido(aid):
            continue
        sev = opcao(celula(l, 1))
        if sev not in SEVERIDADES:
            erros.append(f"ERRO: {aid} com severidade indefinida: '{sev}'.")
        elif sev == "bloqueante":
            bloqueantes.append(aid)
        if not preenchido(celula(l, 3)):
            erros.append(f"ERRO: {aid} sem local.")
        if not preenchido(celula(l, 4)):
            erros.append(f"ERRO: {aid} sem descricao.")
        destino = celula(l, 5).strip()
        if destino not in DESTINOS:
            erros.append(
                f"ERRO: {aid} sem destino valido ('{destino}') — todo achado "
                f"volta para a etapa que o corrige."
            )
        if opcao(celula(l, 2)) == "segurança" and sev in {"relevante", "menor"} \
                and "aprovad" not in celula(l, 4).lower():
            erros.append(
                f"ERRO: {aid} e achado de seguranca rebaixado sem justificativa "
                f"e aprovador nomeados."
            )

    # Seguranca
    seg = [l for l in tabela_da_secao(texto, "Análise de segurança")
           if preenchido(celula(l, 0))]
    if len(seg) < 5:
        erros.append(
            "ERRO: analise de seguranca incompleta — as cinco verificacoes sao "
            "obrigatorias, independentemente de a feature parecer sensivel."
        )
    for l in seg:
        if not respondido(celula(l, 1)):
            erros.append(f"ERRO: verificacao de seguranca '{celula(l, 0)}' sem "
                         f"resultado.")

    # Veredicto
    ver = secao(texto, "Veredicto")
    for gate in ("GATE-REVISAO-TECNICA", "GATE-NFR"):
        valor = opcao(campo(ver, gate))
        if valor not in {"aprovado", "reprovado"}:
            erros.append(f"ERRO: {gate} sem veredicto decidido.")
        elif valor == "aprovado" and bloqueantes:
            erros.append(
                f"ERRO: {gate} aprovado com bloqueante em aberto "
                f"({', '.join(bloqueantes)}) — nao ha waiver para isso."
            )
    if not preenchido(campo(ver, "Revisor humano")):
        erros.append("ERRO: veredicto sem revisor humano nomeado.")

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
