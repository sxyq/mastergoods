#!/usr/bin/env python3
"""从正式源码中提取函数/方法/逻辑单元签名，生成函数审阅台账（只读分析）。"""
import os, csv, re, json

ROOT = "/Users/sunyiyang/Desktop/Project/master-goods"
OUT = os.path.join(ROOT, "优化", "函数审阅台账.csv")

# 正式源码范围（排除依赖/构建/临时目录）
SRC_DIRS = ["Code", "testing/scripts", "deploy", "maintenance", "docs", "data"]
SKIP_PARTS = {"node_modules", "build", "dist", ".gradle", ".kotlin", "Pods", "DerivedData",
              "bin", "admin-web/dist", ".turbo", ".vite", "tmp", "Temp", "stitch_exports"}
CODE_EXTS = {".java", ".kt", ".swift", ".vue", ".ts", ".tsx", ".js", ".mjs", ".py", ".sh", ".ps1", ".bat", ".m"}

JAVA_MOD = r"(?:(?:public|private|protected|static|final|abstract|synchronized|native|default|strictfp|transitive)\s+)+"
RE_JAVA_METHOD = re.compile(rf"^[ \t]*{JAVA_MOD}(?:<[^>]+>\s+)?[\w\[\]<>,\.\?]+\s+(\w+)\s*\(([^;{{)]*)\)\s*(?:throws [\w\.,\s]+)?\s*{{")
RE_JAVA_CLASS = re.compile(r"^[ \t]*(?:{JAVA_MOD})?(?:class|interface|enum|record)\s+(\w+)")
RE_KT_FUN = re.compile(r"^[ \t]*(?:@\w+(?:\([^)]*\))?[ \t]*)*(?:public|private|protected|internal|override|open|final|static|suspend|inline|operator|expect|actual|lateinit\s+)*fun(?:\s+<[^>]+>)?\s+(?:[\w\.\[\]<>,\?]+\.)?(\w+)\s*\(")
RE_KT_CLASS = re.compile(r"^[ \t]*(?:(?:public|private|protected|internal|open|abstract|final|data|sealed|enum|annotation|object)\s+)*(?:class|interface|object)\s+(\w+)")
RE_SW_FUNC = re.compile(r"^[ \t]*(?:@\w+[ \t]*)*(?:public|private|internal|fileprivate|static|class|open|final|mutating|override\s+)*func\s+(\w+)")
RE_SW_CLASS = re.compile(r"^[ \t]*(?:@\w+[ \t]*)*(?:public|private|internal|open|final|static|)\s*(?:class|struct|enum|extension|actor)\s+(\w+)")
RE_PY_DEF = re.compile(r"^(\s*)def\s+(\w+)\s*\(")
RE_PY_CLASS = re.compile(r"^\s*class\s+(\w+)")
RE_TS_FUNC = re.compile(r"^[ \t]*(?:export\s+)?(?:default\s+)?(?:async\s+)?function\s*\*?\s*(\w+)\s*\(")
RE_TS_ARROW = re.compile(r"^[ \t]*(?:export\s+)?(?:const|let|var)\s+(\w+)\s*(?::[^=]+)?=\s*(?:async\s+)?(?:\([^)]*\)|\w+)\s*(?::[^=]+)?=>")
RE_VUE_FUNC = re.compile(r"^[ \t]*(?:const|let|var)\s+(\w+)\s*=\s*(?:async\s+)?(?:function)?\s*\(")
RE_SH_FUNC = re.compile(r"^[ \t]*(?:function\s+)?(\w+)\s*\(\)\s*\{")


def read_lines(path):
    try:
        with open(path, encoding="utf-8", errors="replace") as f:
            return f.readlines()
    except Exception:
        return None


def find_block_end(lines, start_idx):
    """从 start_idx 行向后做花括号配对，返回块结束行号(1-based)。"""
    depth = 0
    opened = False
    for i in range(start_idx, min(len(lines), start_idx + 4000)):
        line = lines[i]
        in_str = False
        j = 0
        while j < len(line):
            c = line[j]
            if c in "\"'`" and not in_str:
                q = c; in_str = True; j += 1; continue
            if in_str:
                if c == "\\": j += 2; continue
                if c == q: in_str = False
                j += 1; continue
            if c == "/" and j + 1 < len(line) and line[j+1] == "/":
                break
            if c == "{":
                depth += 1; opened = True
            elif c == "}":
                depth -= 1
                if opened and depth <= 0:
                    return i + 1
            j += 1
    return min(len(lines), start_idx + 120)


def walk_src():
    for base in SRC_DIRS:
        bp = os.path.join(ROOT, base)
        if not os.path.isdir(bp):
            continue
        for dirpath, dirnames, filenames in os.walk(bp):
            dirnames[:] = [d for d in dirnames if d not in SKIP_PARTS]
            for fn in filenames:
                ext = os.path.splitext(fn)[1].lower()
                if ext in CODE_EXTS or ext == ".sql":
                    yield os.path.join(dirpath, fn), ext


def extract_file(path, ext):
    """返回 [(函数名, 起始行, 结束行, 所属类, 备注)]"""
    rel = os.path.relpath(path, ROOT)
    lines = read_lines(path)
    if lines is None:
        return None  # 无法读取
    out = []
    if ext == ".sql":
        n = len(lines)
        out.append(("(迁移脚本整体逻辑单元)", 1, n, "(文件级)", "DDL/DML 逐条语句审阅"))
        return out
    if ext == ".java":
        cur_cls = "(文件级)"
        for i, line in enumerate(lines):
            m = RE_JAVA_CLASS.match(line)
            if m:
                cur_cls = m.group(1)
            m = RE_JAVA_METHOD.match(line)
            if m:
                end = find_block_end(lines, i)
                out.append((m.group(1), i + 1, end, cur_cls, ""))
        if not out:
            pass
        return out
    if ext == ".kt":
        cur_cls = "(文件级)"
        for i, line in enumerate(lines):
            m = RE_KT_CLASS.match(line)
            if m and "fun " not in line:
                cur_cls = m.group(1)
            m = RE_KT_FUN.match(line)
            if m:
                end = find_block_end(lines, i)
                name = m.group(1)
                sig = line.strip()
                # 表达式体函数无左花括号：取到下一空行或下一个 top-level 声明
                if "{" not in sig and "=" in sig:
                    end = i + 1
                    for j in range(i + 1, min(len(lines), i + 60)):
                        s = lines[j].strip()
                        if s == "" or re.match(r"^\S", lines[j]) and not s.startswith(("return", ".", "?.", ":", "}", ")")):
                            end = j
                            break
                out.append((name, i + 1, end, cur_cls, ""))
        return out
    if ext == ".swift":
        cur_cls = "(文件级)"
        for i, line in enumerate(lines):
            m = RE_SW_CLASS.match(line)
            if m:
                cur_cls = m.group(1)
            m = RE_SW_FUNC.match(line)
            if m:
                end = find_block_end(lines, i)
                out.append((m.group(1), i + 1, end, cur_cls, ""))
        return out
    if ext == ".py":
        cur_cls = "(文件级)"
        for i, line in enumerate(lines):
            m = RE_PY_CLASS.match(line)
            if m:
                cur_cls = m.group(1)
            m = RE_PY_DEF.match(line)
            if m:
                indent = len(m.group(1))
                end = i + 1
                for j in range(i + 1, len(lines)):
                    s = lines[j]
                    if s.strip() and (len(s) - len(s.lstrip())) <= indent and not s.strip().startswith(("#", '"""', "'''")):
                        end = j
                        break
                else:
                    end = len(lines)
                out.append((m.group(2), i + 1, end, cur_cls, ""))
        return out
    if ext in (".ts", ".tsx", ".js", ".mjs"):
        for i, line in enumerate(lines):
            m = RE_TS_FUNC.match(line) or RE_TS_ARROW.match(line)
            if m:
                end = find_block_end(lines, i)
                out.append((m.group(1), i + 1, end, "(模块级)", ""))
        return out
    if ext == ".vue":
        # 仅扫描 <script> 区
        in_script = False
        for i, line in enumerate(lines):
            if re.search(r"<script[^>]*>", line):
                in_script = True
            if re.search(r"</script>", line):
                in_script = False
            if not in_script:
                continue
            m = RE_TS_FUNC.match(line) or RE_TS_ARROW.match(line)
            if m:
                end = find_block_end(lines, i)
                out.append((m.group(1), i + 1, end, "(组件脚本)", ""))
            m = RE_VUE_FUNC.match(line)
            if m and not any(o[0] == m.group(1) for o in out):
                end = find_block_end(lines, i)
                out.append((m.group(1), i + 1, end, "(组件脚本)", ""))
        return out
    if ext in (".sh", ".ps1", ".bat"):
        for i, line in enumerate(lines):
            m = RE_SH_FUNC.match(line)
            if m:
                end = find_block_end(lines, i)
                out.append((m.group(1), i + 1, end, "(脚本)", ""))
        return out
    return []


def main():
    import time as _t
    _t0 = _t.time()
    rows = []
    unreadable = []
    n = 0
    seen = 0
    for path, ext in walk_src():
        seen += 1
        if seen % 100 == 0:
            print("[进度] 已处理 %d 个文件, %.1fs" % (seen, _t.time() - _t0), flush=True)
        rel = os.path.relpath(path, ROOT)
        res = extract_file(path, ext)
        if res is None:
            unreadable.append(rel)
            continue
        for (name, s, e, cls, note) in res:
            n += 1
            rows.append({
                "编号": "M%06d" % n, "文件相对路径": rel, "语言": ext.lstrip("."),
                "所属类/对象": cls, "函数/逻辑单元": name, "起始行": s, "结束行": e,
                "审阅方式": "待定", "状态": "待审阅", "问题编号": "", "备注": note,
            })
    with open(OUT, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()) if rows else
                           ["编号", "文件相对路径", "语言", "所属类/对象", "函数/逻辑单元", "起始行", "结束行", "审阅方式", "状态", "问题编号", "备注"])
        w.writeheader()
        w.writerows(rows)
    print("提取函数/逻辑单元:", len(rows), " 无法读取文件:", len(unreadable))
    for u in unreadable[:10]:
        print("  无法读取:", u)
    from collections import Counter
    print("按语言:", dict(Counter(r["语言"] for r in rows)))
    print("按文件数:", len({r["文件相对路径"] for r in rows}))


if __name__ == "__main__":
    main()
