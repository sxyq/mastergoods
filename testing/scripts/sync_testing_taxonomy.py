#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
TESTING_ROOT = REPO_ROOT / "testing"
SYNC_PLATFORMS = ("安卓", "后端", "Agent")

TOTAL_LEDGER_HEADERS = [
    "test_type",
    "category_id",
    "category_name",
    "primary_scope",
    "must_cover",
    "evidence_standard",
    "priority",
    "status",
    "script_path",
    "ledger_link",
    "matched_module_count",
    "matched_modules",
    "matched_file_count",
    "matched_file_examples",
    "matched_function_count",
    "notes",
]

LEDGER_CONFIG = {
    "单元测试": {
        "file": ("单元测试", "unit_function_coverage.csv"),
        "headers": [
            "platform",
            "module",
            "category_id",
            "category_name",
            "source_file",
            "class_or_object",
            "function_name",
            "line_number",
            "test_status",
            "test_file",
            "test_case",
            "evidence_path",
            "notes",
        ],
    },
    "功能测试": {
        "file": ("功能测试", "functional_feature_matrix.csv"),
        "headers": [
            "platform",
            "module",
            "feature_domain",
            "category_id",
            "category_name",
            "source_file",
            "source_symbol",
            "line_number",
            "scenario_id",
            "scenario_name",
            "test_status",
            "evidence_path",
            "notes",
        ],
    },
    "性能测试": {
        "file": ("性能测试", "performance_scope_matrix.csv"),
        "headers": [
            "platform",
            "module",
            "category_id",
            "category_name",
            "source_file",
            "class_or_object",
            "function_name",
            "line_number",
            "scenario_name",
            "metric_family",
            "target_or_threshold",
            "priority",
            "status",
            "notes",
        ],
    },
}


def category(
    category_id: str,
    category_name: str,
    primary_scope: str,
    must_cover: str,
    evidence_standard: str,
    priority: str,
    notes: str = "",
) -> dict[str, str]:
    return {
        "category_id": category_id,
        "category_name": category_name,
        "primary_scope": primary_scope,
        "must_cover": must_cover,
        "evidence_standard": evidence_standard,
        "priority": priority,
        "status": "规划中",
        "notes": notes,
    }


TAXONOMY = {
    "安卓": {
        "单元测试": [
            category("AND-UT-01", "纯工具函数", "core/common", "formatter,status label,扩展函数,纯映射", "JUnit 用例 + 边界值断言", "P0"),
            category("AND-UT-02", "序列化契约", "core/model", "DTO,Agent stream,event,result block", "JSON fixture + encode/decode 断言", "P0"),
            category("AND-UT-03", "网络拦截器", "core/network", "AuthInterceptor,TokenAuthenticator,BaseUrlInterceptor", "MockWebServer + header/retry 断言", "P0"),
            category("AND-UT-04", "安全 API 调用包装", "core/network", "SafeApiCall,错误映射,取消/超时", "JUnit + fake response", "P0"),
            category("AND-UT-05", "SSE 解析与取消", "core/network", "AgentSseClient,event 顺序,partial delta,cancel", "fixture 流 + 事件断言", "P0"),
            category("AND-UT-06", "DataStore 与本地会话", "core/datastore", "SessionStore,SettingsStore,SyncPreferenceStore", "Robolectric/JUnit", "P1"),
            category("AND-UT-07", "本地加密与安全存储", "core/datastore", "SecureSessionCipher,损坏密文,错误密钥", "JUnit", "P1"),
            category("AND-UT-08", "Room DAO 与 Mapper", "core/database", "DAO 查询,owner scope,paging,migration,EntityMappers", "Room test + SQL 断言", "P0"),
            category("AND-UT-09", "Repository 行为", "data/*", "请求参数,错误映射,缓存,落库,分页,增量同步", "fake api/fake dao + JUnit", "P0"),
            category("AND-UT-10", "ViewModel 状态机", "feature/*", "load,submit,retry,loading,error,pagination", "coroutine test + state 断言", "P0"),
            category("AND-UT-11", "Compose 渲染契约", "feature/*|core/designsystem", "空态,错误态,按钮可用性,Agent result block", "Compose UI test", "P1"),
            category("AND-UT-12", "启动与导航解析", "app/navigation", "MainActivity,launch extra,deeplink,route parse", "JUnit/Robolectric", "P1"),
            category("AND-UT-13", "运行时安全守卫", "app/security", "RuntimeSecurityGuard,SignatureIntegrityChecker", "JUnit/Robolectric", "P0"),
        ],
        "功能测试": [
            category("AND-FT-01", "登录注册链路", "auth", "login,register,logout,session restore", "真机录屏 + 截图", "P0"),
            category("AND-FT-02", "首页与权限分流", "app/home", "首页入口,tab,无权限页,角色差异", "真机截图 + 权限账号", "P0"),
            category("AND-FT-03", "商品域", "feature/products", "列表,详情,编辑,库存调整", "真机步骤 + 后端目标", "P0"),
            category("AND-FT-04", "客户域", "feature/customers", "列表,详情,联系人,创建编辑", "真机步骤 + 截图", "P0"),
            category("AND-FT-05", "供应商域", "feature/suppliers", "列表,详情,对账,联系人", "真机步骤 + 截图", "P1"),
            category("AND-FT-06", "销售域", "feature/sales", "销售单,收款,退货,状态流转", "真机步骤 + 后端数据", "P0"),
            category("AND-FT-07", "采购域", "feature/purchases", "采购,入库,退货,列表回刷", "真机步骤 + 后端数据", "P0"),
            category("AND-FT-08", "财务域", "feature/finance|payments", "收支,账户,转账,付款单", "真机步骤 + 截图", "P0"),
            category("AND-FT-09", "报表与看板", "feature/dashboard|reports", "真实数据,筛选,空态", "截图 + 后端目标", "P1"),
            category("AND-FT-10", "同步与导入", "data/sync|feature/settings", "主路径,失败,重试,进度", "adb + log + 截图", "P1"),
            category("AND-FT-11", "Agent 文本对话", "feature/agent", "发问,收流,取消,历史恢复", "录屏 + SSE 证据", "P0"),
            category("AND-FT-12", "Agent 草稿闭环", "feature/agent", "draft list,confirm,cancel,workbench", "录屏 + 后端草稿状态", "P0"),
            category("AND-FT-13", "Agent 多模态", "feature/agent|data/agent", "图片上传,图文问答,缺图报错", "录屏 + API 证据", "P1"),
            category("AND-FT-14", "设置与安全", "feature/settings|app/security", "员工管理,登出清理,安全提示", "截图 + 录屏", "P1"),
        ],
        "性能测试": [
            category("AND-PT-01", "冷启动", "app", "cold start,TTFF", "Macrobenchmark report", "P0"),
            category("AND-PT-02", "热启动与回前台", "app", "warm start,hot start", "Macrobenchmark report", "P1"),
            category("AND-PT-03", "首页首屏切换", "dashboard/home", "首页渲染,帧稳定性", "gfxinfo + trace", "P1"),
            category("AND-PT-04", "大列表滚动", "products|orders|customers", "frame p95,jank", "gfxinfo/Perfetto", "P0"),
            category("AND-PT-05", "Room 大表查询", "core/database", "query latency,paging latency", "benchmark log", "P1"),
            category("AND-PT-06", "同步与导入批处理", "sync/import", "CPU,内存,总耗时", "trace + log", "P1"),
            category("AND-PT-07", "Agent 首条响应", "feature/agent", "navigation-to-first-delta", "真机计时 + SSE", "P0"),
            category("AND-PT-08", "Agent 长流渲染", "feature/agent", "delta cadence,scroll jank", "Perfetto + 录屏", "P0"),
            category("AND-PT-09", "图片上传与预处理", "data/agent|media", "upload latency,decode,preview", "trace + log", "P1"),
            category("AND-PT-10", "ViewModel 高频更新", "feature/*", "state emission,main-thread load", "benchmark", "P2"),
            category("AND-PT-11", "长会话稳定性", "feature/agent", "30-turn 对话,内存增长,泄漏", "meminfo + trace", "P0"),
        ],
    },
    "后端": {
        "单元测试": [
            category("BE-UT-01", "公共基础类", "api/common", "ApiResponse,ParseUtils,PaginationUtils,枚举,IdGenerator", "JUnit 断言", "P1"),
            category("BE-UT-02", "认证令牌", "security|auth service", "TokenService,session access,refresh/expire", "JUnit", "P0"),
            category("BE-UT-03", "安全链路与 owner 上下文", "infrastructure/security|store access", "TokenAuthenticationFilter,StorePermissionInterceptor,CurrentOwnerService", "Spring test + 权限断言", "P0"),
            category("BE-UT-04", "Controller / DTO 契约", "api/controller|api/dto", "参数校验,错误码,字段名,权限", "MockMvc", "P0"),
            category("BE-UT-05", "业务 Service 规则", "application/service", "success,invalid input,not found,downstream failure", "JUnit + Mockito", "P0"),
            category("BE-UT-06", "Repository 查询", "infrastructure/repository", "owner scope,paging,sorting,aggregation,@Query", "DataJpaTest", "P0"),
            category("BE-UT-07", "Flyway SQL 校验", "db/migration", "表,索引,约束,owner 字段,回填", "migration SQL test", "P0", "迁移脚本属于 SQL 级资产，不进入函数级台账，计数允许为 0。"),
            category("BE-UT-08", "媒体与存储", "infrastructure/storage|media", "metadata,path,binding,failure branch", "JUnit", "P1"),
            category("BE-UT-09", "同步与导入", "application/service sync/import", "sync cursor,import job,worker claim/retry", "JUnit + integration", "P0"),
            category("BE-UT-10", "配置与启动 profile", "infrastructure/config|root", "prod/local guard,feature flag,LLM switch", "SpringBootTest", "P1"),
            category("BE-UT-11", "多租户隔离", "service|repository owner-aware", "owner 正例,反例,默认 owner,跨租户拒绝", "integration test", "P0"),
            category("BE-UT-12", "领域实体与聚合约束", "domain/entity", "状态流转,派生字段,默认值,聚合一致性", "JUnit", "P1"),
            category("BE-UT-13", "AI/图像 provider 适配", "infrastructure/ai|agent image", "LLM provider,image provider,stream/fallback", "fixture test", "P0"),
        ],
        "功能测试": [
            category("BE-FT-01", "认证账户功能", "/v2/auth/*", "register,login,refresh,logout,me", "request/response + DB", "P0"),
            category("BE-FT-02", "门店与成员上下文", "/v2/stores/current*", "默认门店,成员,权限矩阵", "HTTP + DB", "P0"),
            category("BE-FT-03", "主数据域", "product|customer|supplier|partner", "CRUD,搜索,owner 隔离", "HTTP + DB", "P0"),
            category("BE-FT-04", "销售采购主链路", "sales|purchase", "create,detail,status transition,linkage", "HTTP + DB", "P0"),
            category("BE-FT-05", "财务资金链路", "finance|pay|account", "create,aggregate,status linkage", "HTTP + DB", "P0"),
            category("BE-FT-06", "库存链路", "inventory", "snapshot,ledger,adjustment,count draft", "HTTP + DB", "P0"),
            category("BE-FT-07", "同步导入链路", "sync|import", "cursor,job status,retry", "HTTP + log", "P1"),
            category("BE-FT-08", "媒体上传链路", "media", "upload,bind,error path", "HTTP + file evidence", "P1"),
            category("BE-FT-09", "报表看板链路", "report|dashboard", "summary,filters,empty state", "HTTP + response snapshot", "P1"),
            category("BE-FT-10", "Agent 功能链路", "/v2/agent/*", "conversation,draft,chat,stream,audit", "HTTP + SSE + DB", "P0"),
            category("BE-FT-11", "已知回归项", "regression", "历史 blocked finding 不复发", "回归 case", "P0"),
        ],
        "性能测试": [
            category("BE-PT-01", "认证吞吐", "auth", "register/login/refresh p95,error rate,TPS", "k6/JMeter", "P1"),
            category("BE-PT-02", "高频读取接口", "read APIs", "list/detail/search latency", "load log + p95", "P0"),
            category("BE-PT-03", "高频写入接口", "write APIs", "create/update/status change latency", "load log + error rate", "P0"),
            category("BE-PT-04", "报表聚合 SQL", "report", "query latency,index hit,rows scanned", "EXPLAIN + load", "P0"),
            category("BE-PT-05", "同步与导入吞吐", "sync/import", "worker concurrency,completion duration", "worker log", "P1"),
            category("BE-PT-06", "Repository 热点查询", "repository", "热点 @Query 索引命中", "DB plan", "P1"),
            category("BE-PT-07", "Agent 非流式", "/v2/agent/chat", "full latency,tool duration,model duration", "timing log + audit", "P0"),
            category("BE-PT-08", "Agent 流式", "/v2/agent/chat/stream", "first event,first delta,complete,cancel", "SSE harness", "P0"),
            category("BE-PT-09", "审计写入", "RunAuditService", "audit lag,drop count,write TPS", "DB/log", "P0"),
            category("BE-PT-10", "媒体上传读取", "media/image", "upload latency,read latency", "benchmark log", "P2"),
            category("BE-PT-11", "JVM 稳定性", "service runtime", "heap,GC,threads,CPU", "JFR/GC log", "P1"),
        ],
    },
    "Agent": {
        "单元测试": [
            category("AG-UT-BE-01", "后端编排与规划", "backend:V2AgentAiService|ToolPlanner", "plan source,tool selection,planning fallback", "JUnit + fixture", "P0"),
            category("AG-UT-BE-02", "后端安全与上下文", "backend:SafetyGuard|StoreAccessPolicy", "blocked request,owner propagation,cross-tenant deny", "JUnit", "P0"),
            category("AG-UT-BE-03", "后端工具契约与 DTO", "backend:tool contract|dto", "schema,param validation,result shape", "JUnit", "P0"),
            category("AG-UT-BE-04", "后端只读工具", "backend:agent/tool/readonly", "owner scope,summary,evidence,truncation", "tool test", "P0"),
            category("AG-UT-BE-05", "后端写工具与草稿创建", "backend:agent/tool/write", "draft payload,param fallback,no formal write", "tool test", "P0"),
            category("AG-UT-BE-06", "后端会话/草稿/任务持久化", "backend:conversation|draft|task", "conversation,message,draft confirm/cancel,task state", "service test", "P0"),
            category("AG-UT-BE-07", "后端流式/回答/审计", "backend:AnswerSynthesizer|SseStreamEmitter|RunAuditService", "delta order,result block,audit writeback", "fixture + service test", "P0"),
            category("AG-UT-BE-08", "后端控制器与媒体入口", "backend:/v2/agent/*|/v2/media/*", "chat,stream,draft,run,image,permission", "MockMvc", "P0"),
            category("AG-UT-BE-09", "后端 LLM/图像 provider 配置", "backend:LongCatAnthropicClient|AgentImageService", "responses,stream,fallback,image provider config", "fixture test", "P0"),
            category("AG-UT-BE-10", "后端管理/演示支撑", "backend:Admin|DemoData", "admin console hooks,demo seed,agent support ops", "JUnit", "P1"),
            category("AG-UT-AN-01", "安卓网络与仓储", "android:core/network|data/agent", "SSE parsing,request mapping,media upload", "JUnit + fake api", "P0"),
            category("AG-UT-AN-02", "安卓本地审计与通知存储", "android:core/database", "audit cache,notification persistence", "Room test", "P1"),
            category("AG-UT-AN-03", "安卓入口导航与权限分流", "android:app", "startup launch,deeplink,main routing", "Robolectric/JUnit", "P1"),
            category("AG-UT-AN-04", "安卓 ViewModel 状态机", "android:feature/agent viewmodel", "load,send,cancel,draft list,task refresh", "coroutine test", "P0"),
            category("AG-UT-AN-05", "安卓界面渲染与结果块", "android:feature/agent ui", "markdown,result block,evidence panel,workbench ui", "Compose UI test", "P1"),
            category("AG-UT-WEB-01", "Web 流桥接与协议适配", "web:shared/api/agent-stream.ts", "SSE event parsing,cancel,partial block merge", "Vitest + fixture", "P1"),
            category("AG-UT-WEB-02", "Web 页面状态与渲染", "web:pages/agent/AgentPage.vue", "conversation state,side panel,result block render", "component test", "P1"),
            category("AG-UT-IOS-01", "iOS 模型序列化", "ios:Core/Models/AgentModels.swift", "decode/encode,result block parse", "XCTest", "P1"),
            category("AG-UT-IOS-02", "iOS 访问策略与状态机", "ios:AgentAccessPolicy|AgentViewModel", "gate,state transition,history refresh", "XCTest", "P1"),
            category("AG-UT-IOS-03", "iOS 聊天/草稿/任务/工作台视图", "ios:Features/Agent views", "render,draft workflow,task/workbench layout", "XCUITest/XCTest", "P1"),
        ],
        "功能测试": [
            category("AG-FT-BE-01", "后端会话与草稿接口", "backend conversation/draft", "create,continue,delete,confirm,cancel", "HTTP + DB", "P0"),
            category("AG-FT-BE-02", "后端聊天/流式/审计接口", "backend chat/stream/audit", "chat,stream,cancel,audit readback", "HTTP + SSE", "P0"),
            category("AG-FT-BE-03", "后端业务工具查询链", "backend readonly/write tools", "multi-tool order,partial failure,no fake data", "audit + response evidence", "P0"),
            category("AG-FT-BE-04", "后端多模态/媒体/生图入口", "backend media/image", "upload,image ref,text-to-image,image-to-image", "API + file evidence", "P1"),
            category("AG-FT-BE-05", "后端管理/通知支撑", "backend admin/demo/task", "demo seed,task notification,ops support", "HTTP + DB", "P1"),
            category("AG-FT-AN-01", "安卓入口与会话列表", "android app/entry", "launch,conversation list,history restore", "真机录屏", "P0"),
            category("AG-FT-AN-02", "安卓聊天/流式/取消/证据", "android chat", "send,stream,cancel,result block,evidence", "真机录屏 + SSE", "P0"),
            category("AG-FT-AN-03", "安卓草稿/工作台/通知", "android draft/workbench", "draft confirm,cancel,workbench,task notification", "真机录屏", "P0"),
            category("AG-FT-AN-04", "安卓多模态上传与媒体", "android media", "image upload,missing asset,retry", "真机录屏 + API", "P1"),
            category("AG-FT-WEB-01", "Web 会话页面与侧栏", "web AgentPage", "conversation load,side panel,history reload", "UI + network log", "P1"),
            category("AG-FT-WEB-02", "Web 流式/结果块/取消", "web stream", "ordered SSE,result block merge,cancel", "UI + SSE log", "P1"),
            category("AG-FT-IOS-01", "iOS 聊天与历史恢复", "ios chat", "send,history restore,answer render", "XCUITest + log", "P1"),
            category("AG-FT-IOS-02", "iOS 草稿/任务/工作台", "ios drafts/tasks", "draft list,task panel,workbench", "XCUITest", "P1"),
            category("AG-FT-IOS-03", "iOS 模型解析与权限门控", "ios model/access", "mixed payload parse,policy gate", "XCTest/XCUITest", "P1"),
        ],
        "性能测试": [
            category("AG-PT-BE-01", "后端请求接入与非流式总时延", "backend chat endpoints", "request accepted latency,non-stream total latency", "server timing", "P0"),
            category("AG-PT-BE-02", "后端首事件/首 token/流式完成", "backend stream", "run_started,first delta,complete,cancel stop", "SSE timing", "P0"),
            category("AG-PT-BE-03", "后端工具规划与执行耗时", "backend ToolPlanner/tools", "planning cost,single tool,multi-tool chain", "audit log", "P0"),
            category("AG-PT-BE-04", "后端审计写入与持久化", "backend RunAuditService", "event lag,drop count,write TPS", "DB/log", "P0"),
            category("AG-PT-BE-05", "后端并发/长历史/provider fallback", "backend provider + conversation", "concurrent chat,long history,fallback tail latency", "load report", "P1"),
            category("AG-PT-BE-06", "后端多模态/大结果块序列化", "backend media|result blocks", "upload path,serialization,payload size", "benchmark", "P1"),
            category("AG-PT-AN-01", "安卓聊天首响应与长流渲染", "android chat", "first delta,delta cadence,scroll jank", "trace + 录屏", "P0"),
            category("AG-PT-AN-02", "安卓工作台/列表/通知稳定性", "android workbench/list", "refresh cost,long session memory,notification panel", "trace + meminfo", "P1"),
            category("AG-PT-AN-03", "安卓图片上传与本地预处理", "android media", "upload latency,decode,preview", "trace + log", "P1"),
            category("AG-PT-WEB-01", "Web 首载/流式/取消时延", "web page + stream", "page ready,first chunk,cancel stop", "browser timing", "P1"),
            category("AG-PT-WEB-02", "Web 长历史与结果块重渲染", "web result block", "long history rerender,payload diff cost", "browser profile", "P1"),
            category("AG-PT-IOS-01", "iOS 首响应与历史恢复", "ios chat/model", "first response,history restore,decode cost", "XCTest/XCUITest", "P1"),
            category("AG-PT-IOS-02", "iOS 草稿/任务/工作台渲染", "ios draft/task/workbench", "list render,task refresh,workbench redraw", "XCTest/XCUITest", "P1"),
        ],
    },
}


def read_csv(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def write_csv(path: Path, headers: list[str], rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=headers)
        writer.writeheader()
        writer.writerows(rows)


def function_token(row: dict[str, str]) -> str:
    if row.get("function_name"):
        return row["function_name"].lower()
    if row.get("source_symbol"):
        return row["source_symbol"].split(".")[-1].lower()
    return ""


def has_any(text: str, tokens: tuple[str, ...]) -> bool:
    return any(token in text for token in tokens)


def ledger_paths(platform: str, test_type: str) -> tuple[Path, str, str]:
    subdir, filename = LEDGER_CONFIG[test_type]["file"]
    path = TESTING_ROOT / platform / subdir / filename
    return path, f"testing/{platform}/{subdir}/scripts", f"testing/{platform}/{subdir}/{filename}"


def category_name_map(platform: str, test_type: str) -> dict[str, str]:
    return {entry["category_id"]: entry["category_name"] for entry in TAXONOMY[platform][test_type]}


def classify_android_unit(row: dict[str, str]) -> str:
    module = row["module"]
    rel = row["source_file"].lower()
    fn = function_token(row)
    if module == "core/common":
        return "AND-UT-01"
    if module == "core/model":
        return "AND-UT-02"
    if module == "core/network":
        if "agentsseclient" in rel or has_any(fn, ("consumestream", "parsesse", "readnext", "emitdelta")):
            return "AND-UT-05"
        if has_any(rel, ("authinterceptor", "tokenauthenticator", "baseurlinterceptor")) or has_any(
            fn, ("intercept", "authenticate", "attachauthorization", "withbaseurl")
        ):
            return "AND-UT-03"
        return "AND-UT-04"
    if module == "core/datastore":
        return "AND-UT-07" if "securesessioncipher" in rel else "AND-UT-06"
    if module == "core/database":
        return "AND-UT-08"
    if module == "core/designsystem":
        return "AND-UT-11"
    if module.startswith("data/"):
        return "AND-UT-09"
    if module == "app":
        if has_any(rel, ("security", "integrity", "signature")):
            return "AND-UT-13"
        return "AND-UT-12"
    if module.startswith("feature/"):
        return "AND-UT-10" if "viewmodel" in rel else "AND-UT-11"
    return "AND-UT-11"


def classify_android_functional(row: dict[str, str]) -> str:
    module = row["module"]
    rel = row["source_file"].lower()
    fn = function_token(row)
    if module in {"feature/auth", "data/auth"}:
        return "AND-FT-01"
    if module == "app":
        if has_any(rel, ("security", "integrity", "signature")):
            return "AND-FT-14"
        return "AND-FT-02"
    if module in {"feature/products", "data/product"}:
        return "AND-FT-03"
    if module in {"feature/customers", "data/customer"}:
        return "AND-FT-04"
    if module in {"feature/suppliers", "data/supplier"}:
        return "AND-FT-05"
    if module == "feature/sales" or (module == "data/order" and not has_any(rel, ("purchase", "receipt"))):
        return "AND-FT-06"
    if module == "feature/purchases" or has_any(rel, ("purchase", "receipt")):
        return "AND-FT-07"
    if module in {"feature/finance", "feature/payments", "data/finance"}:
        return "AND-FT-08"
    if module in {"feature/dashboard", "feature/reports", "data/report"}:
        return "AND-FT-09"
    if module == "data/sync":
        return "AND-FT-10"
    if module == "feature/settings":
        return "AND-FT-10" if has_any(rel + fn, ("sync", "import")) else "AND-FT-14"
    if module in {"feature/agent", "data/agent", "core/network"}:
        if has_any(rel + fn, ("image", "upload", "attachment", "preview", "multimodal", "media")):
            return "AND-FT-13"
        if has_any(rel, ("draft", "workbench", "notification", "task")):
            return "AND-FT-12"
        return "AND-FT-11"
    return "AND-FT-14"


def classify_android_performance(row: dict[str, str]) -> str:
    module = row["module"]
    rel = row["source_file"].lower()
    fn = function_token(row)
    if module == "app":
        return "AND-PT-01" if has_any(rel + fn, ("oncreate", "startup", "launch")) else "AND-PT-02"
    if module == "core/database":
        return "AND-PT-05"
    if module == "data/sync":
        return "AND-PT-06"
    if module in {"feature/dashboard", "feature/reports"}:
        return "AND-PT-03"
    if module in {"feature/products", "feature/customers", "feature/sales", "feature/purchases"} or module == "data/order":
        return "AND-PT-04"
    if module in {"feature/agent", "data/agent", "core/network"}:
        if has_any(rel + fn, ("image", "upload", "attachment", "preview", "media")):
            return "AND-PT-09"
        if has_any(rel + fn, ("workbench", "draft", "notification", "task")):
            return "AND-PT-11"
        if "viewmodel" in rel and has_any(fn, ("update", "emit", "refresh", "reduce", "state")):
            return "AND-PT-10"
        if has_any(rel + fn, ("stream", "delta", "render", "append", "markdown")):
            return "AND-PT-08"
        return "AND-PT-07"
    if module.startswith("feature/"):
        return "AND-PT-10"
    return "AND-PT-02"


def classify_backend_unit(row: dict[str, str]) -> str:
    module = row["module"]
    rel = row["source_file"].lower()
    fn = function_token(row)
    if module == "api/common":
        return "BE-UT-01"
    if module in {"api/controller", "api/dto"}:
        return "BE-UT-04"
    if module == "domain/entity":
        return "BE-UT-12"
    if module == "infrastructure/repository":
        return "BE-UT-11" if has_any(rel + fn, ("owner", "tenant")) else "BE-UT-06"
    if module == "infrastructure/config":
        return "BE-UT-13" if has_any(rel, ("agentllm", "agentimage")) else "BE-UT-10"
    if module == "infrastructure/storage":
        return "BE-UT-08"
    if module == "infrastructure/ai":
        return "BE-UT-13"
    if module == "infrastructure/security":
        return "BE-UT-02" if has_any(rel + fn, ("token", "refresh")) else "BE-UT-03"
    if module == "root":
        return "BE-UT-10"
    if module == "application/service":
        if has_any(rel + fn, ("owner", "tenant")):
            return "BE-UT-11"
        if has_any(rel + fn, ("token", "refresh", "session")):
            return "BE-UT-02"
        if has_any(rel + fn, ("permission", "access", "currentowner", "requirecurrentowner")):
            return "BE-UT-03"
        if has_any(rel + fn, ("sync", "importjob", "cursor", "worker", "claim")):
            return "BE-UT-09"
        if has_any(rel + fn, ("storage", "upload", "media")) and "agentimage" not in rel:
            return "BE-UT-08"
        if has_any(rel + fn, ("agentimage", "anthropic", "llm", "modelstream", "responses")):
            return "BE-UT-13"
        return "BE-UT-05"
    return "BE-UT-05"


def classify_backend_functional(row: dict[str, str]) -> str:
    text = f"{row['module']} {row['source_file']} {function_token(row)}".lower()
    if has_any(text, ("auth", "login", "logout", "refresh", "register", "session")):
        return "BE-FT-01"
    if has_any(text, ("store", "member", "permission", "currentowner", "storeaccess")):
        return "BE-FT-02"
    if has_any(text, ("product", "customer", "supplier", "partner")):
        return "BE-FT-03"
    if has_any(text, ("sale", "purchase", "receipt", "return", "payorder")):
        return "BE-FT-04"
    if has_any(text, ("finance", "payment", "account", "transfer", "receivable", "payable")):
        return "BE-FT-05"
    if has_any(text, ("inventory", "stock")):
        return "BE-FT-06"
    if has_any(text, ("sync", "import")):
        return "BE-FT-07"
    if has_any(text, ("media", "image", "upload", "file")):
        return "BE-FT-08"
    if has_any(text, ("report", "dashboard", "trend", "overview")):
        return "BE-FT-09"
    if has_any(text, ("agent", "toolplanner", "draft", "conversation", "runaudit", "anthropic")):
        return "BE-FT-10"
    return "BE-FT-11"


def classify_backend_performance(row: dict[str, str]) -> str:
    text = f"{row['module']} {row['source_file']} {function_token(row)}".lower()
    if has_any(text, ("auth", "login", "logout", "refresh", "register", "token")):
        return "BE-PT-01"
    if has_any(text, ("report", "dashboard", "aggregate", "summary")):
        return "BE-PT-04"
    if has_any(text, ("sync", "import", "worker", "cursor")):
        return "BE-PT-05"
    if row["module"] == "infrastructure/repository":
        return "BE-PT-06"
    if has_any(text, ("media", "image", "upload", "file")):
        return "BE-PT-10"
    if has_any(text, ("runaudit", "auditevent", "auditrepository")):
        return "BE-PT-09"
    if has_any(text, ("agent", "toolplanner", "anthropic", "conversation", "draft", "message")):
        if has_any(text, ("stream", "delta", "emit", "cancel")):
            return "BE-PT-08"
        return "BE-PT-07"
    if has_any(text, ("list", "detail", "search", "query", "find", "page")):
        return "BE-PT-02"
    if has_any(text, ("create", "update", "delete", "save", "submit", "confirm", "cancel")):
        return "BE-PT-03"
    return "BE-PT-11"


def classify_agent_unit(row: dict[str, str]) -> str:
    rel = row["source_file"].lower()
    module = row["module"]
    if module.startswith("backend:"):
        if has_any(rel, ("admincontroller.java", "adminservice.java", "demodataservice.java")):
            return "AG-UT-BE-10"
        if has_any(rel, ("v2agentcontroller.java", "v2mediacontroller.java")):
            return "AG-UT-BE-08"
        if has_any(rel, ("tool/readonly/",)):
            return "AG-UT-BE-04"
        if has_any(rel, ("tool/write/",)):
            return "AG-UT-BE-05"
        if has_any(
            rel,
            (
                "v2agentconversationservice.java",
                "agentdraftconfirmservice.java",
                "agentconversationrepository.java",
                "agentdraftrepository.java",
                "agentmessagerepository.java",
                "agenttaskrepository.java",
                "agentnotificationrepository.java",
                "agenttaskentity.java",
                "agentnotificationentity.java",
            ),
        ):
            return "AG-UT-BE-06"
        if has_any(rel, ("answersynthesizer.java", "ssestreamemitter.java", "runauditservice.java", "agentrunaudit")):
            return "AG-UT-BE-07"
        if has_any(rel, ("toolregistry.java", "toolsupport.java", "toolcontext.java", "toolresult.java", "agenttool.java", "v2agentdtos.java", "agenttypes.java")):
            return "AG-UT-BE-03"
        if has_any(rel, ("safetyguard.java", "storeaccesspolicy.java")):
            return "AG-UT-BE-02"
        if has_any(rel, ("longcatanthropicclient.java", "agentimageservice.java", "agentllmproperties.java", "agentimageproperties.java")):
            return "AG-UT-BE-09"
        if has_any(rel, ("toolplanner.java", "v2agentaiservice.java")):
            return "AG-UT-BE-01"
        return "AG-UT-BE-06"
    if module.startswith("android:"):
        if module == "android:core/network" or module == "android:data/agent":
            return "AG-UT-AN-01"
        if module == "android:core/database":
            return "AG-UT-AN-02"
        if module == "android:app":
            return "AG-UT-AN-03"
        if "viewmodel" in rel:
            return "AG-UT-AN-04"
        return "AG-UT-AN-05"
    if module.startswith("web:"):
        return "AG-UT-WEB-01" if rel.endswith("agent-stream.ts") else "AG-UT-WEB-02"
    if module.startswith("ios:"):
        if rel.endswith("agentmodels.swift"):
            return "AG-UT-IOS-01"
        if has_any(rel, ("agentaccesspolicy.swift", "agentviewmodel.swift")):
            return "AG-UT-IOS-02"
        return "AG-UT-IOS-03"
    raise ValueError(f"Unclassified Agent unit row: {row['source_file']}")


def classify_agent_functional(row: dict[str, str]) -> str:
    rel = row["source_file"].lower()
    module = row["module"]
    fn = function_token(row)
    if module.startswith("backend:"):
        if has_any(rel, ("agentimageservice.java", "v2mediacontroller.java", "mediauploadtool.java", "agentimageproperties.java")):
            return "AG-FT-BE-04"
        if has_any(rel, ("admincontroller.java", "adminservice.java", "demodataservice.java", "agenttaskentity.java", "agentnotificationentity.java")):
            return "AG-FT-BE-05"
        if has_any(rel, ("v2agentconversationservice.java", "agentdraftconfirmservice.java", "agentconversationrepository.java", "agentdraftrepository.java", "agentmessagerepository.java")):
            return "AG-FT-BE-01"
        if has_any(rel, ("tool/readonly/", "tool/write/", "toolplanner.java", "toolregistry.java", "toolsupport.java")):
            return "AG-FT-BE-03"
        return "AG-FT-BE-02"
    if module.startswith("android:"):
        if has_any(rel + fn, ("image", "upload", "attachment", "preview", "media")):
            return "AG-FT-AN-04"
        if has_any(rel, ("draft", "workbench", "tasknotification", "notification")):
            return "AG-FT-AN-03"
        if module == "android:app" or "conversationlistpanel" in rel:
            return "AG-FT-AN-01"
        return "AG-FT-AN-02"
    if module.startswith("web:"):
        if rel.endswith("agent-stream.ts") or has_any(fn, ("cancel", "abort", "stream", "delta", "emit")):
            return "AG-FT-WEB-02"
        return "AG-FT-WEB-01"
    if module.startswith("ios:"):
        if rel.endswith("agentmodels.swift") or rel.endswith("agentaccesspolicy.swift"):
            return "AG-FT-IOS-03"
        if has_any(rel, ("drafts", "tasks", "workbench")):
            return "AG-FT-IOS-02"
        return "AG-FT-IOS-01"
    raise ValueError(f"Unclassified Agent functional row: {row['source_file']}")


def classify_agent_performance(row: dict[str, str]) -> str:
    rel = row["source_file"].lower()
    module = row["module"]
    fn = function_token(row)
    text = f"{rel} {fn}"
    if module.startswith("backend:"):
        if has_any(text, ("runaudit", "auditevent", "auditrepository")):
            return "AG-PT-BE-04"
        if has_any(text, ("image", "upload", "media", "serialize", "payload", "resultblock", "decode")) and "anthropic" not in text:
            return "AG-PT-BE-06"
        if has_any(text, ("toolplanner", "/tool/readonly/", "/tool/write/", "execute", "plan")):
            return "AG-PT-BE-03"
        if has_any(text, ("anthropic", "fallback", "conversation", "history", "concurrent", "draft", "message")):
            return "AG-PT-BE-05"
        if has_any(text, ("stream", "delta", "emit", "cancel", "run_started")):
            return "AG-PT-BE-02"
        return "AG-PT-BE-01"
    if module.startswith("android:"):
        if has_any(text, ("image", "upload", "attachment", "preview", "media")):
            return "AG-PT-AN-03"
        if has_any(text, ("workbench", "draft", "notification", "task", "conversationlistpanel")):
            return "AG-PT-AN-02"
        return "AG-PT-AN-01"
    if module.startswith("web:"):
        if rel.endswith("agent-stream.ts") or has_any(text, ("stream", "delta", "cancel", "abort")):
            return "AG-PT-WEB-01"
        return "AG-PT-WEB-02"
    if module.startswith("ios:"):
        if has_any(rel, ("drafts", "tasks", "workbench")):
            return "AG-PT-IOS-02"
        return "AG-PT-IOS-01"
    raise ValueError(f"Unclassified Agent performance row: {row['source_file']}")


CLASSIFIERS = {
    "安卓": {
        "单元测试": classify_android_unit,
        "功能测试": classify_android_functional,
        "性能测试": classify_android_performance,
    },
    "后端": {
        "单元测试": classify_backend_unit,
        "功能测试": classify_backend_functional,
        "性能测试": classify_backend_performance,
    },
    "Agent": {
        "单元测试": classify_agent_unit,
        "功能测试": classify_agent_functional,
        "性能测试": classify_agent_performance,
    },
}


def enrich_rows(platform: str, test_type: str, rows: list[dict[str, str]]) -> list[dict[str, str]]:
    headers = LEDGER_CONFIG[test_type]["headers"]
    names = category_name_map(platform, test_type)
    classifier = CLASSIFIERS[platform][test_type]
    enriched: list[dict[str, str]] = []
    for row in rows:
        item = {key: row.get(key, "") for key in headers}
        category_id = classifier(item)
        item["category_id"] = category_id
        item["category_name"] = names[category_id]
        enriched.append(item)
    return enriched


def compress_files(files: list[str], limit: int = 5) -> str:
    if not files:
        return ""
    shown = files[:limit]
    return " | ".join(shown) + (" | ..." if len(files) > limit else "")


def build_total_rows(platform: str, test_type: str, rows: list[dict[str, str]]) -> list[dict[str, str]]:
    _, script_path, ledger_link = ledger_paths(platform, test_type)
    counts: dict[str, dict[str, set[str] | int]] = {}
    for row in rows:
        category_id = row["category_id"]
        bucket = counts.setdefault(category_id, {"modules": set(), "files": set(), "functions": 0})
        bucket["modules"].add(row["module"])
        bucket["files"].add(row["source_file"])
        bucket["functions"] += 1
    output: list[dict[str, str]] = []
    for entry in TAXONOMY[platform][test_type]:
        bucket = counts.get(entry["category_id"], {"modules": set(), "files": set(), "functions": 0})
        modules = sorted(bucket["modules"])
        files = sorted(bucket["files"])
        output.append(
            {
                "test_type": test_type,
                "category_id": entry["category_id"],
                "category_name": entry["category_name"],
                "primary_scope": entry["primary_scope"],
                "must_cover": entry["must_cover"],
                "evidence_standard": entry["evidence_standard"],
                "priority": entry["priority"],
                "status": entry["status"],
                "script_path": script_path,
                "ledger_link": ledger_link,
                "matched_module_count": str(len(modules)),
                "matched_modules": " | ".join(modules),
                "matched_file_count": str(len(files)),
                "matched_file_examples": compress_files(files),
                "matched_function_count": str(bucket["functions"]),
                "notes": entry["notes"],
            }
        )
    return output


def sync_platform(platform: str) -> None:
    total_rows: list[dict[str, str]] = []
    for test_type in ("单元测试", "功能测试", "性能测试"):
        path, _, _ = ledger_paths(platform, test_type)
        rows = read_csv(path)
        if not rows:
            continue
        enriched = enrich_rows(platform, test_type, rows)
        write_csv(path, LEDGER_CONFIG[test_type]["headers"], enriched)
        total_rows.extend(build_total_rows(platform, test_type, enriched))
    if total_rows:
        write_csv(TESTING_ROOT / platform / "测试分类总台账.csv", TOTAL_LEDGER_HEADERS, total_rows)


def main() -> None:
    parser = argparse.ArgumentParser(description="Sync test ledgers with taxonomy categories and counts.")
    parser.add_argument("--platform", action="append", choices=sorted(SYNC_PLATFORMS), help="Sync only the selected platform(s).")
    args = parser.parse_args()
    targets = args.platform or list(SYNC_PLATFORMS)
    for platform in targets:
        sync_platform(platform)


if __name__ == "__main__":
    main()
