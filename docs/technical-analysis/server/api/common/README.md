# Common 层技术分析

> 路径: `src/main/java/com/zhihuiji/backend/api/common/`

本层提供 API 层公共组件：统一响应体和全局异常处理。

---

## ApiResponse

- **文件**: `ApiResponse.java`
- **类型**: `record`
- **作用**: 统一 API 响应包装体，所有 Controller 返回值均使用此结构。

### 字段

| 字段 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `code` | int | 业务状态码（0=成功，非0=失败） | 无 |
| `message` | String | 状态描述 | 无 |
| `data` | T | 业务数据（泛型） | 无 |
| `timestamp` | long | 响应时间戳（毫秒） | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `success(data)` | 构建成功响应（code=0, message="success"） | 无 |
| `failure(code, message)` | 构建失败响应（data=null） | 无 |

### 修改建议

1. **缺少泛型约束**: `failure` 方法返回 `ApiResponse<Void>` 但类型推断可能产生歧义，建议显式声明。
2. **缺少常用 HTTP 状态码常量**: 可定义 `CODE_SUCCESS = 0`、`CODE_BAD_REQUEST = 400` 等常量。
3. **timestamp 可选**: 对于缓存场景，时间戳可能导致响应体变化，可考虑移除或改为请求 ID。

---

## GlobalExceptionHandler

- **文件**: `GlobalExceptionHandler.java`
- **注解**: `@RestControllerAdvice`
- **作用**: 全局异常拦截，将各类异常转换为统一的 `ApiResponse` 格式。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `log` | Logger | 日志记录器 | 无 |

### 函数

| 函数 | 异常类型 | HTTP 状态码 | 业务码 | 作用 | 修改建议 |
|------|----------|-------------|--------|------|----------|
| `handleValidation` | `MethodArgumentNotValidException` | 400 | 400 | 处理 @Valid 校验失败 | 仅取第一个错误，可改为汇总所有错误 |
| `handleConstraint` | `ConstraintViolationException` | 400 | 400 | 处理约束违反 | 未返回具体字段信息 |
| `handleUnreadableBody` | `HttpMessageNotReadableException` | 400 | 400 | 处理请求体不可读 | 无 |
| `handleTypeMismatch` | `MethodArgumentTypeMismatchException` | 400 | 400 | 处理参数类型不匹配 | 无 |
| `handleMissingParameter` | `MissingServletRequestParameterException` | 400 | 400 | 处理缺少请求参数 | 无 |
| `handleMissingHeader` | `MissingRequestHeaderException` | 401/400 | 401/400 | 处理缺少请求头 | 对 Authorization 头特殊处理返回 401 |
| `handleBusiness` | `IllegalArgumentException` | 422 | 422 | 处理业务逻辑异常 | **所有业务异常都用 IllegalArgumentException**，建议定义专用业务异常类 |
| `handleServiceState` | `IllegalStateException` | 503 | 503 | 处理服务状态异常 | 无 |
| `handleMethodNotSupported` | `HttpRequestMethodNotSupportedException` | 405 | 405 | 处理 HTTP 方法不支持 | 无 |
| `handleNoResource` | `NoResourceFoundException` | 404 | 404 | 处理资源未找到 | 无 |
| `handleUnknown` | `Exception` | 500 | 500 | 兜底异常处理 | ✏️ 响应已脱敏（返回 "Internal server error"），但服务端日志记录了完整异常栈，生产环境应控制日志级别 |

### 修改建议

1. **业务异常类缺失**: 项目中所有业务异常均抛出 `IllegalArgumentException`，与参数校验异常混淆，应定义 `BusinessException` 区分。
2. ✏️ **错误信息国际化**: 当前 handler 方法使用英文消息（如 "Invalid request"、"Internal server error"），但业务层 `IllegalArgumentException` 的中文消息被透传到响应中，混合了中英文。建议统一为 i18n 消息或定义专用业务异常类区分。
3. **验证错误汇总**: `handleValidation` 仅返回第一个字段错误，应返回全部错误以便前端一次性展示。
4. **缺少请求追踪**: 建议在异常响应中加入 `traceId` 或 `requestId`，方便问题定位。
