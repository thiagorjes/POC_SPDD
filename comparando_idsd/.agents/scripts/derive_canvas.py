"""
Derivacao do REASONS Canvas — IDSD.

O canvas deixa de ser mantido a mao e passa a ser projecao das fontes. Cada
dimensao tem uma ancora declarada abaixo; se a ancora nao existe, o canvas
registra a ausencia em vez de inventar conteudo — a lacuna e informacao, e
aparece no relatorio.

    R  Requirements  <- PRD: escopo da entrega + requisitos funcionais
    E  Entities      <- Solucao: entidades e vocabulario; TechSpec: modelo de dados
    A  Approach      <- TechSpec: decisoes arquiteturais
    S  Structure     <- TechSpec: arquitetura e dependencias
    O  Operations    <- Tasks: epicos e tasks
    N  Norms         <- guidelines.yaml: colecoes que governam o sistema
    S  Safeguards    <- code-review: guardrails extraidos

`DRAFT`/`READY` nao existe mais: ou o canvas esta em sincronia com as fontes e
o build passa, ou nao passa.

Uso:
    python derive_canvas.py --feature pagamentos            # gera/atualiza
    python derive_canvas.py --feature pagamentos --check    # canvas-drift
"""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from md_tables import secao, sem_citacoes  # noqa: E402

AVISO = ("<!-- GERADO por .agents/scripts/derive_canvas.py — nao editar. "
         "Altere a fonte e regenere. -->")


class Fonte:
    """Um arquivo de origem, lido uma vez e identificado por hash.

    O caminho exibido e sempre relativo a raiz: caminho absoluto no canvas faz o
    arquivo diferir entre maquinas e o canvas-drift reprovar em CI por um motivo
    que nada tem a ver com as fontes.
    """

    def __init__(self, caminho: Path, raiz: Path):
        self.caminho = caminho
        try:
            self.rotulo = caminho.relative_to(raiz).as_posix()
        except ValueError:
            self.rotulo = caminho.as_posix()
        self.existe = caminho.exists()
        self.texto = caminho.read_text(encoding="utf-8") if self.existe else ""

    @property
    def hash(self) -> str:
        if not self.existe:
            return "ausente"
        return hashlib.sha1(self.texto.encode("utf-8")).hexdigest()[:12]

    def trecho(self, titulo: str, nivel: int = 2) -> str | None:
        """Corpo de uma secao, ou None se a ancora nao existe."""
        if not self.existe:
            return None
        corpo = sem_citacoes(secao(self.texto, titulo, nivel)).strip()
        return corpo or None


def ausente(fonte: Fonte, titulo: str) -> str:
    motivo = "arquivo ausente" if not fonte.existe else f"seção “{titulo}” ausente"
    return f"_Âncora não resolvida: `{fonte.rotulo}` — {motivo}._"


def bloco(fonte: Fonte, titulo: str, nivel: int = 2) -> tuple[str, bool]:
    corpo = fonte.trecho(titulo, nivel)
    if corpo is None:
        return ausente(fonte, titulo), False
    return corpo, True


def requisitos(prd: Fonte) -> tuple[str, bool]:
    if not prd.existe:
        return ausente(prd, "Requisitos funcionais"), False
    partes = []
    escopo = prd.trecho("Escopo da entrega")
    if escopo:
        partes.append(escopo)
    ids = re.findall(r"^###\s+(RF-\d{3})\s*[—-]\s*(.+)$", prd.texto, re.MULTILINE)
    if ids:
        partes.append("**Requisitos:**\n" + "\n".join(
            f"- {rid} — {titulo.strip()}" for rid, titulo in ids))
    if not partes:
        return ausente(prd, "Requisitos funcionais"), False
    return "\n\n".join(partes), True


def operacoes(tasks: Fonte) -> tuple[str, bool]:
    if not tasks.existe:
        return ausente(tasks, "Sumário de épicos"), False
    linhas = []
    for m in re.finditer(r"^##\s+(EPIC-\d{2})\s*[—-]\s*(.+)$", tasks.texto, re.MULTILINE):
        eid, nome = m.group(1), m.group(2).strip()
        bloco_epico = secao(tasks.texto, f"{eid} — {nome}")
        ids = re.findall(r"^###\s+(TASK-\d{2}\.\d{1,2})\s*[—-]\s*(.+)$",
                         bloco_epico, re.MULTILINE)
        linhas.append(f"**{eid} — {nome}**")
        linhas += [f"- {tid} — {t.strip()}" for tid, t in ids]
    if not linhas:
        return ausente(tasks, "Sumário de épicos"), False
    return "\n".join(linhas), True


def normas(raiz: Path) -> tuple[str, bool]:
    """Dimensao N — as normas vigentes, lidas do vinculo, nao do disco.

    A biblioteca de guidelines e compartilhada e organizada por stack; varrer
    um diretorio local acharia ou nada ou tudo. A unica fonte de qual colecao
    governa este sistema e o `guidelines.yaml` da raiz.
    """
    import yaml

    manifesto = raiz / "guidelines.yaml"
    if not manifesto.exists():
        return ("_Âncora não resolvida: `guidelines.yaml` ausente. "
                "Rode o flow de setup (`/guidelines`)._"), False

    dados = yaml.safe_load(manifesto.read_text(encoding="utf-8")) or {}
    biblioteca = (raiz / dados.get("raiz", "../guidelines")).resolve()
    colecoes = dados.get("colecoes") or []

    if not colecoes:
        motivo = (dados.get("motivo") or "").strip()
        if not motivo:
            return ("_`guidelines.yaml` não declara coleção nem motivo._"), False
        return f"_Sem coleção de stack — {motivo}_", True

    linhas = []
    faltando = False
    for rel in colecoes:
        pasta = biblioteca / rel
        arquivos = sorted(pasta.glob("*.md")) if pasta.is_dir() else []
        if not arquivos:
            linhas.append(f"- `{rel}` — **declarada mas ausente na biblioteca**")
            faltando = True
            continue
        linhas.append(f"- `{rel}`")
        linhas += [f"  - `{rel}/{a.name}`" for a in arquivos]

    transversais = sorted((biblioteca / "_shared").glob("*.md"))
    if transversais:
        linhas.append("- `_shared` (transversais)")
        linhas += [f"  - `_shared/{a.name}`" for a in transversais]

    return "\n".join(linhas), not faltando


def salvaguardas(raiz: Path, feature: str) -> tuple[str, bool, list[Fonte]]:
    pasta = raiz / "docs" / "review" / feature
    reviews = sorted(pasta.glob("*-review.md")) if pasta.is_dir() else []
    fontes = [Fonte(r, raiz) for r in reviews]
    linhas = []
    for f in fontes:
        corpo = f.trecho("Guardrails extraídos")
        if corpo:
            linhas.append(f"<!-- {f.caminho.name} -->\n{corpo}")
    if not linhas:
        return (f"_Sem guardrails: nenhuma revisão concluída em "
                f"`docs/review/{feature}/`._"), False, fontes
    return "\n\n".join(linhas), True, fontes


def montar(raiz: Path, feature: str) -> tuple[str, list[str]]:
    prd = Fonte(raiz / "docs" / "prd" / f"{feature}-prd.md", raiz)
    solucao = Fonte(raiz / "docs" / "solution" / f"{feature}-solution.md", raiz)
    techspec = Fonte(raiz / "docs" / "techspec" / f"{feature}-techspec.md", raiz)
    tasks = Fonte(raiz / "docs" / "tasks" / f"{feature}-tasks.md", raiz)

    dimensoes: list[tuple[str, str, str, bool]] = []
    pendencias: list[str] = []

    def add(chave: str, fonte_txt: str, conteudo: str, ok: bool) -> None:
        dimensoes.append((chave, fonte_txt, conteudo, ok))
        if not ok:
            pendencias.append(chave)

    corpo, ok = requisitos(prd)
    add("R — Requirements", prd.rotulo, corpo, ok)

    ent_sol, ok_sol = bloco(solucao, "Entidades e vocabulário")
    ent_ts, ok_ts = bloco(techspec, "3. Modelo de Dados")
    add("E — Entities",
        f"{solucao.rotulo}, {techspec.rotulo}",
        f"{ent_sol}\n\n{ent_ts}", ok_sol or ok_ts)

    corpo, ok = bloco(techspec, "2. Decisões Arquiteturais")
    add("A — Approach", techspec.rotulo, corpo, ok)

    arq, ok_arq = bloco(techspec, "5. Arquitetura e Fluxo")
    dep, ok_dep = bloco(techspec, "6. Dependências Inter-Sistemas")
    add("S — Structure", techspec.rotulo,
        f"{arq}\n\n{dep}", ok_arq or ok_dep)

    corpo, ok = operacoes(tasks)
    add("O — Operations", tasks.rotulo, corpo, ok)

    corpo, ok = normas(raiz)
    add("N — Norms", "guidelines.yaml", corpo, ok)

    corpo, ok, fontes_review = salvaguardas(raiz, feature)
    add("S — Safeguards", f"docs/review/{feature}/", corpo, ok)

    partes = [
        f"# REASONS Canvas — {feature}",
        "",
        AVISO,
        "",
        "> Projeção das fontes. Não há transição manual DRAFT → READY: ou o",
        "> canvas está em sincronia e o check `canvas-drift` passa, ou não passa.",
        "",
        "---",
        "",
        "## Fontes",
        "",
        "| Artefato | Conteúdo |",
        "| --- | --- |",
    ]
    for f in [prd, solucao, techspec, tasks] + fontes_review:
        partes.append(f"| `{f.rotulo}` | {f.hash} |")
    partes += ["", "---", ""]

    for chave, fonte_txt, conteudo, _ in dimensoes:
        partes += [f"## {chave}", "", f"_Fonte: {fonte_txt}_", "", conteudo, "", "---", ""]

    partes += ["## Âncoras não resolvidas", ""]
    if pendencias:
        partes += [f"- {p}" for p in pendencias]
        partes += ["", "> Dimensão sem âncora é lacuna real na cadeia, não defeito",
                   "> do gerador. Corrija a fonte e regenere."]
    else:
        partes.append("Nenhuma.")
    partes.append("")

    return "\n".join(partes), pendencias


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--feature", required=True)
    p.add_argument("--root", default=".")
    p.add_argument("--check", action="store_true",
                   help="nao escreve; falha se o canvas em disco divergir")
    args = p.parse_args()

    raiz = Path(args.root).resolve()
    destino = raiz / "docs" / "spdd" / f"{args.feature}-canvas.md"
    novo, pendencias = montar(raiz, args.feature)

    if args.check:
        if not destino.exists():
            print(f"ERRO: canvas-drift — {destino.as_posix()} nao existe; "
                  f"rode derive_canvas.py.", file=sys.stderr)
            return 1
        atual = destino.read_text(encoding="utf-8")
        if atual != novo:
            print(f"ERRO: canvas-drift — {destino.as_posix()} difere do "
                  f"regenerado a partir das fontes.", file=sys.stderr)
            return 1
        print(f"OK - canvas em sincronia ({len(pendencias)} ancora(s) nao "
              f"resolvida(s)).")
        return 0

    destino.parent.mkdir(parents=True, exist_ok=True)
    destino.write_text(novo, encoding="utf-8")
    print(f"OK - canvas derivado: {destino.as_posix()}")
    for chave in pendencias:
        print(f"  ancora nao resolvida: {chave}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
