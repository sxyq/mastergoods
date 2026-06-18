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

    func testPartnerRecordsDecodeStringIDs() throws {
        let customerData = Data(
            """
            {
              "id": "80001",
              "name": "测试客户",
              "phone": "13800001111",
              "balance": 120.5,
              "status": 1
            }
            """.utf8
        )
        let supplierData = Data(
            """
            {
              "id": 90001,
              "name": "测试供应商",
              "phone": "13800002222",
              "balance": 88.0,
              "status": 1
            }
            """.utf8
        )

        let customer = try JSONDecoder().decode(CustomerRecord.self, from: customerData)
        let supplier = try JSONDecoder().decode(SupplierRecord.self, from: supplierData)

        XCTAssertEqual(customer.id.rawValue, "80001")
        XCTAssertEqual(supplier.id.rawValue, "90001")
        XCTAssertEqual(customer.name, "测试客户")
        XCTAssertEqual(supplier.name, "测试供应商")
    }

    func testPartnerGroupRecordDecodesLargeIDs() throws {
        let data = Data(
            """
            {
              "id": 910000000000001,
              "partner_type": "customer",
              "name": "批发客户",
              "status": 1,
              "sort_order": 5,
              "created_at": 1710000000000,
              "updated_at": 1710003600000
            }
            """.utf8
        )

        let group = try JSONDecoder().decode(PartnerGroupRecord.self, from: data)
        XCTAssertEqual(group.id.rawValue, "910000000000001")
        XCTAssertEqual(group.partnerType, "customer")
        XCTAssertEqual(group.name, "批发客户")
        XCTAssertEqual(group.sortOrder, 5)
    }

    func testCustomerWritePayloadEncodesSnakeCaseShape() throws {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let payload = CustomerWritePayload(
            name: "测试客户",
            phone: "13800001111",
            level: 2,
            groupId: EntityID(rawValue: "3001"),
            primaryContactName: "张三",
            primaryContactPhone: "13800002222",
            address: "上海",
            notes: "重点客户",
            balance: 88.5,
            status: 1
        )

        let data = try encoder.encode(payload)
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]

        XCTAssertEqual(json?["name"] as? String, "测试客户")
        XCTAssertEqual(json?["phone"] as? String, "13800001111")
        XCTAssertEqual(json?["level"] as? Int, 2)
        XCTAssertEqual(json?["group_id"] as? String, "3001")
        XCTAssertEqual(json?["primary_contact_name"] as? String, "张三")
    }

    func testProductWritePayloadEncodesSupplierRelationsWithStringIDs() throws {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let payload = ProductWritePayload(
            code: "P-001",
            name: "测试商品",
            categoryId: EntityID(rawValue: "11"),
            unitId: EntityID(rawValue: "22"),
            salePrice: 18.8,
            purchasePrice: 9.9,
            priceLevels: [
                ProductPriceLevelWritePayload(levelId: EntityID(rawValue: "31"), price: 18.8),
            ],
            supplierRelations: [
                ProductSupplierRelationWritePayload(
                    productId: EntityID(rawValue: "900100000000000001"),
                    supplierId: EntityID(rawValue: "800200000000000002"),
                    isDefault: true,
                    purchasePriority: 1,
                    lastPurchasePrice: 8.6,
                    notes: "主供"
                ),
            ],
            stock: 50,
            safeStock: 12,
            status: 1
        )

        let data = try encoder.encode(payload)
        let json = try XCTUnwrap(try JSONSerialization.jsonObject(with: data) as? [String: Any])
        let supplierRelations = try XCTUnwrap(json["supplier_relations"] as? [[String: Any]])
        let firstRelation = try XCTUnwrap(supplierRelations.first)

        XCTAssertEqual(firstRelation["product_id"] as? String, "900100000000000001")
        XCTAssertEqual(firstRelation["supplier_id"] as? String, "800200000000000002")
        XCTAssertEqual(firstRelation["is_default"] as? Bool, true)
        XCTAssertEqual(firstRelation["purchase_priority"] as? Int, 1)
        XCTAssertEqual(firstRelation["notes"] as? String, "主供")
    }

    func testPayOrderCreatePayloadEncodesSupplierAndAccountIDsAsStrings() throws {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let payload = PayOrderCreatePayload(
            supplierId: EntityID(rawValue: "800200000000000002"),
            supplierName: "华东供货中心",
            amount: 2560.75,
            method: 2,
            referenceNo: "PO-2026-009",
            notes: "六月账期结算",
            accountId: EntityID(rawValue: "700300000000000003"),
            status: 1
        )

        let data = try encoder.encode(payload)
        let json = try XCTUnwrap(try JSONSerialization.jsonObject(with: data) as? [String: Any])

        XCTAssertEqual(json["supplier_id"] as? String, "800200000000000002")
        XCTAssertEqual(json["supplier_name"] as? String, "华东供货中心")
        XCTAssertEqual(json["account_id"] as? String, "700300000000000003")
        XCTAssertEqual(json["reference_no"] as? String, "PO-2026-009")
        XCTAssertEqual(json["status"] as? Int, 1)
    }

    func testReportModelsDecodeLargeIDsAndNumericFields() throws {
        let productProfitData = Data(
            """
            {
              "product_id": 910000000000001,
              "product_code": "P-889",
              "product_name": "高周转商品",
              "total_sales_amount": 5000.5,
              "total_cost_amount": 3200.25,
              "total_profit_amount": 1800.25,
              "profit_rate": 0.3600
            }
            """.utf8
        )

        let receivableData = Data(
            """
            {
              "customer_id": "920000000000002",
              "customer_name": "华东客户",
              "phone": "13800009999",
              "balance": 2600.75
            }
            """.utf8
        )

        let productProfit = try JSONDecoder().decode(ProfitByProductReport.self, from: productProfitData)
        let receivable = try JSONDecoder().decode(CustomerReceivableReport.self, from: receivableData)

        XCTAssertEqual(productProfit.productId.rawValue, "910000000000001")
        XCTAssertEqual(productProfit.productCode, "P-889")
        XCTAssertEqual(productProfit.totalProfitAmount, 1800.25)
        XCTAssertEqual(receivable.customerId.rawValue, "920000000000002")
        XCTAssertEqual(receivable.customerName, "华东客户")
        XCTAssertEqual(receivable.balance, 2600.75)
    }

    func testFinanceRecordDecodesAndResolvesMethodLabel() throws {
        let data = Data(
            """
            {
              "id": "930000000000003",
              "record_no": "FR-2026-001",
              "type": 2,
              "category": "房租水电",
              "partner_name": "物业中心",
              "amount": 1888.50,
              "method": 1,
              "notes": "六月租金",
              "created_at": 1710000000000,
              "updated_at": 1710003600000
            }
            """.utf8
        )

        let record = try JSONDecoder().decode(FinanceRecordSummary.self, from: data)
        XCTAssertEqual(record.id.rawValue, "930000000000003")
        XCTAssertEqual(record.typeLabel, "支出")
        XCTAssertEqual(record.methodLabel, "现金")
        XCTAssertEqual(record.partnerName, "物业中心")
    }

    func testInventorySnapshotSummaryDecodesLargeIDsAndOptionalValue() throws {
        let data = Data(
            """
            {
              "id": 930000000000001,
              "product_id": "920000000000011",
              "product_code": "SKU-7788",
              "product_name": "库存快照商品",
              "warehouse_id": null,
              "quantity": 58.5,
              "unit_cost": 12.8,
              "total_value": 748.8,
              "snapshot_date": 1718668800000,
              "created_at": 1718672400000
            }
            """.utf8
        )

        let snapshot = try JSONDecoder().decode(InventorySnapshotSummary.self, from: data)
        XCTAssertEqual(snapshot.id.rawValue, "930000000000001")
        XCTAssertEqual(snapshot.productId.rawValue, "920000000000011")
        XCTAssertEqual(snapshot.productCode, "SKU-7788")
        XCTAssertEqual(snapshot.quantity, 58.5)
        XCTAssertEqual(snapshot.totalValue, 748.8)
    }

    func testInventoryMonthlyStatsDecodesLargeIDsAndNullableFields() throws {
        let data = Data(
            """
            {
              "id": "940000000000002",
              "product_id": 940000000000003,
              "product_code": "SKU-9900",
              "product_name": "月度库存商品",
              "warehouse_id": null,
              "month": 6,
              "year": 2026,
              "quantity_in": 120.0,
              "quantity_out": 45.0,
              "quantity_adjust": 3.0,
              "quantity_begin": 80.0,
              "quantity_end": 158.0,
              "total_cost_in": 3000.5,
              "total_cost_out": 1120.0,
              "created_at": 1718672400000,
              "updated_at": 1718676000000
            }
            """.utf8
        )

        let stats = try JSONDecoder().decode(InventoryMonthlyStats.self, from: data)
        XCTAssertEqual(stats.id.rawValue, "940000000000002")
        XCTAssertEqual(stats.productId.rawValue, "940000000000003")
        XCTAssertEqual(stats.month, 6)
        XCTAssertEqual(stats.year, 2026)
        XCTAssertEqual(stats.quantityAdjust, 3.0)
        XCTAssertEqual(stats.quantityEnd, 158.0)
        XCTAssertEqual(stats.totalCostIn, 3000.5)
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
              "status": "open",
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
        XCTAssertEqual(draft.status, "open")
        XCTAssertTrue(draft.contentJson.contains("库存和回款风险"))
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
