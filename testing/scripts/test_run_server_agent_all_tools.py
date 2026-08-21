#!/usr/bin/env python3
"""Offline contract checks for the server Agent tool-set evaluator."""

from __future__ import annotations

import unittest
from unittest.mock import patch

from testing.scripts.run_server_agent_all_tools import (
    CASES,
    evaluate,
    finalize_cleanup,
    cleanup_drafts,
    cleanup_conversation,
    is_idempotent_missing,
    upstream_rate_limited,
)


BUSINESS_TABLES = (
    "products", "customers", "suppliers", "sale_orders", "purchase_orders",
    "finance_records", "inventory_snapshots", "inventory_ledger", "accounts",
    "payments", "account_transfers", "media_assets",
)


def case_by_tool(name: str) -> dict:
    return next(item for item in CASES if item["tool"] == name)


def body_for_tools(tool_names: list[str]) -> dict:
    return {
        "code": 0,
        "data": {
            "answer": "基于真实工具结果生成的回答",
            "tool_calls": [
                {
                    "tool_name": name,
                    "status": "completed",
                    "error_code": None,
                }
                for name in tool_names
            ],
        },
    }


def zero_counts() -> dict[str, int]:
    return {table: 0 for table in (*BUSINESS_TABLES, "agent_drafts")}


class AgentToolSetEvaluationTest(unittest.TestCase):
    def test_case_order_and_count_are_stable(self) -> None:
        self.assertEqual(len(CASES), 60)
        self.assertEqual(
            [item["tool"] for item in CASES[44:45]],
            ["create_customer"],
        )
        self.assertEqual(
            [item["tool"] for item in CASES[58:60]],
            ["multi_inventory_restock", "multi_receivable_payable"],
        )

    def evaluate_case(
        self,
        tool: str,
        calls: list[str],
        *,
        drafts: set[int] | None = None,
        draft_rows: dict[int, dict] | None = None,
    ) -> dict:
        counts = zero_counts()
        return evaluate(
            case_by_tool(tool),
            200,
            body_for_tools(calls),
            1,
            counts,
            counts,
            set(),
            drafts or set(),
            draft_rows,
        )

    def test_customer_group_lookup_is_not_allowed_without_explicit_group_request(self) -> None:
        result = self.evaluate_case(
            "create_customer",
            ["partner_group_lookup", "create_customer"],
            drafts={9001},
        )
        self.assertEqual(result["result"], "Failed")
        self.assertFalse(result["expected_set_match"])

    def test_inventory_restock_requires_restock_tool_before_visualization(self) -> None:
        result = self.evaluate_case(
            "multi_inventory_restock",
            ["inventory_panorama_lookup", "result_visualization"],
        )
        self.assertEqual(result["result"], "Failed")
        self.assertFalse(result["expected_set_match"])

    def test_empty_sales_trend_allows_no_chart_result(self) -> None:
        result = evaluate(
            case_by_tool("result_visualization"),
            200,
            {
                "code": 0,
                "data": {
                    "answer": "最近一周没有可用销售数据，暂不生成图表。",
                    "tool_calls": [
                        {
                            "tool_name": "sales_trend_lookup",
                            "status": "completed",
                            "error_code": None,
                        }
                    ],
                    "result_blocks": [],
                },
            },
            10,
            zero_counts(),
            zero_counts(),
            set(),
            set(),
        )
        self.assertEqual(result["result"], "Passed")
        self.assertFalse(result["unexpected_visualization"])
        self.assertFalse(result["false_chart_without_visualization"])

    def test_chart_block_without_visualization_tool_is_failed(self) -> None:
        result = evaluate(
            case_by_tool("result_visualization"),
            200,
            {
                "code": 0,
                "data": {
                    "answer": "图表",
                    "tool_calls": [
                        {
                            "tool_name": "sales_trend_lookup",
                            "status": "completed",
                            "error_code": None,
                        }
                    ],
                    "result_blocks": [{"type": "line_chart"}],
                },
            },
            10,
            zero_counts(),
            zero_counts(),
            set(),
            set(),
        )
        self.assertEqual(result["result"], "Failed")
        self.assertTrue(result["false_chart_without_visualization"])

    def test_missing_required_restock_tool_is_not_hidden_by_larger_candidate(self) -> None:
        result = self.evaluate_case(
            "multi_inventory_restock",
            ["inventory_panorama_lookup"],
        )
        self.assertEqual(result["result"], "Failed")
        self.assertFalse(result["expected_set_match"])

    def test_unapproved_extra_tool_remains_failed(self) -> None:
        result = self.evaluate_case(
            "multi_inventory_restock",
            [
                "inventory_panorama_lookup",
                "smart_restock_lookup",
                "inventory_low_stock_lookup",
                "result_visualization",
            ],
        )
        self.assertEqual(result["result"], "Failed")
        self.assertFalse(result["expected_set_match"])

    def test_empty_sale_order_draft_is_not_a_pass(self) -> None:
        result = self.evaluate_case(
            "create_sale_order",
            ["create_sale_order"],
            drafts={9002},
            draft_rows={
                9002: {
                    "draft_type": "create_sale_order",
                    "content": {"customer_id": None, "items": []},
                }
            },
        )
        self.assertEqual(result["result"], "Failed")
        self.assertTrue(any("positive customer_id" in reason for reason in result["reasons"]))

    def test_complete_sale_order_draft_passes_business_validation(self) -> None:
        result = self.evaluate_case(
            "create_sale_order",
            ["create_sale_order"],
            drafts={9003},
            draft_rows={
                9003: {
                    "draft_type": "create_sale_order",
                    "content": {
                        "customer_id": 17,
                        "items": [
                            {
                                "product_id": 23,
                                "quantity": 1,
                                "unit_price": 1.23,
                            }
                        ],
                    },
                }
            },
        )
        self.assertEqual(result["result"], "Passed")

    def test_transport_timeout_is_blocked(self) -> None:
        counts = zero_counts()
        result = evaluate(
            case_by_tool("create_customer"),
            0,
            {"transport_error": "timed out"},
            180000,
            counts,
            counts,
            set(),
            set(),
        )
        self.assertEqual(result["result"], "Blocked")

    def test_http_200_shell_with_explicit_provider_rate_limit_is_blocked(self) -> None:
        counts = zero_counts()
        body = {
            "code": 0,
            "data": {
                "answer": "",
                "mode": "llm_answer_unavailable",
                "plan_source": "llm_planning_failed",
                "tool_calls": [],
            },
        }
        result = evaluate(
            case_by_tool("create_customer"),
            200,
            body,
            151180,
            counts,
            counts,
            set(),
            set(),
            provider_evidence='HTTP 429 {"code":"UPSTREAM_RATE_LIMITED","message":"Concurrency exceeded"}',
        )
        self.assertEqual(result["result"], "Blocked")
        self.assertTrue(result["provider_rate_limited"])

    def test_http_200_planning_failure_without_provider_evidence_is_failed(self) -> None:
        counts = zero_counts()
        result = evaluate(
            case_by_tool("create_customer"),
            200,
            {
                "code": 0,
                "data": {
                    "answer": "",
                    "mode": "llm_answer_unavailable",
                    "plan_source": "llm_planning_failed",
                    "tool_calls": [],
                },
            },
            151180,
            counts,
            counts,
            set(),
            set(),
        )
        self.assertEqual(result["result"], "Failed")
        self.assertFalse(result["provider_rate_limited"])

    def test_cleanup_failure_downgrades_a_pass(self) -> None:
        counts = zero_counts()
        result = self.evaluate_case("account_balance_lookup", ["account_balance_lookup"])
        finalized = finalize_cleanup(
            result,
            [{"draft_id": 9004, "http_status": 500, "cleanup_pass": False}],
            {"conversation_id": 77, "http_status": 200},
            counts,
            counts,
        )
        self.assertEqual(finalized["result"], "Failed")
        self.assertFalse(finalized["cleanup_pass"])

    def test_already_deleted_draft_is_idempotent_only_with_missing_message(self) -> None:
        self.assertTrue(is_idempotent_missing(404, {"message": "草稿不存在"}, "draft"))
        self.assertTrue(is_idempotent_missing(422, {"message": "draft not found"}, "draft"))
        self.assertFalse(is_idempotent_missing(404, {"message": "权限不足"}, "draft"))

    @patch("testing.scripts.run_server_agent_all_tools.http_json")
    def test_cleanup_drafts_treats_repeated_delete_as_idempotent(self, http_json) -> None:
        http_json.side_effect = [
            (404, {"message": "草稿不存在"}, 2),
            (422, {"message": "权限不足"}, 3),
        ]
        outcomes = cleanup_drafts("token", {9007, 9008})
        self.assertTrue(outcomes[0]["idempotent_missing"])
        self.assertTrue(outcomes[0]["cleanup_pass"])
        self.assertFalse(outcomes[1]["idempotent_missing"])
        self.assertFalse(outcomes[1]["cleanup_pass"])

    @patch("testing.scripts.run_server_agent_all_tools.http_json")
    def test_cleanup_conversation_treats_repeated_delete_as_idempotent(self, http_json) -> None:
        http_json.return_value = (422, {"message": "会话不存在"}, 2)
        outcome = cleanup_conversation("token", 9010)
        self.assertTrue(outcome["idempotent_missing"])
        self.assertTrue(outcome["cleanup_pass"])

    def test_cleanup_final_state_required_after_idempotent_draft_delete(self) -> None:
        result = self.evaluate_case("account_balance_lookup", ["account_balance_lookup"])
        finalized = finalize_cleanup(
            result,
            [{"draft_id": 9005, "http_status": 404, "idempotent_missing": True, "cleanup_pass": True}],
            {"conversation_id": 78, "http_status": 200, "cleanup_pass": True},
            {"agent_drafts": 3},
            {"agent_drafts": 3},
        )
        self.assertTrue(finalized["cleanup_pass"])

    def test_cleanup_does_not_hide_real_draft_delete_failure(self) -> None:
        result = self.evaluate_case("account_balance_lookup", ["account_balance_lookup"])
        finalized = finalize_cleanup(
            result,
            [{"draft_id": 9006, "http_status": 404, "response": {"message": "权限不足"}, "cleanup_pass": False}],
            {"conversation_id": 79, "http_status": 200, "cleanup_pass": True},
            {"agent_drafts": 4},
            {"agent_drafts": 3},
        )
        self.assertEqual(finalized["result"], "Failed")
        self.assertFalse(finalized["cleanup_pass"])

    def test_cleanup_without_drafts_is_valid_when_state_is_restored(self) -> None:
        result = self.evaluate_case("account_balance_lookup", ["account_balance_lookup"])
        finalized = finalize_cleanup(
            result,
            [],
            {"conversation_id": None, "http_status": None},
            zero_counts(),
            zero_counts(),
        )
        self.assertEqual(finalized["result"], "Passed")
        self.assertTrue(finalized["cleanup_pass"])


if __name__ == "__main__":
    unittest.main()
