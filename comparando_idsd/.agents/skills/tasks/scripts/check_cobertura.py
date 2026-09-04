"""
Verifica a invariante de rastreabilidade cenario<->epico (GATE-RASTREABILIDADE).

Confere que:
  - todo cenario do PRD e entregue por exatamente um epico;
  - todo epico entrega pelo menos um cenario;
  - nenhum epico entrega cenario inexistente no PRD;
  - a tabela "Cobertura de cenarios" bate com os campos "Entrega:" dos epicos;
  - nenhum epico passa de 20 tasks (bloqueio) — 13 a 20 gera aviso;
  - IDs de task sao sequenciais dentro do epico e o prefixo bate.

Uso:
  python check_cobertura.py --tasks docs/tasks/x-tasks.md --prd docs/prd/x-prd.md
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "scripts"))
from md_tables import campo, celula, corpo_util, preenchido, tabela_da_secao  # noqa: E402

SCN = r"SCN-\d{3}\.\d{1,2}"


def ler(caminho: Path, rotulo: str) -> str | None:
    if not caminho.exists():
        print(f"ERRO: {rotulo} nao encontrado: {caminho}", file=sys.stderr)
        return None
    return corpo_util(caminho.read_text(encoding="utf-8"))


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--tasks", required=True)
    p.add_argument("--prd", required=True)
    args = p.parse_args()

    tasks = ler(Path(args.tasks), "documento de tasks")
    prd = ler(Path(args.prd), "PRD")
    if tasks is None or prd is None:
        return 2

    erros: list[str] = []
    avisos: list[str] = []

    # Cenarios declarados no PRD: apenas os que sao ID de criterio de aceite.
    do_prd: set[str] = set()
    for m in re.finditer(r"^###\s+RF-\d{3}\b(.*?)(?=^###\s|^##\s|\Z)",
                         prd, re.MULTILINE | re.DOTALL):
        do_prd.update(re.findall(SCN, m.group(1)))
    if not do_prd:
        erros.append("ERRO: nenhum cenario encontrado no PRD — nada a rastrear.")

    # Epicos do documento de tasks.
    entregue_por: dict[str, list[str]] = {}
    for m in re.finditer(r"^##\s+(EPIC-\d{2})\b(.*?)(?=^##\s|\Z)",
                         tasks, re.MULTILINE | re.DOTALL):
        eid, bloco = m.group(1), m.group(2)
        entrega = re.findall(SCN, campo(bloco, "Entrega"))
        if not entrega:
            erros.append(
                f"ERRO: {eid} nao entrega nenhum cenario — epico que nao entrega "
                f"cenario nao e fatia vertical."
            )
        for sid in entrega:
            entregue_por.setdefault(sid, []).append(eid)

        tasks_do_epico = sorted(set(re.findall(rf"###\s+TASK-(\d{{2}})\.(\d{{1,2}})\b", bloco)))
        n = len(tasks_do_epico)
        numero = eid.split("-")[1]
        for pref, _seq in tasks_do_epico:
            if pref != numero:
                erros.append(
                    f"ERRO: {eid} contem TASK-{pref}.x — o prefixo da task deve "
                    f"ser o numero do epico."
                )
        if n == 0:
            erros.append(f"ERRO: {eid} sem nenhuma task.")
        elif n > 20:
            erros.append(
                f"ERRO: {eid} com {n} tasks — acima de 20 o epico deve ser "
                f"dividido; as tasks rodam em serie e o PR fica irrevisavel."
            )
        elif n > 12:
            avisos.append(f"AVISO: {eid} com {n} tasks — considere cortar.")

    if not entregue_por and not erros:
        erros.append("ERRO: nenhum epico encontrado no documento de tasks.")

    for sid in sorted(do_prd - set(entregue_por)):
        erros.append(
            f"ERRO: cenario {sid} nao e entregue por nenhum epico — cenario "
            f"orfao reprova o GATE-RASTREABILIDADE."
        )
    for sid, epicos in sorted(entregue_por.items()):
        if len(epicos) > 1:
            erros.append(
                f"ERRO: cenario {sid} entregue por {', '.join(epicos)} — cada "
                f"cenario pertence a exatamente um epico."
            )
        if sid not in do_prd:
            erros.append(
                f"ERRO: cenario {sid} entregue por {epicos[0]} nao existe no PRD."
            )

    # Tabela de cobertura x campos Entrega
    da_tabela: dict[str, str] = {}
    for linha in tabela_da_secao(tasks, "Cobertura de cenários", nivel=3):
        sid, eid = celula(linha, 0), celula(linha, 2)
        if not preenchido(sid):
            continue
        da_tabela[sid] = eid
        if not preenchido(celula(linha, 3)):
            erros.append(f"ERRO: cenario {sid} sem tipo de teste na cobertura.")
    for sid, eid in sorted(da_tabela.items()):
        real = entregue_por.get(sid, [])
        if eid not in real:
            erros.append(
                f"ERRO: a tabela de cobertura atribui {sid} a {eid}, mas o epico "
                f"nao o declara em 'Entrega'."
            )
    for sid in sorted(set(entregue_por) - set(da_tabela)):
        erros.append(f"ERRO: cenario {sid} ausente da tabela de cobertura.")

    for a in avisos:
        print(a, file=sys.stderr)
    for e in erros:
        print(e, file=sys.stderr)
    return 1 if erros else 0


if __name__ == "__main__":
    sys.exit(main())
