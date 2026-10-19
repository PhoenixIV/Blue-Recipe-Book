"""Pull reference sources into references/sources/ and write references/OVERVIEW.md.

Run from the repo root:

    py tools/pull_references.py
    py tools/pull_references.py --only jei emi
    py tools/pull_references.py --only minecraft
    py tools/pull_references.py --refresh

references/ is gitignored. The overview is rewritten on every run, so a missing
file means this script has not run yet.

If local/reference_sources.py exists, its EXTRA_SOURCES, EXTRA_WATCH, and
EXTRA_SKIPPED are appended. Nothing pulled here is copied into the mod.

Minecraft is not published as Java source, so those trees are built here.
minecraft-<version>-neoforge is the decompile this mod compiles against, at the
versions in gradle.properties. minecraft-<version>-vanilla is data/ and assets/,
copied rather than decompiled. When DOWNLOAD_MINECRAFT_26_3 is true, a later
version is decompiled in a throwaway project.

A tree is rebuilt only when its stamp no longer matches. --refresh rebuilds all
of them. --only minecraft checks only the Minecraft trees. A checkout already
at the remote tip, with a clean worktree, is left alone.
"""

from __future__ import annotations

import argparse
import json
import os
import runpy
import shutil
import subprocess
import sys
import tempfile
import zipfile
from collections.abc import Callable
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCES_DIR = ROOT / "references" / "sources"
OVERVIEW_PATH = ROOT / "references" / "OVERVIEW.md"
LOCAL_CATALOGUE = ROOT / "local" / "reference_sources.py"
PULL_WORKERS = 4


@dataclass(frozen=True)
class Source:
    slug: str
    title: str
    url: str
    branches: tuple[str, ...]
    license_name: str
    why: str
    start_here: tuple[str, ...]
    caution: str = ""


@dataclass(frozen=True)
class Watched:
    title: str
    url: str
    why: str


@dataclass
class PullResult:
    source: Source
    ok: bool
    branch: str = ""
    commit: str = ""
    committed_at: str = ""
    action: str = ""
    dest: Path | None = None
    start_here_found: list[str] = field(default_factory=list)
    start_here_missing: list[str] = field(default_factory=list)
    error: str = ""


def load_local_catalogue() -> dict[str, tuple]:
    """Return the local catalogue globals, with Source and Watched already bound, or {} when the file is absent."""
    if not LOCAL_CATALOGUE.is_file():
        return {}
    return runpy.run_path(str(LOCAL_CATALOGUE), init_globals={"Source": Source, "Watched": Watched})


# Projects this mod names, depends on, or tests against.
PUBLIC_SOURCES: tuple[Source, ...] = (
    Source(
        slug="architectury",
        title="Architectury API",
        url="https://github.com/architectury/architectury-api.git",
        branches=("1.21",),
        license_name="LGPL-3.0",
        why=(
            "Shared API for a mod that ships on both NeoForge and Fabric. "
            "The 1.21 branch is the 1.21.1 line; there is no 1.21.1 branch."
        ),
        start_here=("README.md", "common/src/main/java"),
        caution="Depend on this library. Do not copy it into the mod.",
    ),
    Source(
        slug="jei",
        title="Just Enough Items",
        url="https://github.com/mezz/JustEnoughItems.git",
        branches=("1.21.1",),
        license_name="MIT",
        why=(
            "Recipe pictures for modded stations come from JEI. This is the "
            "API for categories, drawable layouts, fluids, and moving items into the open grid."
        ),
        start_here=(
            "Common/src/api/java/mezz/jei/api/runtime/IJeiRuntime.java",
            "Common/src/api/java/mezz/jei/api/recipe/IRecipeManager.java",
            "Common/src/api/java/mezz/jei/api/gui/IRecipeLayoutDrawable.java",
            "Common/src/api/java/mezz/jei/api/runtime/IRecipesGui.java",
        ),
    ),
    Source(
        slug="emi",
        title="EMI",
        url="https://github.com/emilyploszaj/emi.git",
        branches=("1.21.1", "1.21"),
        license_name="MIT",
        why=(
            "The other common recipe viewer, with its own plugin API."
        ),
        start_here=(
            "xplat/src/main/java/dev/emi/emi/api/EmiPlugin.java",
            "xplat/src/main/java/dev/emi/emi/api/recipe",
        ),
    ),
    Source(
        slug="rei",
        title="Roughly Enough Items",
        url="https://github.com/shedaniel/RoughlyEnoughItems.git",
        branches=("16.x-1.21",),
        license_name="MIT (LICENSE also mentions bundled Auto Config under Apache-2.0)",
        why=(
            "Third recipe viewer. Branch 16.x-1.21 matches Minecraft 1.21.1. "
            "Later branches in this repo are newer game versions."
        ),
        start_here=(),
    ),
    Source(
        slug="create",
        title="Create",
        url="https://github.com/Creators-of-Create/Create.git",
        branches=("mc1.21.1/dev",),
        license_name="MIT for code; assets under src/main/resources/assets/ are All Rights Reserved",
        why=(
            "The first mod to test against. Branch mc1.21.1/dev is the 1.21.1 line; there is no branch named mc1.21.1. "
            "JEI exposes its categories when Create is installed. This checkout is for Create's own "
            "recipes, unlock advancements, and station registration."
        ),
        start_here=("README.md", "src/main/java/com/simibubi/create"),
        caution="Read it for behavior. Do not copy it into this mod.",
    ),
    Source(
        slug="inventorio",
        title="Inventorio",
        url="https://github.com/RubixDev/Inventorio.git",
        branches=("main",),
        license_name="No license file in the repository",
        why=(
            "Used to test the book with mods that change the default inventory. "
            "Its inventory screen is enlarged, so the blue book's placement has to adapt."
        ),
        start_here=("README.md", "src"),
        caution="Read it for behavior. Do not copy it into this mod.",
    ),
    Source(
        slug="yacl",
        title="YetAnotherConfigLib",
        url="https://github.com/isXander/YetAnotherConfigLib.git",
        branches=("lts",),
        license_name="LGPL-3.0-or-later",
        why=(
            "Config screen for both loaders. Branch lts is 3.8.2 and has versions/1.21.1-neoforge "
            "and versions/1.21.1-fabric. main is a later Minecraft line. "
            "The Java follows versions/current, which is 1.21.11 on this branch, so it uses later names such as Identifier."
        ),
        start_here=(
            "README.md",
            "src/main/java/dev/isxander/yacl3/api/YetAnotherConfigLib.java",
            "src/main/java/dev/isxander/yacl3/config/v2/api/ConfigClassHandler.java",
            "versions/1.21.1-neoforge/gradle.properties",
        ),
        caution="Depend on this library. Do not copy it into the mod.",
    ),
)

PUBLIC_WATCH: tuple[Watched, ...] = ()

PUBLIC_SKIPPED: tuple[tuple[str, str], ...] = (
    (
        "NeoForge and Fabric API",
        "Not cloned. NeoForge publishes its own API as a sources jar on maven.neoforged.net. "
        "That jar is the loader, and Gradle attaches it. The game classes are the Minecraft sections above. "
        "Fabric API is a large platform repo and a Gradle dependency. "
        "Architectury API is pulled above.",
    ),
)

_local = load_local_catalogue()
SOURCES: tuple[Source, ...] = PUBLIC_SOURCES + tuple(_local.get("EXTRA_SOURCES", ()))
WATCH: tuple[Watched, ...] = PUBLIC_WATCH + tuple(_local.get("EXTRA_WATCH", ()))
SKIPPED: tuple[tuple[str, str], ...] = PUBLIC_SKIPPED + tuple(_local.get("EXTRA_SKIPPED", ()))

MINECRAFT_SLUG = "minecraft"
STAMP_NAME = "STAMP.txt"
# A class the mod reads. Its presence means the extract finished.
MINECRAFT_CLASS = Path("net/minecraft/client/gui/screens/recipebook/RecipeBookComponent.java")
# Loom's client-extra jar holds these vanilla folders. They are copied as they are.
VANILLA_RESOURCE_PREFIXES = ("data/", "assets/")
VANILLA_RECIPE_DIR = Path("data/minecraft/recipe")

# Off until a port needs that tree. When False, the later tree is not built,
# described, or written into the overview.
DOWNLOAD_MINECRAFT_26_3 = False
# Newest 26.3 build in the NeoForge maven-metadata.xml on 2026-10-10.
LOOKUP_MINECRAFT_VERSION = "26.3"
LOOKUP_NEOFORGE_VERSION = "26.3.0.64-beta"
# Newest net.neoforged:moddev-gradle on maven.neoforged.net that same day.
LOOKUP_MODDEV_VERSION = "2.0.148"


# ─── Git ──────────────────────────────────────────────────────────────────────


def git(*args: str, cwd: Path | None = None) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", *args],
        cwd=cwd,
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )


def require_git() -> None:
    if git("--version").returncode != 0:
        sys.exit("git is not available on PATH.")


def web_url(url: str) -> str:
    """Return the page a reader opens. A GitHub wiki clones from <repo>.wiki.git and is read at <repo>/wiki."""
    page = url.removesuffix(".git")
    if page.endswith(".wiki"):
        return page.removesuffix(".wiki") + "/wiki"
    return page


def remote_tips(url: str, branches: tuple[str, ...]) -> dict[str, str]:
    refs = [f"refs/heads/{branch}" for branch in branches]
    listed = git("ls-remote", url, *refs)
    if listed.returncode != 0:
        raise RuntimeError(listed.stderr.strip() or f"ls-remote failed for {url}")
    heads: dict[str, str] = {}
    prefix = "refs/heads/"
    for line in listed.stdout.splitlines():
        sha, ref = line.split("\t", 1)
        if ref.startswith(prefix):
            heads[ref[len(prefix) :]] = sha
    return heads


def choose_branch(source: Source, heads: dict[str, str]) -> str:
    for branch in source.branches:
        if branch in heads:
            return branch
    raise RuntimeError(f"none of the wanted branches exist: {', '.join(source.branches)}")


def local_head(dest: Path) -> str | None:
    if not (dest / ".git").exists():
        return None
    sha = git("rev-parse", "HEAD", cwd=dest)
    return sha.stdout.strip() if sha.returncode == 0 else None


def worktree_dirty(dest: Path) -> bool:
    status = git("status", "--porcelain", cwd=dest)
    if status.returncode != 0:
        raise RuntimeError(status.stderr.strip() or "git status failed")
    return bool(status.stdout.strip())


def clone_or_update(source: Source, branch: str, remote_sha: str, dest: Path) -> str:
    if not dest.exists():
        dest.parent.mkdir(parents=True, exist_ok=True)
        cloned = git("clone", "--depth", "1", "--single-branch", "--branch", branch, source.url, str(dest))
        if cloned.returncode != 0:
            raise RuntimeError(cloned.stderr.strip() or "git clone failed")
        return "cloned"

    if not (dest / ".git").exists():
        raise RuntimeError(f"{dest} exists but is not a git checkout")

    # ls-remote already returned this tip, so a clean checkout needs no fetch.
    if local_head(dest) == remote_sha and not worktree_dirty(dest):
        return "up to date"

    for step, args in (
        ("fetch", ("fetch", "--depth", "1", "origin", branch)),
        ("checkout", ("checkout", branch)),
        ("reset", ("reset", "--hard", f"origin/{branch}")),
    ):
        done = git(*args, cwd=dest)
        if done.returncode != 0:
            raise RuntimeError(done.stderr.strip() or f"git {step} failed")
    return "updated"


def head_info(dest: Path) -> tuple[str, str]:
    sha = git("rev-parse", "--short", "HEAD", cwd=dest)
    when = git("log", "-1", "--format=%cI", cwd=dest)
    if sha.returncode != 0 or when.returncode != 0:
        raise RuntimeError("could not read HEAD")
    return sha.stdout.strip(), when.stdout.strip()


def describe_checkout(source: Source, dest: Path, branch: str, commit: str, committed_at: str) -> PullResult:
    found = [hint for hint in source.start_here if (dest / hint).exists()]
    missing = [hint for hint in source.start_here if not (dest / hint).exists()]
    return PullResult(
        source=source,
        ok=True,
        branch=branch,
        commit=commit,
        committed_at=committed_at,
        dest=dest,
        start_here_found=found,
        start_here_missing=missing,
    )


def pull_one(source: Source) -> PullResult:
    dest = SOURCES_DIR / source.slug
    try:
        heads = remote_tips(source.url, source.branches)
        branch = choose_branch(source, heads)
        action = clone_or_update(source, branch, heads[branch], dest)
        commit, committed_at = head_info(dest)
    except RuntimeError as error:
        return PullResult(source=source, ok=False, dest=dest, error=str(error))
    result = describe_checkout(source, dest, branch, commit, committed_at)
    result.action = action
    return result


def existing_checkout(source: Source) -> PullResult:
    dest = SOURCES_DIR / source.slug
    if not (dest / ".git").exists():
        return PullResult(source=source, ok=False, dest=dest, error="not pulled yet")
    try:
        commit, committed_at = head_info(dest)
        branch = git("rev-parse", "--abbrev-ref", "HEAD", cwd=dest)
        if branch.returncode != 0:
            raise RuntimeError("could not read the checked-out branch")
    except RuntimeError as error:
        return PullResult(source=source, ok=False, dest=dest, error=str(error))
    return describe_checkout(source, dest, branch.stdout.strip(), commit, committed_at)


def display_path(path: Path) -> str:
    try:
        return path.relative_to(ROOT).as_posix()
    except ValueError:
        return path.as_posix()


# ─── Properties and gradle ────────────────────────────────────────────────────


def read_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.is_file():
        return values
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def gradle_versions() -> tuple[str, str]:
    props = read_properties(ROOT / "gradle.properties")
    minecraft = props.get("minecraft_version", "")
    neoforge = props.get("neoforge_version", "")
    if not minecraft or not neoforge:
        raise RuntimeError("gradle.properties is missing minecraft_version or neoforge_version")
    return minecraft, neoforge


def gradle_wrapper() -> Path:
    name = "gradlew.bat" if sys.platform == "win32" else "gradlew"
    path = ROOT / name
    if not path.is_file():
        raise RuntimeError(f"{name} is not in the repo root")
    return path


def gradle_user_home() -> Path:
    """Return a Gradle home that already contains this wrapper. An empty GRADLE_USER_HOME would re-download Gradle and miss the remap cache in the user home."""
    props = read_properties(ROOT / "gradle" / "wrapper" / "gradle-wrapper.properties")
    dist_name = Path(props.get("distributionUrl", "").replace("\\", "/")).name.removesuffix(".zip")
    default = Path.home() / ".gradle"
    candidates = [default]
    env_home = os.environ.get("GRADLE_USER_HOME")
    if env_home and Path(env_home) not in candidates:
        candidates.append(Path(env_home))
    for home in candidates:
        if any((home / "wrapper" / "dists" / dist_name).glob("*/gradle-*/bin/gradle*")):
            return home
    return default


def gradle_env(extra: dict[str, str] | None = None) -> dict[str, str]:
    env = os.environ.copy()
    env["GRADLE_USER_HOME"] = str(gradle_user_home())
    env.update(extra or {})
    return env


SOURCES_RECORD = """\
def sourcesMarker = new File(gradle.startParameter.projectProperties["bluerecipebook.minecraftSourcesFile"])

gradle.taskGraph.whenReady { graph ->
    def matches = graph.allTasks.findAll { task ->
        task.project.path == ":neoforge" && task.hasProperty("sourcesOutputJar")
    }
    if (matches.size() != 1) {
        throw new GradleException("`:neoforge:genSources` must schedule exactly one Loom sources task, found: " + matches.collect { it.path }.join(", "))
    }
    sourcesMarker.setText(matches[0].sourcesOutputJar.get().asFile.absolutePath, "UTF-8")
}
"""

CLIENT_EXTRA_RECORD = """\
def clientExtraMarker = new File(gradle.startParameter.projectProperties["bluerecipebook.clientExtraFile"])

gradle.taskGraph.whenReady { graph ->
    def neoforge = gradle.rootProject.findProject(':neoforge')
    if (neoforge == null) {
        throw new GradleException("no :neoforge project")
    }
    def hits = [] as Set
    neoforge.configurations.each { conf ->
        if (!conf.canBeResolved) {
            return
        }
        try {
            conf.files.each { f ->
                if (f.name.startsWith('client-extra') && f.name.endsWith('.jar')) {
                    hits << f.absolutePath
                }
            }
        } catch (Exception ignored) {
        }
    }
    if (hits.size() != 1) {
        throw new GradleException("expected one client-extra jar in :neoforge, found: " + hits)
    }
    clientExtraMarker.setText(hits.first(), "UTF-8")
}
"""


def read_recorded_jar(marker: Path, task: str) -> Path:
    if not marker.is_file():
        raise RuntimeError(f"`{task}` finished without recording {marker.name}")
    jar = Path(marker.read_text(encoding="utf-8-sig").strip())
    if not jar.is_file():
        raise RuntimeError(f"`{task}` recorded a missing jar: {jar}")
    return jar


def run_neoforge_gradle(task: str, want_sources: bool) -> tuple[Path | None, Path]:
    """Return the client-extra jar, and the sources jar when `want_sources` is true.

    Args:
        task: Either `:neoforge:genSources`, when the sources jar is wanted, or `:neoforge:help`.

    Returns:
        The sources jar or None, then the client-extra jar.
    """
    temp = Path(tempfile.mkdtemp())
    try:
        markers = [("bluerecipebook.clientExtraFile", temp / "client-extra.txt")]
        snippets = [CLIENT_EXTRA_RECORD]
        if want_sources:
            markers.insert(0, ("bluerecipebook.minecraftSourcesFile", temp / "minecraft-sources.txt"))
            snippets.insert(0, SOURCES_RECORD)
        script = temp / "records.init.gradle"
        script.write_text("\n".join(snippets), encoding="utf-8")
        command = [
            str(gradle_wrapper()),
            task,
            "--init-script",
            str(script),
            "--no-daemon",
            "--console=plain",
            *(f"-P{name}={path.as_posix()}" for name, path in markers),
        ]
        completed = subprocess.run(command, cwd=ROOT, env=gradle_env(), check=False)
        if completed.returncode != 0:
            raise RuntimeError(f"`{task}` failed")
        jars = {name: read_recorded_jar(path, task) for name, path in markers}
        return jars.get("bluerecipebook.minecraftSourcesFile"), jars["bluerecipebook.clientExtraFile"]
    finally:
        shutil.rmtree(temp, ignore_errors=True)


LOOKUP_SETTINGS = """\
pluginManagement {
    repositories {
        maven { url = "https://maven.neoforged.net/releases/" }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "minecraft-lookup"
"""

LOOKUP_SOURCES_INIT = """\
def marker = new File(gradle.startParameter.projectProperties["bluerecipebook.minecraftSourcesFile"])

gradle.taskGraph.whenReady { graph ->
    def matches = graph.allTasks.findAll { task ->
        task.name == "createMinecraftArtifacts" && task.hasProperty("gameSourcesArtifact")
    }
    if (matches.size() != 1) {
        throw new GradleException("`createMinecraftArtifacts` must schedule exactly one sources jar")
    }
    marker.setText(matches[0].gameSourcesArtifact.get().asFile.absolutePath, "UTF-8")
}
"""


def lookup_build_script(neoforge_version: str) -> str:
    return f"""\
plugins {{
    id "net.neoforged.moddev" version "{LOOKUP_MODDEV_VERSION}"
}}

neoForge {{
    version = "{neoforge_version}"
}}
"""


def installed_java_home() -> Path:
    completed = subprocess.run(
        ["java", "-XshowSettings:properties", "-version"],
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    props: dict[str, str] = {}
    for line in (completed.stderr + "\n" + completed.stdout).splitlines():
        if "=" in line:
            key, value = (part.strip() for part in line.split("=", 1))
            props[key] = value
    home, version = props.get("java.home", ""), props.get("java.version", "")
    if not home or not version:
        raise RuntimeError("could not read java.home from `java` on PATH")
    if not version.split(".", 1)[0].isdigit() or int(version.split(".", 1)[0]) < 25:
        raise RuntimeError(f"Minecraft {LOOKUP_MINECRAFT_VERSION} decompile needs Java 25. `java` on PATH is {version}.")
    if not Path(home).is_dir():
        raise RuntimeError(f"java.home is not a directory: {home}")
    return Path(home)


def write_lookup_project(project: Path, neoforge_version: str, java_home: Path) -> None:
    wrapper_dir = project / "gradle" / "wrapper"
    wrapper_dir.mkdir(parents=True, exist_ok=True)
    for name in ("gradle-wrapper.jar", "gradle-wrapper.properties"):
        shutil.copy2(ROOT / "gradle" / "wrapper" / name, wrapper_dir / name)
    for name in ("gradlew", "gradlew.bat"):
        if (ROOT / name).is_file():
            shutil.copy2(ROOT / name, project / name)
    (project / "settings.gradle").write_text(LOOKUP_SETTINGS, encoding="utf-8")
    (project / "build.gradle").write_text(lookup_build_script(neoforge_version), encoding="utf-8")
    (project / "gradle.properties").write_text(
        "org.gradle.jvmargs=-Xmx4G -Djava.net.preferIPv4Stack=true\n"
        "org.gradle.parallel=false\n"
        "org.gradle.configuration-cache=false\n"
        "org.gradle.java.installations.auto-download=false\n"
        f"org.gradle.java.installations.paths={java_home.as_posix()}\n",
        encoding="utf-8",
    )


def run_lookup_gradle(neoforge_version: str) -> Path:
    java_home = installed_java_home()
    project = Path(tempfile.mkdtemp(prefix="bluerecipebook-minecraft-lookup-"))
    init_dir = Path(tempfile.mkdtemp(prefix="bluerecipebook-minecraft-lookup-init-"))
    try:
        write_lookup_project(project, neoforge_version, java_home)
        init_script = init_dir / "minecraft-sources.init.gradle"
        init_script.write_text(LOOKUP_SOURCES_INIT, encoding="utf-8")
        marker = init_dir / "minecraft-sources.txt"
        wrapper = project / ("gradlew.bat" if sys.platform == "win32" else "gradlew")
        completed = subprocess.run(
            [
                str(wrapper),
                "createMinecraftArtifacts",
                "--no-daemon",
                "--init-script",
                str(init_script),
                f"-Pbluerecipebook.minecraftSourcesFile={marker.as_posix()}",
                "--console=plain",
            ],
            cwd=project,
            env=gradle_env({"JAVA_HOME": str(java_home)}),
            check=False,
        )
        if completed.returncode != 0:
            raise RuntimeError("`createMinecraftArtifacts` failed")
        return read_recorded_jar(marker, "createMinecraftArtifacts")
    finally:
        shutil.rmtree(project, ignore_errors=True)
        shutil.rmtree(init_dir, ignore_errors=True)


# ─── Minecraft Trees ──────────────────────────────────────────────────────────


@dataclass(frozen=True)
class TreeSpec:
    role: str
    dest: Path
    minecraft_version: str
    neoforge_version: str  # empty for the vanilla tree, which has no NeoForge decompile
    present: Path          # proves the extract finished
    expect_dir: bool


@dataclass
class TreeResult:
    spec: TreeSpec
    action: str
    error: str = ""

    @property
    def ok(self) -> bool:
        return not self.error


def stamp_values(spec: TreeSpec) -> dict[str, str]:
    values = {"minecraft_version": spec.minecraft_version}
    if spec.neoforge_version:
        values["neoforge_version"] = spec.neoforge_version
    return values


def tree_is_current(spec: TreeSpec) -> bool:
    stamp = read_properties(spec.dest / STAMP_NAME)
    if any(stamp.get(key) != value for key, value in stamp_values(spec).items()):
        return False
    target = spec.dest / spec.present
    return target.is_dir() if spec.expect_dir else target.is_file()


def verify_minecraft_jar(jar: Path, minecraft_version: str) -> None:
    with zipfile.ZipFile(jar) as archive:
        if "version.json" not in archive.namelist():
            raise RuntimeError(f"{jar.name} has no version.json")
        found = json.loads(archive.read("version.json").decode("utf-8")).get("id", "")
    if found != minecraft_version:
        raise RuntimeError(f"{jar.name} is Minecraft {found}, not {minecraft_version}")


def extract_entries(
    jar: Path,
    dest: Path,
    wanted: Callable[[str], bool],
    present: Path,
    expect_dir: bool,
) -> int:
    """Replace dest with the wanted jar entries only when `present` is in the extract.

    Args:
        present: Path relative to dest that must exist once the copy has finished.

    Returns:
        The number of files written.

    Raises:
        RuntimeError: No entry matched, or `present` is not in the extract.
    """
    staging = dest.parent / f"{dest.name}.partial"
    if staging.exists():
        shutil.rmtree(staging)
    staging.mkdir(parents=True)
    count = 0
    try:
        with zipfile.ZipFile(jar) as archive:
            for info in archive.infolist():
                name = info.filename.replace("\\", "/")
                if info.is_dir() or not wanted(name):
                    continue
                parts = Path(name).parts
                if name.startswith("/") or Path(name).anchor or ".." in parts:
                    continue
                target = staging.joinpath(*parts)
                target.parent.mkdir(parents=True, exist_ok=True)
                with archive.open(info) as src, target.open("wb") as out:
                    shutil.copyfileobj(src, out)
                count += 1
        found = (staging / present).is_dir() if expect_dir else (staging / present).is_file()
        if count == 0 or not found:
            raise RuntimeError(f"{jar.name} has no {present.as_posix()}")
        if dest.exists():
            shutil.rmtree(dest)
        staging.rename(dest)
    except Exception:
        shutil.rmtree(staging, ignore_errors=True)
        raise
    return count


def install_tree(spec: TreeSpec, jar: Path, wanted: Callable[[str], bool]) -> int:
    count = extract_entries(jar, spec.dest, wanted, spec.present, spec.expect_dir)
    spec.dest.mkdir(parents=True, exist_ok=True)
    text = "".join(f"{key}={value}\n" for key, value in stamp_values(spec).items())
    (spec.dest / STAMP_NAME).write_text(text, encoding="utf-8")
    return count


BUILD_ERRORS = (RuntimeError, OSError, zipfile.BadZipFile)


def ensure_minecraft(minecraft_version: str, neoforge_version: str, refresh: bool, build: bool) -> dict[str, TreeResult]:
    """Return the compile and vanilla trees, extracting only when `build` is true."""
    specs = {
        "compile": TreeSpec(
            "compile",
            SOURCES_DIR / f"minecraft-{minecraft_version}-neoforge",
            minecraft_version,
            neoforge_version,
            MINECRAFT_CLASS,
            False,
        ),
        "vanilla": TreeSpec(
            "vanilla",
            SOURCES_DIR / f"minecraft-{minecraft_version}-vanilla",
            minecraft_version,
            "",
            VANILLA_RECIPE_DIR,
            True,
        ),
    }
    stale = [role for role, spec in specs.items() if refresh or not tree_is_current(spec)]
    current_action = "up to date" if build else "present"
    trees = {
        role: TreeResult(spec, current_action)
        for role, spec in specs.items()
        if role not in stale
    }
    if not stale:
        return trees
    if not build:
        for role in stale:
            trees[role] = TreeResult(specs[role], "missing", "not extracted yet")
        return trees

    task = ":neoforge:genSources" if "compile" in stale else ":neoforge:help"
    print(f"Minecraft {minecraft_version}: running {task} for {', '.join(stale)}.", flush=True)
    try:
        sources_jar, client_extra = run_neoforge_gradle(task, want_sources="compile" in stale)
    except RuntimeError as error:
        for role in stale:
            trees[role] = TreeResult(specs[role], "failed", str(error))
        return trees

    for role in stale:
        spec = specs[role]
        try:
            if role == "compile":
                assert sources_jar is not None
                count = install_tree(spec, sources_jar, lambda name: name.endswith(".java"))
            else:
                verify_minecraft_jar(client_extra, minecraft_version)
                count = install_tree(spec, client_extra, lambda name: name.startswith(VANILLA_RESOURCE_PREFIXES))
        except BUILD_ERRORS as error:
            trees[role] = TreeResult(spec, "failed", str(error))
            continue
        trees[role] = TreeResult(spec, "extracted")
        print(f"  extracted {count} files for {spec.dest.name}", flush=True)
    return trees


def ensure_lookup(refresh: bool, build: bool) -> TreeResult:
    spec = TreeSpec(
        "lookup",
        SOURCES_DIR / f"minecraft-{LOOKUP_MINECRAFT_VERSION}-neoforge",
        LOOKUP_MINECRAFT_VERSION,
        LOOKUP_NEOFORGE_VERSION,
        MINECRAFT_CLASS,
        False,
    )
    if not refresh and tree_is_current(spec):
        return TreeResult(spec, "up to date" if build else "present")
    if not build:
        return TreeResult(spec, "missing", "not extracted yet")
    print(f"Minecraft {LOOKUP_MINECRAFT_VERSION}: running createMinecraftArtifacts in an isolated project.", flush=True)
    try:
        jar = run_lookup_gradle(LOOKUP_NEOFORGE_VERSION)
        count = install_tree(spec, jar, lambda name: name.endswith(".java"))
    except BUILD_ERRORS as error:
        return TreeResult(spec, "failed", str(error))
    print(f"  extracted {count} files for {spec.dest.name}", flush=True)
    return TreeResult(spec, "extracted")


# ─── Overview ─────────────────────────────────────────────────────────────────


def tree_lines(tree: TreeResult, refresh_note: str, look_up: str = "", crash_note: bool = False) -> list[str]:
    spec = tree.spec
    lines: list[str] = []
    if spec.neoforge_version:
        lines.append(f"- Version: Minecraft {spec.minecraft_version}, NeoForge {spec.neoforge_version}")
    lines.append(f"- Path: `{display_path(spec.dest)}`")
    if not tree.ok:
        lines.append(f"- Status: {tree.error}")
        lines.append("- Run `py tools/pull_references.py` to build it.")
        return [*lines, ""]
    if look_up:
        lines.append(look_up)
    if crash_note:
        lines.append("- A crash line from the running game does not match these files. Read the exception text, then the Java file.")
    lines.append(refresh_note)
    return [*lines, ""]


def minecraft_section(trees: dict[str, TreeResult]) -> list[str]:
    compile_tree = trees["compile"]
    vanilla_tree = trees["vanilla"]
    later = trees.get("lookup")
    version = compile_tree.spec.minecraft_version

    lines = ["## Minecraft", ""]
    if later is not None:
        lines += ["Two snapshots. Both are gitignored.", ""]
    lines.append(
        f"- Minecraft {version}, NeoForge {compile_tree.spec.neoforge_version}, "
        f"`{display_path(compile_tree.spec.dest)}`. "
        "This is the decompile this mod compiles against. Open classes here when changing the mod."
    )
    if later is not None:
        lines.append(
            f"- Minecraft {later.spec.minecraft_version}, NeoForge {later.spec.neoforge_version}, "
            f"`{display_path(later.spec.dest)}`. "
            "For a future port. Do not open it while writing a 1.21.1 feature, and do not copy its signatures into `common`."
        )
        lines += ["", "`ResourceLocation` is still the id type on 1.21.1. `Identifier` and `RecipeDisplayId` are 26.3 names.", ""]
    lines += [""]

    lines += [
        f"### Minecraft {version}",
        "",
        "Mojang does not publish this game version as Java source. NeoForge's sources jar is the loader, not the game. "
        "This tree is Loom's `:neoforge:genSources` decompile of the mapped jar this mod compiles against, "
        "the same decompile an IDE attaches on import.",
        "",
    ]
    lines += tree_lines(
        compile_tree,
        refresh_note=(
            "- Refresh: `py tools/pull_references.py` leaves this tree alone while `minecraft_version` and "
            "`neoforge_version` in `gradle.properties` are unchanged. `--refresh` builds it again."
        ),
        look_up=f"- Open a class by name: `{display_path(compile_tree.spec.dest / MINECRAFT_CLASS)}`",
        crash_note=True,
    )

    lines += [
        f"### Minecraft {version} vanilla resources",
        "",
        "Vanilla `data/` and `assets/` from NeoForge's client-extra artifact, which Gradle resolves. "
        "It holds the same bytes as Mojang's client jar. Recipes, tags, advancements, textures, models, and lang files are here. "
        "They are plain files, not decompiled.",
        "",
    ]
    lines += tree_lines(
        vanilla_tree,
        refresh_note=(
            "- Refresh: `py tools/pull_references.py` leaves this folder alone while `minecraft_version` in "
            "`gradle.properties` is unchanged. `--refresh` copies it again."
        ),
        look_up=f"- Example: `{display_path(vanilla_tree.spec.dest / VANILLA_RECIPE_DIR / 'beacon.json')}`",
    )

    if later is not None:
        lines += [
            f"### Minecraft {later.spec.minecraft_version}",
            "",
            "For a future port, not for feature work on 1.21.1. "
            "Do not copy its signatures into `common`. Copying a 26.3 signature into this tree will not compile.",
            "",
            "This tree is ModDevGradle's `createMinecraftArtifacts` decompile of the mapped game, unpacked from "
            "`gameSourcesArtifact`. The Gradle project is a temp directory. It does not run `:neoforge:genSources` "
            "in this repo, and it does not change `gradle.properties` or the 1.21.1 tree.",
            "",
        ]
        lines += tree_lines(
            later,
            refresh_note=(
                "- Refresh: `py tools/pull_references.py` leaves this tree alone while the NeoForge version in "
                "`tools/pull_references.py` is unchanged. `--refresh` builds it again. That rebuild does not change "
                "`gradle.properties` or the 1.21.1 tree."
            ),
            look_up=f"- Open a class by name: `{display_path(later.spec.dest / MINECRAFT_CLASS)}`",
        )
    return lines


def write_overview(results: list[PullResult], trees: dict[str, TreeResult]) -> None:
    pulled_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    compile_version = trees["compile"].spec.minecraft_version
    version_line = f"This mod compiles against Minecraft {compile_version}."
    if "lookup" in trees:
        version_line += (
            f" Minecraft {trees['lookup'].spec.minecraft_version} is a second decompile "
            "for a future port, not for feature work."
        )

    lines: list[str] = [
        "# Reference snapshots",
        "",
        "Local copies of other projects, for lookup while building this mod.",
        "Read this file first. Open a path below only when you need that API or that mod's approach.",
        "",
        "`references/` is gitignored. This file is written by "
        "`py tools/pull_references.py`. If it is missing, run that from the repo root.",
        "",
        f"Last pull: {pulled_at}. Refresh with `py tools/pull_references.py`.",
        "",
        "These copies are not dependencies and are not vendored into the mod.",
        version_line,
        "",
        *minecraft_section(trees),
        "## Pulled",
        "",
    ]

    for result in results:
        source = result.source
        lines += [
            f"### {source.title}",
            "",
            source.why,
            "",
            f"- License: {source.license_name}",
            f"- Remote: {web_url(source.url)}",
        ]
        if source.caution:
            lines.append(f"- Caution: {source.caution}")
        if not result.ok:
            lines += [f"- Status: not pulled ({result.error})", ""]
            continue
        assert result.dest is not None
        lines += [
            f"- Status: pulled `{result.branch}` at `{result.commit}` ({result.committed_at})",
            f"- Path: `{display_path(result.dest)}`",
        ]
        if result.start_here_found:
            lines.append("- Start here:")
            lines += [f"  - `{display_path(result.dest / hint)}`" for hint in result.start_here_found]
        if result.start_here_missing:
            lines.append("- Expected paths that were not in this checkout:")
            lines += [f"  - `{hint}`" for hint in result.start_here_missing]
        lines.append("")

    if WATCH:
        lines += [
            "## Watch",
            "",
            "Related projects that stay on the web. They are not cloned.",
            "Look again only if the question named here comes up.",
            "",
        ]
        for watched in WATCH:
            lines += [f"### {watched.title}", "", watched.why, "", f"- Remote: {web_url(watched.url)}", ""]

    if SKIPPED:
        lines += ["## Not pulled", ""]
        for title, reason in SKIPPED:
            lines += [f"### {title}", "", reason, ""]

    OVERVIEW_PATH.parent.mkdir(parents=True, exist_ok=True)
    OVERVIEW_PATH.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")


# ─── Main ─────────────────────────────────────────────────────────────────────


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Pull reference sources and write references/OVERVIEW.md.")
    parser.add_argument(
        "--only",
        nargs="+",
        metavar="SLUG",
        help="Pull only these slugs. `minecraft` checks the Minecraft trees and rebuilds only the stale ones. "
        "Other trees are left as they are.",
    )
    parser.add_argument(
        "--refresh",
        action="store_true",
        help="Rebuild the Minecraft trees even when their stamps match.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    require_git()
    try:
        minecraft_version, neoforge_version = gradle_versions()
    except RuntimeError as error:
        sys.exit(str(error))

    known = {source.slug for source in SOURCES} | {MINECRAFT_SLUG}
    if args.only:
        unknown = [slug for slug in args.only if slug not in known]
        if unknown:
            sys.exit(f"Unknown slug(s): {', '.join(unknown)}. Known: {', '.join(sorted(known))}")
        wanted = set(args.only)
    else:
        wanted = known
    selected = tuple(source for source in SOURCES if source.slug in wanted)
    include_minecraft = MINECRAFT_SLUG in wanted

    with ThreadPoolExecutor(max_workers=PULL_WORKERS) as pool:
        pulled = {result.source.slug: result for result in pool.map(pull_one, selected)}
    results = [
        pulled[source.slug] if source.slug in pulled else existing_checkout(source)
        for source in SOURCES
    ]

    trees = ensure_minecraft(minecraft_version, neoforge_version, args.refresh, build=include_minecraft)
    if DOWNLOAD_MINECRAFT_26_3:
        trees["lookup"] = ensure_lookup(args.refresh, build=include_minecraft)

    write_overview(results, trees)
    print(f"Wrote {display_path(OVERVIEW_PATH)}")
    for result in results:
        if result.source.slug not in wanted:
            continue
        if result.ok:
            print(f"  ok   {result.source.slug} ({result.branch} {result.commit}, {result.action})")
        else:
            print(f"  fail {result.source.slug}: {result.error}")
    if include_minecraft:
        for tree in trees.values():
            if tree.ok:
                print(f"  ok   {tree.spec.dest.name} ({tree.action})")
            else:
                print(f"  fail {tree.spec.dest.name}: {tree.error}")

    failed = any(not result.ok for result in results if result.source.slug in wanted)
    minecraft_failed = include_minecraft and any(not tree.ok for tree in trees.values())
    return 1 if failed or minecraft_failed else 0


if __name__ == "__main__":
    sys.exit(main())
