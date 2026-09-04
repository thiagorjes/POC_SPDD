"""
Verifica o congelamento do Gherkin (GATE-GHERKIN-CONGELADO).

O PRD e a fonte: os blocos ```gherkin aprovados no gate de spec. Os arquivos
.feature sao a copia executavel. Divergencia entre os dois so e legitima se
houver emenda registrada na tabela de emendas do PRD, com o ID preservado.

Confere que:
  - todo cenario do PRD existe nos .feature e vice-versa;
  - os passos batem, ignorando espacamento e comentarios;
  - cenario divergente tem emenda registrada;
  - nenhuma emenda muda o ID do cenario.

Uso:
  python check_congelamento.py --prd docs/prd/x-prd.md --features docs/prd/x
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "scripts"))
from md_tables import celula, corpo_util, preenchido, tabela_da_secao  # noqa: E402

SCN = re.compile(r"SCN-\d{3}\.\d{1,2}")
PASSO = re.compile(r"^\s*(Dado|Quando|Então|E|Mas)\b\s*(.*)$")


def passos(bloco: str) -> list[str]:
    saida = []
    for linha in bloco.splitlines():
        linha = linha.split("#")[0]
        m = PASSO.match(linha)
        if m:
            saida.append(f"{m.group(1)} {' '.join(m.group(2).split())}".strip())
    return saida


def do_prd(texto: str) -> dict[str, list[str]]:
    cenarios: dict[str, list[str]] = {}
    for bloco in re.findall(r"```gherkin\n(.*?)```", texto, re.DOTALL):
        m = SCN.search(bloco)
        if m:
            cenarios[m.group(0)] = passos(bloco)
    return cenarios


def dos_features(pasta: Path) -> dict[str, list[str]]:
    cenarios: dict[str, list[str]] = {}
    for arquivo in sorted(pasta.glob("*.feature")):
        texto = arquivo.read_text(encoding="utf-8")
        partes = re.split(r"^\s*(?:Cenário|Cenario|Scenario)\b", texto, flags=re.MULTILINE)
        for parte in partes[1:]:
            m = SCN.search(parte)
            if m:
                cenarios[m.group(0)] = passos(parte)
    return cenarios


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--prd", required=True)
    p.add_argument("--features", required=True)
    args = p.parse_args()

    caminho_prd = Path(args.prd)
    pasta = Path(args.features)
    if not caminho_prd.exists():
        print(f"ERRO: PRD nao encontrado: {caminho_prd}", file=sys.stderr)
        return 2
    if not pasta.is_dir():
        print(f"ERRO: pasta de .feature nao encontrada: {pasta}", file=sys.stderr)
        return 2

    bruto = caminho_prd.read_text(encoding="utf-8")
    prd = do_prd(corpo_util(bruto))
    feats = dos_features(pasta)

    emendados = set()
    erros: list[str] = []
    for linha in tabela_da_secao(bruto, "Emendas de cenário"):
        alvo = celula(linha, 1)
        if not preenchido(alvo):
            continue
        ids = SCN.findall(alvo)
        if not ids:
            erros.append(
                f"ERRO: emenda sem ID de cenario valido: '{alvo}' — cenario "
                f"emendado mantem o ID."
            )
            continue
        emendados.update(ids)
        if not preenchido(celula(linha, 4)):
            erros.append(f"ERRO: emenda de {ids[0]} sem aprovador.")

    if not prd:
        erros.append("ERRO: nenhum cenario Gherkin no PRD.")
    if not feats:
        erros.append(f"ERRO: nenhum cenario nos .feature de {pasta}.")

    for sid in sorted(set(prd) - set(feats)):
        erros.append(f"ERRO: cenario {sid} existe no PRD e nao nos .feature.")
    for sid in sorted(set(feats) - set(prd)):
        erros.append(
            f"ERRO: cenario {sid} existe nos .feature e nao no PRD — cenario "
            f"que ninguem aprovou."
        )
    for sid in sorted(set(prd) & set(feats)):
        if prd[sid] != feats[sid] and sid not in emendados:
            erros.append(
                f"ERRO: cenario {sid} divergente entre PRD e .feature sem emenda "
                f"registrada — reprova o GATE-GHERKIN-CONGELADO."
            )

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
