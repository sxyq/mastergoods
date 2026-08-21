#!/usr/bin/env python3
"""Run real server-side Agent coverage against the current 8220 runtime.

The script deliberately talks to the application API instead of invoking Java
tool classes directly. It uses the cloned database's active session and keeps
the provider key in the remote process environment only.
"""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


BASE_URL = os.environ.get("BASE_URL", "http://127.0.0.1:28081").rstrip("/")
EVIDENCE_ROOT = Path(os.environ.get("EVIDENCE_ROOT", "/tmp/mg-agent-eval-all-tools"))
DB_CONTAINER = os.environ.get("DB_CONTAINER", "mg-agent-eval-pg")
DB_USER = os.environ.get("DB_USER", "eval")
DB_NAME = os.environ.get("DB_NAME", "zhihuiji_eval")
TEST_HOST = os.environ.get("TEST_HOST", "8.220.206.9")
TEST_RUNTIME = os.environ.get("TEST_RUNTIME", "server-backend")
TEST_DATABASE = os.environ.get("TEST_DATABASE", DB_NAME)
TEST_SOURCE = os.environ.get("TEST_SOURCE", "latest deployed Agent-bearing backend")
TEST_ACCOUNT_SOURCE = os.environ.get("TEST_ACCOUNT_SOURCE", "active authenticated account from 8220 database")
TEST_OWNER_USER_ID = int(os.environ.get("TEST_OWNER_USER_ID", "0") or "0")
CASE_FILTER = {
    value.strip()
    for value in os.environ.get("CASE_FILTER", "").split(",")
    if value.strip()
}
try:
    PROMPT_OVERRIDES = json.loads(os.environ.get("PROMPT_OVERRIDES_JSON", "{}"))
except json.JSONDecodeError:
    PROMPT_OVERRIDES = {}
MODEL = os.environ.get("AGENT_LLM_MODEL", "gpt-5.6-luna")
PROVIDER_BASE_URL = os.environ.get("AGENT_LLM_BASE_URL", "https://oneapi.sxyq27.online/v1")
WIRE_API = os.environ.get("AGENT_LLM_WIRE_API", "chat_completions")
TOOL_CHOICE = os.environ.get("AGENT_TOOL_CHOICE", "auto")
PROVIDER_EVIDENCE_PATH = os.environ.get("PROVIDER_EVIDENCE_PATH", "")


def case(name: str, kind: str, prompt: str, expected_tools: list[str] | None = None,
         expected_tool_sets: list[list[str]] | None = None,
         expected_tool_sequences: list[list[str]] | None = None) -> dict:
    tool_sets = expected_tool_sets or [expected_tools or [name]]
    return {
        "tool": name,
        "kind": kind,
        "prompt": prompt,
        "expected_tools": expected_tools or tool_sets[0],
        "expected_tool_sets": tool_sets,
        **({"expected_tool_sequences": expected_tool_sequences} if expected_tool_sequences else {}),
    }


# Every registered business tool has a natural user question. The expected tool
# stays in the case metadata and is never injected into the request sent to the
# Agent, so selection is evaluated from the user's wording alone.
CASES = [
    case("account_balance_lookup", "read", "帮我看看现在有几个资金账户，各自还剩多少钱？"),
    case("account_health_lookup", "read", "帮我看下资金账户最近状态，有没有什么异常？"),
    case("account_transfer_lookup", "read", "最近账户之间转过哪些钱？把明细和状态给我看看。"),
    case("anomaly_alert_lookup", "read", "最近一周生意有没有异常？销售下滑、缺货、客户欠款这些帮我扫一遍。"),
    case("cash_change_lookup", "read", "最近的钱都怎么进出的？把资金变动列一下。"),
    case("cashflow_summary_lookup", "read", "最近现金流怎么样？收入、支出和净现金流帮我算一下。"),
    case("cross_analysis_lookup", "read", "把销售、采购和库存放在一起看一下，有什么关系？"),
    case("customer_profile_lookup", "read", "帮我看看客户整体情况，余额、下单、收款和退货都说说。"),
    case("customer_receivable_lookup", "read", "哪些客户还欠我钱？按优先收款帮我排一下。"),
    case("data_export_tool", "read", "我想把销售数据导出来，先看看能导哪些字段、多少条。"),
    case("finance_record_lookup", "read", "最近的收入支出流水给我看下，按类别分一下。"),
    case(
        "generate_poster_prompt",
        "read",
        "拿商品信息帮我写个海报提示词，先不要生成图片。",
        expected_tool_sets=[
            ["generate_poster_prompt"],
            ["product_catalog_lookup", "generate_poster_prompt"],
        ],
    ),
    case("import_job_lookup", "read", "之前的数据导入现在到哪一步了？有没有失败重试的？"),
    case("inventory_adjustment_lookup", "read", "最近库存都调整过什么？盘盈盘亏也列出来。"),
    case("inventory_ledger_lookup", "read", "把库存出入库流水和来源给我看看。"),
    case(
        "inventory_low_stock_lookup",
        "read",
        "哪些商品快没货了？顺便看看该补多少。",
        expected_tool_sets=[
            ["inventory_low_stock_lookup"],
            ["smart_restock_lookup"],
            ["inventory_low_stock_lookup", "smart_restock_lookup"],
            ["inventory_panorama_lookup"],
        ],
    ),
    case("inventory_panorama_lookup", "read", "我想看库存全貌，安全库存、最近销量、周转和补货建议一起给我。"),
    case("inventory_snapshot_lookup", "read", "库存盘点和历史快照还有吗？帮我看一下。"),
    case("partner_contact_lookup", "read", "客户和供应商的联系人信息帮我找一下。"),
    case("partner_group_lookup", "read", "客户和供应商分组现在是什么情况？每组有多少人？"),
    case("pay_order_lookup", "read", "最近给供应商付了哪些款？状态怎么样？"),
    case("payment_lookup", "read", "最近收款和付款的记录帮我理一下。"),
case(
    "product_catalog_lookup",
    "read",
    "把现在的商品、库存、价格和分类一起给我看下。",
    expected_tool_sets=[
        ["product_catalog_lookup"],
        ["product_catalog_lookup", "product_category_lookup"],
    ],
),
    case("product_category_lookup", "read", "商品分类现在怎么分的？每类有多少？"),
    case("product_price_level_lookup", "read", "商品价格等级现在有哪些？名称和状态也带上。"),
    case("product_supplier_relation_lookup", "read", "这些商品分别是从哪家供应商进的？最近采购价是多少？"),
    case("purchase_order_lookup", "read", "最近的采购单和到货情况帮我看一下。"),
    case("purchase_receipt_lookup", "read", "最近入了哪些采购货？入库明细和状态给我看看。"),
    case("purchase_return_lookup", "read", "最近退给供应商的货有哪些？状态怎么样？"),
    case(
        "purchase_tracking_lookup",
        "read",
        "帮我把采购单、入库和退货的关联过程串起来看看。",
        expected_tool_sets=[
            ["purchase_tracking_lookup"],
            ["purchase_tracking_lookup", "purchase_order_lookup"],
            ["purchase_tracking_lookup", "purchase_order_lookup", "purchase_receipt_lookup"],
            ["purchase_tracking_lookup", "purchase_order_lookup", "purchase_receipt_lookup", "purchase_return_lookup"],
        ],
    ),
    case(
        "receivable_payable_lookup",
        "read",
        "客户欠我的和我欠供应商的分别有多少？重点对象列一下。",
        expected_tool_sets=[
            ["receivable_payable_lookup"],
            ["receivable_payable_lookup", "supplier_payable_lookup"],
        ],
    ),
    case("report_query", "read", "帮我看看这个月销售怎么样，给我一个经营汇总。"),
    case(
        "result_visualization",
        "read",
        "最近一周销售和回款给我画一张趋势图。",
        expected_tool_sets=[
            ["sales_overview_lookup", "result_visualization"],
            ["sales_trend_lookup", "payment_lookup", "result_visualization"],
            # The request explicitly asks for a chart, but an empty real-data
            # window is a valid no-chart result; never force a blank chart.
            ["sales_overview_lookup"],
            ["sales_trend_lookup"],
        ],
    ),
    case("sale_order_lookup", "read", "最近卖出去的单子帮我看看，客户和收款情况也带上。"),
    case("sales_full_chain_lookup", "read", "把销售单、收款和退货的关联记录串起来给我看。"),
    case("sales_overview_lookup", "read", "看一下最近一周销售和回款的整体情况。"),
    case("sales_return_lookup", "read", "最近有哪些销售退货？退货明细和状态给我看看。"),
    case("sales_trend_lookup", "read", "最近一个月每天卖得怎么样？按天看一下趋势。"),
    case("smart_restock_lookup", "read", "哪些东西该补货了？按紧急程度和建议数量给我排一下。"),
    case("store_info_lookup", "read", "当前门店的信息和成员数量帮我看下。"),
    case("supplier_payable_lookup", "read", "我还欠哪些供应商钱？金额和采购情况一起看看。"),
    case("supplier_statement_lookup", "read", "帮我和供应商对一下账，余额、采购和退货都算进去。"),
    case("sync_status_lookup", "read", "数据同步现在正常吗？哪些内容会同步？"),
    case(
        "create_account_transfer",
        "create",
        "把第一个资金账户的钱转到第二个资金账户，转 1.23 元，备注写全量工具测试，先给我看一下再保存。",
        expected_tool_sets=[
            ["create_account_transfer"],
            ["account_balance_lookup", "create_account_transfer"],
        ],
    ),
    case(
        "create_customer",
        "create",
        "帮我加一个客户，名字叫全量工具测试客户，电话 13900000001，先把要保存的内容给我确认。",
        expected_tool_sets=[
            ["create_customer"],
        ],
    ),
    case("create_finance_record", "create", "记一笔收入 1.23 元，分类写全量工具测试，先做成草稿让我确认。"),
    case(
        "create_inventory_adjustment",
        "create",
        "把商品列表里的第一个商品库存加 1 件，原因写全量工具测试，先做个调整草稿。",
        expected_tool_sets=[
            ["create_inventory_adjustment"],
            ["product_catalog_lookup", "create_inventory_adjustment"],
        ],
    ),
    case(
        "create_inventory_count_draft",
        "create",
        "选一个商品按现在的库存做一次盘点，先生成草稿给我确认。",
        expected_tool_sets=[
            ["create_inventory_count_draft"],
            ["product_catalog_lookup", "create_inventory_count_draft"],
        ],
    ),
    case(
        "create_pay_order",
        "create",
        "给供应商记一笔 1.23 元付款，备注全量工具测试，先别直接付款，做成草稿。",
        expected_tool_sets=[
            ["create_pay_order"],
            ["supplier_directory_lookup", "create_pay_order"],
        ],
    ),
    case(
        "create_product",
        "create",
        "帮我加个商品，名称全量工具测试商品，编码 EVAL-ONLY-20260802，先生成草稿。",
        expected_tool_sets=[
            ["create_product"],
            ["product_category_lookup", "create_product"],
        ],
        expected_tool_sequences=[
            ["create_product"],
            ["product_category_lookup", "create_product"],
        ],
    ),
    case(
        "create_purchase_order",
        "create",
        "向现有供应商买一个真实商品，数量 1、单价 1.23，先做采购草稿让我看看。",
        expected_tool_sets=[
            ["create_purchase_order"],
            ["supplier_directory_lookup", "product_catalog_lookup", "create_purchase_order"],
        ],
    ),
    case(
        "create_purchase_receipt",
        "create",
        "把一张采购单里的 1 件货做入库，先生成入库草稿，不要直接记账。",
        expected_tool_sets=[
            ["create_purchase_receipt"],
            ["purchase_order_lookup", "create_purchase_receipt"],
        ],
    ),
    case(
        "create_purchase_return",
        "create",
        "采购来的货退 1 件，原因写全量工具测试，先给我退货草稿。",
        expected_tool_sets=[
            ["create_purchase_return"],
            ["purchase_order_lookup", "create_purchase_return"],
        ],
    ),
    case(
        "create_sale_order",
        "create",
        "给一个现有客户开一单，商品 1 件、单价 1.23，先生成销售草稿。",
        expected_tool_sets=[
            ["create_sale_order"],
            ["customer_directory_lookup", "product_catalog_lookup", "create_sale_order"],
        ],
    ),
    case(
        "create_sales_return",
        "create",
        "把一张销售单退 1 件，原因写全量工具测试，先做草稿让我确认。",
        expected_tool_sets=[
            ["create_sales_return"],
            ["sale_order_lookup", "create_sales_return"],
        ],
    ),
    case("create_supplier", "create", "帮我加一个供应商，名字全量工具测试供应商，电话 13900000002，先生成草稿。"),
    case("media_upload_tool", "create", "我有个 all-tools-eval.txt 文件，文本类型、16 字节，先生成上传意图草稿。"),
    case(
        "multi_sales_cashflow",
        "read",
        "最近一周销售和现金流放在一起看下，合适的话用图展示。",
        expected_tool_sets=[
            ["sales_overview_lookup", "cashflow_summary_lookup"],
            ["sales_overview_lookup", "cashflow_summary_lookup", "result_visualization"],
            ["sales_trend_lookup", "cashflow_summary_lookup", "result_visualization"],
        ],
    ),
    case(
        "multi_inventory_restock",
        "read",
        "库存和补货一起帮我看，哪些要马上补？用合适的方式展示。",
        expected_tool_sets=[
            ["inventory_panorama_lookup", "smart_restock_lookup", "result_visualization"],
            ["inventory_panorama_lookup", "smart_restock_lookup"],
        ],
    ),
    case(
        "multi_receivable_payable",
        "read",
        "客户欠款和供应商应付款一起算一下，重点对象用表格列出来。",
        expected_tool_sets=[
            ["receivable_payable_lookup", "result_visualization"],
            ["receivable_payable_lookup"],
            ["customer_receivable_lookup", "supplier_payable_lookup", "result_visualization"],
            ["customer_receivable_lookup", "supplier_payable_lookup"],
        ],
    ),
]


TABLES = [
    "users", "sessions", "stores", "store_memberships", "products", "customers",
    "suppliers", "sale_orders", "purchase_orders", "finance_records",
    "inventory_snapshots", "inventory_ledger", "accounts", "payments",
    "account_transfers", "media_assets", "agent_conversations", "agent_messages", "agent_run_audits",
    "agent_run_audit_events", "agent_drafts",
]


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def run_command(args: list[str]) -> str:
    result = subprocess.run(args, check=False, capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(f"command failed: {' '.join(args)}: {result.stderr.strip()}")
    return result.stdout


def psql(sql: str) -> str:
    return run_command([
        "docker", "exec", DB_CONTAINER, "psql", "-U", DB_USER, "-d", DB_NAME,
        "-At", "-c", sql,
    ])


def live_prompt_overrides(owner_user_id: int) -> dict[str, str]:
    """Build realistic write prompts from the isolated database, never fixtures."""
    if owner_user_id <= 0:
        return {}

    def rows(sql: str) -> list[list[str]]:
        output = psql(sql)
        return [line.split("|", 2) for line in output.splitlines() if line.strip()]

    products = rows(
        "SELECT id, name, stock FROM products "
        f"WHERE owner_user_id = {owner_user_id} ORDER BY id LIMIT 3"
    )
    customers = rows(
        "SELECT id, name FROM customers "
        f"WHERE owner_user_id = {owner_user_id} ORDER BY id LIMIT 1"
    )
    suppliers = rows(
        "SELECT id, name FROM suppliers "
        f"WHERE owner_user_id = {owner_user_id} ORDER BY id LIMIT 1"
    )
    sale_orders = rows(
        "SELECT so.id, so.order_no, COALESCE(soi.product_name, '') "
        "FROM sale_orders so LEFT JOIN sale_order_items soi ON soi.order_id = so.id "
        f"WHERE so.owner_user_id = {owner_user_id} ORDER BY so.id DESC LIMIT 1"
    )
    overrides: dict[str, str] = {}
    if products:
        product_name = products[0][1]
        stock = products[0][2]
        overrides["generate_poster_prompt"] = (
            f"帮我用商品“{product_name}”写一段海报提示词，先不要生成图片。"
        )
        overrides["create_inventory_count_draft"] = (
            f"帮我按商品“{product_name}”现在的库存 {stock} 件做一次盘点，先生成草稿给我确认。"
        )
    if suppliers and products:
        overrides["create_purchase_order"] = (
            f"向供应商“{suppliers[0][1]}”买商品“{products[0][1]}”，数量 1、单价 1.23，"
            "先做采购草稿让我看看。"
        )
    if customers and products:
        overrides["create_sale_order"] = (
            f"给客户“{customers[0][1]}”开一单，商品用“{products[0][1]}”，数量 1、单价 1.23，"
            "先生成销售草稿。"
        )
    if sale_orders:
        order_no = sale_orders[0][1]
        product_name = sale_orders[0][2] or (products[0][1] if products else "这张单里的商品")
        overrides["sales_full_chain_lookup"] = (
            f"把销售单号“{order_no}”的销售、收款和退货关联记录串起来给我看。"
        )
        overrides["create_sales_return"] = (
            f"把销售单号“{order_no}”里的“{product_name}”退 1 件，原因写全量工具测试，"
            "先做草稿让我确认。"
        )
    return overrides


def db_counts() -> dict[str, int]:
    query = " UNION ALL ".join(f"SELECT '{table}', count(*) FROM {table}" for table in TABLES)
    output = psql(query)
    return {line.split("|", 1)[0]: int(line.split("|", 1)[1]) for line in output.splitlines() if "|" in line}


def db_ids(table: str, owner_user_id: int = 0) -> set[int]:
    owner_filter = f" WHERE owner_user_id = {owner_user_id}" if owner_user_id > 0 else ""
    output = psql(f"SELECT id FROM {table}{owner_filter} ORDER BY id")
    return {int(line.strip()) for line in output.splitlines() if line.strip().isdigit()}


def http_json(path: str, method: str = "GET", payload: dict | None = None, token: str | None = None,
              timeout: int = 180) -> tuple[int, dict | str, int]:
    headers = {"Accept": "application/json", "Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8") if payload is not None else None
    request = urllib.request.Request(BASE_URL + path, data=data, headers=headers, method=method)
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read()
            status = response.status
    except urllib.error.HTTPError as error:
        raw = error.read()
        status = error.code
    except Exception as error:  # network or timeout evidence belongs in the case JSON
        return 0, {"transport_error": str(error)}, int((time.perf_counter() - started) * 1000)
    elapsed = int((time.perf_counter() - started) * 1000)
    try:
        return status, json.loads(raw.decode("utf-8")), elapsed
    except json.JSONDecodeError:
        return status, raw.decode("utf-8", errors="replace"), elapsed


def find_active_token() -> tuple[str, dict]:
    owner_filter = f" AND user_id = {TEST_OWNER_USER_ID}" if TEST_OWNER_USER_ID > 0 else ""
    sql = (
        "SELECT token FROM sessions WHERE is_active = true "
        "AND expires_at > (extract(epoch from now()) * 1000)::bigint"
        f"{owner_filter} ORDER BY id DESC"
    )
    candidates = [line.strip() for line in psql(sql).splitlines() if line.strip()]
    for candidate in candidates:
        status, body, _ = http_json("/v2/auth/users/me", token=candidate, timeout=30)
        if status == 200 and isinstance(body, dict):
            return candidate, body
    raise RuntimeError("no active cloned-database session can authenticate to the isolated API")


def tool_calls(body: object) -> list[dict]:
    if not isinstance(body, dict):
        return []
    data = body.get("data")
    return data.get("tool_calls", []) if isinstance(data, dict) and isinstance(data.get("tool_calls"), list) else []


def response_data(body: object) -> dict:
    if not isinstance(body, dict):
        return {}
    return body.get("data", {}) if isinstance(body.get("data"), dict) else {}


def new_draft_ids(before: set[int]) -> set[int]:
    return db_ids("agent_drafts", TEST_OWNER_USER_ID) - before


def db_draft_rows(draft_ids: set[int]) -> dict[int, dict]:
    """Read newly-created draft payloads for business-level evaluation."""
    if not draft_ids:
        return {}
    ids = ",".join(str(draft_id) for draft_id in sorted(draft_ids))
    output = psql(
        "SELECT json_build_object(" 
        "'id', id, 'draft_type', draft_type, 'status', status, "
        "'content_json', content_json) "
        f"FROM agent_drafts WHERE owner_user_id = {TEST_OWNER_USER_ID} "
        f"AND id IN ({ids}) ORDER BY id"
    )
    rows: dict[int, dict] = {}
    for line in output.splitlines():
        if not line.strip():
            continue
        row = json.loads(line)
        content = row.get("content_json")
        if isinstance(content, str):
            try:
                row["content"] = json.loads(content)
            except json.JSONDecodeError:
                row["content"] = None
        else:
            row["content"] = content
        rows[int(row["id"])] = row
    return rows


def choose_expected_set(actual: set[str], candidates: list[set[str]]) -> set[str]:
    """Choose an allowed set for diagnostics, preferring an exact match.

    The returned set is only diagnostic metadata when no exact match exists.
    It must not turn optional tools in a larger candidate into required tools.
    """
    exact = [candidate for candidate in candidates if actual == candidate]
    if exact:
        return min(exact, key=lambda candidate: sorted(candidate))
    return min(
        candidates,
        key=lambda candidate: (
            len(candidate - actual),
            len(actual - candidate),
            len(candidate),
        ),
    )


def sequence_matches(actual: list[str], candidates: list[list[str]]) -> bool:
    return any(actual == candidate for candidate in candidates)


def blocked_http_status(status: int) -> bool:
    """Return whether the response prevents a functional verdict."""
    return status in {0, 401, 403, 408, 429, 502, 503, 504}


def upstream_rate_limited(body: object, provider_evidence: str = "") -> bool:
    """Detect explicit upstream rate-limit evidence, including HTTP 200 shells."""
    response_text = json.dumps(body, ensure_ascii=False) if body is not None else ""
    evidence_text = f"{response_text}\n{provider_evidence}"
    return (
        "UPSTREAM_RATE_LIMITED" in evidence_text
        or "Concurrency exceeded" in evidence_text
        or bool(re.search(
            r"(?:HTTP(?: status)?|status|code)\s*[:=]?\s*429\b",
            evidence_text,
            re.IGNORECASE,
        ))
    )


def read_provider_evidence(path: str) -> str:
    if not path:
        return ""
    try:
        with open(path, encoding="utf-8") as evidence_file:
            return evidence_file.read()
    except OSError:
        return ""


def provider_evidence_for(template: str, test_id: str, tool: str) -> tuple[str, str]:
    """Resolve a case-scoped evidence path without changing the artifact."""
    if not template:
        return "", ""
    path = template.format(test_id=test_id, tool=tool)
    return path, read_provider_evidence(path)


def finalize_cleanup(
    evaluation: dict,
    draft_outcomes: list[dict],
    conversation_cleanup: dict,
    post_cleanup_counts: dict[str, int],
    before_counts: dict[str, int],
) -> dict:
    """Attach cleanup evidence and fail a pass when cleanup was incomplete."""
    draft_cleanup_pass = all(item.get("cleanup_pass") is True for item in draft_outcomes)
    conversation_status = conversation_cleanup.get("http_status")
    conversation_cleanup_pass = conversation_cleanup.get(
        "cleanup_pass", conversation_status in {None, 200}
    ) is True
    agent_drafts_restored = (
        post_cleanup_counts.get("agent_drafts") == before_counts.get("agent_drafts")
    )
    cleanup_pass = draft_cleanup_pass and conversation_cleanup_pass and agent_drafts_restored
    evaluation["cleanup_pass"] = cleanup_pass
    evaluation["cleanup_details"] = {
        "draft_cleanup_pass": draft_cleanup_pass,
        "conversation_cleanup_pass": conversation_cleanup_pass,
        "agent_drafts_restored": agent_drafts_restored,
    }
    if not cleanup_pass:
        evaluation["result"] = "Failed" if evaluation["result"] == "Passed" else evaluation["result"]
        evaluation["reasons"].append("cleanup did not restore the pre-case state")
    return evaluation


def evaluate(case_info: dict, status: int, body: object, elapsed: int,
             before_counts: dict[str, int], after_counts: dict[str, int],
             before_drafts: set[int], after_drafts: set[int],
             draft_rows: dict[int, dict] | None = None,
             provider_evidence: str = "") -> dict:
    name = case_info["tool"]
    expected_tool_sets = case_info.get("expected_tool_sets") or [case_info.get("expected_tools") or [name]]
    calls = tool_calls(body)
    completed_sequence = [
        item.get("tool_name")
        for item in calls
        if item.get("status") == "completed" and not item.get("error_code") and item.get("tool_name")
    ]
    completed_names = {
        item.get("tool_name")
        for item in calls
        if item.get("status") == "completed" and not item.get("error_code")
    }
    matching_set = choose_expected_set(
        set(completed_names),
        [set(tool_set) for tool_set in expected_tool_sets],
    )
    expected_tools = matching_set
    actual_tools = {item.get("tool_name") for item in calls if item.get("tool_name")}
    expected_set_match = any(actual_tools == set(tool_set) for tool_set in expected_tool_sets)
    unexpected_tools = sorted(actual_tools - expected_tools)
    selected = [item for item in calls if item.get("tool_name") in expected_tools]
    completed = [item for item in selected if item.get("status") == "completed" and not item.get("error_code")]
    missing_tools = [tool for tool in expected_tools if not any(item.get("tool_name") == tool for item in selected)]
    incomplete_tools = [tool for tool in expected_tools if not any(
        item.get("tool_name") == tool and item.get("status") == "completed" and not item.get("error_code")
        for item in selected
    )]
    data = response_data(body)
    answer = data.get("answer")
    visualization_called = "result_visualization" in [item.get("tool_name") for item in calls]
    result_blocks = data.get("result_blocks")
    false_chart_without_visualization = (
        not visualization_called
        and isinstance(result_blocks, list)
        and any(
            isinstance(block, dict)
            and block.get("type") in {"chart", "line_chart", "bar_chart", "pie_chart"}
            for block in result_blocks
        )
    )
    business_tables = [table for table in TABLES if table not in {
        "users", "sessions", "stores", "store_memberships", "agent_conversations",
        "agent_messages", "agent_run_audits", "agent_run_audit_events", "agent_drafts",
    }]
    business_delta = {table: after_counts.get(table, 0) - before_counts.get(table, 0) for table in business_tables}
    draft_delta = after_counts.get("agent_drafts", 0) - before_counts.get("agent_drafts", 0)
    reasons: list[str] = []
    result = "Passed"
    rate_limited = upstream_rate_limited(body, provider_evidence)
    if status != 200:
        result = "Blocked" if blocked_http_status(status) else "Failed"
        reasons.append(f"http_status={status}")
    elif not isinstance(body, dict) or body.get("code") not in (0, "0"):
        result = "Failed"
        reasons.append(f"api_code={body.get('code') if isinstance(body, dict) else 'non_json'}")
    elif not expected_set_match:
        result = "Failed"
        reasons.append(
            f"actual tool set did not match any allowed set: {sorted(actual_tools)}"
        )
    elif missing_tools:
        result = "Failed"
        reasons.append(f"expected tools were not all selected: {missing_tools}")
    elif incomplete_tools:
        result = "Failed"
        reasons.append(f"expected tools did not all complete: {incomplete_tools}")
    if unexpected_tools:
        result = "Failed"
        reasons.append(f"unapproved extra tools were called: {unexpected_tools}")
    expected_tool_sequences = case_info.get("expected_tool_sequences") or []
    if expected_tool_sequences and not sequence_matches(completed_sequence, expected_tool_sequences):
        result = "Failed"
        reasons.append(
            f"completed tool order did not match allowed sequences: {completed_sequence}"
        )
    if not isinstance(answer, str) or not answer.strip():
        result = "Failed"
        reasons.append("formal answer is empty")
    if case_info["kind"] == "create":
        if not after_drafts:
            result = "Failed"
            reasons.append("create-only tool produced no draft")
        if any(value != 0 for value in business_delta.values()):
            result = "Failed"
            reasons.append(f"business table changed: {business_delta}")
        if name == "create_customer":
            customer_drafts = [
                row for row in (draft_rows or {}).values()
                if row.get("draft_type") == "create_customer"
            ]
            if not any(isinstance(row.get("content"), dict)
                       and str(row["content"].get("name", "")).strip()
                       for row in customer_drafts):
                result = "Failed"
                reasons.append("customer draft has no non-empty customer name")
        if name == "create_sale_order":
            sale_drafts = [
                row for row in (draft_rows or {}).values()
                if row.get("draft_type") == "create_sale_order"
            ]
            valid_sale_draft = False
            for row in sale_drafts:
                content = row.get("content")
                if not isinstance(content, dict):
                    continue
                customer_id = content.get("customer_id")
                items = content.get("items")
                if not isinstance(customer_id, int) or isinstance(customer_id, bool) or customer_id <= 0:
                    continue
                if not isinstance(items, list) or not items:
                    continue
                if all(
                    isinstance(item, dict)
                    and isinstance(item.get("product_id"), int)
                    and not isinstance(item.get("product_id"), bool)
                    and item["product_id"] > 0
                    and isinstance(item.get("quantity"), (int, float))
                    and not isinstance(item.get("quantity"), bool)
                    and item["quantity"] > 0
                    and isinstance(item.get("unit_price"), (int, float))
                    and not isinstance(item.get("unit_price"), bool)
                    and item["unit_price"] >= 0
                    for item in items
                ):
                    valid_sale_draft = True
                    break
            if not valid_sale_draft:
                result = "Failed"
                reasons.append("sale order draft must contain a positive customer_id and at least one valid product line")
    elif any(value != 0 for value in business_delta.values()):
        result = "Failed"
        reasons.append(f"read-only tool changed business table: {business_delta}")
    if status != 200 and blocked_http_status(status):
        result = "Blocked"
    if rate_limited:
        result = "Blocked"
        reasons.append("upstream provider rate limit is traceable for this run")
    if false_chart_without_visualization:
        result = "Failed"
        reasons.append("response contains a chart block without result_visualization")
    return {
        "result": result,
        "reasons": reasons,
        "target_selected": not missing_tools,
        "target_completed": not incomplete_tools,
        "selected_tools": [item.get("tool_name") for item in selected],
        "expected_tools": sorted(expected_tools),
        "expected_tool_sets": expected_tool_sets,
        "expected_set_match": expected_set_match,
        "expected_tool_sequences": expected_tool_sequences,
        "missing_tools": missing_tools,
        "unexpected_tools": unexpected_tools,
        "answer_present": bool(isinstance(answer, str) and answer.strip()),
        "tool_names": [item.get("tool_name") for item in calls],
        "unexpected_visualization": not any("result_visualization" in tool_set for tool_set in expected_tool_sets)
            and visualization_called,
        "false_chart_without_visualization": false_chart_without_visualization,
        "business_delta": business_delta,
        "draft_delta": draft_delta,
        "new_draft_ids": sorted(after_drafts),
        "elapsed_ms": elapsed,
        "provider_rate_limited": rate_limited,
    }


def cleanup_message(body: object) -> str:
    if isinstance(body, dict):
        values = [body.get("message"), body.get("error"), body.get("detail")]
        data = body.get("data")
        if isinstance(data, dict):
            values.extend([data.get("message"), data.get("error"), data.get("detail")])
        return " ".join(str(value) for value in values if value is not None)
    return str(body)


def is_idempotent_missing(status: int, body: object, entity: str) -> bool:
    if status not in {404, 422}:
        return False
    message = cleanup_message(body).lower()
    if entity == "draft":
        return "草稿不存在" in message or "draft not found" in message
    if entity == "conversation":
        return "会话不存在" in message or "conversation not found" in message
    return False


def cleanup_drafts(token: str, draft_ids: set[int]) -> list[dict]:
    outcomes = []
    for draft_id in sorted(draft_ids):
        status, body, elapsed = http_json(f"/v2/agent/drafts/{draft_id}", method="DELETE", payload={}, token=token, timeout=30)
        idempotent = is_idempotent_missing(status, body, "draft")
        outcomes.append({
            "draft_id": draft_id,
            "http_status": status,
            "response": body,
            "elapsed_ms": elapsed,
            "idempotent_missing": idempotent,
            "cleanup_pass": status == 200 or idempotent,
        })
    return outcomes


def conversation_id_from(body: object) -> int | None:
    value = response_data(body).get("conversation_id")
    try:
        return int(value) if value is not None else None
    except (TypeError, ValueError):
        return None


def cleanup_conversation(token: str, conversation_id: int | None) -> dict:
    if conversation_id is None:
        return {"conversation_id": None, "http_status": None, "response": None, "elapsed_ms": 0}
    status, body, elapsed = http_json(
        f"/v2/agent/conversations/{conversation_id}",
        method="DELETE",
        payload={},
        token=token,
        timeout=30,
    )
    idempotent = is_idempotent_missing(status, body, "conversation")
    return {
        "conversation_id": conversation_id,
        "http_status": status,
        "response": body,
        "elapsed_ms": elapsed,
        "idempotent_missing": idempotent,
        "cleanup_pass": status == 200 or idempotent,
    }


def main() -> int:
    EVIDENCE_ROOT.mkdir(parents=True, exist_ok=True)
    (EVIDENCE_ROOT / "cases").mkdir(exist_ok=True)
    started_at = utc_now()
    run_id = os.environ.get("RUN_ID", f"agent-all-tools-{started_at.replace(':', '').replace('+00:00', 'Z')}")
    token, auth_body = find_active_token()
    active_prompt_overrides = dict(PROMPT_OVERRIDES)
    active_prompt_overrides.update(live_prompt_overrides(TEST_OWNER_USER_ID))
    (EVIDENCE_ROOT / "auth-me.json").write_text(json.dumps(auth_body, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    pre_counts = db_counts()
    (EVIDENCE_ROOT / "db-counts-pre.json").write_text(json.dumps(pre_counts, indent=2) + "\n", encoding="utf-8")
    (EVIDENCE_ROOT / "case-status.tsv").write_text(
        "run_id\ttest_id\ttool\tkind\thttp_status\tresult\tselected\tcompleted\tcleanup_pass\telapsed_ms\n",
        encoding="utf-8",
    )
    results = []
    selected_cases = [
        (index, {
            **case_info,
            "prompt": active_prompt_overrides.get(case_info["tool"], case_info["prompt"]),
        })
        for index, case_info in enumerate(CASES, start=1)
        if not CASE_FILTER or case_info["tool"] in CASE_FILTER
    ]
    for index, case_info in selected_cases:
        test_id = f"AG-FT-BE-ALL-{index:03d}"
        before = db_counts()
        before_drafts = db_ids("agent_drafts", TEST_OWNER_USER_ID)
        payload = {"message": case_info["prompt"], "stream": False}
        status, body, elapsed = http_json("/v2/agent/chat", method="POST", payload=payload, token=token)
        after = db_counts()
        after_drafts = new_draft_ids(before_drafts)
        draft_rows = db_draft_rows(after_drafts)
        provider_evidence_path, provider_evidence = provider_evidence_for(
            PROVIDER_EVIDENCE_PATH, test_id, case_info["tool"]
        )
        evaluation = evaluate(
            case_info, status, body, elapsed, before, after,
            before_drafts, after_drafts, draft_rows, provider_evidence
        )
        record = {
            "schema_version": "server-agent-tool-case.v1",
            "test_id": test_id,
            "category_id": "agent-server-tool-invocation",
            "wave_id": "Wave 1",
            "captured_at": utc_now(),
            "run_id": run_id,
            "env": {
            "host": TEST_HOST,
            "runtime": TEST_RUNTIME,
            "database": TEST_DATABASE,
            "base_url": BASE_URL,
            "model": MODEL,
                "wire_api": WIRE_API,
                "tool_choice": TOOL_CHOICE,
                "provider_evidence_path": provider_evidence_path or None,
            "provider_base_url": PROVIDER_BASE_URL,
            "api_key": "[REDACTED]",
            },
            "account": {
                "source": TEST_ACCOUNT_SOURCE,
                "owner_user_id": TEST_OWNER_USER_ID or None,
                "details": "see auth-me.json",
            },
            "pre_state": before,
            "actions": {
                "request": payload,
                "http_status": status,
                "elapsed_ms": elapsed,
                "tool_choice": TOOL_CHOICE,
            },
            "response": body,
            "model_and_tool_trace": {
                "plan_source": response_data(body).get("plan_source"),
                "mode": response_data(body).get("mode"),
                "llm_status": response_data(body).get("llm_status"),
                "plan_summary": response_data(body).get("plan_summary"),
                "tool_calls": tool_calls(body),
                "observability": response_data(body).get("observability"),
            },
            "database_after": after,
            "draft_rows": draft_rows,
            "expected": {
                "target_tool": case_info["tool"],
                "expected_tools": case_info.get("expected_tools", [case_info["tool"]]),
                "expected_tool_sets": case_info.get("expected_tool_sets", [case_info.get("expected_tools", [case_info["tool"]])]),
                "kind": case_info["kind"],
                "read_tools_do_not_change_business_tables": True,
                "create_tools_only_create_drafts": True,
                "formal_answer_present": True,
            },
            "actual": evaluation,
            "cleanup": {"new_drafts": sorted(after_drafts), "pending_cleanup": True},
            "result": evaluation["result"],
        }
        path = EVIDENCE_ROOT / "cases" / f"{index:03d}-{case_info['tool']}.json"
        path.write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        cleanup = cleanup_drafts(token, after_drafts)
        conversation_id = conversation_id_from(body)
        conversation_cleanup = cleanup_conversation(token, conversation_id)
        record["cleanup"] = {
            "new_drafts": sorted(after_drafts),
            "draft_outcomes": cleanup,
            "conversation": conversation_cleanup,
        }
        record["post_cleanup_counts"] = db_counts()
        evaluation = finalize_cleanup(
            evaluation,
            cleanup,
            conversation_cleanup,
            record["post_cleanup_counts"],
            before,
        )
        record["actual"] = evaluation
        record["result"] = evaluation["result"]
        with (EVIDENCE_ROOT / "case-status.tsv").open("a", encoding="utf-8") as status_file:
            status_file.write("\t".join([
                run_id, test_id, case_info["tool"], case_info["kind"], str(status), evaluation["result"],
                str(evaluation["target_selected"]), str(evaluation["target_completed"]),
                str(evaluation["cleanup_pass"]), str(elapsed),
            ]) + "\n")
        path.write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        results.append({"test_id": test_id, "run_id": run_id, "tool": case_info["tool"], "kind": case_info["kind"], **evaluation, "http_status": status})
        print(json.dumps(results[-1], ensure_ascii=False), flush=True)
    post_counts = db_counts()
    summary = {
        "schema_version": "server-agent-all-tools.v1",
        "started_at": started_at,
        "completed_at": utc_now(),
        "run_id": run_id,
        "host": TEST_HOST,
        "runtime": TEST_RUNTIME,
        "database": TEST_DATABASE,
        "source": TEST_SOURCE,
        "owner_user_id": TEST_OWNER_USER_ID or None,
        "provider": {"base_url": PROVIDER_BASE_URL, "model": MODEL, "wire_api": WIRE_API, "api_key": "[REDACTED]"},
        "total": len(results),
        "passed": sum(item["result"] == "Passed" for item in results),
        "failed": sum(item["result"] == "Failed" for item in results),
        "blocked": sum(item["result"] == "Blocked" for item in results),
        "read_total": sum(item["kind"] == "read" for item in results),
        "create_total": sum(item["kind"] == "create" for item in results),
        "read_passed": sum(item["kind"] == "read" and item["result"] == "Passed" for item in results),
        "create_passed": sum(item["kind"] == "create" and item["result"] == "Passed" for item in results),
        "results": results,
        "pre_counts": pre_counts,
        "post_counts": post_counts,
        "post_business_mutation": {table: post_counts.get(table, 0) - pre_counts.get(table, 0) for table in TABLES if table not in {"users", "sessions", "stores", "store_memberships", "agent_conversations", "agent_messages", "agent_run_audits", "agent_run_audit_events", "agent_drafts"}},
        "release_gate": "Passed only if every case is Passed and no business table changed",
    }
    (EVIDENCE_ROOT / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: summary[key] for key in ("total", "passed", "failed", "blocked", "read_total", "read_passed", "create_total", "create_passed")}, ensure_ascii=False))
    return 0 if summary["passed"] == summary["total"] else 1


if __name__ == "__main__":
    sys.exit(main())
