"""
Verificação de registro de flow — IDSD.

Um flow só é registrável se seu manifesto cobre todos os gates obrigatórios
das classes que declara atender. Esta verificação acontece no registro, não
em runtime: um flow mal formado nunca chega a executar.

Uso:
    python validate_flow.py                       # valida todos os flows
    python validate_flow.py --flow feature        # valida um flow
    python validate_flow.py --governance ../..    # raiz alternativa

Saída: 0 se todos os flows conformes; 1 caso contrário.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import yaml


def carregar(caminho: Path) -> dict:
    if not caminho.exists():
        raise SystemExit(f"ERRO: arquivo nao encontrado: {caminho}")
    return yaml.safe_load(caminho.read_text(encoding="utf-8")) or {}


def gates_conhecidos(catalogo: dict) -> dict[str, dict]:
    return {g["id"]: g for g in catalogo.get("gates", [])}


def obrigatorios_da_classe(policy: dict, classe: str) -> tuple[set[str], set[str]]:
    """Retorna (gates exigidos agora, gates exigidos posteriormente)."""
    nucleo = set(policy.get("nucleo_inegociavel", []))
    entrada = policy.get("classes", {}).get(classe)
    if entrada is None:
        raise SystemExit(f"ERRO: classe '{classe}' nao existe na policy.")
    agora = nucleo | set(entrada.get("gates", []))
    posteriores = set((entrada.get("posteriores") or {}).get("gates", []))
    return agora, posteriores


def validar_flow(flow: dict, catalogo: dict, policy: dict) -> list[str]:
    erros: list[str] = []
    fid = flow.get("id", "?")
    conhecidos = gates_conhecidos(catalogo)

    cobertos_agora: set[str] = set()
    cobertos_depois: set[str] = set()

    for etapa in flow.get("etapas", []):
        satisfaz = etapa.get("satisfaz") or []
        alvo = cobertos_depois if etapa.get("posterior") else cobertos_agora
        for gid in satisfaz:
            if gid not in conhecidos:
                erros.append(
                    f"[{fid}] etapa {etapa.get('skill')} declara gate inexistente: {gid}"
                )
            alvo.add(gid)

    # Flow de setup nao nasce de um intent: prepara o sistema para que os
    # demais possam rodar. Nao atende classe e, por isso, nao cobre gate de
    # classe — mas continua sujeito as restricoes estruturais abaixo.
    tipo = flow.get("tipo", "execucao")
    if tipo not in {"execucao", "setup"}:
        erros.append(f"[{fid}] tipo de flow invalido: '{tipo}'.")

    atende = flow.get("atende") or []
    if tipo == "setup":
        if atende:
            erros.append(
                f"[{fid}] flow de setup nao pode declarar 'atende' — ele nao "
                f"nasce de um intent classificado."
            )
    elif not atende:
        erros.append(f"[{fid}] manifesto nao declara 'atende'.")

    for classe in atende:
        exigidos, posteriores = obrigatorios_da_classe(policy, classe)
        faltando = exigidos - cobertos_agora - cobertos_depois
        for gid in sorted(faltando):
            erros.append(f"[{fid}] classe '{classe}': gate obrigatorio nao coberto: {gid}")

        # Gate exigido agora, mas coberto apenas por etapa posterior, só é
        # aceitável se a policy da classe o listar explicitamente em posteriores.
        adiados = (exigidos & cobertos_depois) - cobertos_agora
        for gid in sorted(adiados - posteriores):
            erros.append(
                f"[{fid}] classe '{classe}': gate {gid} adiado sem previsao na policy."
            )

    # Restrições estruturais que nenhum flow pode violar.
    for etapa in flow.get("etapas", []):
        if etapa.get("skill") == "/tests" and etapa.get("restricao") is None:
            if "GATE-VERIFICACAO-INDEPENDENTE" in (etapa.get("satisfaz") or []):
                erros.append(
                    f"[{fid}] /tests satisfaz GATE-VERIFICACAO-INDEPENDENTE sem "
                    f"declarar restricao de isolamento."
                )
        if etapa.get("skill") == "/code-review" and etapa.get("restricao") != "revisor_sem_escrita":
            if not etapa.get("posterior"):
                erros.append(
                    f"[{fid}] /code-review sem restricao 'revisor_sem_escrita' (IDSD 4.9.3)."
                )

    return erros


def main() -> int:
    parser = argparse.ArgumentParser(description="Valida manifestos de flow contra a policy de gates")
    parser.add_argument("--governance", default=None, help="Raiz da pasta governance/")
    parser.add_argument("--flow", default=None, help="Valida apenas este flow (id)")
    args = parser.parse_args()

    raiz = Path(args.governance).resolve() if args.governance else (
        Path(__file__).resolve().parents[2] / "governance"
    )

    catalogo = carregar(raiz / "gates.yaml")
    policy = carregar(raiz / "policies" / "gates-por-classe.yaml")

    arquivos = sorted((raiz / "flows").glob("*.yaml"))
    if args.flow:
        arquivos = [f for f in arquivos if f.stem == args.flow]
        if not arquivos:
            raise SystemExit(f"ERRO: flow '{args.flow}' nao encontrado em {raiz / 'flows'}")

    total_erros = 0
    for arquivo in arquivos:
        flow = carregar(arquivo)
        erros = validar_flow(flow, catalogo, policy)
        if erros:
            total_erros += len(erros)
            for e in erros:
                print(f"  FALHA {e}")
        else:
            n = len(flow.get("etapas", []))
            print(f"  OK    {flow.get('id')} ({n} etapas)")

    if total_erros:
        print(f"\n{total_erros} falha(s) de conformidade de flow.")
        return 1
    print(f"\n{len(arquivos)} flow(s) conformes.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
