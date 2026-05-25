# 智慧记后端（Spring Boot 3 + Java 21）

## 启动方式

### 1) Docker Compose（推荐）

```bash
cd backend
cp .env.example .env
docker compose up --build -d
```

服务地址：`http://localhost:18080`

健康检查：

```bash
curl http://localhost:18080/v1/sync/health
```

### 2) 本地运行

要求：
- JDK 21
- PostgreSQL 15+
- Redis 7+

```bash
cd backend
gradle bootRun
```

## 测试

```bash
cd backend
gradle test
```

## 已实现接口

- `POST /v1/auth/register`
- `POST /v1/auth/login`
- `POST /v1/auth/refresh`
- `POST /v1/auth/logout`
- `POST /v1/auth/verify-code`
- `GET /v1/auth/users/me`
- `GET /v1/sync/health`
- `POST /v1/sync/upload`
- `POST /v1/sync/pull`
- `GET/POST/PUT/DELETE /v1/products`
- `GET/POST/PUT/DELETE /v1/customers`
- `POST /v1/sale-orders`
- `GET /v1/sale-orders`
- `GET /v1/sale-orders/{id}`
- `PUT /v1/sale-orders/{id}`
- `POST /v1/sale-orders/{id}/payments`
- `PUT /v1/sale-orders/{id}/cancel`
- `POST /v1/purchase-orders`
- `GET /v1/purchase-orders`
- `GET /v1/purchase-orders/{id}`
- `GET /v1/reports/sales/summary`
- `GET /v1/reports/sales/by-product`
- `GET /v1/reports/sales/by-customer`
- `GET /v1/reports/sales/receivable`
- `GET /v1/reports/inventory`
- `GET /v1/reports/inventory/flow`

## 说明

- 邀请码注册码固定：`021218`
- `sync` 接口已提供契约与游标持久化，业务变更应用在下轮继续增强
- 报表目前按本地聚合逻辑实现基础统计，便于 Android 远端包先联调闭环

