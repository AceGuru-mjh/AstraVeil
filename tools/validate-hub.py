#!/usr/bin/env python3
"""
AstraHub 索引验证工具

用法:
    python3 tools/validate-hub.py astrahub/modules/index.json
    python3 tools/validate-hub.py --schema astrahub/schema/module-index.schema.json --index astrahub/modules/index.json
    python3 tools/validate-hub.py --all   # 验证所有 schema + index

功能:
    1. JSON Schema 验证 (draft-07)
    2. 语义验证（交叉引用、版本一致性）
    3. 安全验证（签名格式、URL 协议）
    4. 输出报告（JSON / 人类可读）

依赖:
    pip install jsonschema semver
"""

import argparse
import json
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

try:
    import jsonschema
    from jsonschema import Draft7Validator, ValidationError
    HAS_JSONSCHEMA = True
except ImportError:
    HAS_JSONSCHEMA = False

try:
    import semver
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


def ok(msg: str):
