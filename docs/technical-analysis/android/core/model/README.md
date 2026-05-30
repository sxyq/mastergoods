# model 技术分析

## 文件清单
- AgentModels.kt
- ApiResponse.kt
- AuthModels.kt
- FinanceModels.kt
- OrderModels.kt
- PartyModels.kt
- ProductModels.kt
- ReportModels.kt
- StatusConstants.kt
- SyncModels.kt

---

## AgentModels.kt

### AgentWorkbenchDto
- data class / 注解：@Serializable / 职责：Agent 工作台数据传输对象 / 设计模式：DTO

#### kpis: List\<AgentKpi\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：KPI 指标列表
- 建议：无

#### insights: List\<AgentInsight\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：智能洞察列表
- 建议：无

#### receivableReminders: List\<AgentReceivableReminder\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：应收提醒列表
- 建议：无

#### stockAlerts: List\<AgentStockAlert\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：库存预警列表
- 建议：无

#### quickActions: List\<AgentQuickAction\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：快捷操作列表
- 建议：无

### AgentKpi
- data class / 注解：@Serializable / 职责：Agent KPI 指标

#### label: String
- 作用域：成员变量 / 使用场景：指标标签
- 建议：无

#### value: String
- 作用域：成员变量 / 使用场景：指标值
- 建议：无

#### trend: String?
- 作用域：成员变量 / 初始值：null / 使用场景：趋势描述
- 建议：无

#### tone: String?
- 作用域：成员变量 / 初始值：null / 使用场景：色调标识（如 "primary", "success"）
- 建议：建议改为枚举类型，避免字符串硬编码

### AgentInsight
- data class / 注解：@Serializable / 职责：Agent 智能洞察

#### title: String
- 作用域：成员变量 / 使用场景：洞察标题
- 建议：无

#### content: String
- 作用域：成员变量 / 使用场景：洞察内容
- 建议：无

#### severity: String?
- 作用域：成员变量 / 初始值：null / 使用场景：严重程度
- 建议：同 AgentKpi.tone，建议改为枚举

### AgentReceivableReminder
- data class / 注解：@Serializable / 职责：应收款项提醒

#### customerName: String
- 作用域：成员变量 / 使用场景：客户名称
- 建议：无

#### amount: Double
- 作用域：成员变量 / 使用场景：应收金额
- 建议：金融数据建议使用 BigDecimal

#### agingDays: Int
- 作用域：成员变量 / 使用场景：账龄天数
- 建议：无

### AgentStockAlert
- data class / 注解：@Serializable / 职责：库存预警

#### productName: String
- 作用域：成员变量 / 使用场景：商品名称
- 建议：无

#### currentStock: Double
- 作用域：成员变量 / 使用场景：当前库存
- 建议：无

#### safeStock: Double
- 作用域：成员变量 / 使用场景：安全库存
- 建议：无

### AgentQuickAction
- data class / 注解：@Serializable / 职责：快捷操作

#### label: String
- 作用域：成员变量 / 使用场景：操作标签
- 建议：无

#### actionType: String
- 作用域：成员变量 / 使用场景：操作类型标识
- 建议：建议改为枚举

#### params: Map\<String, String\>
- 作用域：成员变量 / 初始值：emptyMap() / 使用场景：操作参数
- 建议：无

### AgentQueryRequest
- data class / 注解：@Serializable / 职责：Agent 查询请求

#### query: String
- 作用域：成员变量 / 使用场景：自然语言查询文本
- 建议：无

### AgentAnswerDto
- data class / 注解：@Serializable / 职责：Agent 查询回答

#### query: String
- 作用域：成员变量 / 初始值："" / 使用场景：原始查询
- 建议：无

#### intent: String
- 作用域：成员变量 / 初始值："" / 使用场景：识别的意图
- 建议：无

#### answer: String
- 作用域：成员变量 / 初始值："" / 使用场景：回答文本
- 建议：无

#### highlights: List\<String\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：高亮关键词
- 建议：无

#### columns: List\<String\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：表格列名
- 建议：无

#### rows: List\<List\<String\>\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：表格行数据
- 建议：无

#### suggestedActions: List\<String\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：建议操作
- 建议：无

### OperationDraftRequest
- data class / 注解：@Serializable / 职责：操作草稿请求

#### instruction: String
- 作用域：成员变量 / 使用场景：操作指令文本
- 建议：无

### OperationDraftDto
- data class / 注解：@Serializable / 职责：操作草稿数据

#### operationType: String
- 作用域：成员变量 / 初始值："" / 使用场景：操作类型
- 建议：建议改为枚举

#### summary: String
- 作用域：成员变量 / 初始值："" / 使用场景：操作摘要
- 建议：无

#### partnerRole: String
- 作用域：成员变量 / 初始值："" / 使用场景：往来方角色
- 建议：无

#### partnerId: Long?
- 作用域：成员变量 / 初始值：null / 使用场景：往来方 ID
- 建议：无

#### partnerName: String
- 作用域：成员变量 / 初始值："" / 使用场景：往来方名称
- 建议：无

#### items: List\<OperationDraftItemDto\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：草稿行项列表
- 建议：无

#### notes: String?
- 作用域：成员变量 / 初始值：null / 使用场景：备注
- 建议：无

#### canSubmit: Boolean
- 作用域：成员变量 / 初始值：false / 使用场景：是否可提交
- 建议：无

#### warnings: List\<String\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：警告列表
- 建议：无

#### suggestedActions: List\<String\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：建议操作
- 建议：无

### OperationDraftItemDto
- data class / 注解：@Serializable / 职责：操作草稿行项

#### productId: Long?
- 作用域：成员变量 / 初始值：null / 使用场景：商品 ID
- 建议：无

#### productCode: String
- 作用域：成员变量 / 初始值："" / 使用场景：商品编码
- 建议：无

#### productName: String
- 作用域：成员变量 / 初始值："" / 使用场景：商品名称
- 建议：无

#### quantity: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：数量
- 建议：无

#### unitPrice: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：单价
- 建议：无

#### amount: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：金额
- 建议：无

#### currentStock: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：当前库存
- 建议：无

### OperationSubmitRequest
- data class / 注解：@Serializable / 职责：操作提交请求

#### draft: OperationDraftDto
- 作用域：成员变量 / 使用场景：要提交的草稿
- 建议：无

### OperationSubmitResultDto
- data class / 注解：@Serializable / 职责：操作提交结果

#### operationType: String
- 作用域：成员变量 / 初始值："" / 使用场景：操作类型
- 建议：无

#### orderId: Long?
- 作用域：成员变量 / 初始值：null / 使用场景：生成的订单 ID
- 建议：无

#### orderNo: String?
- 作用域：成员变量 / 初始值：null / 使用场景：生成的订单编号
- 建议：无

#### message: String
- 作用域：成员变量 / 初始值："" / 使用场景：结果消息
- 建议：无

#### nextAction: String
- 作用域：成员变量 / 初始值："" / 使用场景：下一步建议操作
- 建议：无

### CreateAgentTaskRequest
- data class / 注解：@Serializable / 职责：创建 Agent 任务请求

#### taskType: String
- 作用域：成员变量 / 使用场景：任务类型
- 建议：无

#### title: String
- 作用域：成员变量 / 使用场景：任务标题
- 建议：无

#### input: String?
- 作用域：成员变量 / 初始值：null / 使用场景：任务输入
- 建议：无

### AgentTaskSummaryDto
- data class / 注解：@Serializable / 职责：Agent 任务摘要

#### id: Long
- 作用域：成员变量 / 初始值：0 / 使用场景：任务 ID
- 建议：无

#### taskType: String
- 作用域：成员变量 / 初始值："" / 使用场景：任务类型
- 建议：无

#### title: String
- 作用域：成员变量 / 初始值："" / 使用场景：任务标题
- 建议：无

#### status: String
- 作用域：成员变量 / 初始值："" / 使用场景：任务状态
- 建议：建议改为枚举

#### progress: Int
- 作用域：成员变量 / 初始值：0 / 使用场景：进度百分比
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / 初始值：0L / 使用场景：创建时间
- 建议：无

#### completedAt: Long?
- 作用域：成员变量 / 初始值：null / 使用场景：完成时间
- 建议：无

### AgentTaskDetailDto
- data class / 注解：@Serializable / 职责：Agent 任务详情

#### id: Long
- 作用域：成员变量 / 初始值：0 / 使用场景：任务 ID
- 建议：无

#### taskType: String
- 作用域：成员变量 / 初始值："" / 使用场景：任务类型
- 建议：无

#### title: String
- 作用域：成员变量 / 初始值："" / 使用场景：任务标题
- 建议：无

#### status: String
- 作用域：成员变量 / 初始值："" / 使用场景：任务状态
- 建议：建议改为枚举

#### progress: Int
- 作用域：成员变量 / 初始值：0 / 使用场景：进度
- 建议：无

#### input: String?
- 作用域：成员变量 / 初始值：null / 使用场景：任务输入
- 建议：无

#### result: String?
- 作用域：成员变量 / 初始值：null / 使用场景：任务结果
- 建议：无

#### error: String?
- 作用域：成员变量 / 初始值：null / 使用场景：错误信息
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / 初始值：0L / 使用场景：创建时间
- 建议：无

#### startedAt: Long?
- 作用域：成员变量 / 初始值：null / 使用场景：开始时间
- 建议：无

#### completedAt: Long?
- 作用域：成员变量 / 初始值：null / 使用场景：完成时间
- 建议：无

### AgentNotificationDto
- data class / 注解：@Serializable / 职责：Agent 通知数据传输对象

#### id: Long
- 作用域：成员变量 / 初始值：0 / 使用场景：通知 ID
- 建议：无

#### type: String
- 作用域：成员变量 / 初始值："" / 使用场景：通知类型
- 建议：无

#### title: String
- 作用域：成员变量 / 初始值："" / 使用场景：通知标题
- 建议：无

#### content: String
- 作用域：成员变量 / 初始值："" / 使用场景：通知内容
- 建议：无

#### isRead: Boolean
- 作用域：成员变量 / 初始值：false / 使用场景：是否已读
- 建议：无

#### isDelivered: Boolean
- 作用域：成员变量 / 初始值：false / 使用场景：是否已推送
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / 初始值：0L / 使用场景：创建时间
- 建议：无

---

## ApiResponse.kt

### ApiResponse\<T\>
- data class / 注解：@Serializable / 职责：统一 API 响应包装类 / 设计模式：统一响应模式

#### code: Int
- 作用域：成员变量 / 初始值：0 / 使用场景：业务状态码（0=成功）
- 建议：无

#### message: String
- 作用域：成员变量 / 初始值："" / 使用场景：响应消息
- 建议：无

#### data: T?
- 作用域：成员变量 / 初始值：null / 使用场景：响应数据负载
- 建议：无

#### timestamp: Long
- 作用域：成员变量 / 初始值：0L / 使用场景：服务器时间戳
- 建议：无

---

## AuthModels.kt

### LoginRequest
- data class / 注解：@Serializable / 职责：登录请求体

#### phone: String
- 作用域：成员变量 / 使用场景：手机号
- 建议：无

#### password: String
- 作用域：成员变量 / 使用场景：密码
- 建议：无

### RegisterRequest
- data class / 注解：@Serializable / 职责：注册请求体

#### phone: String
- 作用域：成员变量 / 使用场景：手机号
- 建议：无

#### password: String
- 作用域：成员变量 / 使用场景：密码
- 建议：无

#### verifyCode: String
- 作用域：成员变量 / @SerialName("verify_code") / 使用场景：验证码
- 建议：无

### RefreshRequest
- data class / 注解：@Serializable / 职责：Token 刷新请求体

#### refreshToken: String
- 作用域：成员变量 / @SerialName("refresh_token") / 使用场景：刷新令牌
- 建议：无

### VerifyCodeRequest
- data class / 注解：@Serializable / 职责：验证码请求体

#### phone: String
- 作用域：成员变量 / 使用场景：手机号
- 建议：无

#### type: String
- 作用域：成员变量 / 使用场景：验证码类型
- 建议：建议改为枚举（如 "register", "login"）

### VerifyCodeResponse
- data class / 注解：@Serializable / 职责：验证码响应

#### success: Boolean
- 作用域：成员变量 / 使用场景：是否发送成功
- 建议：无

#### expireSeconds: Int
- 作用域：成员变量 / @SerialName("expire_seconds") / 使用场景：验证码过期秒数
- 建议：无

### AuthResult
- data class / 注解：@Serializable / 职责：认证结果

#### userId: Long
- 作用域：成员变量 / @SerialName("user_id") / 使用场景：用户 ID
- 建议：无

#### token: String
- 作用域：成员变量 / 使用场景：访问令牌
- 建议：无

#### refreshToken: String
- 作用域：成员变量 / @SerialName("refresh_token") / 使用场景：刷新令牌
- 建议：无

#### expiresIn: Int
- 作用域：成员变量 / @SerialName("expires_in") / 使用场景：有效期（秒）
- 建议：无

### UserProfile
- data class / 注解：@Serializable / 职责：用户资料

#### id: Long
- 作用域：成员变量 / 使用场景：用户 ID
- 建议：无

#### phone: String
- 作用域：成员变量 / 使用场景：手机号
- 建议：无

#### nickname: String
- 作用域：成员变量 / 使用场景：昵称
- 建议：无

#### status: Int
- 作用域：成员变量 / 使用场景：用户状态
- 建议：无

---

## FinanceModels.kt

### FinanceRecordDto
- data class / 注解：@Serializable / 职责：财务记录数据传输对象

#### id: Long
- 作用域：成员变量 / 使用场景：记录 ID
- 建议：无

#### recordNo: String
- 作用域：成员变量 / @SerialName("record_no") / 使用场景：记录编号
- 建议：无

#### type: Int
- 作用域：成员变量 / 使用场景：类型（1=收入, 2=支出）
- 建议：无

#### category: String
- 作用域：成员变量 / 使用场景：分类
- 建议：无

#### partnerName: String?
- 作用域：成员变量 / @SerialName("partner_name") / 初始值：null / 使用场景：往来方名称
- 建议：无

#### amount: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：金额
- 建议：金融数据建议使用 BigDecimal

#### method: Int
- 作用域：成员变量 / 使用场景：支付方式
- 建议：无

#### notes: String?
- 作用域：成员变量 / 初始值：null / 使用场景：备注
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / @SerialName("created_at") / 初始值：0L / 使用场景：创建时间
- 建议：无

#### updatedAt: Long
- 作用域：成员变量 / @SerialName("updated_at") / 初始值：0L / 使用场景：更新时间
- 建议：无

### CreateFinanceRecordRequest
- data class / 注解：@Serializable / 职责：创建财务记录请求

#### type: Int
- 作用域：成员变量 / 使用场景：类型
- 建议：无

#### category: String
- 作用域：成员变量 / 使用场景：分类
- 建议：无

#### partnerName: String?
- 作用域：成员变量 / @SerialName("partner_name") / 初始值：null / 使用场景：往来方名称
- 建议：无

#### amount: Double
- 作用域：成员变量 / 使用场景：金额
- 建议：无

#### method: Int?
- 作用域：成员变量 / 初始值：null / 使用场景：支付方式（可选）
- 建议：无

#### notes: String?
- 作用域：成员变量 / 初始值：null / 使用场景：备注
- 建议：无

### FinanceFilter
- data class / 职责：财务记录筛选条件（非序列化，纯客户端使用）

#### keyword: String?
- 作用域：成员变量 / 初始值：null / 使用场景：关键词搜索
- 建议：无

#### type: Int?
- 作用域：成员变量 / 初始值：null / 使用场景：类型筛选
- 建议：无

#### createdAfter: String?
- 作用域：成员变量 / 初始值：null / 使用场景：开始日期
- 建议：无

#### createdBefore: String?
- 作用域：成员变量 / 初始值：null / 使用场景：结束日期
- 建议：无

---

## OrderModels.kt

### SaleOrderDto
- data class / 注解：@Serializable / 职责：销售订单数据传输对象

#### id: Long
- 作用域：成员变量 / 使用场景：订单 ID
- 建议：无

#### orderNo: String
- 作用域：成员变量 / @SerialName("order_no") / 使用场景：订单编号
- 建议：无

#### customerId: Long?
- 作用域：成员变量 / @SerialName("customer_id") / 初始值：null / 使用场景：客户 ID
- 建议：无

#### customerName: String?
- 作用域：成员变量 / @SerialName("customer_name") / 初始值：null / 使用场景：客户名称
- 建议：无

#### items: List\<SaleOrderItemDto\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：订单行项列表
- 建议：无

#### subtotalAmount: Double
- 作用域：成员变量 / @SerialName("subtotal_amount") / 初始值：0.0 / 使用场景：小计金额
- 建议：无

#### discountAmount: Double
- 作用域：成员变量 / @SerialName("discount_amount") / 初始值：0.0 / 使用场景：折扣金额
- 建议：无

#### totalAmount: Double
- 作用域：成员变量 / @SerialName("total_amount") / 初始值：0.0 / 使用场景：总金额
- 建议：无

#### paidAmount: Double
- 作用域：成员变量 / @SerialName("paid_amount") / 初始值：0.0 / 使用场景：已付金额
- 建议：无

#### notes: String?
- 作用域：成员变量 / 初始值：null / 使用场景：备注
- 建议：无

#### status: Int
- 作用域：成员变量 / 初始值：0 / 使用场景：状态
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / @SerialName("created_at") / 初始值：0L / 使用场景：创建时间
- 建议：无

#### updatedAt: Long
- 作用域：成员变量 / @SerialName("updated_at") / 初始值：0L / 使用场景：更新时间
- 建议：无

### SaleOrderItemDto
- data class / 注解：@Serializable / 职责：销售订单行项

#### id: Long
- 作用域：成员变量 / 使用场景：行项 ID
- 建议：无

#### orderId: Long
- 作用域：成员变量 / @SerialName("order_id") / 使用场景：订单 ID
- 建议：无

#### productId: Long
- 作用域：成员变量 / @SerialName("product_id") / 使用场景：商品 ID
- 建议：无

#### productCode: String
- 作用域：成员变量 / @SerialName("product_code") / 使用场景：商品编码
- 建议：无

#### productName: String
- 作用域：成员变量 / @SerialName("product_name") / 使用场景：商品名称
- 建议：无

#### customerId: Long?
- 作用域：成员变量 / @SerialName("customer_id") / 初始值：null / 使用场景：客户 ID
- 建议：行项中包含客户信息存在冗余

#### customerName: String?
- 作用域：成员变量 / @SerialName("customer_name") / 初始值：null / 使用场景：客户名称
- 建议：同上

#### quantity: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：数量
- 建议：无

#### unitPrice: Double
- 作用域：成员变量 / @SerialName("unit_price") / 初始值：0.0 / 使用场景：单价
- 建议：无

#### amount: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：金额
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / @SerialName("created_at") / 初始值：0L / 使用场景：创建时间
- 建议：无

### CreateSaleOrderRequest
- data class / 注解：@Serializable / 职责：创建销售订单请求

#### customerId: Long?
- 作用域：成员变量 / @SerialName("customer_id") / 初始值：null / 使用场景：客户 ID
- 建议：无

#### customerName: String?
- 作用域：成员变量 / @SerialName("customer_name") / 初始值：null / 使用场景：客户名称
- 建议：无

#### items: List\<CreateSaleOrderItemRequest\>
- 作用域：成员变量 / 使用场景：订单行项
- 建议：无

#### notes: String?
- 作用域：成员变量 / 初始值：null / 使用场景：备注
- 建议：无

#### discountAmount: Double
- 作用域：成员变量 / @SerialName("discount_amount") / 初始值：0.0 / 使用场景：折扣金额
- 建议：无

### CreateSaleOrderItemRequest
- data class / 注解：@Serializable / 职责：创建销售订单行项请求

#### productId: Long
- 作用域：成员变量 / @SerialName("product_id") / 使用场景：商品 ID
- 建议：无

#### quantity: Double
- 作用域：成员变量 / 使用场景：数量
- 建议：无

#### unitPrice: Double
- 作用域：成员变量 / @SerialName("unit_price") / 使用场景：单价
- 建议：无

### UpdateSaleDraftRequest
- data class / 注解：@Serializable / 职责：更新销售草稿请求

#### discountAmount: Double?
- 作用域：成员变量 / @SerialName("discount_amount") / 初始值：null / 使用场景：折扣金额
- 建议：无

#### notes: String?
- 作用域：成员变量 / 初始值：null / 使用场景：备注
- 建议：无

### PaymentDto
- data class / 注解：@Serializable / 职责：付款记录数据传输对象

#### id: Long
- 作用域：成员变量 / 使用场景：付款 ID
- 建议：无

#### orderId: Long
- 作用域：成员变量 / @SerialName("order_id") / 使用场景：订单 ID
- 建议：无

#### amount: Double
- 作用域：成员变量 / 使用场景：金额
- 建议：无

#### method: Int
- 作用域：成员变量 / 使用场景：支付方式
- 建议：无

#### referenceNo: String?
- 作用域：成员变量 / @SerialName("reference_no") / 初始值：null / 使用场景：参考编号
- 建议：无

#### type: Int
- 作用域：成员变量 / 使用场景：支付类型（1=收款, 2=退款）
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / @SerialName("created_at") / 初始值：0L / 使用场景：创建时间
- 建议：无

### PaymentRequest
- data class / 注解：@Serializable / 职责：付款请求

#### amount: Double
- 作用域：成员变量 / 使用场景：金额
- 建议：无

#### method: Int
- 作用域：成员变量 / 使用场景：支付方式
- 建议：无

#### referenceNo: String?
- 作用域：成员变量 / @SerialName("reference_no") / 初始值：null / 使用场景：参考编号
- 建议：无

### StatusRequest
- data class / 注解：@Serializable / 职责：状态更新请求

#### status: Int
- 作用域：成员变量 / 使用场景：目标状态值
- 建议：无

### PurchaseOrderDto
- data class / 注解：@Serializable / 职责：采购订单数据传输对象

#### id: Long
- 作用域：成员变量 / 使用场景：订单 ID
- 建议：无

#### orderNo: String
- 作用域：成员变量 / @SerialName("order_no") / 使用场景：订单编号
- 建议：无

#### supplierName: String
- 作用域：成员变量 / @SerialName("supplier_name") / 使用场景：供应商名称
- 建议：无

#### items: List\<PurchaseOrderItemDto\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：订单行项
- 建议：无

#### totalAmount: Double
- 作用域：成员变量 / @SerialName("total_amount") / 初始值：0.0 / 使用场景：总金额
- 建议：无

#### notes: String?
- 作用域：成员变量 / 初始值：null / 使用场景：备注
- 建议：无

#### status: Int
- 作用域：成员变量 / 初始值：0 / 使用场景：状态
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / @SerialName("created_at") / 初始值：0L / 使用场景：创建时间
- 建议：无

#### updatedAt: Long
- 作用域：成员变量 / @SerialName("updated_at") / 初始值：0L / 使用场景：更新时间
- 建议：无

### PurchaseOrderItemDto
- data class / 注解：@Serializable / 职责：采购订单行项

#### id: Long
- 作用域：成员变量 / 使用场景：行项 ID
- 建议：无

#### orderId: Long
- 作用域：成员变量 / @SerialName("order_id") / 使用场景：订单 ID
- 建议：无

#### productCode: String
- 作用域：成员变量 / @SerialName("product_code") / 使用场景：商品编码
- 建议：无

#### productName: String
- 作用域：成员变量 / @SerialName("product_name") / 使用场景：商品名称
- 建议：无

#### quantity: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：数量
- 建议：无

#### unitCost: Double
- 作用域：成员变量 / @SerialName("unit_cost") / 初始值：0.0 / 使用场景：单位成本
- 建议：无

#### amount: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：金额
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / @SerialName("created_at") / 初始值：0L / 使用场景：创建时间
- 建议：无

### CreatePurchaseOrderRequest
- data class / 注解：@Serializable / 职责：创建采购订单请求

#### supplierName: String
- 作用域：成员变量 / @SerialName("supplier_name") / 使用场景：供应商名称
- 建议：无

#### items: List\<CreatePurchaseOrderItemRequest\>
- 作用域：成员变量 / 使用场景：订单行项
- 建议：无

#### notes: String?
- 作用域：成员变量 / 初始值：null / 使用场景：备注
- 建议：无

#### status: Int?
- 作用域：成员变量 / 初始值：null / 使用场景：状态
- 建议：无

### CreatePurchaseOrderItemRequest
- data class / 注解：@Serializable / 职责：创建采购订单行项请求

#### productId: Long?
- 作用域：成员变量 / @SerialName("product_id") / 初始值：null / 使用场景：商品 ID
- 建议：无

#### productCode: String?
- 作用域：成员变量 / @SerialName("product_code") / 初始值：null / 使用场景：商品编码
- 建议：无

#### productName: String?
- 作用域：成员变量 / @SerialName("product_name") / 初始值：null / 使用场景：商品名称
- 建议：无

#### quantity: Double
- 作用域：成员变量 / 使用场景：数量
- 建议：无

#### unitCost: Double
- 作用域：成员变量 / @SerialName("unit_cost") / 使用场景：单位成本
- 建议：无

### PayOrderDto
- data class / 注解：@Serializable / 职责：付款单数据传输对象

#### id: Long
- 作用域：成员变量 / 使用场景：付款单 ID
- 建议：无

#### orderNo: String
- 作用域：成员变量 / @SerialName("order_no") / 使用场景：付款单编号
- 建议：无

#### supplierId: Long?
- 作用域：成员变量 / @SerialName("supplier_id") / 初始值：null / 使用场景：供应商 ID
- 建议：无

#### supplierName: String
- 作用域：成员变量 / @SerialName("supplier_name") / 使用场景：供应商名称
- 建议：无

#### amount: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：金额
- 建议：无

#### method: Int
- 作用域：成员变量 / 初始值：1 / 使用场景：支付方式
- 建议：无

#### referenceNo: String?
- 作用域：成员变量 / @SerialName("reference_no") / 初始值：null / 使用场景：参考编号
- 建议：无

#### notes: String?
- 作用域：成员变量 / 初始值：null / 使用场景：备注
- 建议：无

#### status: Int
- 作用域：成员变量 / 初始值：0 / 使用场景：状态
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / @SerialName("created_at") / 初始值：0L / 使用场景：创建时间
- 建议：无

#### updatedAt: Long
- 作用域：成员变量 / @SerialName("updated_at") / 初始值：0L / 使用场景：更新时间
- 建议：无

### CreatePayOrderRequest
- data class / 注解：@Serializable / 职责：创建付款单请求

#### supplierId: Long?
- 作用域：成员变量 / @SerialName("supplier_id") / 初始值：null / 使用场景：供应商 ID
- 建议：无

#### supplierName: String?
- 作用域：成员变量 / @SerialName("supplier_name") / 初始值：null / 使用场景：供应商名称
- 建议：无

#### amount: Double
- 作用域：成员变量 / 使用场景：金额
- 建议：无

#### method: Int
- 作用域：成员变量 / 使用场景：支付方式
- 建议：无

#### referenceNo: String?
- 作用域：成员变量 / @SerialName("reference_no") / 初始值：null / 使用场景：参考编号
- 建议：无

#### notes: String?
- 作用域：成员变量 / 初始值：null / 使用场景：备注
- 建议：无

#### status: Int?
- 作用域：成员变量 / 初始值：null / 使用场景：状态
- 建议：无

### SaleOrderFilter
- data class / 职责：销售订单筛选条件（非序列化）

#### keyword: String?
- 作用域：成员变量 / 初始值：null / 使用场景：关键词
- 建议：无

#### status: Int?
- 作用域：成员变量 / 初始值：null / 使用场景：状态筛选
- 建议：无

#### minTotalAmount: String?
- 作用域：成员变量 / 初始值：null / 使用场景：最小金额
- 建议：类型为 String 而非 Double，需调用方转换

#### maxTotalAmount: String?
- 作用域：成员变量 / 初始值：null / 使用场景：最大金额
- 建议：同上

#### createdAfter: String?
- 作用域：成员变量 / 初始值：null / 使用场景：开始日期
- 建议：无

#### createdBefore: String?
- 作用域：成员变量 / 初始值：null / 使用场景：结束日期
- 建议：无

#### productKeyword: String?
- 作用域：成员变量 / 初始值：null / 使用场景：商品关键词
- 建议：无

#### paymentStatus: String?
- 作用域：成员变量 / 初始值：null / 使用场景：付款状态
- 建议：类型为 String 而非 Int，与其他 Filter 不一致

### PurchaseOrderFilter
- data class / 职责：采购订单筛选条件

#### keyword: String?
- 作用域：成员变量 / 初始值：null / 使用场景：关键词
- 建议：无

#### status: Int?
- 作用域：成员变量 / 初始值：null / 使用场景：状态筛选
- 建议：无

### PayOrderFilter
- data class / 职责：付款单筛选条件

#### keyword: String?
- 作用域：成员变量 / 初始值：null / 使用场景：关键词
- 建议：无

#### status: Int?
- 作用域：成员变量 / 初始值：null / 使用场景：状态筛选
- 建议：无

#### createdAfter: String?
- 作用域：成员变量 / 初始值：null / 使用场景：开始日期
- 建议：无

#### createdBefore: String?
- 作用域：成员变量 / 初始值：null / 使用场景：结束日期
- 建议：无

---

## PartyModels.kt

### CustomerDto
- data class / 注解：@Serializable / 职责：客户数据传输对象

#### id: Long?
- 作用域：成员变量 / 初始值：null / 使用场景：客户 ID（新建时为 null）
- 建议：无

#### name: String
- 作用域：成员变量 / 初始值："" / 使用场景：客户名称
- 建议：无

#### phone: String
- 作用域：成员变量 / 初始值："" / 使用场景：联系电话
- 建议：无

#### level: Int
- 作用域：成员变量 / 初始值：0 / 使用场景：客户等级
- 建议：无

#### address: String?
- 作用域：成员变量 / 初始值：null / 使用场景：地址
- 建议：无

#### notes: String?
- 作用域：成员变量 / 初始值：null / 使用场景：备注
- 建议：无

#### balance: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：余额
- 建议：金融数据建议使用 BigDecimal

#### status: Int
- 作用域：成员变量 / 初始值：1 / 使用场景：状态
- 建议：无

#### syncStatus: Int?
- 作用域：成员变量 / @SerialName("sync_status") / 初始值：null / 使用场景：同步状态
- 建议：无

#### syncVersion: Long?
- 作用域：成员变量 / @SerialName("sync_version") / 初始值：null / 使用场景：同步版本
- 建议：无

#### createdAt: Long?
- 作用域：成员变量 / @SerialName("created_at") / 初始值：null / 使用场景：创建时间
- 建议：无

#### updatedAt: Long?
- 作用域：成员变量 / @SerialName("updated_at") / 初始值：null / 使用场景：更新时间
- 建议：无

### SupplierDto
- data class / 注解：@Serializable / 职责：供应商数据传输对象

#### id: Long?
- 作用域：成员变量 / 初始值：null / 使用场景：供应商 ID
- 建议：无

#### name: String
- 作用域：成员变量 / 初始值："" / 使用场景：供应商名称
- 建议：无

#### phone: String
- 作用域：成员变量 / 初始值："" / 使用场景：联系电话
- 建议：无

#### address: String?
- 作用域：成员变量 / 初始值：null / 使用场景：地址
- 建议：无

#### notes: String?
- 作用域：成员变量 / 初始值：null / 使用场景：备注
- 建议：无

#### balance: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：余额
- 建议：同 CustomerDto.balance

#### status: Int
- 作用域：成员变量 / 初始值：1 / 使用场景：状态
- 建议：无

#### syncStatus: Int?
- 作用域：成员变量 / @SerialName("sync_status") / 初始值：null / 使用场景：同步状态
- 建议：无

#### syncVersion: Long?
- 作用域：成员变量 / @SerialName("sync_version") / 初始值：null / 使用场景：同步版本
- 建议：无

#### createdAt: Long?
- 作用域：成员变量 / @SerialName("created_at") / 初始值：null / 使用场景：创建时间
- 建议：无

#### updatedAt: Long?
- 作用域：成员变量 / @SerialName("updated_at") / 初始值：null / 使用场景：更新时间
- 建议：无

---

## ProductModels.kt

### ProductDto
- data class / 注解：@Serializable / 职责：商品数据传输对象

#### id: Long?
- 作用域：成员变量 / 初始值：null / 使用场景：商品 ID（新建时为 null）
- 建议：无

#### code: String
- 作用域：成员变量 / 初始值："" / 使用场景：商品编码
- 建议：无

#### name: String
- 作用域：成员变量 / 初始值："" / 使用场景：商品名称
- 建议：无

#### category: String
- 作用域：成员变量 / 初始值："" / 使用场景：分类
- 建议：无

#### unit: String
- 作用域：成员变量 / 初始值："" / 使用场景：计量单位
- 建议：无

#### salePrice: Double
- 作用域：成员变量 / @SerialName("sale_price") / 初始值：0.0 / 使用场景：销售价格
- 建议：金融数据建议使用 BigDecimal

#### purchasePrice: Double
- 作用域：成员变量 / @SerialName("purchase_price") / 初始值：0.0 / 使用场景：采购价格
- 建议：同上

#### stock: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：当前库存
- 建议：无

#### safeStock: Double
- 作用域：成员变量 / @SerialName("safe_stock") / 初始值：0.0 / 使用场景：安全库存
- 建议：无

#### status: Int
- 作用域：成员变量 / 初始值：1 / 使用场景：状态
- 建议：无

#### syncStatus: Int?
- 作用域：成员变量 / @SerialName("sync_status") / 初始值：null / 使用场景：同步状态
- 建议：无

#### syncVersion: Long?
- 作用域：成员变量 / @SerialName("sync_version") / 初始值：null / 使用场景：同步版本
- 建议：无

#### createdAt: Long?
- 作用域：成员变量 / @SerialName("created_at") / 初始值：null / 使用场景：创建时间
- 建议：无

#### updatedAt: Long?
- 作用域：成员变量 / @SerialName("updated_at") / 初始值：null / 使用场景：更新时间
- 建议：无

### ProductAdjustStockRequest
- data class / 注解：@Serializable / 职责：库存调整请求

#### delta: Double
- 作用域：成员变量 / 使用场景：调整量（正数入库，负数出库）
- 建议：无

#### reason: String?
- 作用域：成员变量 / 初始值：null / 使用场景：调整原因
- 建议：无

#### operator: String?
- 作用域：成员变量 / 初始值：null / 使用场景：操作人
- 建议：无

### ProductDraft
- data class / 职责：商品草稿（非序列化，纯客户端使用）

#### code: String
- 作用域：成员变量 / 初始值："" / 使用场景：商品编码
- 建议：无

#### name: String
- 作用域：成员变量 / 初始值："" / 使用场景：商品名称
- 建议：无

#### category: String
- 作用域：成员变量 / 初始值："" / 使用场景：分类
- 建议：无

#### unit: String
- 作用域：成员变量 / 初始值："" / 使用场景：计量单位
- 建议：无

#### salePrice: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：销售价格
- 建议：无

#### purchasePrice: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：采购价格
- 建议：无

#### safeStock: Double
- 作用域：成员变量 / 初始值：0.0 / 使用场景：安全库存
- 建议：无

#### status: Int
- 作用域：成员变量 / 初始值：1 / 使用场景：状态
- 建议：无

#### toDto(): ProductDto
- 返回值：`ProductDto` — 转换为网络传输对象，stock 固定为 0.0
- 实现逻辑：逐字段映射，stock 硬编码为 0.0（新建商品无库存）
- 调用关系：被创建商品时调用
- 建议：无

---

## ReportModels.kt

### SalesSummaryReportDto
- data class / 注解：@Serializable / 职责：销售汇总报表

#### startAt: Long
- 作用域：成员变量 / @SerialName("start_at") / 使用场景：统计开始时间
- 建议：无

#### endAt: Long
- 作用域：成员变量 / @SerialName("end_at") / 使用场景：统计结束时间
- 建议：无

#### totalSalesAmount: Double
- 作用域：成员变量 / @SerialName("total_sales_amount") / 初始值：0.0 / 使用场景：销售总额
- 建议：无

#### totalPaidAmount: Double
- 作用域：成员变量 / @SerialName("total_paid_amount") / 初始值：0.0 / 使用场景：已付总额
- 建议：无

#### totalRefundAmount: Double
- 作用域：成员变量 / @SerialName("total_refund_amount") / 初始值：0.0 / 使用场景：退款总额
- 建议：无

#### totalUnpaidAmount: Double
- 作用域：成员变量 / @SerialName("total_unpaid_amount") / 初始值：0.0 / 使用场景：未付总额
- 建议：无

#### totalOrderCount: Int
- 作用域：成员变量 / @SerialName("total_order_count") / 初始值：0 / 使用场景：订单总数
- 建议：无

### ProfitSummaryReportDto
- data class / 注解：@Serializable / 职责：利润汇总报表

#### startAt: Long
- 作用域：成员变量 / @SerialName("start_at") / 使用场景：统计开始时间
- 建议：无

#### endAt: Long
- 作用域：成员变量 / @SerialName("end_at") / 使用场景：统计结束时间
- 建议：无

#### estimatedCostAmount: Double
- 作用域：成员变量 / @SerialName("estimated_cost_amount") / 初始值：0.0 / 使用场景：估算成本
- 建议：无

#### estimatedProfitAmount: Double
- 作用域：成员变量 / @SerialName("estimated_profit_amount") / 初始值：0.0 / 使用场景：估算利润
- 建议：无

#### estimatedProfitRate: Double
- 作用域：成员变量 / @SerialName("estimated_profit_rate") / 初始值：0.0 / 使用场景：估算利润率
- 建议：无

### RefundRecordReportDto
- data class / 注解：@Serializable / 职责：退款记录报表

#### id: Long
- 作用域：成员变量 / 使用场景：记录 ID
- 建议：无

#### orderNo: String
- 作用域：成员变量 / @SerialName("order_no") / 使用场景：订单编号
- 建议：无

#### customerName: String
- 作用域：成员变量 / @SerialName("customer_name") / 使用场景：客户名称
- 建议：无

#### amount: Double
- 作用域：成员变量 / 使用场景：退款金额
- 建议：无

#### reason: String?
- 作用域：成员变量 / 初始值：null / 使用场景：退款原因
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / @SerialName("created_at") / 使用场景：创建时间
- 建议：无

### StockOutRecordReportDto
- data class / 注解：@Serializable / 职责：出库记录报表

#### productId: Long
- 作用域：成员变量 / @SerialName("product_id") / 使用场景：商品 ID
- 建议：无

#### productCode: String
- 作用域：成员变量 / @SerialName("product_code") / 使用场景：商品编码
- 建议：无

#### productName: String
- 作用域：成员变量 / @SerialName("product_name") / 使用场景：商品名称
- 建议：无

#### quantity: Double
- 作用域：成员变量 / 使用场景：出库数量
- 建议：无

#### saleAmount: Double
- 作用域：成员变量 / @SerialName("sale_amount") / 使用场景：销售金额
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / @SerialName("created_at") / 使用场景：创建时间
- 建议：无

### TopSellingProductReportDto
- data class / 注解：@Serializable / 职责：热销商品报表

#### productId: Long
- 作用域：成员变量 / @SerialName("product_id") / 使用场景：商品 ID
- 建议：无

#### productCode: String
- 作用域：成员变量 / @SerialName("product_code") / 使用场景：商品编码
- 建议：无

#### productName: String
- 作用域：成员变量 / @SerialName("product_name") / 使用场景：商品名称
- 建议：无

#### totalQuantity: Double
- 作用域：成员变量 / @SerialName("total_quantity") / 使用场景：总销量
- 建议：无

#### totalAmount: Double
- 作用域：成员变量 / @SerialName("total_amount") / 使用场景：总金额
- 建议：无

### ProfitByProductReportDto
- data class / 注解：@Serializable / 职责：商品利润报表

#### productId: Long
- 作用域：成员变量 / @SerialName("product_id") / 使用场景：商品 ID
- 建议：无

#### productCode: String
- 作用域：成员变量 / @SerialName("product_code") / 使用场景：商品编码
- 建议：无

#### productName: String
- 作用域：成员变量 / @SerialName("product_name") / 使用场景：商品名称
- 建议：无

#### totalRevenue: Double
- 作用域：成员变量 / @SerialName("total_revenue") / 初始值：0.0 / 使用场景：总收入
- 建议：无

#### totalCost: Double
- 作用域：成员变量 / @SerialName("total_cost") / 初始值：0.0 / 使用场景：总成本
- 建议：无

#### totalProfit: Double
- 作用域：成员变量 / @SerialName("total_profit") / 初始值：0.0 / 使用场景：总利润
- 建议：无

### ProfitByCustomerReportDto
- data class / 注解：@Serializable / 职责：客户利润报表

#### customerId: Long
- 作用域：成员变量 / @SerialName("customer_id") / 使用场景：客户 ID
- 建议：无

#### customerName: String
- 作用域：成员变量 / @SerialName("customer_name") / 使用场景：客户名称
- 建议：无

#### totalRevenue: Double
- 作用域：成员变量 / @SerialName("total_revenue") / 初始值：0.0 / 使用场景：总收入
- 建议：无

#### totalCost: Double
- 作用域：成员变量 / @SerialName("total_cost") / 初始值：0.0 / 使用场景：总成本
- 建议：无

#### totalProfit: Double
- 作用域：成员变量 / @SerialName("total_profit") / 初始值：0.0 / 使用场景：总利润
- 建议：无

### InventoryFlowRecordDto
- data class / 注解：@Serializable / 职责：库存流水记录

#### id: Long
- 作用域：成员变量 / 使用场景：记录 ID
- 建议：无

#### productId: Long
- 作用域：成员变量 / @SerialName("product_id") / 使用场景：商品 ID
- 建议：无

#### productCode: String
- 作用域：成员变量 / @SerialName("product_code") / 使用场景：商品编码
- 建议：无

#### productName: String
- 作用域：成员变量 / @SerialName("product_name") / 使用场景：商品名称
- 建议：无

#### flowType: Int
- 作用域：成员变量 / @SerialName("flow_type") / 使用场景：流向类型（0=出库, 1=入库）
- 建议：无

#### quantity: Double
- 作用域：成员变量 / 使用场景：数量
- 建议：无

#### sourceType: Int
- 作用域：成员变量 / @SerialName("source_type") / 使用场景：来源类型（0=销售, 1=调整）
- 建议：无

#### sourceId: Long?
- 作用域：成员变量 / @SerialName("source_id") / 初始值：null / 使用场景：来源 ID
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / @SerialName("created_at") / 使用场景：创建时间
- 建议：无

### CustomerSalesReportDto
- data class / 注解：@Serializable / 职责：客户销售报表

#### customerId: Long
- 作用域：成员变量 / @SerialName("customer_id") / 使用场景：客户 ID
- 建议：无

#### customerName: String
- 作用域：成员变量 / @SerialName("customer_name") / 使用场景：客户名称
- 建议：无

#### totalAmount: Double
- 作用域：成员变量 / @SerialName("total_amount") / 初始值：0.0 / 使用场景：总金额
- 建议：无

#### orderCount: Int
- 作用域：成员变量 / 初始值：0 / 使用场景：订单数
- 建议：无

### CustomerReceivableReportDto
- data class / 注解：@Serializable / 职责：客户应收报表

#### customerId: Long
- 作用域：成员变量 / @SerialName("customer_id") / 使用场景：客户 ID
- 建议：无

#### customerName: String
- 作用域：成员变量 / @SerialName("customer_name") / 使用场景：客户名称
- 建议：无

#### phone: String
- 作用域：成员变量 / 使用场景：联系电话
- 建议：无

#### balance: Double
- 作用域：成员变量 / 使用场景：应收余额
- 建议：无

### LowStockProductReportDto
- data class / 注解：@Serializable / 职责：低库存商品报表

#### productId: Long
- 作用域：成员变量 / @SerialName("product_id") / 使用场景：商品 ID
- 建议：无

#### productCode: String
- 作用域：成员变量 / @SerialName("product_code") / 使用场景：商品编码
- 建议：无

#### productName: String
- 作用域：成员变量 / @SerialName("product_name") / 使用场景：商品名称
- 建议：无

#### stock: Double
- 作用域：成员变量 / 使用场景：当前库存
- 建议：无

#### safeStock: Double
- 作用域：成员变量 / @SerialName("safe_stock") / 使用场景：安全库存
- 建议：无

### ReconciliationSummaryReportDto
- data class / 注解：@Serializable / 职责：对账汇总报表

#### startAt: Long
- 作用域：成员变量 / @SerialName("start_at") / 使用场景：统计开始时间
- 建议：无

#### endAt: Long
- 作用域：成员变量 / @SerialName("end_at") / 使用场景：统计结束时间
- 建议：无

#### totalReceivableAmount: Double
- 作用域：成员变量 / @SerialName("total_receivable_amount") / 初始值：0.0 / 使用场景：应收总额
- 建议：无

#### totalPayableAmount: Double
- 作用域：成员变量 / @SerialName("total_payable_amount") / 初始值：0.0 / 使用场景：应付总额
- 建议：无

#### totalReceivedAmount: Double
- 作用域：成员变量 / @SerialName("total_received_amount") / 初始值：0.0 / 使用场景：已收总额
- 建议：无

#### totalPaidAmount: Double
- 作用域：成员变量 / @SerialName("total_paid_amount") / 初始值：0.0 / 使用场景：已付总额
- 建议：无

#### netCashFlow: Double
- 作用域：成员变量 / @SerialName("net_cash_flow") / 初始值：0.0 / 使用场景：净现金流
- 建议：无

---

## StatusConstants.kt

### StatusConstants
- object / 职责：定义全局业务状态码常量 / 设计模式：常量类

#### STATUS_ACTIVE: Int
- 作用域：const / 初始值：1 / 使用场景：启用状态
- 建议：无

#### STATUS_INACTIVE: Int
- 作用域：const / 初始值：0 / 使用场景：停用状态
- 建议：无

#### SALE_DRAFT: Int
- 作用域：const / 初始值：0 / 使用场景：销售草稿
- 建议：无

#### SALE_COMPLETED: Int
- 作用域：const / 初始值：1 / 使用场景：销售已完成
- 建议：无

#### SALE_CANCELLED: Int
- 作用域：const / 初始值：2 / 使用场景：销售已取消
- 建议：无

#### PAYMENT_UNPAID: Int
- 作用域：const / 初始值：0 / 使用场景：未付款
- 建议：无

#### PAYMENT_PAID: Int
- 作用域：const / 初始值：1 / 使用场景：已付款
- 建议：无

#### PAYMENT_TYPE_COLLECT: Int
- 作用域：const / 初始值：1 / 使用场景：收款类型
- 建议：无

#### PAYMENT_TYPE_REFUND: Int
- 作用域：const / 初始值：2 / 使用场景：退款类型
- 建议：无

#### PURCHASE_DRAFT: Int
- 作用域：const / 初始值：0 / 使用场景：采购草稿
- 建议：无

#### PURCHASE_RECEIVED: Int
- 作用域：const / 初始值：1 / 使用场景：采购已收货
- 建议：无

#### PAY_ORDER_DRAFT: Int
- 作用域：const / 初始值：0 / 使用场景：付款单草稿
- 建议：无

#### PAY_ORDER_PAID: Int
- 作用域：const / 初始值：1 / 使用场景：付款单已付
- 建议：无

#### PAY_ORDER_CANCELLED: Int
- 作用域：const / 初始值：2 / 使用场景：付款单已取消
- 建议：无

#### FINANCE_INCOME: Int
- 作用域：const / 初始值：1 / 使用场景：收入类型
- 建议：无

#### FINANCE_EXPENSE: Int
- 作用域：const / 初始值：2 / 使用场景：支出类型
- 建议：无

#### FLOW_OUT: Int
- 作用域：const / 初始值：0 / 使用场景：出库流向
- 建议：无

#### FLOW_IN: Int
- 作用域：const / 初始值：1 / 使用场景：入库流向
- 建议：无

#### SOURCE_SALE: Int
- 作用域：const / 初始值：0 / 使用场景：销售来源
- 建议：无

#### SOURCE_ADJUST: Int
- 作用域：const / 初始值：1 / 使用场景：调整来源
- 建议：无

---

## SyncModels.kt

### SyncHealthResult
- data class / 注解：@Serializable / 职责：同步健康检查结果

#### status: String
- 作用域：成员变量 / 初始值："" / 使用场景：健康状态
- 建议：无

#### message: String
- 作用域：成员变量 / 初始值："" / 使用场景：状态消息
- 建议：无

#### serverTime: Long
- 作用域：成员变量 / @SerialName("serverTime") / 初始值：0L / 使用场景：服务器时间
- 建议：无

### SyncChangeDto
- data class / 注解：@Serializable / 职责：同步变更数据

#### entityType: String
- 作用域：成员变量 / @SerialName("entityType") / 使用场景：实体类型
- 建议：无

#### entityId: String
- 作用域：成员变量 / @SerialName("entityId") / 使用场景：实体 ID（String 类型，支持非数字 ID）
- 建议：无

#### operation: String
- 作用域：成员变量 / 使用场景：操作类型（如 "create", "update", "delete"）
- 建议：建议改为枚举

#### payload: String
- 作用域：成员变量 / 使用场景：变更数据 JSON 字符串
- 建议：无

#### updatedAt: Long
- 作用域：成员变量 / @SerialName("updatedAt") / 使用场景：更新时间
- 建议：无

### PullRequest
- data class / 注解：@Serializable / 职责：同步拉取请求

#### sinceCursor: String?
- 作用域：成员变量 / @SerialName("sinceCursor") / 初始值：null / 使用场景：起始游标
- 建议：无

#### limit: Int?
- 作用域：成员变量 / 初始值：200 / 使用场景：拉取数量限制
- 建议：无

### PullResult
- data class / 注解：@Serializable / 职责：同步拉取结果

#### changes: List\<SyncChangeDto\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：变更列表
- 建议：无

#### nextCursor: String
- 作用域：成员变量 / @SerialName("nextCursor") / 初始值："" / 使用场景：下一页游标
- 建议：无

#### hasMore: Boolean
- 作用域：成员变量 / @SerialName("hasMore") / 初始值：false / 使用场景：是否有更多数据
- 建议：无

### UploadRequest
- data class / 注解：@Serializable / 职责：同步上传请求

#### clientId: String
- 作用域：成员变量 / @SerialName("clientId") / 使用场景：客户端唯一标识
- 建议：无

#### changes: List\<SyncChangeDto\>
- 作用域：成员变量 / 初始值：emptyList() / 使用场景：变更列表
- 建议：无

#### lastSyncCursor: String?
- 作用域：成员变量 / @SerialName("lastSyncCursor") / 初始值：null / 使用场景：上次同步游标
- 建议：无

### UploadResult
- data class / 注解：@Serializable / 职责：同步上传结果

#### acceptedCount: Int
- 作用域：成员变量 / @SerialName("acceptedCount") / 初始值：0 / 使用场景：接受数量
- 建议：无

#### failedCount: Int
- 作用域：成员变量 / @SerialName("failedCount") / 初始值：0 / 使用场景：失败数量
- 建议：无

#### message: String
- 作用域：成员变量 / 初始值："" / 使用场景：结果消息
- 建议：无

#### nextCursor: String
- 作用域：成员变量 / @SerialName("nextCursor") / 初始值："" / 使用场景：下一页游标
- 建议：无
