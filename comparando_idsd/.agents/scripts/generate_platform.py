"""
Gerador de arquivos de plataforma — IDSD.

Princípio: `.agents/` é a única fonte. Tudo que aparece em `.claude/`,
`.cursor/`, `.opencode/` e `.github/` é GERADO e referencia o `.agents/`.
Nenhum corpo de skill é duplicado quando a plataforma suporta include.

Suporte a include por plataforma:
    claude    sim  — `@caminho` dentro do SKILL.md
    opencode  sim  — `@caminho` no arquivo de comando
    cursor    sim  — `@caminho` dentro da regra .mdc
    copilot   nao  — corpo embutido, marcado como gerado

Uso:
    python generate_platform.py --platform all --path . --source .agents
"""

from __future__ import annotations

import argparse
import re
import shutil
import sys
from pathlib import Path

AVISO = "<!-- GERADO por .agents/scripts/generate_platform.py — nao editar. -->"


def parse_frontmatter(content: str) -> dict:
    m = re.search(r"^---\n(.*?)\n---", content, re.DOTALL)
    if not m:
        return {}
    fm: dict = {}
    lines = m.group(1).splitlines()
    i = 0
    while i < len(lines):
        line = lines[i]
        if ":" in line and not line.startswith((" ", "\t", "-")):
            key, _, val = line.partition(":")
            key, val = key.strip(), val.strip()
            if val == ">":
                folded = []
                i += 1
                while i < len(lines) and (lines[i].startswith((" ", "\t")) or not lines[i].strip()):
                    if lines[i].strip():
                        folded.append(lines[i].strip())
                    i += 1
                fm[key] = " ".join(folded)
                continue
            if val == "":
                # lista ou bloco: coleta itens indentados
                itens = []
                i += 1
                while i < len(lines) and lines[i].startswith((" ", "\t")):
                    item = lines[i].strip()
                    if item.startswith("- "):
                        itens.append(item[2:].strip())
                    i += 1
                fm[key] = itens
                continue
            fm[key] = val
        i += 1
    return fm


def yaml_str(valor: str) -> str:
    """Escalar YAML sempre citado.

    Description em texto corrido carrega ':' com frequencia ("nao escreve
    codigo: achado e devolvido"), e sem aspas isso quebra o frontmatter da
    plataforma em silencio — a skill carrega sem descricao.
    """
    return '"' + str(valor).replace("\\", "\\\\").replace('"', '\\"') + '"'


def strip_frontmatter(content: str) -> str:
    m = re.search(r"^---\n.*?\n---\n", content, re.DOTALL)
    return content[m.end():] if m else content


def discover_skills(source: Path) -> list[dict]:
    skills = []
    skills_dir = source / "skills"
    if not skills_dir.exists():
        return skills
    for skill_dir in sorted(skills_dir.iterdir()):
        skill_md = skill_dir / "SKILL.md"
        if not skill_dir.is_dir() or not skill_md.exists():
            continue
        content = skill_md.read_text(encoding="utf-8")
        fm = parse_frontmatter(content)
        skills.append({
            "name": fm.get("name", skill_dir.name),
            "description": fm.get("description", ""),
            "dir": skill_dir.name,
            "body": strip_frontmatter(content),
        })
    return skills


def discover_agents(source: Path) -> list[dict]:
    agents = []
    agents_dir = source / "agents"
    if not agents_dir.exists():
        return agents
    for agent_file in sorted(agents_dir.glob("*.md")):
        fm = parse_frontmatter(agent_file.read_text(encoding="utf-8"))
        agents.append({
            "name": fm.get("name", agent_file.stem),
            "description": fm.get("description", ""),
            "tools": fm.get("tools", "Read, Glob, Grep, Bash"),
            "file": agent_file.stem,
        })
    return agents


def escrever(destino: Path, conteudo: str) -> None:
    """Grava com fim de linha LF, em qualquer plataforma.

    `Path.write_text` usa a traducao de fim de linha do sistema: o mesmo
    gerador produz CRLF no Windows e LF no Linux. Como `check_drift.py` compara
    byte a byte contra uma regeneracao, um derivado gerado em uma plataforma e
    conferido em outra aparece como "conteudo divergente" sem que ninguem tenha
    editado nada — foi o que aconteceu com `.github/instructions/` (pendencia
    19). O derivado precisa ser funcao apenas da fonte.
    """
    destino.write_text(conteudo, encoding="utf-8", newline="\n")


def limpar(diretorio: Path) -> None:
    """Remove saída anterior para que renomear/remover uma skill se propague."""
    if diretorio.exists():
        shutil.rmtree(diretorio)


def gerar_claude(skills, agents, path: Path, src: str) -> None:
    skills_dir = path / ".claude" / "skills"
    limpar(skills_dir)
    for s in skills:
        d = skills_dir / s["dir"]
        d.mkdir(parents=True, exist_ok=True)
        escrever(
            d / "SKILL.md",
            f"---\nname: {s['name']}\ndescription: {yaml_str(s['description'])}\n---\n\n"
            f"{AVISO}\n\n@{src}/skills/{s['dir']}/SKILL.md\n",
        )
    print(f"  claude: {len(skills)} skills -> {skills_dir}")

    agents_dir = path / ".claude" / "agents"
    limpar(agents_dir)
    if agents:
        agents_dir.mkdir(parents=True, exist_ok=True)
        for a in agents:
            escrever(
                agents_dir / f"{a['name']}.md",
                f"---\nname: {a['name']}\ndescription: {yaml_str(a['description'])}\n"
                f"tools: {a['tools']}\n---\n\n{AVISO}\n\n@{src}/agents/{a['file']}.md\n",
            )
        print(f"  claude: {len(agents)} agents -> {agents_dir}")


def gerar_opencode(skills, path: Path, src: str) -> None:
    d = path / ".opencode" / "commands"
    limpar(d)
    d.mkdir(parents=True, exist_ok=True)
    for s in skills:
        escrever(d / f"{s['dir']}.md",
                 f"{AVISO}\n\n@{src}/skills/{s['dir']}/SKILL.md\n")
    print(f"  opencode: {len(skills)} commands -> {d}")


def gerar_cursor(skills, path: Path, src: str) -> None:
    d = path / ".cursor" / "rules"
    limpar(d)
    d.mkdir(parents=True, exist_ok=True)
    for s in skills:
        escrever(
            d / f"idsd-{s['dir']}.mdc",
            f"---\ndescription: {yaml_str(s['description'])}\nalwaysApply: false\n---\n\n"
            f"{AVISO}\n\n@{src}/skills/{s['dir']}/SKILL.md\n",
        )
    print(f"  cursor: {len(skills)} regras -> {d} (por referencia)")


def gerar_copilot(skills, path: Path, src: str) -> None:
    # Copilot nao suporta include: corpo embutido, marcado como gerado.
    d = path / ".github" / "instructions"
    limpar(d)
    d.mkdir(parents=True, exist_ok=True)
    for s in skills:
        escrever(
            d / f"{s['dir']}.instructions.md",
            f"---\ndescription: {yaml_str(s['description'])}\napplyTo: \"**\"\n---\n\n"
            f"{AVISO}\n<!-- Fonte: {src}/skills/{s['dir']}/SKILL.md -->\n\n"
            f"# /{s['name']}\n{s['body']}",
        )
    print(f"  copilot: {len(skills)} instructions -> {d} (corpo embutido)")


def main() -> int:
    p = argparse.ArgumentParser(description="Gera arquivos de plataforma a partir de .agents/")
    p.add_argument("--platform", required=True,
                   choices=["claude", "cursor", "copilot", "opencode", "all"])
    p.add_argument("--path", required=True, help="Raiz do sistema destino")
    p.add_argument("--source", required=True, help="Caminho para .agents/")
    args = p.parse_args()

    source = Path(args.source).resolve()
    if not source.exists():
        print(f"ERRO: source '{source}' nao encontrado.", file=sys.stderr)
        return 1

    path = Path(args.path).resolve()
    path.mkdir(parents=True, exist_ok=True)

    try:
        src = source.relative_to(path).as_posix()
    except ValueError:
        src = ".agents"

    skills = discover_skills(source)
    agents = discover_agents(source)
    if not skills:
        print("[AVISO] nenhum SKILL.md encontrado em source/skills/.")

    alvos = (["claude", "cursor", "copilot", "opencode"]
             if args.platform == "all" else [args.platform])

    for plat in alvos:
        if plat == "claude":
            gerar_claude(skills, agents, path, src)
        elif plat == "opencode":
            gerar_opencode(skills, path, src)
        elif plat == "cursor":
            gerar_cursor(skills, path, src)
        elif plat == "copilot":
            gerar_copilot(skills, path, src)

    print(f"OK - plataformas geradas: {', '.join(alvos)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
