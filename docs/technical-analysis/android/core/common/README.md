# common 技术分析

## 文件清单
- MoneyFormatter.kt
- ResultExt.kt
- StatusLabels.kt
- TimeFormatter.kt
- UiMessage.kt

---

## MoneyFormatter.kt

### MoneyFormatter
- object（单例）/ 职责：统一格式化金额显示，支持带/不带货币符号、正负号等场景 / 设计模式：工具类单例模式

#### format(amount: BigDecimal?): String
- 参数：`amount: BigDecimal?` — 金额值，可为空
- 返回值：`String` — 格式化后的金额字符串，如 "¥1,234.56"
- 实现逻辑：若 amount 为 null 返回 "¥0.00"，否则使用 DecimalFormat 格式化并拼接 "¥" 前缀
- 调用关系：被 UI 层金额展示处调用
- 建议：DecimalFormat 非线程安全，当前为 object 单例共享实例，高并发场景下可能存在线程安全问题，建议使用 `synchronized` 或改为每次创建实例

#### format(amount: Double?): String
- 参数：`amount: Double?` — 金额值，可为空
- 返回值：`String` — 格式化后的金额字符串
- 实现逻辑：与 BigDecimal 重载逻辑一致，对 null 返回 "¥0.00"，否则格式化拼接 "¥"
- 调用关系：被 UI 层金额展示处调用
- 建议：Double 存在精度问题，金融场景建议统一使用 BigDecimal 版本

#### formatWithoutSymbol(amount: BigDecimal?): String
- 参数：`amount: BigDecimal?` — 金额值，可为空
- 返回值：`String` — 不带 "¥" 符号的格式化字符串，如 "1,234.56"
- 实现逻辑：若 amount 为 null 返回 "0.00"，否则仅格式化不拼接符号
- 调用关系：被需要纯数字展示的场景调用
- 建议：无

#### formatWithoutSymbol(amount: Double?): String
- 参数：`amount: Double?` — 金额值，可为空
- 返回值：`String` — 不带 "¥" 符号的格式化字符串
- 实现逻辑：与 BigDecimal 重载逻辑一致
- 调用关系：被需要纯数字展示的场景调用
- 建议：同上，建议统一使用 BigDecimal

#### formatSigned(amount: Double?): String
- 参数：`amount: Double?` — 金额值，可为空
- 返回值：`String` — 带正负号的格式化字符串，如 "¥+1,234.56" 或 "¥-500.00"
- 实现逻辑：若 amount 为 null 返回 "¥0.00"；若 amount >= 0 添加 "+" 前缀，否则保留负号
- 调用关系：被趋势/变动金额展示处调用
- 建议：缺少 BigDecimal 重载版本，建议补充以保持 API 一致性

#### formatter: DecimalFormat
- 作用域：private / 初始值：DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.CHINA)) / 使用场景：所有格式化函数共用
- 建议：DecimalFormat 非线程安全，多线程访问可能产生异常，建议加锁或改用线程安全方案

---

## ResultExt.kt

### BusinessException
- class / 父类：Exception / 职责：封装业务层错误码和错误消息 / 设计模式：自定义异常

#### BusinessException(code: Int, message: String)
- 参数：`code: Int` — 业务错误码；`message: String` — 错误描述
- 返回值：无（构造函数）
- 实现逻辑：将 code 和 message 传递给父类 Exception
- 调用关系：由 requireData() 和 getOrThrow() 在业务错误时抛出
- 建议：可增加 code 的语义化常量定义，避免调用方硬编码判断错误码

### requireData() — 扩展函数
- 父类/接口：ApiResponse\<T\> 的扩展函数 / 职责：强制获取 ApiResponse 中的 data，失败则抛异常

#### requireData(): T
- 参数：无（接收者为 ApiResponse\<T\>）
- 返回值：`T` — ApiResponse 中的非空 data
- 实现逻辑：若 code != 0 抛出 BusinessException(code, message)；若 data 为 null 抛出 BusinessException(code, "数据为空")；否则返回 data
- 调用关系：被 Repository 层在确定需要数据时调用
- 建议：data 为 null 时的异常 code 仍使用原始 code，可能丢失语义，建议使用专用错误码如 -1

### getOrThrow() — 扩展函数
- 父类/接口：ApiResponse\<T\> 的扩展函数 / 职责：将 ApiResponse 转换为 Kotlin Result

#### getOrThrow(): Result\<T\>
- 参数：无（接收者为 ApiResponse\<T\>）
- 返回值：`Result<T>` — 成功包含 data，失败包含 BusinessException
- 实现逻辑：若 code == 0 且 data != null，返回 Result.success(data)；否则返回 Result.failure(BusinessException(code, message))
- 调用关系：被 ViewModel/Repository 层调用，配合 safeApiCall 使用
- 建议：存在 UNCHECKED_CAST 抑制，当 data 为 null 且 code == 0 时不会进入 success 分支，逻辑安全但可增加注释说明

---

## StatusLabels.kt

### StatusLabels
- object（单例）/ 职责：将业务状态码映射为中文标签 / 设计模式：查找表模式

#### saleOrderStatus(code: Int): String
- 参数：`code: Int` — 销售订单状态码
- 返回值：`String` — 状态标签：0→"草稿"，1→"已完成"，2→"已取消"，其他→"未知"
- 实现逻辑：when 表达式匹配状态码返回中文标签
- 调用关系：被销售订单 UI 展示处调用
- 建议：状态码应引用 StatusConstants 常量而非硬编码数字，提高可维护性

#### purchaseOrderStatus(code: Int): String
- 参数：`code: Int` — 采购订单状态码
- 返回值：`String` — 状态标签：0→"草稿"，1→"已收货"，其他→"未知"
- 实现逻辑：when 表达式匹配状态码返回中文标签
- 调用关系：被采购订单 UI 展示处调用
- 建议：同上，应引用 StatusConstants

#### payOrderStatus(code: Int): String
- 参数：`code: Int` — 付款单状态码
- 返回值：`String` — 状态标签：0→"待付款"，1→"已付款"，2→"已取消"，其他→"未知"
- 实现逻辑：when 表达式匹配状态码返回中文标签
- 调用关系：被付款单 UI 展示处调用
- 建议：同上

#### financeType(code: Int): String
- 参数：`code: Int` — 财务记录类型码
- 返回值：`String` — 类型标签：1→"收入"，2→"支出"，其他→"未知"
- 实现逻辑：when 表达式匹配类型码返回中文标签
- 调用关系：被财务记录 UI 展示处调用
- 建议：同上

#### supplierStatus(code: Int): String
- 参数：`code: Int` — 供应商状态码
- 返回值：`String` — 状态标签：1→"启用"，0→"停用"，其他→"未知"
- 实现逻辑：when 表达式匹配状态码返回中文标签
- 调用关系：被供应商 UI 展示处调用
- 建议：同上

#### productStatus(code: Int): String
- 参数：`code: Int` — 商品状态码
- 返回值：`String` — 状态标签：1→"正常"，0→"停用"，其他→"未知"
- 实现逻辑：when 表达式匹配状态码返回中文标签
- 调用关系：被商品 UI 展示处调用
- 建议：同上

#### customerLevel(code: Int): String
- 参数：`code: Int` — 客户等级码
- 返回值：`String` — 等级标签：0→"普通"，1→"VIP"，2→"SVIP"，其他→"未知"
- 实现逻辑：when 表达式匹配等级码返回中文标签
- 调用关系：被客户 UI 展示处调用
- 建议：同上

#### paymentMethod(code: Int): String
- 参数：`code: Int` — 支付方式码
- 返回值：`String` — 方式标签：1→"现金"，2→"微信"，3→"支付宝"，4→"银行卡"，5→"其他"，其他→"未知"
- 实现逻辑：when 表达式匹配支付方式码返回中文标签
- 调用关系：被支付相关 UI 展示处调用
- 建议：同上

#### paymentType(code: Int): String
- 参数：`code: Int` — 支付类型码
- 返回值：`String` — 类型标签：1→"收款"，2→"退款"，其他→"未知"
- 实现逻辑：when 表达式匹配支付类型码返回中文标签
- 调用关系：被支付记录 UI 展示处调用
- 建议：同上

#### inventoryFlowType(code: Int): String
- 参数：`code: Int` — 库存流向类型码
- 返回值：`String` — 类型标签：0→"出库"，1→"入库"，其他→"未知"
- 实现逻辑：when 表达式匹配流向码返回中文标签
- 调用关系：被库存流水 UI 展示处调用
- 建议：同上

#### agentTaskStatus(status: String): String
- 参数：`status: String` — Agent 任务状态字符串
- 返回值：`String` — 状态标签："queued"→"排队中"，"running"→"运行中"，"completed"→"已完成"，"failed"→"失败"，其他→"未知"
- 实现逻辑：when 表达式匹配字符串状态返回中文标签
- 调用关系：被 Agent 任务 UI 展示处调用
- 建议：参数为 String 而非 Int，与其他函数风格不一致，建议统一或定义枚举

#### stockStatus(stock: Double, safeStock: Double): String
- 参数：`stock: Double` — 当前库存量；`safeStock: Double` — 安全库存量
- 返回值：`String` — 状态标签：stock <= 0→"缺货"，stock < safeStock→"低库存"，其他→"正常"
- 实现逻辑：基于库存与安全库存的比较返回状态标签
- 调用关系：被库存状态 UI 展示处调用
- 建议：使用 Double 比较库存量，存在浮点精度问题，建议改用 BigDecimal

---

## TimeFormatter.kt

### TimeFormatter
- object（单例）/ 职责：统一格式化时间戳为可读日期时间字符串 / 设计模式：工具类单例模式

#### formatDate(epochMillis: Long?): String
- 参数：`epochMillis: Long?` — Unix 时间戳（毫秒），可为空
- 返回值：`String` — 格式化日期 "yyyy-MM-dd"，null 或 0 返回 "-"
- 实现逻辑：若为 null 或 0L 返回 "-"，否则使用 SimpleDateFormat 格式化
- 调用关系：被 UI 层日期展示处调用
- 建议：SimpleDateFormat 非线程安全，object 单例共享实例存在线程安全隐患

#### formatDateTime(epochMillis: Long?): String
- 参数：`epochMillis: Long?` — Unix 时间戳（毫秒），可为空
- 返回值：`String` — 格式化日期时间 "yyyy-MM-dd HH:mm"，null 或 0 返回 "-"
- 实现逻辑：若为 null 或 0L 返回 "-"，否则使用 SimpleDateFormat 格式化
- 调用关系：被 UI 层日期时间展示处调用
- 建议：同上

#### formatTime(epochMillis: Long?): String
- 参数：`epochMillis: Long?` — Unix 时间戳（毫秒），可为空
- 返回值：`String` — 格式化时间 "HH:mm"，null 或 0 返回 "-"
- 实现逻辑：若为 null 或 0L 返回 "-"，否则使用 SimpleDateFormat 格式化
- 调用关系：被 UI 层时间展示处调用
- 建议：同上

#### dateFormatter: SimpleDateFormat
- 作用域：private / 初始值：SimpleDateFormat("yyyy-MM-dd", Locale.CHINA) / 使用场景：formatDate 使用
- 建议：非线程安全，建议改为 java.time.format.DateTimeFormatter（API 26+）或加锁

#### dateTimeFormatter: SimpleDateFormat
- 作用域：private / 初始值：SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA) / 使用场景：formatDateTime 使用
- 建议：同上

#### timeFormatter: SimpleDateFormat
- 作用域：private / 初始值：SimpleDateFormat("HH:mm", Locale.CHINA) / 使用场景：formatTime 使用
- 建议：同上

---

## UiMessage.kt

### UiMessage
- data class / 职责：封装 UI 层用户提示消息，包含文本和类型 / 设计模式：值对象

#### UiMessage(text: String, type: Type)
- 参数：`text: String` — 消息文本；`type: Type` — 消息类型，默认 ERROR
- 返回值：无（构造函数）
- 实现逻辑：data class 自动生成 getter/equals/hashCode/copy
- 调用关系：被 ViewModel 向 UI 层传递消息时使用
- 建议：无

### UiMessage.Type — 嵌套枚举
- enum class / 职责：定义消息类型

#### ERROR
- 作用域：enum 常量 / 使用场景：错误消息
- 建议：无

#### WARNING
- 作用域：enum 常量 / 使用场景：警告消息
- 建议：无

#### INFO
- 作用域：enum 常量 / 使用场景：信息消息
- 建议：无

#### SUCCESS
- 作用域：enum 常量 / 使用场景：成功消息
- 建议：无

### UiMessage.Companion — 伴生对象
- 职责：提供工厂方法创建 UiMessage 实例

#### fromThrowable(throwable: Throwable): UiMessage
- 参数：`throwable: Throwable` — 异常对象
- 返回值：`UiMessage` — type 为 ERROR 的消息，text 取 throwable.message 或 "未知错误"
- 实现逻辑：提取 throwable.message，为空则使用 "未知错误"，构建 ERROR 类型 UiMessage
- 调用关系：被 ViewModel 捕获异常后转换为 UI 消息时调用
- 建议：无

#### error(text: String): UiMessage
- 参数：`text: String` — 错误消息文本
- 返回值：`UiMessage` — ERROR 类型的消息
- 实现逻辑：调用构造函数创建 ERROR 类型实例
- 调用关系：被需要显示错误提示处调用
- 建议：无

#### warning(text: String): UiMessage
- 参数：`text: String` — 警告消息文本
- 返回值：`UiMessage` — WARNING 类型的消息
- 实现逻辑：调用构造函数创建 WARNING 类型实例
- 调用关系：被需要显示警告提示处调用
- 建议：无

#### info(text: String): UiMessage
- 参数：`text: String` — 信息消息文本
- 返回值：`UiMessage` — INFO 类型的消息
- 实现逻辑：调用构造函数创建 INFO 类型实例
- 调用关系：被需要显示信息提示处调用
- 建议：无

#### success(text: String): UiMessage
- 参数：`text: String` — 成功消息文本
- 返回值：`UiMessage` — SUCCESS 类型的消息
- 实现逻辑：调用构造函数创建 SUCCESS 类型实例
- 调用关系：被需要显示成功提示处调用
- 建议：无
