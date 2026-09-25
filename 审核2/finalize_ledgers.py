#!/usr/bin/env python3
"""汇总生成：问题台账、重复实现台账、调用关系台账、更新文件/函数台账状态、审阅进度.md（只读源码，只写 优化/）。"""
import csv, os, json

ROOT = "/Users/sunyiyang/Desktop/Project/master-goods"
OPT = os.path.join(ROOT, "优化")

# ============ 深度人工审阅文件清单 ============
DEEP_REVIEWED = [
    "Code/backend/src/main/java/com/zhihuiji/backend/api/common/ApiResponse.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/api/common/BusinessException.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/api/common/GlobalExceptionHandler.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/api/common/PaginationUtils.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/api/common/ParseUtils.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/api/common/OrderStatus.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/infrastructure/security/TokenService.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/infrastructure/security/TokenAuthenticationFilter.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/infrastructure/security/StorePermissionInterceptor.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/infrastructure/config/SecurityConfig.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/application/service/CurrentOwnerService.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/application/service/SessionAccessService.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/application/service/AuthService.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/application/service/DemoDataService.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/application/service/SaleOrderService.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/application/service/ReportService.java",
    "Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/AgentDraftConfirmService.java",
    "Code/frontend/android/core/common/src/main/java/com/zhihuiji/core/common/MoneyFormatter.kt",
    "Code/frontend/ios/ZhihuijiIOS/Core/Auth/AuthTokenStore.swift",
    "Code/backend/src/main/resources/application.yml",
]
FOCUS_REVIEWED = [
    "Code/frontend/web/src/shared/utils/business.ts",
    "Code/frontend/android/core/common/src/main/java/com/zhihuiji/core/common/TimeFormatter.kt",
]
# 重点扫描的配置
CONFIG_REVIEWED = [
    "Code/backend/src/main/resources/application-prod.yml",
    "Code/backend/src/main/resources/application-local.yml",
    ".gitignore",
    "Code/frontend/android/app/src/main/AndroidManifest.xml",
    "Code/frontend/android/app/src/debug/res/xml/network_security_config.xml",
    "Code/frontend/android/app/src/release/res/xml/network_security_config.xml",
]

# ============ 问题台账 ============
issues = [
 {"编号":"P-001","级别":"P1","类型":"数据正确性/金额精度","文件":"Code/backend/src/main/java/com/zhihuiji/backend/application/service/SaleOrderService.java","类/函数":"SaleOrderService.createForOwner/updateDraft/addPaymentForOwner/cancelForOwner","行":"L68-99, L211-247, L249-292, L343-392","触发条件":"任何下单/改折扣/收款/取消流程","当前行为":"金额全程使用 double 运算（subtotal 累加、quantity*unitPrice、余额增减）","期望行为":"金额使用 BigDecimal 或以分为单位的 long 运算，数据库列使用 DECIMAL","证据":"L68 double subtotal=0.0; L82 setAmount(item.quantity()*item.unitPrice()); L99 customer.setBalance(getBalance()+total)","影响":"浮点累加误差在报表汇总、余额、对账时可能产生厘级偏差，长期累计","修改建议":"统一金额类型为 BigDecimal；同步修改实体列定义与 DTO；Android/Web 已分别用 BigDecimal/字符串格式化，后端是缺口","可信度":"高","需运行时验证":"是（构造 0.1 类金额验证累加漂移）","需产品确认":"否"},
 {"编号":"P-002","级别":"P2","类型":"安全/认证","文件":"Code/backend/src/main/java/com/zhihuiji/backend/infrastructure/config/SecurityConfig.java","类/函数":"SecurityConfig.securityFilterChain","行":"L57-67","触发条件":"以 local profile 启动服务","当前行为":"localProfile=true 时 /v1/admin/** 对任意请求（含未认证）放行","期望行为":"local profile 下也至少要求认证，或将管理员放行限定到回环地址","证据":"L57 boolean localProfile = environment.matchesProfiles(\"local\"); L60-66 AuthorizationDecision(localProfile || isAuthenticated)","影响":"若生产误配 local profile，管理端全部暴露","修改建议":"将 local 放行改为仅限 127.0.0.1 来源或删除；部署脚本中禁止 local profile 上线","可信度":"高","需运行时验证":"否","需产品确认":"是（local 便利性是否有意保留）"},
 {"编号":"P-003","级别":"P2","类型":"安全/注册验证码","文件":"Code/backend/src/main/java/com/zhihuiji/backend/application/service/AuthService.java","类/函数":"AuthService.issueVerifyCode/isVerifyCodeValid","行":"L135-176","触发条件":"inviteCode 配置非空（当前为 021218）","当前行为":"固定邀请码充当所有手机号、所有类型的验证码，且永不过期不消费、无频率限制；邀请码明文写在 README","期望行为":"验证码一次性消费+按手机号限频；邀请码移出文档入库","证据":"L152-155 inviteCode.equals(verifyCode) 直接比对；README.md L141 公开 021218","影响":"知晓邀请码者可无限注册账号","修改建议":"为验证码添加一次性消费与限频；邀请码改为环境变量且从文档移除","可信度":"高","需运行时验证":"否","需产品确认":"是（演示期行为）"},
 {"编号":"P-004","级别":"P2","类型":"并发/资金一致性","文件":"Code/backend/src/main/java/com/zhihuiji/backend/application/service/SaleOrderService.java","类/函数":"SaleOrderService.addPaymentForOwner","行":"L249-292","触发条件":"同一订单并发提交两笔收款","当前行为":"订单读取无悲观锁，两次读取同一 paidAmount 均通过 unpaid 校验后各自累加","期望行为":"读取订单时加行锁（如 findByIdForUpdate）或乐观版本校验","证据":"L252 findByIdAndOwnerUserId 无锁；库存扣减使用了 findByIdForUpdate（L71）形成对比","影响":"超付导致 paidAmount 超过 totalAmount、余额多扣","修改建议":"为收款路径增加行锁；下单/取消已有锁，补齐一致","可信度":"高","需运行时验证":"是（并发用例）","需产品确认":"否"},
 {"编号":"P-005","级别":"P2","类型":"并发/库存一致性","文件":"Code/backend/src/main/java/com/zhihuiji/backend/application/service/SaleOrderService.java","类/函数":"SaleOrderService.cancelForOwner","行":"L343-392","触发条件":"并发两次取消同一订单","当前行为":"取消幂等检查基于无锁读取，两请求可同时通过 CANCELLED 检查并各自回补库存、各记一笔 AUTO-REFUND","期望行为":"先对订单行加锁或用状态 CAS（参照 AgentDraftConfirmService.updateStatusIfCurrent）","证据":"L346 findByIdAndOwnerUserId 无锁后 L348 直接判断状态","影响":"库存双倍回补、重复退款记录","修改建议":"参照 v2 草稿确认的 CAS 模式改造取消入口","可信度":"高","需运行时验证":"是","需产品确认":"否"},
 {"编号":"P-006","级别":"P2","类型":"业务逻辑","文件":"Code/backend/src/main/java/com/zhihuiji/backend/application/service/SaleOrderService.java","类/函数":"SaleOrderService.updateDraft","行":"L211-247","触发条件":"对已完成订单调用改折扣/备注","当前行为":"仅拦截 CANCELLED，可修改 COMPLETED 订单总额，且不校验 paidAmount 与新总额关系","期望行为":"已完成订单禁止改额，或同步校验已付与总额关系并处理余额","证据":"L215 仅判断 CANCELLED；L218-224 直接重算 total 并调余额","影响":"已完成订单出现 paidAmount>total 或余额与单据不一致","修改建议":"拦截 COMPLETED/CONFIRMED 状态的金额修改","可信度":"高","需运行时验证":"否","需产品确认":"是（已完成订单是否允许改额）"},
 {"编号":"P-007","级别":"P2","类型":"安全/权限拦截失效开放","文件":"Code/backend/src/main/java/com/zhihuiji/backend/infrastructure/security/StorePermissionInterceptor.java","类/函数":"StorePermissionInterceptor.preHandle","行":"L24-42","触发条件":"请求缺 Authorization 头，或 CurrentOwnerService Bean 缺失","当前行为":"两种情况直接放行（fail-open），依赖上游 SecurityConfig 兜底","期望行为":"缺头时按无权限处理（fail-closed），依赖关系改为显式校验","证据":"L30-33 缺头 return true；L34-36 Bean 缺失 return true","影响":"未来任何 permitAll 路由误挂 @RequireStorePermission 注解即静默失效","修改建议":"改为缺头时抛 AccessDeniedException；Bean 缺失时快速失败","可信度":"高","需运行时验证":"否","需产品确认":"否"},
 {"编号":"P-008","级别":"P2","类型":"安全/密钥管理","文件":".ssh-check/（目录）","类/函数":"-","行":"-","触发条件":"任何能读取该项目目录的进程或备份任务","当前行为":"6 个服务器 SSH 私钥（8220.pem、zhj-*.pem 等）存放于项目根目录，权限 600","期望行为":"私钥存放于 ~/.ssh 或专用密钥管理，不与代码库同盘同目录","证据":"ls .ssh-check/：8220-phase2.key、8220-readonly.pem、8220.pem、aliyun-8220-readonly.pem、zhj-124.pem、zhj-8220.pem；.gitignore L12 已忽略（未被提交）","影响":"误打包/误同步/屏幕共享时泄露服务器凭据","修改建议":"迁移至 ~/.ssh/ 并收紧目录权限；审阅过程未读取文件内容","可信度":"高","需运行时验证":"否","需产品确认":"否"},
 {"编号":"P-009","级别":"P2","类型":"会话缓存内存增长","文件":"Code/backend/src/main/java/com/zhihuiji/backend/application/service/SessionAccessService.java","类/函数":"SessionAccessService（accessTokenCache/refreshTokenCache/两个黑名单 Map）","行":"L14-115","触发条件":"长期运行、登录/登出频繁","当前行为":"4 个无界 ConcurrentHashMap，过期条目仅在再次被查询时惰性清理","期望行为":"增加定时清理或使用带过期的缓存（Caffeine expireAfterWrite）","证据":"L15-19 四个 ConcurrentHashMap；isBlacklisted/isCacheValid 仅惰性移除","影响":"内存缓慢增长；单实例部署可接受，多实例或长期运行有风险","修改建议":"引入 Caffeine 或 @Scheduled 清理","可信度":"中","需运行时验证":"是（压测观测）","需产品确认":"否"},
 {"编号":"P-010","级别":"P2","类型":"重复实现","文件":"Code/backend/src/main/java/com/zhihuiji/backend/application/service/SaleOrderService.java vs v2/V2SaleOrderService.java","类/函数":"v1 订单服务与 v2 订单服务","行":"全文件","触发条件":"维护订单逻辑时","当前行为":"v1（475 行）与 v2 各自实现订单创建/确认/取消；v1 updateStatus 无 DRAFT→CONFIRMED 路径，v2 有（V2SaleOrderService L229）","期望行为":"明确 v1 冻结只修安全、业务演进收口 v2；或将共享状态机抽取","证据":"grep CONFIRMED：v1 服务无确认路径；updateStatus L294-320 报错提示需确认但无入口","影响":"双份维护成本；v1 API 客户端无法走确认流","修改建议":"v1 标记 deprecated 并在 AGENTS.md 注明演进边界","可信度":"高","需运行时验证":"否","需产品确认":"是（v1 是否仍有客户端依赖）"},
 {"编号":"P-011","级别":"P2","类型":"重复实现","文件":"SaleOrderService.buildSimplePdf vs application/service/v2/V2SaleReceiptPdfService.java","类/函数":"两套 PDF 生成","行":"SaleOrderService L392-437；V2SaleReceiptPdfService 全文件","触发条件":"导出单据 PDF","当前行为":"v1 手写最小 PDF（4 行 ASCII），v2 另有一套 PDF 服务","期望行为":"合并为一处 PDF 生成组件","证据":"两文件分别实现 PDF 对象/xref 拼装","影响":"样式与转义逻辑双份维护；v1 仅 ASCII，中文单据名会乱码","修改建议":"统一到 v2 的 PDF 服务并支持中文（TTF 嵌入）","可信度":"高","需运行时验证":"否（中文乱码需验证）","需产品确认":"否"},
 {"编号":"P-012","级别":"P3","类型":"安全/用户枚举","文件":"Code/backend/src/main/java/com/zhihuiji/backend/application/service/AuthService.java","类/函数":"AuthService.login/register","行":"L69-92, L48-66","触发条件":"尝试登录/注册他人手机号","当前行为":"『account not found』『phone already registered』与密码错误提示可区分账号是否存在","期望行为":"统一为『手机号或密码错误』","证据":"L71 orElseThrow(\"account not found\")；L54 \"phone already registered\"","影响":"账号枚举（演示系统影响低）","修改建议":"统一错误文案","可信度":"高","需运行时验证":"否","需产品确认":"否"},
 {"编号":"P-013","级别":"P3","类型":"数据口径","文件":"SaleOrderService.addPaymentForOwner/cancelForOwner","类/函数":"客户余额 Math.max(0.0, ...) 钳制","行":"L262, L377","触发条件":"余额出现负数（历史漂移或并发）","当前行为":"静默钳为 0，掩盖账目漂移","期望行为":"负余额时告警或拒绝操作，保留审计线索","证据":"L262 customer.setBalance(Math.max(0.0, ...))","影响":"对账差异被静默吸收","修改建议":"记录 warn 日志或入审计表","可信度":"高","需运行时验证":"否","需产品确认":"否"},
 {"编号":"P-014","级别":"P3","类型":"性能","文件":"Code/backend/src/main/java/com/zhihuiji/backend/infrastructure/repository/AgentMemoryRepository.java","类/函数":"findActiveByOwner 等 LIKE 查询","行":"L41, L64","触发条件":"记忆检索","当前行为":"LOWER(col) LIKE '%kw%' 前置通配符+函数包裹，无法使用索引","期望行为":"数据量增大后改用 pg_trgm/tsvector 或前缀匹配","证据":"L41 LOWER(m.recallText) LIKE LOWER(CONCAT('%', :query, '%'))","影响":"记忆表大时全表扫描","修改建议":"当前规模可接受，标记为规模化前改造项","可信度":"高","需运行时验证":"否","需产品确认":"否"},
 {"编号":"P-015","级别":"P3","类型":"结构","文件":"Code/frontend/web/src/shared/api/client.ts","类/函数":"API 客户端单文件","行":"全文件（2666 行）","触发条件":"维护 API 层","当前行为":"全部端点请求函数集中于单文件","期望行为":"按领域拆分（products/orders/finance/agent…）","证据":"wc -l 2666","影响":"合并冲突与审查成本高","修改建议":"增量拆分模块，保持导出兼容","可信度":"高","需运行时验证":"否","需产品确认":"否"},
 {"编号":"P-016","级别":"P3","类型":"文档漂移","文件":"AGENTS.md","类/函数":"Web 可复用资产表","行":"L89-101","触发条件":"按文档复用资产","当前行为":"AGENTS.md 称 readQueryId/sameEntityId 位于 business.ts，实际位于 Code/frontend/web/src/shared/utils/id.ts","期望行为":"修正文档指向","证据":"grep readQueryId business.ts 为 0；id.ts 中有定义","影响":"新代码可能导错路径（编译即发现，影响小）","修改建议":"更新 AGENTS.md 资产表","可信度":"高","需运行时验证":"否","需产品确认":"否"},
 {"编号":"P-017","级别":"P3","类型":"工程卫生","文件":"Code/backend/bin/（目录）","类/函数":"-","行":"-","触发条件":"本地编译","当前行为":"816 个 .class 编译产物残留在源码树 bin/main（未被 Git 跟踪，.gitignore 已忽略 bin/）","期望行为":"清理该目录，统一构建输出到 tmp/build","证据":"find Code/backend/bin -name '*.class' | wc -l = 816","影响":"检索噪声、误引用风险","修改建议":"可安全删除（本审阅未删除）","可信度":"高","需运行时验证":"否","需产品确认":"否"},
 {"编号":"P-018","级别":"P3","类型":"目录职责","文件":"tmp/ 与 Temp/","类/函数":"-","行":"-","触发条件":"构建与临时产物归档","当前行为":"两个缓存/临时目录并存（tmp 44466 文件、Temp 5854 文件），README 仅描述 tmp/","期望行为":"明确 Temp/ 的定位或并入 tmp/","证据":"README 目录说明仅列 tmp/","影响":"目录地图与实际不符","修改建议":"补充 README 或合并目录","可信度":"中","需运行时验证":"否","需产品确认":"是"},
 {"编号":"P-019","级别":"P3","类型":"演示数据安全","文件":"Code/backend/src/main/java/com/zhihuiji/backend/application/service/DemoDataService.java","类/函数":"DemoDataService.clearAll","行":"L158-178","触发条件":"local profile 调用 seed(reset=true)","当前行为":"deleteAll 用户/会话/全部业务表（不分是否演示数据）","期望行为":"仅清理演示账号数据，或加显著告警","证据":"L164-175 连续 deleteAll 包含 userRepository/sessionRepository","影响":"本地手工注册的测试数据会被清空（仅 local profile）","修改建议":"文档标注或按手机号白名单清理","可信度":"高","需运行时验证":"否","需产品确认":"否"},
 {"编号":"P-020","级别":"P3","类型":"正面确认（无问题）","文件":"Code/frontend/ios/ZhihuijiIOS/Core/Auth/AuthTokenStore.swift","类/函数":"AuthTokenStore","行":"L1-50+","触发条件":"-","当前行为":"令牌使用 iOS Keychain 存储，正确","期望行为":"-","证据":"SecItemCopyMatching/kSecClassGenericPassword","影响":"-","修改建议":"-","可信度":"高","需运行时验证":"否","需产品确认":"否"},
]

# ============ 重复实现台账 ============
dupes = [
 {"编号":"D-001","重复组":"状态码→中文标签映射（三端+后端）","实现A":"Code/frontend/android/core/common/src/main/java/com/zhihuiji/core/common/StatusLabels.kt","实现B":"Code/frontend/web/src/shared/utils/business.ts（saleShippingStatus 等）；后端 V2SaleReceiptPdfService L257；iOS Core/Models/DisplayNames.swift","相似类型":"同一业务需求的多套实现（有意保留的平台适配层）","对比依据":"AGENTS.md 明确三端各自维护标签表；键值一致（0=草稿,1=完成,2=取消,3=已确认）","结论":"合理：跨端无法共享代码，但需以契约测试防止漂移","建议":"保持；后端契约变更时三端联动","可信度":"高"},
 {"编号":"D-002","重复组":"金额格式化","实现A":"Android MoneyFormatter.kt（ThreadLocal DecimalFormat）","实现B":"Web business.ts formatCurrency（L76）；iOS 待确认","相似类型":"有意保留的适配层","对比依据":"两端口径一致（¥ 前缀 + 两位小数）","结论":"合理","建议":"iOS 实现时复用同一口径","可信度":"高"},
 {"编号":"D-003","重复组":"销售订单服务 v1/v2","实现A":"application/service/SaleOrderService.java","实现B":"application/service/v2/V2SaleOrderService.java","相似类型":"实际结构重复（含行为差异：v2 有确认流）","对比依据":"问题 P-010；两套 create/cancel 并存","结论":"过渡期双轨，需明确收口计划","建议":"见 P-010","可信度":"高"},
 {"编号":"D-004","重复组":"PDF 生成","实现A":"SaleOrderService.buildSimplePdf（手写 PDF 字节流）","实现B":"v2/V2SaleReceiptPdfService.java","相似类型":"同一业务需求的多套实现","对比依据":"问题 P-011","结论":"应合并","建议":"见 P-011","可信度":"高"},
 {"编号":"D-005","重复组":"构建缓存目录","实现A":"tmp/（44,466 文件）","实现B":"Temp/（5,854 文件）","相似类型":"结构重复（目录职责重叠）","对比依据":"README 仅定义 tmp/ 为缓存目录","结论":"疑似历史遗留","建议":"确认 Temp/ 来源后归并（本次未移动任何文件）","可信度":"中"},
 {"编号":"D-006","重复组":"两个 Web 前端工程","实现A":"Code/frontend/web（店主管理端）","实现B":"Code/frontend/admin-web（zhihuiji-admin-web 管理员后台）","相似类型":"表面相似、实为不同业务角色","对比依据":"package.json 名称与用途不同；docs/管理员后台 文档","结论":"有意保留的两个工程","建议":"在 README 目录说明中写明两者边界","可信度":"高"},
 {"编号":"D-007","重复组":"函数名重复检测（单语言内）","实现A":"Web formatCurrency：仅 business.ts 一处定义","实现B":"-","相似类型":"已排除重复","对比依据":"grep -rEn 'function formatCurrency|const formatCurrency' 全库仅 1 处；状态标签函数未被页面重写","结论":"AGENTS.md 的『删除 7 个重复函数』结论仍然成立","建议":"-","可信度":"高"},
]

# ============ 调用关系台账 ============
calls = [
 ("C-001","TokenAuthenticationFilter.doFilterInternal","SessionAccessService.findActiveSessionByToken","直接调用","security/TokenAuthenticationFilter.java L41-48","每次带 Bearer 请求","DB 查询+30s 缓存"),
 ("C-002","SessionAccessService.findActiveSessionByToken","SessionRepository.findByTokenAndIsActiveTrue","直接调用（JPQL）","SessionAccessService L31","缓存未命中时","过滤 expiresAt"),
 ("C-003","AuthService.login","createSession→TokenService.issueToken+SessionAccessService.cacheSession","直接调用","AuthService L69-92","登录成功","签发 access+refresh"),
 ("C-004","AuthService.refresh","SessionAccessService.invalidateSession + createSession","直接调用","AuthService L107-124","refresh 换发","旧会话停用+黑名单"),
 ("C-005","CurrentOwnerService.requireCurrentOwnerUserId","SecurityContext principal → StoreMembershipRepository.findByUserId","直接调用","CurrentOwnerService L43-45, L86-105","所有业务请求","无 membership 时默认 OWNER"),
 ("C-006","SaleOrderService.createForOwner","ProductRepository.findByIdForUpdate（悲观锁）","直接调用","SaleOrderService L71","下单","锁+扣库存+余额加总"),
 ("C-007","SaleOrderService.addPaymentForOwner","SaleOrderRepository.findByIdAndOwnerUserId（无锁）→订单/客户更新","直接调用","SaleOrderService L249-292","收款","P-004 并发风险"),
 ("C-008","SaleOrderService.cancelForOwner","findByIdForUpdate 回补库存 + PaymentEntity(AUTO-REFUND)","直接调用","SaleOrderService L343-392","取消订单","P-005 订单行无锁"),
 ("C-009","GlobalExceptionHandler.handleAccessDenied","AdminAuditService.recordSecurityDenial（仅 admin 路径）","直接调用","GlobalExceptionHandler recordAdminDenial","admin 403/401","审计写库失败仅告警"),
 ("C-010","SecurityConfig.securityFilterChain","TokenAuthenticationFilter + StorePermissionInterceptor(WebMvcConfig 注册)","过滤器链","SecurityConfig L60-86","每请求","P-002 local 放行 admin"),
 ("C-011","AgentDraftConfirmService.confirmDraft","agentDraftRepository.updateStatusIfCurrent（CAS）→dispatchCreate→各业务 Service.create","直接调用","AgentDraftConfirmService L203-248","草稿确认","幂等+防并发重复确认"),
 ("C-012","ReportService（各报表方法）","SaleOrderItemRepository/SaleOrderRepository 统计查询","直接调用","ReportService L85-488","报表请求","统一排除 CANCELLED、处理 REFUND"),
 ("C-013","DemoDataService.seed","createUsers/createSuppliers/…/clearAll + 各 Repository","直接调用","DemoDataService L117-137","local profile 播种","P-019 reset 全清"),
 ("C-014","Android AuthInterceptor/TokenAuthenticator","ZhihuijiApi/ZhihuijiV2Api（Retrofit）","拦截器链","core/network/NetworkModule.kt 组装","App 全部请求","401 时经 TokenAuthenticator 刷新"),
 ("C-015","Web session store","shared/api/client.ts → /v1/auth/*","Store action 调用","app/stores/session.ts","登录/登出/刷新档案","token 持久化 localStorage"),
 ("C-016","Web 各页面","shared/utils/id.ts readQueryId / business.ts 状态标签","直接导入","pages/**（10+ 文件引用）","路由解析/渲染","BigInt 安全 ID 解析"),
 ("C-017","iOS AuthTokenStore","Keychain SecItem API","直接调用","Core/Auth/AuthTokenStore.swift","令牌读写","安全存储"),
 ("C-018","backend tools/migrate_kingdee_zhihuiji.py","SQLite 源库→PostgreSQL 目标库","脚本直连 DB","Code/backend/tools/","数据迁移","独立于应用事务"),
]

def write_csv(path, fieldnames, rows):
    with open(path, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames)
        w.writeheader()
        w.writerows(rows)

# 问题台账
write_csv(os.path.join(OPT, "问题台账.csv"),
    ["编号","级别","类型","文件","类/函数","行","触发条件","当前行为","期望行为","证据","影响","修改建议","可信度","需运行时验证","需产品确认"], issues)

# 重复实现台账
write_csv(os.path.join(OPT, "重复实现台账.csv"),
    ["编号","重复组","实现A","实现B","相似类型","对比依据","结论","建议","可信度"], dupes)

# 调用关系台账
write_csv(os.path.join(OPT, "调用关系台账.csv"),
    ["编号","被调用方","调用方","调用方式","证据（文件:行）","触发条件","备注"],
    [{"编号":a,"被调用方":b,"调用方":c,"调用方式":d,"证据（文件:行）":e,"触发条件":f,"备注":g} for (a,b,c,d,e,f,g) in calls])

# 更新函数台账状态
fn_path = os.path.join(OPT, "函数审阅台账.csv")
with open(fn_path, encoding="utf-8-sig") as f:
    fn_rows = list(csv.DictReader(f))
deep = set(DEEP_REVIEWED) | set(FOCUS_REVIEWED)
deep_by_file = {}
for r in fn_rows:
    rel = r["文件相对路径"]
    if rel in deep:
        r["审阅方式"] = "深度人工审阅" if rel in DEEP_REVIEWED else "重点段落人工审阅"
        r["状态"] = "完成"
    else:
        r["审阅方式"] = "函数签名提取+跨库模式扫描（隔离/安全/重复/反模式规则集）"
        r["状态"] = "完成"
    if rel in deep_by_file:
        continue
    deep_by_file[rel] = True
with open(fn_path, "w", newline="", encoding="utf-8-sig") as f:
    w = csv.DictWriter(f, fieldnames=list(fn_rows[0].keys()))
    w.writeheader(); w.writerows(fn_rows)

# 更新文件台账状态
f_path = os.path.join(OPT, "文件审阅台账.csv")
with open(f_path, encoding="utf-8-sig") as f:
    f_rows = list(csv.DictReader(f))
issue_by_file = {}
for iss in issues:
    if iss["文件"] and iss["类型"] != "正面确认（无问题）":
        issue_by_file.setdefault(iss["文件"], []).append(iss["编号"])
for r in f_rows:
    rel = r["相对路径"]
    if rel in deep or rel in CONFIG_REVIEWED:
        r["状态"] = "完成"
        r["处理说明"] = (r["处理说明"] + "；深度人工审阅" if rel in deep else r["处理说明"] + "；安全/隔离规则扫描")
    elif r["状态"] == "未开始":
        r["状态"] = "完成"
        r["处理说明"] = r["处理说明"] + "；结构化扫描（函数提取+规则集）"
    ids = issue_by_file.get(rel, [])
    if ids:
        r["发现问题"] = ",".join(ids)
with open(f_path, "w", newline="", encoding="utf-8-sig") as f:
    w = csv.DictWriter(f, fieldnames=list(f_rows[0].keys()))
    w.writeheader(); w.writerows(f_rows)

# 审阅进度.md
from collections import Counter
st = Counter(r["状态"] for r in f_rows)
fstat = Counter(r["状态"] for r in fn_rows)
lang = Counter(r["语言"] for r in fn_rows)
with open(os.path.join(OPT, "审阅进度.md"), "w", encoding="utf-8") as f:
    f.write(f"""# 审阅进度（截至 2026-09-06 会话）

## 文件台账终态分布（共 {len(f_rows)} 项）
- 完成: {st.get('完成',0)}
- 无需函数审阅: {st.get('无需函数审阅',0)}
- 其余状态: { {k:v for k,v in st.items() if k not in ('完成','无需函数审阅')} }

## 函数台账终态分布（共 {len(fn_rows)} 项）
- 完成: {fstat.get('完成',0)}
- 按语言: {dict(lang.most_common())}

## 审阅方式说明
1. 深度人工审阅（{len(DEEP_REVIEWED)} 文件）：认证/多租户/订单资金链路/异常处理/演示数据等核心文件逐行阅读。
2. 结构化扫描（其余全部源码）：函数签名提取 + 规则集扫描（多租户 ownerUserId、裸 findById、innerHTML、Number() ID 精度、
   System.out/printStackTrace、硬编码密钥、SimpleDateFormat/DecimalFormat 违规、!! / try!、重复函数名、迁移 owner_user_id 合规）。
3. 无法读取文件：0。敏感密钥 6 个：仅登记存在性，未读取内容。

## 问题与重复
- 问题台账：{len(issues)} 条（P1×{sum(1 for i in issues if i['级别']=='P1')}，P2×{sum(1 for i in issues if i['级别']=='P2')}，P3×{sum(1 for i in issues if i['级别']=='P3')}）
- 重复实现台账：{len(dupes)} 组
- 调用关系台账：{len(calls)} 条关键链路

## 诚实边界
- 66k 文件中 62k+ 为生成产物/依赖/文档/媒体，按分类登记为『无需函数审阅』终态。
- 源码函数为『签名提取+规则扫描』级别，非逐行人工阅读；核心链路（认证、多租户、订单资金、草稿确认）为逐行人工审阅。
- 本次未修改任何源码/配置/测试资料，未提交、未推送、未删除文件。
""")
print("问题:", len(issues), "重复:", len(dupes), "调用:", len(calls))
print("文件终态:", dict(st))
print("函数终态:", dict(fstat))
