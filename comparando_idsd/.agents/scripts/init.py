"""
Inicialização/regeneração do sistema IDSD.

Idempotente: pode rodar quantas vezes for necessário. Tudo que este script
escreve é derivado de `.agents/` e de `governance/` — nunca o contrário.

Ordem:
    1. valida os manifestos de flow contra a policy de gates
    2. valida a estrutura das skills
    3. gera os arquivos de plataforma (.claude, .cursor, .opencode, .github)
    4. gera AGENTS.md (canonico) e CLAUDE.md (referencia)
    5. cria a arvore de docs/ e memory/ se ausente

Uso:
    python .agents/scripts/init.py
    python .agents/scripts/init.py --skip-validate
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from datetime import date
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_platform import discover_agents, discover_skills  # noqa: E402

RAIZ = Path(__file__).resolve().parents[2]
AGENTS = RAIZ / ".agents"
SCRIPTS = AGENTS / "scripts"

DOCS = [
    "docs/intent", "docs/context", "docs/discovery", "docs/shape",
    "docs/solution", "docs/design", "docs/prd", "docs/techspec",
    "docs/analyze", "docs/tasks", "docs/review", "docs/evidence",
    "docs/decisions", "docs/architecture", "docs/spdd", "docs/checklists",
    "memory",
]


def rodar(script: str, *args: str) -> int:
    return subprocess.call([sys.executable, str(SCRIPTS / script), *args])


def gerar_agents_md(skills: list[dict], agents: list[dict]) -> None:
    linhas = [
        "# AGENTS.md — IDSD",
        "",
        "<!-- GERADO por .agents/scripts/init.py — nao editar. -->",
        "",
        "> Referencia canonica cross-vendor de skills e agents deste sistema.",
        "> `CLAUDE.md` referencia este arquivo via `@`.",
        "",
        "---",
        "",
        "## Camadas",
        "",
        "- **Governanca** — `governance/`: constitution, policies e o catalogo de gates.",
        "  Declarativa. Agentes nao tem permissao de escrita (IDSD 4.1.1).",
        "- **Controle** — `/intent`, `/classify`, `/context` e a orquestracao.",
        "  Resolve o flow aplicavel e confronta cada transicao com as policies.",
        "- **Execucao** — os flows registrados em `governance/flows/`.",
        "  O SSPDD e um deles, nao o unico.",
        "",
        "Nenhum flow e obrigado a rodar o SSPDD. Todo flow e obrigado a satisfazer",
        "os gates que a policy exige para a classe do intent que o originou —",
        "verificado no registro por `validate_flow.py`.",
        "",
        "---",
        "",
        "## Skills disponiveis",
        "",
    ]
    for s in skills:
        linhas.append(f"- **/{s['name']}** — {s['description']}")
    linhas += ["", "---", "", "## Agents disponiveis", ""]
    for a in agents:
        linhas.append(f"- **{a['name']}** — {a['description']}")
    linhas += [
        "",
        "---",
        "",
        "## Convencoes",
        "",
        "- Toda skill tem `SKILL.md` em `.agents/skills/[skill]/` e `validate-rules.json`.",
        "- Todo agent tem definicao em `.agents/agents/[agent].md`.",
        "- `memory/constitution.md` — principios estaveis e DRs.",
        "  `memory/state.md` — estado operacional.",
        "- Arquivos em `.claude/`, `.cursor/`, `.opencode/` e `.github/` sao gerados.",
        "- O REASONS Canvas e derivado das fontes, nao mantido a mao:",
        "  `.agents/scripts/derive_canvas.py --feature <nome>`.",
        "- Sincronia dos derivados: `.agents/scripts/check_drift.py`",
        "  (`canvas-drift` e `skill-drift`).",
        "",
    ]
    (RAIZ / "AGENTS.md").write_text("\n".join(linhas), encoding="utf-8")
    print(f"  AGENTS.md: {len(skills)} skills, {len(agents)} agents")


def gerar_claude_md() -> None:
    conteudo = (
        "# CLAUDE.md — IDSD\n\n"
        "<!-- GERADO por .agents/scripts/init.py — nao editar. -->\n\n"
        "@comportamento.md\n"
        "@AGENTS.md\n"
        "@memory/constitution.md\n"
        "@memory/state.md\n"
        "@README.md\n\n"
        "> `AGENTS.md` e a fonte canonica cross-vendor; este arquivo apenas referencia.\n"
    )
    (RAIZ / "CLAUDE.md").write_text(conteudo, encoding="utf-8")
    print("  CLAUDE.md: gerado")


def bootstrap() -> int:
    """Cria os arquivos que o CLAUDE.md referencia, se ainda nao existirem.

    O CLAUDE.md gerado aponta para quatro arquivos via `@`. Sem eles a sessao
    abre com referencias quebradas — e o sintoma (agente sem contexto de
    comportamento) nao se parece com a causa. Nunca sobrescreve: sao arquivos
    que o time edita, e o init roda muitas vezes.
    """
    hoje = date.today().isoformat()
    origem = AGENTS / "templates"
    pares = [
        (origem / "comportamento.md-template", RAIZ / "comportamento.md"),
        (origem / "memory" / "constitution-template.md", RAIZ / "memory" / "constitution.md"),
        (origem / "memory" / "state-template.md", RAIZ / "memory" / "state.md"),
    ]

    criados = 0
    for template, destino in pares:
        if destino.exists() or not template.exists():
            continue
        texto = template.read_text(encoding="utf-8")
        texto = texto.replace("{{PROJECT_NAME}}", RAIZ.name).replace("{{DATE}}", hoje)
        destino.parent.mkdir(parents=True, exist_ok=True)
        destino.write_text(texto, encoding="utf-8")
        criados += 1

    readme = RAIZ / "README.md"
    if not readme.exists():
        readme.write_text(f"# {RAIZ.name}\n", encoding="utf-8")
        criados += 1

    print(f"  {criados} arquivo(s) de contexto criado(s)")
    return criados


def main() -> int:
    p = argparse.ArgumentParser(description="Inicializa/regenera o sistema IDSD")
    p.add_argument("--skip-validate", action="store_true")
    args = p.parse_args()

    print(f"IDSD init — raiz: {RAIZ}")

    if not args.skip_validate:
        print("\n[1/5] conformidade de flows")
        if rodar("validate_flow.py") != 0:
            print("ERRO: flows nao conformes. Corrija antes de gerar.", file=sys.stderr)
            return 1

        print("\n[2/5] estrutura das skills")
        if (SCRIPTS / "validate_skills.py").exists():
            if rodar("validate_skills.py", str(AGENTS / "skills")) != 0:
                print("ERRO: skills invalidas.", file=sys.stderr)
                return 1
        else:
            print("  (validate_skills.py ausente — pulado)")

    print("\n[3/5] arquivos de plataforma")
    if rodar("generate_platform.py", "--platform", "all",
             "--path", str(RAIZ), "--source", str(AGENTS)) != 0:
        return 1

    print("\n[4/5] AGENTS.md / CLAUDE.md")
    skills = discover_skills(AGENTS)
    agents = discover_agents(AGENTS)
    gerar_agents_md(skills, agents)
    gerar_claude_md()

    print("\n[5/5] arvore de diretorios")
    criados = 0
    for rel in DOCS:
        d = RAIZ / rel
        if not d.exists():
            d.mkdir(parents=True, exist_ok=True)
            (d / ".gitkeep").touch()
            criados += 1
    print(f"  {criados} diretorio(s) criado(s), {len(DOCS) - criados} ja existentes")
    bootstrap()

    print("\nOK - sistema IDSD pronto.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
