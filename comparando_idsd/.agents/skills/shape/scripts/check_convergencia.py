"""
Verifica que o Shape Brief realmente convergiu (GATE-DIRECAO).

Confere que:
  - ha ao menos duas alternativas descartadas, cada uma com motivo;
  - todo item 'adiado' tem gatilho de retomada;
  - a metrica de sucesso tem linha de base e alvo;
  - hipoteses (obrigatorias em modo ingestao) tem experimento e criterio de
    descarte;
  - a aprovacao humana esta preenchida.

Uso: python check_convergencia.py --artifact docs/shape/x-brief.md
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

VAZIOS = {"", "-", "—", "tbd", "a definir", "n/a"}


def tabela(texto: str, secao: str) -> list[list[str]]:
    m = re.search(rf"^##\s+{secao}\s*$(.*?)(?=^##\s|\Z)", texto,
                  re.MULTILINE | re.DOTALL)
    if not m:
        return []
    linhas, cabecalho_visto = [], False
    for linha in m.group(1).splitlines():
        linha = linha.strip()
        if not linha.startswith("|"):
            continue
        celulas = [c.strip() for c in linha.strip("|").split("|")]
        if all(set(c) <= set("- :") for c in celulas if c):
            continue
        if not cabecalho_visto:
            cabecalho_visto = True
            continue
        if any(celulas):
            linhas.append(celulas)
    return linhas


def preenchido(valor: str) -> bool:
    return valor.strip().lower() not in VAZIOS


def campo(texto: str, rotulo: str) -> str:
    m = re.search(rf"\*\*{re.escape(rotulo)}:?\*\*\s*(.*)", texto)
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

    # Alternativas descartadas
    alts = tabela(texto, "Alternativas descartadas")
    validas = [a for a in alts if len(a) > 2 and preenchido(a[0]) and preenchido(a[2])]
    if len(validas) < 2:
        erros.append(
            f"ERRO: convergencia exige ao menos 2 alternativas descartadas com "
            f"motivo (encontradas {len(validas)})."
        )
    for a in alts:
        if preenchido(a[0]) and len(a) > 2 and not preenchido(a[2]):
            erros.append(f"ERRO: alternativa '{a[0][:40]}' descartada sem motivo.")

    # Fronteira de escopo: coluna 'Adiado' exige gatilho
    for linha in tabela(texto, "Fronteira de escopo"):
        if len(linha) > 2 and preenchido(linha[2]):
            if not re.search(r"\bse\b|\bquando\b|\bap[óo]s\b", linha[2], re.IGNORECASE):
                erros.append(
                    f"ERRO: item adiado '{linha[2][:40]}' sem gatilho de retomada."
                )

    # Metrica de sucesso
    metricas = tabela(texto, "Métrica de sucesso")
    if not metricas:
        erros.append("ERRO: nenhuma metrica de sucesso definida.")
    for m in metricas:
        nome = m[0]
        base = m[1] if len(m) > 1 else ""
        alvo = m[2] if len(m) > 2 else ""
        if not preenchido(base):
            erros.append(f"ERRO: metrica '{nome}' sem linha de base.")
        if not preenchido(alvo):
            erros.append(f"ERRO: metrica '{nome}' sem alvo.")

    # Hipoteses: se houver, exigem experimento e criterio de descarte
    for h in tabela(texto, "Hipóteses a validar"):
        if len(h) > 1 and preenchido(h[1]):
            exp = h[2] if len(h) > 2 else ""
            crit = h[3] if len(h) > 3 else ""
            if not preenchido(exp) or not preenchido(crit):
                erros.append(
                    f"ERRO: hipotese '{h[1][:40]}' sem experimento ou sem "
                    f"criterio de descarte."
                )

    # Modo ingestao exige ao menos uma hipotese
    modo = campo(texto, "Modo").lower()
    if "ingest" in modo and not tabela(texto, "Hipóteses a validar"):
        erros.append(
            "ERRO: modo ingestao sem hipoteses registradas — saida de workshop "
            "e hipotese, nao requisito."
        )

    # Aprovacao
    aprovador = campo(texto, "Aprovado por")
    data = campo(texto, "Data")
    if not preenchido(aprovador) or aprovador.startswith("{{"):
        erros.append("ERRO: GATE-DIRECAO sem aprovador humano.")
    if not preenchido(data) or data.startswith("{{"):
        erros.append("ERRO: GATE-DIRECAO sem data de aprovacao.")

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
