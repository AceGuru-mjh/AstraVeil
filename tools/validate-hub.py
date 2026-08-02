#!/usr/bin/env python3
"""
AstraHub 索引验证工具

用法:
    python3 tools/validate-hub.py astrahub/modules/index.json
    python3 tools/validate-hub.py --schema astrahub/schema/module-index.schema.json --index astrahub/modules/index.json
    python3 tools/validate-hub.py --all   # 验证所有 schema + index
    python3 tools/validate-hub.py --all --strict   # 严格模式（警告视为错误）

功能:
    1. JSON Schema 验证 (draft-07)
    2. 语义验证（交叉引用、版本一致性）
    3. 安全验证（签名格式、URL 协议）
    4. 输出报告（人类可读 + 退出码）

依赖:
    pip install jsonschema semver
"""

import argparse
import json
import re
import sys
from datetime import datetime
from pathlib import Path
from typing import Any

try:
    import jsonschema
    from jsonschema import Draft7Validator
    HAS_JSONSCHEMA = True
except ImportError:
    HAS_JSONSCHEMA = False

try:
    import semver  # noqa: F401  (optional, for future version-range checks)
    HAS_SEMVER = True
except ImportError:
    HAS_SEMVER = False


# ─────────────────────────────────────────────────────────────────────────────
# 颜色输出
# ─────────────────────────────────────────────────────────────────────────────

class Colors:
    RED = "\033[0;31m"
    GREEN = "\033[0;32m"
    YELLOW = "\033[1;33m"
    BLUE = "\033[0;34m"
    NC = "\033[0m"

    @classmethod
    def disable(cls):
        cls.RED = cls.GREEN = cls.YELLOW = cls.BLUE = cls.NC = ""


# ─────────────────────────────────────────────────────────────────────────────
# 路径
# ─────────────────────────────────────────────────────────────────────────────

ROOT = Path(__file__).resolve().parent.parent
HUB_DIR = ROOT / "astrahub"
SCHEMA_DIR = HUB_DIR / "schema"
DEFAULT_INDEX = HUB_DIR / "modules" / "index.json"

SCHEMAS = {
    "module-index": SCHEMA_DIR / "module-index.schema.json",
    "module-package": SCHEMA_DIR / "module-package.schema.json",
    "repo-manifest": SCHEMA_DIR / "repo-manifest.schema.json",
}


# ─────────────────────────────────────────────────────────────────────────────
# 报告
# ─────────────────────────────────────────────────────────────────────────────

class Report:
    def __init__(self, strict: bool = False):
        self.errors: list[str] = []
        self.warnings: list[str] = []
        self.strict = strict

    def ok(self, msg: str) -> None:
        print(f"  {Colors.GREEN}✓{Colors.NC} {msg}")

    def warn(self, msg: str) -> None:
        print(f"  {Colors.YELLOW}⚠{Colors.NC} {msg}")
        self.warnings.append(msg)
        if self.strict:
            self.errors.append(msg)

    def err(self, msg: str) -> None:
        print(f"  {Colors.RED}✗{Colors.NC} {msg}")
        self.errors.append(msg)

    @property
    def failed(self) -> bool:
        return bool(self.errors)

    def summary(self) -> int:
        print(f"\n{Colors.BLUE}── Summary ──{Colors.NC}")
        print(f"  Errors:   {len(self.errors)}")
        print(f"  Warnings: {len(self.warnings)}")
        if self.failed:
            print(f"{Colors.RED}FAILED{Colors.NC}")
        else:
            print(f"{Colors.GREEN}PASSED{Colors.NC}")
        return 1 if self.failed else 0


# ─────────────────────────────────────────────────────────────────────────────
# 辅助
# ─────────────────────────────────────────────────────────────────────────────

def load_json(path: Path) -> Any:
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def is_iso8601(value: str) -> bool:
    try:
        datetime.fromisoformat(value.replace("Z", "+00:00"))
        return True
    except Exception:
        return False


# ─────────────────────────────────────────────────────────────────────────────
# 验证步骤
# ─────────────────────────────────────────────────────────────────────────────

def validate_schema_files(rpt: Report) -> None:
    """验证所有 schema 文件本身是合法的 JSON Schema (draft-07)。"""
    print(f"\n{Colors.BLUE}── Schema 文件合法性 ──{Colors.NC}")
    if not HAS_JSONSCHEMA:
        rpt.warn("jsonschema 未安装 — 跳过 schema 元验证")
        return
    for name, path in SCHEMAS.items():
        if not path.exists():
            rpt.err(f"schema 文件缺失: {path}")
            continue
        try:
            schema = load_json(path)
            Draft7Validator.check_schema(schema)
            rpt.ok(f"{name}: 合法 JSON Schema (draft-07)")
        except Exception as e:
            rpt.err(f"{name}: schema 无效 — {e}")


def validate_index_schema(index: Any, schema_path: Path, rpt: Report) -> None:
    """验证 index.json 符合 module-index.schema.json。"""
    print(f"\n{Colors.BLUE}── Index Schema 验证 ──{Colors.NC}")
    if not HAS_JSONSCHEMA:
        rpt.warn("jsonschema 未安装 — 跳过 schema 验证")
        return
    if not schema_path.exists():
        rpt.err(f"module-index schema 缺失: {schema_path}")
        return
    schema = load_json(schema_path)
    validator = Draft7Validator(schema)
    errors = sorted(validator.iter_errors(index), key=lambda e: list(e.path))
    if not errors:
        rpt.ok(f"index.json 符合 {schema_path.name}")
        return
    for err in errors:
        loc = "/".join(str(p) for p in err.path) or "<root>"
        rpt.err(f"schema: {loc}: {err.message}")


def semantic_checks(index: Any, rpt: Report) -> None:
    """交叉引用 / 版本一致性 / 安全验证。"""
    print(f"\n{Colors.BLUE}── 语义验证 ──{Colors.NC}")

    repo = index.get("repository", {})
    base_url = repo.get("base_url", "")
    if base_url and not base_url.startswith("https://"):
        rpt.err(f"repository.base_url 必须为 https:// (实际: {base_url})")
    else:
        rpt.ok("repository.base_url 使用 https")

    if not repo.get("public_key"):
        rpt.warn("repository.public_key 未设置（签名验证将无法进行）")

    updated_at = index.get("updated_at")
    if updated_at and not is_iso8601(updated_at):
        rpt.err(f"index.updated_at 不是 ISO 8601: {updated_at}")

    categories = {c.get("id") for c in index.get("categories", [])}
    modules = index.get("modules", [])

    # 重复 module_id
    ids = [m.get("module_id") for m in modules]
    seen: set[str] = set()
    dupes: set[str] = set()
    for mid in ids:
        if mid in seen:
            dupes.add(mid)
        seen.add(mid)
    if dupes:
        rpt.err(f"重复的 module_id: {sorted(dupes)}")
    else:
        rpt.ok(f"{len(ids)} 个唯一 module_id")

    id_set = set(ids)

    for m in modules:
        mid = m.get("module_id", "<unknown>")

        # category 引用
        cat = m.get("category")
        if cat and cat not in categories:
            rpt.err(f"{mid}: category '{cat}' 未在 categories 中定义")

        # deprecated 必须有 notice
        if m.get("deprecated") and not m.get("deprecation_notice"):
            rpt.err(f"{mid}: deprecated=true 但缺少 deprecation_notice")

        # version / version_code 一致性
        version = m.get("version", "")
        if not re.match(r"^\d+\.\d+\.\d+", version):
            rpt.err(f"{mid}: version 不符合 SemVer: {version}")

        # 历史版本 version_code 必须小于当前
        versions = m.get("versions", [])
        cur_code = m.get("version_code", 0)
        for v in versions:
            if v.get("version_code", 0) >= cur_code:
                rpt.warn(f"{mid}: 历史版本 {v.get('version')} 的 version_code "
                         f"({v.get('version_code')}) >= 当前 ({cur_code})")

        # 依赖解析
        for dep in m.get("dependencies", []):
            did = dep.get("module_id")
            if did and did not in id_set:
                rpt.err(f"{mid}: 依赖模块 '{did}' 不在索引中")

        # checksum 长度
        cs = m.get("checksum", {})
        alg = cs.get("algorithm")
        val = cs.get("value", "")
        expected = {"sha256": 64, "sha512": 128}.get(alg)
        if expected and len(val) != expected:
            rpt.err(f"{mid}: {alg} 校验和应为 {expected} hex 字符 (实际 {len(val)})")
        if val and not re.match(r"^[a-f0-9]+$", val):
            rpt.err(f"{mid}: checksum.value 含非十六进制字符")

        # 签名存在
        sig = m.get("signature")
        if not sig or not sig.get("value"):
            rpt.err(f"{mid}: 缺少 signature")
        elif sig.get("algorithm") not in (None, "ed25519", "rsa-pss-sha256"):
            rpt.err(f"{mid}: signature.algorithm 非法: {sig.get('algorithm')}")

        # 时间戳可解析
        for field in ("published_at", "updated_at"):
            v = m.get(field)
            if v and not is_iso8601(v):
                rpt.err(f"{mid}: {field} 不是 ISO 8601: {v}")


def validate_index_format(index_path: Path, rpt: Report) -> None:
    """验证 index.json 使用规范格式（2 空格缩进 + 尾随换行 + 非 ASCII 保留）。"""
    print(f"\n{Colors.BLUE}── JSON 格式规范 ──{Colors.NC}")
    raw = index_path.read_text(encoding="utf-8")
    try:
        data = json.loads(raw)
    except Exception as e:
        rpt.err(f"{index_path.name}: 无法解析为 JSON: {e}")
        return
    canonical = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    if raw == canonical:
        rpt.ok(f"{index_path.name}: 格式规范 (2-space indent + trailing newline)")
    else:
        rpt.err(f"{index_path.name}: 格式不规范 — 运行 "
                f"`python3 -c \"import json;...\"` 或重新格式化为 "
                f"`json.dumps(data, indent=2, ensure_ascii=False)`")


# ─────────────────────────────────────────────────────────────────────────────
# 入口
# ─────────────────────────────────────────────────────────────────────────────

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="AstraHub 索引 / Schema 验证工具")
    parser.add_argument("index", nargs="?",
                        help="index.json 路径 (默认: astrahub/modules/index.json)")
    parser.add_argument("--schema", help="module-index schema 路径")
    parser.add_argument("--all", action="store_true",
                        help="验证所有 schema 文件 + index")
    parser.add_argument("--strict", action="store_true",
                        help="严格模式：警告视为错误")
    parser.add_argument("--no-color", action="store_true", help="禁用颜色输出")
    args = parser.parse_args(argv)

    if args.no_color or not sys.stdout.isatty():
        Colors.disable()

    index_path = Path(args.index) if args.index else DEFAULT_INDEX
    schema_path = Path(args.schema) if args.schema else SCHEMAS["module-index"]

    rpt = Report(strict=args.strict)

    print(f"{Colors.BLUE}═══ AstraHub Validation ═══{Colors.NC}")
    print(f"  index:  {index_path}")
    print(f"  schema: {schema_path}")
    print(f"  strict: {args.strict}")

    if args.all:
        validate_schema_files(rpt)

    if not index_path.exists():
        rpt.err(f"index 文件不存在: {index_path}")
        return rpt.summary()

    try:
        index = load_json(index_path)
        rpt.ok(f"{index_path.name} 可解析为 JSON")
    except Exception as e:
        rpt.err(f"{index_path.name} 解析失败: {e}")
        return rpt.summary()

    validate_index_schema(index, schema_path, rpt)
    semantic_checks(index, rpt)
    validate_index_format(index_path, rpt)

    return rpt.summary()


if __name__ == "__main__":
    sys.exit(main())
