#!/usr/bin/env python3
"""全量文件清单生成：为项目根目录下每个文件登记一条记录（只读，不修改任何源文件）。"""
import os, csv, subprocess, datetime

ROOT = "/Users/sunyiyang/Desktop/Project/master-goods"
OUT = os.path.join(ROOT, "优化", "文件审阅台账.csv")
EXCLUDE_TOP = {".git", "优化"}

LANG_EXTS = {
    ".java": "Java", ".kt": "Kotlin", ".kts": "Kotlin脚本", ".swift": "Swift",
    ".vue": "Vue", ".ts": "TypeScript", ".tsx": "TypeScript", ".js": "JavaScript",
    ".mjs": "JavaScript", ".cjs": "JavaScript", ".py": "Python", ".sql": "SQL",
    ".sh": "Shell", ".ps1": "PowerShell", ".bat": "批处理", ".rb": "Ruby",
}
CONFIG_EXTS = {".json": "JSON", ".yml": "YAML", ".yaml": "YAML", ".properties": "属性配置",
               ".xml": "XML", ".toml": "TOML", ".pro": "ProGuard", ".gradle": "Gradle",
               ".plist": "Plist", ".pbxproj": "Xcode工程", ".xcconfig": "Xcode配置",
               ".gitignore": "Git配置", ".editorconfig": "编辑器配置", ".env": "环境配置", ".cvsignore": "忽略配置"}
DOC_EXTS = {".md": "Markdown", ".txt": "文本", ".csv": "CSV", ".tsv": "TSV", ".rst": "RST"}
MEDIA_EXTS = {".png": "图片", ".jpg": "图片", ".jpeg": "图片", ".gif": "图片", ".webp": "图片",
              ".svg": "矢量图", ".ico": "图标", ".pdf": "PDF", ".mp4": "视频", ".mov": "视频",
              ".mp3": "音频", ".wav": "音频", ".ttf": "字体", ".otf": "字体", ".woff": "字体", ".woff2": "字体"}
BIN_EXTS = {".db": "SQLite数据库", ".sqlite": "SQLite数据库", ".jar": "JAR包", ".apk": "APK",
            ".zip": "压缩包", ".gz": "压缩包", ".tgz": "压缩包", ".bin": "二进制", ".so": "动态库",
            ".dylib": "动态库", ".a": "静态库", ".o": "目标文件", ".class": "Java字节码",
            ".jks": "密钥库", ".keystore": "密钥库", ".pem": "密钥", ".key": "密钥", ".mobileprovision": "签名描述文件",
            ".xcuserstate": "Xcode状态", ".icns": "图标", ".har": "HTTP归档", ".log": "日志"}
SKIP_DIRS = {"node_modules", "build", "dist", ".gradle", ".kotlin", "Pods", "DerivedData",
             ".turbo", ".vite", ".idea", ".dart_tool", "bin"}


def classify(rel: str, ext: str):
    """返回 (分类, 是否含业务逻辑, 处理说明)"""
    p = rel.replace("\\", "/")
    parts = p.split("/")
    name = parts[-1]
    # 敏感密钥目录
    if p.startswith(".ssh-check/"):
        return "敏感密钥", "否", "SSH 私钥文件，禁止读取/输出内容；登记存在性与安全风险"
    if name.endswith((".pem", ".key")) or name in ("id_rsa", "id_ed25519"):
        return "敏感密钥", "否", "疑似密钥文件，禁止读取/输出内容"
    # 依赖与生成产物
    for d in ("node_modules/", "build/", "dist/", ".gradle/", ".kotlin/", "Pods/", "DerivedData/", "bin/main/"):
        if "/" + d in "/" + p:
            return "生成产物/依赖", "否", "构建输出或第三方依赖，非源码，无需函数审阅"
    if ext == ".class":
        return "生成产物/依赖", "否", "Java 编译字节码（backend/bin，未被 Git 跟踪）"
    if name == ".DS_Store":
        return "系统文件", "否", "macOS Finder 元数据"
    if parts[0] in ("tmp", "Temp"):
        if ext in LANG_EXTS or ext in CONFIG_EXTS:
            return "临时构建/缓存", "否", "tmp|Temp 目录内的构建缓存或脚本副本，非正式源码"
        return "临时构建/缓存", "否", "tmp|Temp 目录内临时产物"
    if ext in LANG_EXTS:
        return "源码/脚本", "是", LANG_EXTS[ext] + " 源码或脚本"
    if ext == ".sql":
        return "迁移/SQL", "是", "SQL 迁移或查询脚本"
    if ext in CONFIG_EXTS:
        return "配置", "可能含逻辑", CONFIG_EXTS[ext] + " 配置"
    if ext == ".gradle" or name.endswith(".gradle"):
        return "配置", "可能含逻辑", "Gradle 构建脚本"
    if ext in DOC_EXTS:
        return "文档", "否", DOC_EXTS[ext] + " 文档/记录"
    if ext in MEDIA_EXTS:
        return "媒体资源", "否", MEDIA_EXTS[ext]
    if ext in BIN_EXTS:
        return "二进制/数据", "否", BIN_EXTS[ext]
    if name in ("Dockerfile", "Makefile", "LICENSE", "Packagefile"):
        return "配置", "可能含逻辑", "构建/许可文件"
    if name == "gradlew":
        return "源码/脚本", "是", "Gradle Wrapper 启动脚本"
    if ext == "":
        return "无扩展名", "否", "无扩展名文件，按内容定性"
    return "其他", "否", "未分类"


def main():
    # Git 跟踪集合
    try:
        tracked = set(subprocess.run(
            ["git", "ls-files"], cwd=ROOT, capture_output=True, text=True, timeout=120
        ).stdout.splitlines())
    except Exception as e:
        print("git ls-files 失败:", e)
        tracked = set()

    rows = []
    n = 0
    for dirpath, dirnames, filenames in os.walk(ROOT, topdown=True):
        dirnames[:] = [d for d in dirnames if d not in EXCLUDE_TOP]
        for fn in filenames:
            full = os.path.join(dirpath, fn)
            rel = os.path.relpath(full, ROOT)
            n += 1
            ext = os.path.splitext(fn)[1].lower()
            try:
                st = os.stat(full)
                size, mtime = st.st_size, datetime.datetime.fromtimestamp(st.st_mtime).strftime("%Y-%m-%d %H:%M")
            except OSError:
                size, mtime = -1, ""
            cat, has_logic, note = classify(rel, ext)
            tracked_flag = "是" if rel in tracked else "否"
            # 初始状态
            if cat in ("生成产物/依赖", "临时构建/缓存", "系统文件", "媒体资源", "二进制/数据"):
                status = "无需函数审阅"
            elif cat == "敏感密钥":
                status = "无需函数审阅"
            elif cat in ("文档", "无扩展名", "其他"):
                status = "无需函数审阅"
            else:
                status = "未开始"
            top = rel.split("/")[0] if "/" in rel else "(根目录)"
            # 模块判定
            mod = top
            if rel.startswith("Code/backend"): mod = "Code/backend"
            elif rel.startswith("Code/frontend/"):
                seg = rel.split("/")
                mod = "/".join(seg[:3]) if len(seg) >= 3 else rel
            rows.append({
                "编号": "F%06d" % n, "相对路径": rel, "顶层目录": top, "模块": mod,
                "扩展名": ext or "(无)", "分类": cat, "是否含函数或业务逻辑": has_logic,
                "大小字节": size, "最后修改": mtime, "Git跟踪": tracked_flag,
                "状态": status, "发现问题": "", "重复实现编号": "", "未确认事项": "", "处理说明": note,
            })

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)
    print("登记文件总数:", n)
    from collections import Counter
    print("状态分布:", dict(Counter(r["状态"] for r in rows)))
    print("分类分布:", dict(Counter(r["分类"] for r in rows).most_common(15)))


if __name__ == "__main__":
    main()
