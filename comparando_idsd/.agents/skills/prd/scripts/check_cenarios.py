"""
Verifica os cenarios Gherkin do PRD.

Confere que:
  - todo RF tem ao menos um cenario;
  - todo ID de cenario segue SCN-<RF>.<seq> e o prefixo bate com o RF que o
    contem (SCN-002.1 dentro do RF-001 e erro de rastreabilidade);
  - nao ha ID de cenario duplicado;
  - todo cenario tem tipo de teste declarado;
  - todo bloco gherkin tem Dado/Quando/Entao;
  - nenhum cenario cita detalhe de implementacao;
  - cenario com mais de um 'Entao' independente e sinalizado.

Uso: python check_cenarios.py --artifact docs/prd/x-prd.md
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "scripts"))
from md_tables import celula, corpo_util, preenchido, tabela  # noqa: E402

TIPOS_TESTE = {"unitário", "unitario", "integração", "integracao", "e2e"}

IMPLEMENTACAO = re.compile(
    r"\b(GET|POST|PUT|PATCH|DELETE)\s+/|"
    r"\b(status|c[óo]digo)\s+(200|201|204|400|401|403|404|409|422|500)\b|"
    r"\b(endpoint|tabela|coluna|query|SQL|JSON|payload)\b",
    re.IGNORECASE,
)


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
    vistos: dict[str, str] = {}

    blocos = list(re.finditer(
        r"^###\s+(RF-(\d{3}))\b(.*?)(?=^###\s|^##\s|\Z)",
        texto, re.MULTILINE | re.DOTALL,
    ))
    if not blocos:
        erros.append("ERRO: nenhum requisito funcional encontrado.")

    for m in blocos:
        rid, numero, bloco = m.group(1), m.group(2), m.group(3)

        ids_tabela = [
            linha for linha in tabela(bloco) if preenchido(celula(linha, 0))
        ]
        if not ids_tabela:
            erros.append(f"ERRO: {rid} sem criterios de aceite.")

        for linha in ids_tabela:
            sid = celula(linha, 0)
            if not re.fullmatch(r"SCN-\d{3}\.\d{1,2}", sid):
                erros.append(f"ERRO: {rid}: ID de cenario malformado: '{sid}'.")
                continue
            if not sid.startswith(f"SCN-{numero}."):
                erros.append(
                    f"ERRO: {rid} contem o cenario {sid} — o prefixo deveria ser "
                    f"SCN-{numero}."
                )
            if sid in vistos:
                erros.append(f"ERRO: cenario {sid} duplicado (ja em {vistos[sid]}).")
            else:
                vistos[sid] = rid

            if not preenchido(celula(linha, 1)):
                erros.append(f"ERRO: cenario {sid} sem descricao.")
            tipo = celula(linha, 2).lower().strip()
            if not preenchido(tipo):
                erros.append(f"ERRO: cenario {sid} sem tipo de teste declarado.")
            elif tipo not in TIPOS_TESTE and "|" not in tipo:
                erros.append(
                    f"ERRO: cenario {sid} com tipo de teste invalido: '{tipo}'."
                )

        gherkins = re.findall(r"```gherkin\n(.*?)```", bloco, re.DOTALL)
        if not gherkins:
            erros.append(f"ERRO: {rid} sem bloco Gherkin.")
        for g in gherkins:
            m_id = re.search(r"(SCN-\d{3}\.\d{1,2})", g)
            rotulo = m_id.group(1) if m_id else f"{rid} (cenario sem ID)"
            if not m_id:
                erros.append(f"ERRO: {rid}: bloco Gherkin sem ID de cenario.")
            for palavra in ("Dado", "Quando", "Então"):
                if not re.search(rf"^\s*{palavra}\b", g, re.MULTILINE):
                    erros.append(f"ERRO: {rotulo} sem passo '{palavra}'.")
            entaos = len(re.findall(r"^\s*Então\b", g, re.MULTILINE))
            if entaos > 1:
                erros.append(
                    f"ERRO: {rotulo} tem {entaos} passos 'Então' — um "
                    f"comportamento por cenario; use 'E' para continuacao ou "
                    f"separe em cenarios."
                )
            achado = IMPLEMENTACAO.search(g)
            if achado:
                erros.append(
                    f"ERRO: {rotulo} cita detalhe de implementacao "
                    f"('{achado.group(0)}') — o cenario e contrato de "
                    f"comportamento."
                )

    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
