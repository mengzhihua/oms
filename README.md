# OMS 订单管理系统

OTWB 供应链平台（OMS / TMS / WMS / BMS）中的订单中枢：承接多渠道（天猫/京东/抖音/线下/API）订单，完成审核、
分仓路由、库存预占、推送 WMS、发货/签收回传、售后退换货与退款，并向渠道提供可售库存。
与本组织的 [WMS](https://github.com/mengzhihua/wms) / [TMS](https://github.com/mengzhihua/tms) 同构
（Spring Boot 2.7 + MyBatis-Plus / Vue 3 + Element Plus）。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | JDK 17、Spring Boot 2.7.18、MyBatis-Plus 3.5.3、H2（本地/测试）、MySQL 8（生产） |
| 前端 | Vue 3、Vite 5、Element Plus、Vue Router、Axios |
| 鉴权 | 登录 Bearer Token（HMAC 签名）、角色 ADMIN / OPERATOR / VIEWER；开放接口 `X-Api-Key` |
| 测试 | JUnit 5 + Spring Boot Test（H2 内存库）、`scripts/smoke.sh`（curl + jq 全链路冒烟） |

## 功能范围

| 模块 | 能力 |
| --- | --- |
| 基础数据 | 渠道、店铺（默认仓/自动审核）、客户、商品（普通/组合 BOM）、仓库、承运商、分仓路由规则 |
| 库存中心 | 多仓库存（在库/预占/安全）、渠道库存策略（比例/固定/限定仓）、调整、预占/释放/扣减/退回流水、CSV 导出 |
| 订单中心 | 订单状态机、挂起/取消、按省份/渠道路由、多仓自动拆单、手工拆单、批量操作、一键处理、CSV 导入/导出、操作日志 |
| 售后 | 退货退款 / 仅退款 / 换货，审核、驳回、退货入库回增库存、退款记录 |
| 系统集成 | WMS（出库单推送/取消、退货入库单）、TMS（运单创建）适配器，本地 mock；开放接口供渠道/WMS/TMS 回调；集成日志 |
| 报表 | 工作台（今日概览、待办、趋势、渠道分布）、每日订单、渠道/店铺、SKU 销量、仓库发货、售后、履约时效 |
| 系统管理 | 用户与角色、修改密码、操作日志 |

## 目录结构

```text
backend/                          Spring Boot 后端（端口 8080）
  src/main/java/com/oms/
    common/      通用：R 统一返回、BaseEntity、BaseCrudController、分页、CSV、全局异常
    system/      登录/令牌/权限拦截器、用户、操作日志
    basic/       渠道、店铺、客户、商品、组合 BOM、仓库、承运商、路由规则
    inventory/   多仓库存、渠道库存策略、库存流水
    order/       销售订单、订单明细、订单日志、状态机、分仓路由服务
    aftersale/   售后单、退货明细、退款记录
    integration/ WMS/TMS 适配器、集成日志、开放接口（/api/open/**）
    report/      工作台与报表
  src/main/resources/
    application.yml   默认 H2 profile + mysql profile
    schema.sql        建表（H2/MySQL 兼容）
    data.sql          幂等演示数据
frontend/                         Vue 3 + Vite 前端（端口 5173，/api 代理到 8080）
  src/views/  Dashboard、order/、aftersale/、inventory/、basic/、integration/、report/、system/
scripts/smoke.sh                  端到端冒烟脚本
```

## 快速开始

### 1. 后端

要求 JDK 17 和 Maven 3.6+：

```bash
cd backend
OMS_OPEN_API_KEY=dev-key mvn spring-boot:run
```

- 默认 H2 文件库 `backend/data/oms`，启动时自动执行 `schema.sql`（建表）与 `data.sql`
  （幂等演示数据：渠道/店铺/仓库/承运商/商品/组合 BOM/路由规则/库存/渠道库存策略）。
- 默认管理员 `admin / admin123`（可用 `OMS_ADMIN_PASSWORD` 覆盖，仅首次初始化生效），登录后请及时修改。
- MySQL：先建库，然后 `mvn spring-boot:run -Dspring-boot.run.profiles=mysql`，
  通过 `DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD` 配置连接。
- 国内网络若 Maven Central 拉取依赖被限流（HTTP 429），可在 `~/.m2/settings.xml` 配置阿里云镜像：

  ```xml
  <settings><mirrors><mirror>
    <id>aliyun</id><mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror></mirrors></settings>
  ```

### 2. 前端

要求 Node.js 18+：

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173 ，/api 代理到 http://localhost:8080
npm run build      # 产物在 frontend/dist，可交给 Nginx 等静态托管并把 /api 反代到后端
```

浏览器打开 http://localhost:5173 ，使用 `admin / admin123` 登录。
（Linux 服务器/无头环境预览时如中文显示为方块，安装中文字体即可：`sudo apt-get install -y fonts-noto-cjk`。）

### 3. 测试与冒烟

```bash
cd backend && mvn -q test                # 单元 + Spring Boot 流程测试（H2 内存库）
OMS_OPEN_API_KEY=dev-key scripts/smoke.sh # 需先以相同 OMS_OPEN_API_KEY 启动后端；依赖 curl、jq
```

冒烟链路：登录 → 基础数据/库存调整 → Open API 渠道下单 → 审核/分仓/推 WMS → WMS 发货回传（扣减库存、创建 TMS 运单）
→ TMS 签收回传 → 退货售后（入库回增、退款）→ 组合商品订单一键处理并取消释放 → 工作台/报表，末尾输出 `SMOKE OK`。

## 配置项

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `OMS_AUTH_SECRET` / `OMS_TOKEN_TTL` | 空（每次启动随机）/ 12h | 登录令牌签名密钥与有效期；生产必须设置密钥，否则重启后全员需重新登录 |
| `OMS_ADMIN_PASSWORD` | `admin123` | 首次启动初始化的管理员密码 |
| `OMS_OPEN_API_KEY` | `oms-open-key` | `/api/open/**` 开放接口的 `X-Api-Key`（生产务必更换；设为空则开放接口全部拒绝） |
| `OMS_WMS_URL` / `OMS_TMS_URL` | 空 | WMS / TMS 基地址 |
| `OMS_INTEGRATION_MOCK` | `true` | 为 `true` 时不真正外调 WMS/TMS，本地生成 `WMS-xxx` / `TMS-xxx` 单号 |
| `OMS_CORS_ORIGINS` | `http://localhost:5173` | 允许的跨域来源，逗号分隔 |
| `OMS_H2_CONSOLE` | `false` | 是否开启 H2 控制台 `/h2-console` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | localhost / 3306 / oms / 必填 / 必填 | mysql profile 数据库连接 |

## 业务规则

### 订单状态机

```text
CREATED ──审核──▶ AUDITED ──分仓(预占库存)──▶ ALLOCATED ──推WMS──▶ PUSHED ──WMS发货回传(扣减库存, 建TMS运单)──▶ SHIPPED ──TMS签收──▶ COMPLETED
   │  ▲              │                            │
   ▼  │挂起/解挂      ▼                            ▼ 多仓无法整单满足 → 自动拆单，母单置为 SPLIT
  HOLD               └─ CREATED/AUDITED/HOLD/ALLOCATED/PUSHED 均可取消 → CANCELLED（释放预占、通知 WMS 取消）
```

- 自动审核店铺（`autoAudit=1`）仅对 `payStatus=PAID` 的订单自动审核；未付款订单不可审核。
- 分仓路由：先按路由规则（省份/渠道 → 仓库优先级）与店铺默认仓找单仓能整单满足者；否则按可用量多仓拆单。
- 组合商品（BUNDLE）下单时按 BOM 展开为子 SKU 明细。
- 同一店铺 + 渠道单号幂等，重复下单被拒绝。

### 库存

- 可用量 = 在库 − 预占；渠道可售 = Σ各仓 max(0, 可用 − 安全库存)，再按店铺策略取比例（RATIO）或固定值（FIXED）。
- 流水类型：`SYNC`（WMS 同步）、`ADJUST`、`RESERVE`、`RELEASE`、`DEDUCT`、`RETURN`。

### 售后

```text
CREATED → AUDITED → [RECEIVED 退货入库回增库存] → [REFUNDED] → COMPLETED；任意未完成态可 REJECTED / CANCELLED
```

- `RETURN` 退货退款：审核后推 WMS 退货入库单，收货回增库存后退款完成（订单需已发货/已完成）。
- `REFUND_ONLY` 仅退款：审核后直接退款完成，无需入库。
- `EXCHANGE` 换货：退款金额为 0，收货后自动创建 0 元换货销售订单并完成。

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

示例：

```bash
curl -X POST http://localhost:8080/api/open/channel/orders \
  -H 'Content-Type: application/json' -H 'X-Api-Key: dev-key' \
  -d '{"shopCode":"SHOP-JD01","channelOrderNo":"JD20260907001","payStatus":"PAID",
       "receiverName":"张三","receiverPhone":"13800000000","province":"上海","city":"上海市","address":"xx路1号",
       "items":[{"sku":"SKU001","qty":2,"price":99}]}'
```

前端「系统集成 → 开放接口说明」页面也提供了完整说明。所有接口统一返回 `{"code":0,"msg":"ok","data":...}`，非 0 为业务错误。

## 演示数据

渠道 `TMALL / JD / DOUYIN / OFFLINE / API`；店铺 `SHOP-TM01 / SHOP-JD01 / SHOP-DY01 / SHOP-OFF01`；
仓库 `WH-SH / WH-BJ / WH-GZ`；承运商 `SF / JDL / ZTO / SELF`；商品 `SKU001`–`SKU005` 与组合商品 `BUNDLE-01`；
客户 `C001`–`C003`；三条分仓路由规则及多仓库存、渠道库存策略。
