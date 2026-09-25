import XCTest
@testable import ZhihuijiIOS

final class ModelDecodingTests: XCTestCase {
    func testEntityIDDecodesFromNumber() throws {
        let value = try JSONDecoder().decode(EntityID.self, from: Data("123456".utf8))
        XCTAssertEqual(value.rawValue, "123456")
    }

    func testCurrentStoreProfileDecodesPermissionsAndIDs() throws {
        let data = Data(
            """
            {
              "store_id": "90001",
              "store_name": "智慧记示例门店",
              "owner_user_id": 80001,
              "current_user_id": "80002",
              "current_user_name": "小杨",
              "current_user_phone": "13800000001",
              "role": "OWNER",
              "title": "店长",
              "status": 1,
              "permissions": ["dashboard:view", "sales:view", "agent:view", "users:manage"],
              "member_count": 4,
              "enabled_member_count": 3,
              "disabled_member_count": 1
            }
            """.utf8
        )

        let profile = try JSONDecoder().decode(CurrentStoreProfile.self, from: data)
        XCTAssertEqual(profile.storeId.rawValue, "90001")
        XCTAssertEqual(profile.ownerUserId.rawValue, "80001")
        XCTAssertEqual(profile.currentUserId.rawValue, "80002")
        XCTAssertEqual(profile.role, .owner)
        XCTAssertTrue(profile.permissions.contains(.usersManage))
        XCTAssertTrue(profile.permissions.contains(.agentView))
    }

    func testAgentStreamEventDecodesResultBlockAndRunID() throws {
        let data = Data(
            """
            {
              "event_type": "result_block",
              "run_id": "run-001",
              "conversation_id": "conv-001",
              "block": {
                "block_type": "kpi_grid",
                "title": "经营概览",
                "data": {
                  "kpis": [
                    { "label": "销售额", "value": "¥888.00", "trend_direction": "up" }
                  ]
                }
              },
              "timestamp": 1710000000000
            }
            """.utf8
        )

        let event = try JSONDecoder().decode(AgentStreamEvent.self, from: data)
        XCTAssertEqual(event.eventType, "result_block")
        XCTAssertEqual(event.runId, "run-001")
        XCTAssertEqual(event.conversationId?.rawValue, "conv-001")
        XCTAssertEqual(event.block?.blockType, "kpi_grid")
        XCTAssertEqual(event.block?.title, "经营概览")
    }

    func testAgentStreamEventDecodesProgressDraftAndContextFields() throws {
        let draftData = Data(
            """
            {
              "event_type": "draft_created",
              "run_id": "run-draft",
              "conversation_id": "conv-draft",
              "draft_id": "99001",
              "draft_type": "purchase_plan",
              "title": "补货草稿",
              "timestamp": 1710000000000
            }
            """.utf8
        )
        let progressData = Data(
            """
            {
              "event_type": "tool_progress",
              "run_id": "run-tool",
              "tool_name": "inventory_low_stock_lookup",
              "message": "正在读取低库存商品",
              "timestamp": 1710000000001
            }
            """.utf8
        )
        let compactedData = Data(
            """
            {
              "event_type": "context_compacted",
              "run_id": "run-context",
              "compacted_count": 18,
              "summary": "已将早期追问整理为经营上下文",
              "timestamp": 1710000000002
            }
            """.utf8
        )

        let draft = try JSONDecoder().decode(AgentStreamEvent.self, from: draftData)
        XCTAssertEqual(draft.eventType, "draft_created")
        XCTAssertEqual(draft.draftId?.rawValue, "99001")
        XCTAssertEqual(draft.draftType, "purchase_plan")
        XCTAssertEqual(draft.title, "补货草稿")

        let progress = try JSONDecoder().decode(AgentStreamEvent.self, from: progressData)
        XCTAssertEqual(progress.eventType, "tool_progress")
        XCTAssertEqual(progress.toolName, "inventory_low_stock_lookup")
        XCTAssertEqual(progress.message, "正在读取低库存商品")

        let compacted = try JSONDecoder().decode(AgentStreamEvent.self, from: compactedData)
        XCTAssertEqual(compacted.eventType, "context_compacted")
        XCTAssertEqual(compacted.compactedCount, 18)
        XCTAssertEqual(compacted.summary, "已将早期追问整理为经营上下文")
    }

    func testAgentResultBlockPayloadsDecodeIntoStructuredModels() throws {
        let data = Data(
            """
            {
              "run_id": "run-structured-1",
              "conversation_id": "conv-structured-1",
              "answer": "已完成分析",
              "blocks": [],
              "tool_calls": [],
              "evidence_refs": [],
              "result_blocks": [
                {
                  "block_type": "markdown",
                  "title": "销售结论",
                  "data": {
                    "markdown": "## 销售结论\\n- 今日销售额 1280 元"
                  }
                },
                {
                  "block_type": "evidence_card",
                  "title": "查询依据",
                  "data": {
                    "items": [
                      {
                        "label": "欠款客户数",
                        "value": "2个",
                        "source": "tool:customer_receivable_lookup",
                        "tool_call_id": "run-structured-1:customer_receivable_lookup:0",
                        "query_window": { "owner_scope": "current_owner", "limit": 10 },
                        "is_truncated": false
                      }
                    ]
                  }
                },
                {
                  "block_type": "line_chart",
                  "title": "近7天销售趋势",
                  "data": {
                    "labels": ["06-13", "06-14", "06-15"],
                    "series": [
                      { "name": "销售额", "data": [120.0, 188.5, 210.0], "color": "#005BBF" }
                    ]
                  }
                },
                {
                  "block_type": "draft_card",
                  "title": "补货草稿",
                  "data": {
                    "draft_id": "90001",
                    "draft_type": "purchase_plan",
                    "title": "补货建议",
                    "summary": "建议优先补齐低库存商品",
                    "item_count": 3,
                    "total_amount": "1280.00",
                    "partner_name": "华东供货中心",
                    "warnings": ["库存阈值仍需人工确认"]
                  }
                }
              ]
            }
            """.utf8
        )

        let response = try JSONDecoder().decode(AgentChatResponse.self, from: data)
        XCTAssertEqual(response.resultBlocks.count, 4)

        let markdown = try XCTUnwrap(response.resultBlocks[0].data?.decode(as: AgentTextBlockData.self))
        XCTAssertEqual(markdown.markdown, "## 销售结论\n- 今日销售额 1280 元")

        let evidence = try XCTUnwrap(response.resultBlocks[1].data?.decode(as: AgentEvidenceCardBlockData.self))
        XCTAssertEqual(evidence.items.first?.label, "欠款客户数")
        XCTAssertEqual(evidence.items.first?.toolCallId, "run-structured-1:customer_receivable_lookup:0")

        let lineChart = try XCTUnwrap(response.resultBlocks[2].data?.decode(as: AgentLineChartBlockData.self))
        XCTAssertEqual(lineChart.labels.count, 3)
        XCTAssertEqual(lineChart.series.first?.data.last, 210.0)

        let draftCard = try XCTUnwrap(response.resultBlocks[3].data?.decode(as: AgentDraftCardBlockData.self))
        XCTAssertEqual(draftCard.draftId.rawValue, "90001")
        XCTAssertEqual(draftCard.partnerName, "华东供货中心")
        XCTAssertEqual(draftCard.warnings?.first, "库存阈值仍需人工确认")
    }

    func testAgentMessageDecodesPersistedResultBlocksFromStructuredDataJsonArray() throws {
        let message = AgentMessage(
            id: "msg-001",
            conversationId: "conv-001",
            role: "assistant",
            messageType: "text",
            content: "analysis completed",
            structuredDataJson: """
            [
              {
                "block_type": "markdown",
                "title": "Summary",
                "data": {
                  "markdown": "## Summary\\n- Sales amount 1280"
                }
              },
              {
                "block_type": "table",
                "title": "Customers",
                "data": {
                  "headers": ["Customer", "Balance"],
                  "rows": [["Alice", "128.00"]]
                }
              }
            ]
            """,
            createdAt: 1710000000000
        )

        XCTAssertEqual(message.resultBlocks.count, 2)
        XCTAssertEqual(message.resultBlocks[0].blockType, "markdown")
        XCTAssertEqual(message.resultBlocks[1].blockType, "table")

        let markdown = try XCTUnwrap(message.resultBlocks[0].data?.decode(as: AgentTextBlockData.self))
        XCTAssertTrue(markdown.markdown?.contains("Sales amount") == true)
    }

    func testAgentMessageDecodesPersistedResultBlocksFromEnvelopeShapes() throws {
        let resultBlocksMessage = AgentMessage(
            id: "msg-002",
            conversationId: "conv-001",
            role: "assistant",
            messageType: "text",
            content: "chart completed",
            structuredDataJson: """
            {
              "result_blocks": [
                {
                  "block_type": "bar_chart",
                  "title": "Sales",
                  "data": {
                    "labels": ["Mon", "Tue"],
                    "series": [
                      { "name": "Sales", "data": [100, 180], "color": "#005BBF" }
                    ]
                  }
                }
              ]
            }
            """,
            createdAt: 1710000000000
        )
        let blocksMessage = AgentMessage(
            id: "msg-003",
            conversationId: "conv-001",
            role: "assistant",
            messageType: "text",
            content: "evidence completed",
            structuredDataJson: """
            {
              "blocks": [
                {
                  "block_type": "evidence_card",
                  "title": "Evidence",
                  "data": {
                    "items": [
                      {
                        "label": "Sale orders",
                        "value": "2",
                        "source": "tool:sales_lookup",
                        "tool_call_id": "tool-001",
                        "is_truncated": false
                      }
                    ]
                  }
                }
              ]
            }
            """,
            createdAt: 1710000000000
        )

        XCTAssertEqual(resultBlocksMessage.resultBlocks.first?.blockType, "bar_chart")
        XCTAssertEqual(blocksMessage.resultBlocks.first?.blockType, "evidence_card")

        let chart = try XCTUnwrap(resultBlocksMessage.resultBlocks.first?.data?.decode(as: AgentBarChartBlockData.self))
        XCTAssertEqual(chart.labels, ["Mon", "Tue"])
        XCTAssertEqual(chart.series.first?.data.last, 180)

        let evidence = try XCTUnwrap(blocksMessage.resultBlocks.first?.data?.decode(as: AgentEvidenceCardBlockData.self))
        XCTAssertEqual(evidence.items.first?.toolCallId, "tool-001")
    }

    func testAgentChartBlockInferenceFallsBackWhenChartTypeIsMissing() throws {
        let lineChartBlock = AgentResultBlock(
            blockType: "chart",
            title: "Sales Trend",
            data: JSONValue.object([
                "labels": .array([.string("Mon"), .string("Tue")]),
                "series": .array([
                    .object([
                        "name": .string("Sales"),
                        "data": .array([.number(100), .number(180)])
                    ])
                ])
            ])
        )
        let barChartBlock = AgentResultBlock(
            blockType: "chart",
            title: "Sales By Category",
            data: JSONValue.object([
                "categories": .array([.string("A"), .string("B")]),
                "series": .array([
                    .object([
                        "name": .string("Sales"),
                        "data": .array([.number(12), .number(28)])
                    ])
                ])
            ])
        )

        XCTAssertEqual(zhihuijiInferChartBlockType(lineChartBlock), "line_chart")
        XCTAssertEqual(zhihuijiInferChartBlockType(barChartBlock), "bar_chart")
    }

    func testJSONValueObjectPreviewTextSummarizesKeyFields() {
        let value = JSONValue.object([
            "alpha": .string("A"),
            "beta": .number(2),
            "gamma": .bool(true),
            "delta": .null
        ])

        let preview = value.previewText

        XCTAssertEqual(preview, "alpha：A | beta：2 | delta：- | gamma：是")
    }

    func testStoreStaffMemberDecodesPermissionsAndSessions() throws {
        let data = Data(
            """
            {
              "user_id": "90011",
              "phone": "13800000099",
              "nickname": "采购小李",
              "role": "PURCHASING",
              "title": "采购员工",
              "status": 1,
              "permissions": ["purchase:view", "purchase:write"],
              "created_at": 1710000000000,
              "updated_at": 1710003600000,
              "active_sessions": 2,
              "store_id": "90001",
              "store_name": "智慧记示例门店"
            }
            """.utf8
        )

        let member = try JSONDecoder().decode(StoreStaffMember.self, from: data)
        XCTAssertEqual(member.userId.rawValue, "90011")
        XCTAssertEqual(member.role, .purchasing)
        XCTAssertEqual(member.activeSessions, 2)
        XCTAssertTrue(member.permissions.contains(.purchaseWrite))
    }

    func testAgentDraftDecodesLargeIDsAndContentJSON() throws {
        let data = Data(
            """
            {
              "id": "950000000000001",
              "conversation_id": 950000000000009,
              "draft_type": "question",
              "title": "今日经营风险",
              "content_json": "{\\"question\\":\\"今天有哪些库存和回款风险？\\"}",
              "status": "active",
              "created_at": 1718672400000,
              "updated_at": 1718676000000
            }
            """.utf8
        )

        let draft = try JSONDecoder().decode(AgentDraft.self, from: data)
        XCTAssertEqual(draft.id.rawValue, "950000000000001")
        XCTAssertEqual(draft.conversationId?.rawValue, "950000000000009")
        XCTAssertEqual(draft.draftType, "question")
        XCTAssertEqual(draft.title, "今日经营风险")
        XCTAssertEqual(draft.status, "active")
        XCTAssertTrue(draft.contentJson.contains("库存和回款风险"))
    }

    func testSyncAndMediaModelsDecodeSnakeCaseFields() throws {
        let data = Data(
            """
            {
              "status": "ok",
              "message": "ready",
              "owner_scoped": true,
              "server_time": 1718676000000,
              "supported_entity_types": ["product", "sale_order"],
              "uploadable_entity_types": ["product"]
            }
            """.utf8
        )
        let health = try JSONDecoder().decode(SyncHealthRecord.self, from: data)
        XCTAssertEqual(health.status, "ok")
        XCTAssertTrue(health.ownerScoped)
        XCTAssertEqual(health.supportedEntityTypes.count, 2)

        let mediaData = Data(
            """
            {
              "id": "990000000000001",
              "asset_type": "product_cover",
              "storage_provider": "object_storage",
              "bucket_name": "zhihuiji",
              "object_key": "assets/cover-1.png",
              "original_file_name": "cover.png",
              "mime_type": "image/png",
              "size_bytes": 4096,
              "checksum": "sha256-abc",
              "width": 800,
              "height": 600,
              "metadata_json": "{\\"source\\":\\"ios\\"}",
              "created_at": 1718672400000,
              "updated_at": 1718676000000
            }
            """.utf8
        )
        let asset = try JSONDecoder().decode(MediaAssetRecord.self, from: mediaData)
        XCTAssertEqual(asset.id.rawValue, "990000000000001")
        XCTAssertEqual(asset.assetType, "product_cover")
        XCTAssertEqual(asset.objectKey, "assets/cover-1.png")
        XCTAssertEqual(asset.sizeBytes, 4096)
    }

    func testMediaBindingRecordDecodesTargetIDsAndSortOrder() throws {
        let data = Data(
            """
            {
              "id": "990000000000099",
              "asset_id": "990000000000001",
              "target_type": "product",
              "target_id": "500000000000001",
              "sort_order": 2,
              "created_at": 1718672400000
            }
            """.utf8
        )

        let binding = try JSONDecoder().decode(MediaBindingRecord.self, from: data)
        XCTAssertEqual(binding.id.rawValue, "990000000000099")
        XCTAssertEqual(binding.assetId.rawValue, "990000000000001")
        XCTAssertEqual(binding.targetType, "product")
        XCTAssertEqual(binding.targetId.rawValue, "500000000000001")
        XCTAssertEqual(binding.sortOrder, 2)
    }

    func testImportJobAndSyncPullModelsDecodeOpaqueCursorFields() throws {
        let importJobData = Data(
            """
            {
              "id": 1001,
              "client_id": "ios-sync-1",
              "source_type": "legacy_sqlite",
              "source_uri": "/tmp/legacy.db",
              "source_checksum": null,
              "idempotency_key": "import-ios-1",
              "status": "pending",
              "stage": "queued",
              "retry_count": 0,
              "replay_cursor": "cursor:001",
              "summary_json": null,
              "options_json": "{\\"resetOwnedData\\":false}",
              "failure_code": null,
              "failure_message": null,
              "created_at": 1718672400000,
              "updated_at": 1718676000000,
              "started_at": null,
              "finished_at": null,
              "last_heartbeat_at": null
            }
            """.utf8
        )
        let job = try JSONDecoder().decode(ImportJobRecord.self, from: importJobData)
        XCTAssertEqual(job.id.rawValue, "1001")
        XCTAssertEqual(job.clientId, "ios-sync-1")
        XCTAssertEqual(job.sourceType, "legacy_sqlite")
        XCTAssertEqual(job.status, "pending")
        XCTAssertEqual(job.replayCursor, "cursor:001")

        let pullData = Data(
            """
            {
              "changes": [
                {
                  "entity_type": "product",
                  "entity_id": "10001",
                  "operation": "upsert",
                  "payload": "{}",
                  "updated_at": 1718676000000
                }
              ],
              "effective_cursor": "cursor:001",
              "next_cursor": "cursor:002",
              "has_more": true
            }
            """.utf8
        )
        let pull = try JSONDecoder().decode(SyncPullResponse.self, from: pullData)
        XCTAssertEqual(pull.changes.count, 1)
        XCTAssertEqual(pull.changes.first?.entityId.rawValue, "10001")
        XCTAssertEqual(pull.nextCursor, "cursor:002")
        XCTAssertTrue(pull.hasMore)
    }

    func testLegacySQLiteImportResultDecodesCounts() throws {
        let data = Data(
            """
            {
              "user_id": "980000000000001",
              "phone": "13800000088",
              "nickname": "旧库导入",
              "legacy_db_path": "/tmp/legacy.db",
              "accounts": 3,
              "customers": 12,
              "suppliers": 5,
              "products": 28,
              "sale_orders": 19,
              "sale_order_items": 54,
              "payments": 11,
              "purchase_orders": 14,
              "purchase_order_items": 31,
              "pay_orders": 7,
              "finance_records": 9,
              "inventory_snapshots": 4
            }
            """.utf8
        )

        let result = try JSONDecoder().decode(LegacySQLiteImportResult.self, from: data)
        XCTAssertEqual(result.userId.rawValue, "980000000000001")
        XCTAssertEqual(result.legacyDbPath, "/tmp/legacy.db")
        XCTAssertEqual(result.products, 28)
        XCTAssertEqual(result.inventorySnapshots, 4)
    }

    func testStoreMemberUpdatePayloadEncodesKeepSessionsAndRole() throws {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let payload = StoreMemberUpdatePayload(
            nickname: "采购小李",
            password: nil,
            role: .purchasing,
            title: "采购主管",
            status: 1,
            keepSessions: false
        )

        let data = try encoder.encode(payload)
        let json = try XCTUnwrap(try JSONSerialization.jsonObject(with: data) as? [String: Any])

        XCTAssertEqual(json["nickname"] as? String, "采购小李")
        XCTAssertNil(json["password"])
        XCTAssertEqual(json["role"] as? String, "PURCHASING")
        XCTAssertEqual(json["title"] as? String, "采购主管")
        XCTAssertEqual(json["keep_sessions"] as? Bool, false)
    }
}
