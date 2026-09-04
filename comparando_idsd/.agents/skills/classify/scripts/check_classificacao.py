"""
Verifica a coerencia da classificacao contra a governanca.

Confere que:
  - os quatro eixos foram preenchidos com valores do dominio permitido;
  - o flow citado existe em governance/flows/ e declara atender a classe;
  - os gates transcritos batem exatamente com os que a policy exige;
  - override, se houver, tem autor e motivo.

Uso: python check_classificacao.py --artifact docs/intent/x-classification.md
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

import yaml

TIPOS = {"feature", "bug", "dependencia", "migracao", "emergencia"}
RISCOS = {"baixo", "medio", "alto"}
IMPACTOS = {"local", "sistema", "multi-sistema"}
VAZIOS = {"", "-", "—", "tbd", "a definir"}


def raiz_do_sistema(artefato: Path) -> Path:
    for pai in [artefato.resolve()] + list(artefato.resolve().parents):
        if (pai / "governance" / "gates.yaml").exists():
            return pai
    raise SystemExit("ERRO: nao encontrei governance/gates.yaml acima do artefato.")


def celula(texto: str, eixo: str) -> str | None:
    m = re.search(rf"^\|\s*{eixo}\s*\|([^|]*)\|", texto, re.IGNORECASE | re.MULTILINE)
    return m.group(1).strip().lower() if m else None


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--artifact", required=True)
    args = p.parse_args()

    caminho = Path(args.artifact)
    if not caminho.exists():
        print(f"ERRO: artefato nao encontrado: {caminho}", file=sys.stderr)
        return 2

    texto = caminho.read_text(encoding="utf-8")
    raiz = raiz_do_sistema(caminho)
    erros: list[str] = []

    tipo = celula(texto, "Tipo")
    risco = celula(texto, "Risco")
    impacto = celula(texto, "Impacto")
    dominio = celula(texto, r"Dom[ií]nio")

    for nome, valor, permitidos in (
        ("Tipo", tipo, TIPOS),
        ("Risco", risco, RISCOS),
        ("Impacto", impacto, IMPACTOS),
    ):
        if valor is None or valor in VAZIOS:
            erros.append(f"ERRO: eixo '{nome}' nao preenchido.")
        elif "|" in (valor or "") or valor not in permitidos:
            erros.append(
                f"ERRO: eixo '{nome}' com valor invalido: '{valor}'. "
                f"Permitidos: {', '.join(sorted(permitidos))}."
            )
    if dominio is None or dominio in VAZIOS:
        erros.append("ERRO: eixo 'Dominio' nao preenchido.")

    # Flow citado
    m = re.search(r"governance/flows/([A-Za-z0-9_-]+)\.yaml", texto)
    if not m:
        erros.append("ERRO: nenhum flow resolvido citado.")
    elif tipo in TIPOS:
        fid = m.group(1)
        arquivo = raiz / "governance" / "flows" / f"{fid}.yaml"
        if not arquivo.exists():
            erros.append(f"ERRO: flow citado nao existe: {arquivo.name}")
        else:
            flow = yaml.safe_load(arquivo.read_text(encoding="utf-8")) or {}
            if tipo not in (flow.get("atende") or []):
                erros.append(
                    f"ERRO: flow '{fid}' nao declara atender a classe '{tipo}'."
                )

    # Gates transcritos x policy
    if tipo in TIPOS:
        policy = yaml.safe_load(
            (raiz / "governance" / "policies" / "gates-por-classe.yaml")
            .read_text(encoding="utf-8")
        ) or {}
        entrada = policy.get("classes", {}).get(tipo, {})
        esperados = set(policy.get("nucleo_inegociavel", []))
        esperados |= set(entrada.get("gates") or [])
        esperados |= set((entrada.get("posteriores") or {}).get("gates") or [])
        citados = set(re.findall(r"\bGATE-[A-Z-]+\b", texto))
        for gid in sorted(esperados - citados):
            erros.append(f"ERRO: gate obrigatorio nao transcrito: {gid}")
        for gid in sorted(citados - esperados):
            erros.append(f"ERRO: gate citado nao exigido pela policy da classe: {gid}")

    # Override
    bloco = texto.split("## Override", 1)
    if len(bloco) > 1:
        corpo = bloco[1].split("## ", 1)[0]
        preenchido = re.search(r"\*\*Classificação após override:\*\*\s*(\S.*)", corpo)
        if preenchido and preenchido.group(1).strip() not in VAZIOS:
            for campo in ("Autor", "Motivo"):
                m2 = re.search(rf"\*\*{campo}:\*\*\s*(\S.*)", corpo)
                if not m2 or m2.group(1).strip().lower() in VAZIOS:
                    erros.append(f"ERRO: override sem '{campo}' e invalido.")

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
