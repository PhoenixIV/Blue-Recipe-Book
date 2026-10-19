"""GitHub, Modrinth, and CurseForge pages rendered from the readme template.

An `only:` block is kept for the platforms it names. The marker lines are left out.
The opening HTML comment is left out. The GitHub page starts with a generated-file note.

An empty link takes its URL from the lookup table. GitHub uses the first URL listed.
An unused lookup name prints a warning.

A [text](#anchor) link must match a heading on that platform's page.
GitHub keeps the link. CurseForge has no anchors, so the link points at
https://github.com/PhoenixIV/Blue-Recipe-Book#anchor instead.
Modrinth needs the id written on the heading. A heading line becomes
<hN id="slug">text</hN>. N is the number of leading #.
slug is the GitHub heading slug.

A relative link names a path in this repository, such as
[text](docs/images/shot.webp). GitHub keeps the link. Modrinth and
CurseForge point it at that file on GitHub:
https://github.com/PhoenixIV/Blue-Recipe-Book/blob/main/docs/images/shot.webp

Modrinth and CurseForge do not support GitHub alerts.
GitHub alerts stay on GitHub. On Modrinth and CurseForge they become:
    [!WARNING] ->  `⚠️ Warning`
    [!NOTE]    ->  `ℹ️ Note`

CurseForge does not support Markdown inside HTML tags.
The CurseForge page therefore is written entirely as HTML.

After a run, the script stores the modification times of README.template.md and
render_readme.py in tools/readme/out/template.mtime, one per line. A later
run skips rendering when both times still match and every page is still
present. Delete that file to force a run.
"""

from __future__ import annotations

import posixpath
import re
import sys
from pathlib import Path
from urllib.parse import quote, unquote, urlparse

import markdown
from markdown.util import BLOCK_LEVEL_ELEMENTS

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE / "vendor"))
from github_slugger import GithubSlugger

SCRIPT = Path(__file__).resolve()
TEMPLATE = HERE / "README.template.md"
OUT = HERE / "out"
STAMP = OUT / "template.mtime"
PLATFORMS = ("github", "modrinth", "curseforge")
OUTPUTS = {
    "github": ROOT / "README.md",
    "modrinth": OUT / "modrinth.md",
    "curseforge": OUT / "curseforge.md",
}

ONLY_LINE = re.compile(r"^<!--\s*only:\s*(.*?)\s*-->\s*$")
END_LINE = re.compile(r"^<!--\s*end\s*-->\s*$")
LEADING_COMMENT = re.compile(r"\A<!--.*?-->[ \t]*\r?\n(?:\r?\n)?", re.DOTALL)
NAME_LINE = re.compile(r"^- (.+?)\s*$")
URL_LINE = re.compile(r"^[ \t]+- (https://\S+)\s*$")
EMPTY_LINK = re.compile(r"\[([^\]]+)\]\(\)")
ANCHOR_LINK = re.compile(r"\[[^\]]*\]\(#([^)]*)\)")
MARKDOWN_LINK = re.compile(r"\[[^\]]*\]\(([^)]*)\)")
BLOCK_OPEN = re.compile(
    r"<(" + "|".join(BLOCK_LEVEL_ELEMENTS) + r")\b([^>]*)>",
    re.IGNORECASE,
)
GITHUB_NOTE = "<!-- Generated from tools/readme/README.template.md. Do not edit by hand. -->\n"
GITHUB_PAGE = "https://github.com/PhoenixIV/Blue-Recipe-Book"
GITHUB_BLOB = f"{GITHUB_PAGE}/blob/main/"
SITE_HOSTS = {"modrinth": "modrinth.com", "curseforge": "curseforge.com"}
ALERTS = {"[!WARNING]": "⚠️ Warning", "[!NOTE]": "ℹ️ Note"}


class TemplateError(Exception):
    """A template line the renderer refuses to publish."""

    def __init__(self, line: int, message: str) -> None:
        super().__init__(f"{TEMPLATE.name}:{line}: {message}")


def allowed_names(raw: str, line: int) -> set[str]:
    names = [part for part in re.split(r"[\s,]+", raw) if part]
    if not names:
        raise TemplateError(line, "only: needs a platform name")
    unknown = [name for name in names if name not in PLATFORMS]
    if unknown:
        joined = ", ".join(unknown)
        raise TemplateError(line, f"unknown platform {joined}")
    return set(names)


def parse_links(text: str) -> tuple[dict[str, list[str]], dict[str, int]]:
    """Return each lookup name, its URLs in listed order, and the name's line. Stops at the first blank line."""
    lines = text.splitlines()
    heading = "## Link lookup table"
    try:
        start = lines.index(heading)
    except ValueError:
        raise TemplateError(1, "missing link lookup table") from None
    links: dict[str, list[str]] = {}
    name_lines: dict[str, int] = {}
    name: str | None = None
    for number, line in enumerate(lines[start + 1 :], start=start + 2):
        if not line.strip() or line.strip() == "-->":
            break
        url_match = URL_LINE.fullmatch(line)
        if url_match:
            if name is None:
                raise TemplateError(number, "link URL has no name")
            links[name].append(url_match.group(1))
            continue
        name_match = NAME_LINE.fullmatch(line)
        if name_match:
            name = name_match.group(1).strip()
            if not name or name.startswith("https://"):
                raise TemplateError(number, "link URL has no name")
            if name in links:
                raise TemplateError(number, f"duplicate link name {name}")
            links[name] = []
            name_lines[name] = number
            continue
        raise TemplateError(number, "link table line is not a name or a URL")
    return links, name_lines


def site_host(url: str) -> str:
    return urlparse(url).netloc.removeprefix("www.")


def url_for(name: str, urls: list[str], platform: str, line: int) -> str:
    if platform == "github":
        if not urls:
            raise TemplateError(line, f"{name} has no links")
        return urls[0]
    host = SITE_HOSTS[platform]
    matches = [url for url in urls if site_host(url) == host]
    if len(matches) != 1:
        raise TemplateError(line, f"{name} needs one {platform} link")
    return matches[0]


def flatten_alerts(line: str) -> str:
    """Replace GitHub alert markers and leave the rest of the line."""
    for marker, label in ALERTS.items():
        line = line.replace(marker, label)
    return line


def fill_links(line: str, number: int, platform: str, links: dict[str, list[str]]) -> str:
    """Replace empty links on this line. Anchors and filled links stay."""

    def replace(match: re.Match[str]) -> str:
        name = match.group(1)
        urls = links.get(name)
        if urls is None:
            raise TemplateError(number, f"missing link {name}")
        return f"[{name}]({url_for(name, urls, platform, number)})"

    return EMPTY_LINK.sub(replace, line)


def warn_unused(body: str, name_lines: dict[str, int]) -> None:
    """Print one warning for each lookup name the page never uses."""
    used = set(EMPTY_LINK.findall(body))
    for name, line in name_lines.items():
        if name not in used:
            print(f"{TEMPLATE.name}:{line}: warning: unused link {name}", file=sys.stderr)


def check_anchors(lines: list[tuple[int, str]], platform: str) -> None:
    """Raise when a #anchor link matches no heading on this page.

    A heading is a kept line that starts with #. Its anchor is the line text
    after the hashes, slugged the way GitHub slugs a heading.
    """
    slugger = GithubSlugger()
    targets: set[str] = set()
    for _, body in lines:
        if body.startswith("#"):
            targets.add(slugger.slug(body.lstrip("#").strip()))
    for number, body in lines:
        for match in ANCHOR_LINK.finditer(body):
            anchor = match.group(1)
            if anchor not in targets:
                raise TemplateError(
                    number,
                    f"Anchor #{anchor} has no target / matching heading on {platform}",
                )


def point_anchors_at_github(text: str) -> str:
    """Point each #anchor link at the same anchor on the GitHub readme."""

    def replace(match: re.Match[str]) -> str:
        anchor = match.group(1)
        href = f"](#{anchor})"
        head, _, tail = match.group(0).rpartition(href)
        return f"{head}]({GITHUB_PAGE}#{anchor}){tail}"

    return ANCHOR_LINK.sub(replace, text)


def repo_relative(url: str) -> str | None:
    """Return the repository path for a relative link, or None when the URL stays.

    Raises:
        ValueError: The path leaves the repository.
    """
    stripped = url.strip()
    if not stripped or stripped.startswith("#"):
        return None
    parsed = urlparse(stripped)
    if parsed.scheme or parsed.netloc or not parsed.path or parsed.path.startswith("/"):
        return None
    path = posixpath.normpath(unquote(parsed.path))
    if path == "." or path.startswith(".."):
        raise ValueError(stripped)
    suffix = ""
    if parsed.query:
        suffix += "?" + parsed.query
    if parsed.fragment:
        suffix += "#" + parsed.fragment
    return quote(path, safe="/") + suffix


def check_relative_links(lines: list[tuple[int, str]]) -> None:
    """Raise when a relative link leaves the repository or names a missing path."""
    for number, body in lines:
        for match in MARKDOWN_LINK.finditer(body):
            url = match.group(1)
            try:
                path = repo_relative(url)
            except ValueError:
                raise TemplateError(
                    number, f"relative link {url.strip()} leaves the repository"
                ) from None
            if path is None:
                continue
            local = unquote(path.partition("?")[0].partition("#")[0])
            if not (ROOT / local).exists():
                raise TemplateError(
                    number, f"relative link {url.strip()} is not in the repository"
                )


def point_relative_at_github(text: str) -> str:
    """Point each relative link at the same file on the GitHub repository."""

    def replace(match: re.Match[str]) -> str:
        path = repo_relative(match.group(1))
        if path is None:
            return match.group(0)
        href = f"]({match.group(1)})"
        head, _, tail = match.group(0).rpartition(href)
        return f"{head}]({GITHUB_BLOB}{path}){tail}"

    return MARKDOWN_LINK.sub(replace, text)


def modrinth_headings(text: str) -> str:
    """Turn each heading line into an HTML heading with its GitHub slug as the id."""
    slugger = GithubSlugger()
    rewritten: list[str] = []
    for line in text.splitlines(keepends=True):
        body = line.rstrip("\r\n")
        newline = line[len(body) :]
        if body.startswith("#"):
            level = len(body) - len(body.lstrip("#"))
            title = body.lstrip("#").strip()
            anchor = slugger.slug(title)
            body = f'<h{level} id="{anchor}">{title}</h{level}>'
        rewritten.append(body + newline)
    return "".join(rewritten)


def strip_leading_comment(text: str) -> tuple[str, int]:
    """Return the template body and the number of lines the opening comment used."""
    match = LEADING_COMMENT.match(text)
    if not match:
        raise TemplateError(1, "template must open with an HTML comment")
    removed = match.group(0)
    return text[match.end() :], removed.count("\n")


def render(
    text: str, platform: str, line_offset: int, links: dict[str, list[str]]
) -> tuple[str, list[tuple[int, str]]]:
    """Return this platform's page, and each kept line with its template line number."""
    kept: list[str] = []
    published: list[tuple[int, str]] = []
    allowed: set[str] | None = None
    open_line = 0
    for number, line in enumerate(text.splitlines(keepends=True), start=line_offset + 1):
        body = line.rstrip("\r\n")
        only = ONLY_LINE.fullmatch(body)
        if only:
            if allowed is not None:
                raise TemplateError(number, "only: is already open")
            allowed = allowed_names(only.group(1), number)
            open_line = number
            continue
        if END_LINE.fullmatch(body):
            if allowed is None:
                raise TemplateError(number, "end has no only:")
            allowed = None
            continue
        if allowed is None or platform in allowed:
            filled = fill_links(line, number, platform, links)
            if platform != "github":
                filled = flatten_alerts(filled)
            kept.append(filled)
            published.append((number, filled.rstrip("\r\n")))
    if allowed is not None:
        raise TemplateError(open_line, "only: has no end")
    return "".join(kept), published


def html(text: str) -> str:
    """Return the page as HTML, including Markdown that sits inside HTML tags."""
    marked = BLOCK_OPEN.sub(r'<\1\2 markdown="1">', text)
    return markdown.markdown(marked, extensions=["md_in_html", "toc"])


def stamp_value(template_mtime: int, script_mtime: int) -> str:
    """Return the stamp text: README.template.md's time, then this script's time."""
    return f"{template_mtime}\n{script_mtime}"


def up_to_date(template_mtime: int, script_mtime: int) -> bool:
    """Return whether every page is present and was rendered from these modification times."""
    if not STAMP.is_file() or any(not path.is_file() for path in OUTPUTS.values()):
        return False
    return STAMP.read_text(encoding="utf-8") == stamp_value(
        template_mtime, script_mtime
    )


def main() -> None:
    template_mtime = TEMPLATE.stat().st_mtime_ns
    script_mtime = SCRIPT.stat().st_mtime_ns
    if up_to_date(template_mtime, script_mtime):
        stamp = STAMP.relative_to(ROOT).as_posix()
        print(f"Readme pages are already up to date. Delete {stamp} to render again.")
        return
    with TEMPLATE.open(encoding="utf-8", newline="") as handle:
        text = handle.read()
    links, name_lines = parse_links(text)
    body, line_offset = strip_leading_comment(text)
    warn_unused(body, name_lines)
    rendered = {
        platform: render(body, platform, line_offset, links) for platform in OUTPUTS
    }
    for platform, (_, lines) in rendered.items():
        check_anchors(lines, platform)
        check_relative_links(lines)
    pages = {platform: page for platform, (page, _) in rendered.items()}
    pages["github"] = GITHUB_NOTE + "\n" + pages["github"]
    pages["modrinth"] = modrinth_headings(point_relative_at_github(pages["modrinth"]))
    pages["curseforge"] = html(
        point_anchors_at_github(point_relative_at_github(pages["curseforge"]))
    )
    for platform, path in OUTPUTS.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w", encoding="utf-8", newline="") as handle:
            handle.write(pages[platform])
    STAMP.write_text(stamp_value(template_mtime, script_mtime), encoding="utf-8")


if __name__ == "__main__":
    try:
        main()
    except TemplateError as error:
        print(error, file=sys.stderr)
        sys.exit(1)
