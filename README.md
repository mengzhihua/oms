# OMS 订单管理系统

OTWB 供应链平台中的订单中枢：承接多渠道（天猫/京东/抖音/线下/API）订单，完成审核、分仓路由、库存预占、
推送 WMS、发货/签收回传、售后退换货与退款，并为渠道提供可售库存。与本组织的 WMS / TMS 系统同构
（Spring Boot 2.7 + MyBatis-Plus / Vue 3 + Element Plus）。

## 功能范围

| 模块 | 能力 |
| --- | --- |
| 基础数据 | 渠道、店铺（默认仓/自动审核）、客户、商品（普通/组合 BOM）、仓库、承运商、分仓路由规则 |
| 库存中心 | 多仓库存（在库/预占/安全）、渠道库存策略（比例/固定/限定仓）、调整、预占/释放/扣减/退回流水、CSV 导出 |
| 订单中心 | 状态机 CREATED→AUDITED→ALLOCATED→PUSHED→SHIPPED→COMPLETED，挂起/取消、按省份/渠道路由、多仓自动拆单、手工拆单、批量操作、一键处理、CSV 导入/导出、操作日志 |
| 售后 | 退货退款 / 仅退款 / 换货，审核、驳回、退货入库回增库存、退款记录 |
| 系统集成 | WMS（出库单推送/取消、退货入库单）、TMS（运单创建）适配器，本地 mock；开放接口供渠道/WMS/TMS 回调；集成日志 |
| 报表 | 工作台（今日概览、待办、趋势、渠道分布）、每日订单、渠道/店铺、SKU 销量、仓库发货、售后、履约时效 |
| 系统管理 | 用户与角色（ADMIN / OPERATOR / VIEWER）、操作日志 |

## 目录

```text
backend/   Spring Boot 2.7 + MyBatis-Plus 后端，端口 8080
frontend/  Vue 3 + Vite + Element Plus 前端，端口 5173（/api 代理到后端）
scripts/   smoke.sh 端到端冒烟脚本
```

## 快速开始

### 后端

要求 JDK 17 和 Maven：

```bash
cd backend
mvn spring-boot:run
```

默认 H2 文件库 `backend/data/oms`，启动时自动执行 `schema.sql`（建表）与 `data.sql`（幂等演示数据：
渠道/店铺/仓库/承运商/商品/组合 BOM/路由规则/库存/渠道库存策略）。默认管理员 `admin / admin123`。
MySQL 通过 `--spring.profiles.active=mysql` 启用（`DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD`）。

主要环境变量：

| 变量 | 说明 |
| --- | --- |
| `OMS_AUTH_SECRET` / `OMS_TOKEN_TTL` | 登录令牌签名密钥与有效期 |
| `OMS_ADMIN_PASSWORD` | 首次启动初始化的管理员密码 |
| `OMS_OPEN_API_KEY` | `/api/open/**` 开放接口的 `X-Api-Key`（未设置则开放接口不可用） |
| `OMS_WMS_URL` / `OMS_TMS_URL` / `OMS_INTEGRATION_MOCK` | WMS/TMS 地址；mock=true 时不真正外调 |
| `OMS_CORS_ORIGINS` / `OMS_H2_CONSOLE` | 跨域来源、是否开启 H2 控制台 |

### 前端

```bash
cd frontend
npm install
npm run dev
```

### 测试与冒烟

```bash
cd backend && mvn -q test          # 单元 + Spring Boot 流程测试（H2 内存库）
OMS_OPEN_API_KEY=xxx scripts/smoke.sh   # 需先以相同 OMS_OPEN_API_KEY 启动后端
```

冒烟链路：登录 → 基础数据/库存调整 → Open API 渠道下单 → 审核/分仓/推 WMS → WMS 发货回传（扣减库存、创建 TMS 运单）
→ TMS 签收回传 → 退货售后（入库回增、退款）→ 组合商品订单一键处理并取消释放 → 工作台/报表。

## 开放接口（`X-Api-Key`）

| 调用方 | 接口 | 说明 |
| --- | --- | --- |
| 渠道 | `POST /api/open/channel/orders` | 下单（店铺+渠道单号幂等，自动审核店铺可直通至推 WMS） |
| 渠道 | `GET /api/open/channel/orders/{shopCode}/{channelOrderNo}` | 订单状态/物流 |
| 渠道 | `POST /api/open/channel/orders/cancel` | 取消 |
| 渠道 | `GET /api/open/channel/inventory?shopCode=&skus=` | 可售库存 |
| WMS | `POST /api/open/wms/inventory` | 库存同步 |
| WMS | `POST /api/open/wms/shipped` | 发货回传 |
| WMS | `POST /api/open/wms/return-received` | 退货入库回传 |
| TMS | `POST /api/open/tms/signed` / `tms/track` | 签收 / 轨迹回传 |
