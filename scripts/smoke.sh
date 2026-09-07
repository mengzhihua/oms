#!/usr/bin/env bash
# OMS 端到端冒烟：登录 -> 基础数据/库存 -> 渠道下单(Open API) -> 审核/分仓/推WMS -> WMS发货回传 -> TMS签收回传 -> 售后退货/退款 -> 报表
# 用法: scripts/smoke.sh [BASE_URL] ; 环境变量 OMS_ADMIN_PASSWORD / OMS_OPEN_API_KEY 需与后端一致
set -euo pipefail
BASE="${1:-http://localhost:8080}/api"
J='Content-Type: application/json'
PASS="${OMS_ADMIN_PASSWORD:-admin123}"
API_KEY="${OMS_OPEN_API_KEY:-}"
need(){ command -v "$1" >/dev/null || { echo "missing $1"; exit 1; }; }
need curl; need jq
TOKEN=""
call(){ local out; out=$(curl -s -X "$1" "$BASE$2" -H "$J" -H "Authorization: Bearer $TOKEN" ${3:+-d "$3"}); [ "$(echo "$out"|jq -r .code)" = "0" ] || { echo "FAIL $1 $2 -> $out"; exit 1; }; echo "$out"|jq -c .data; }
open(){ local out; out=$(curl -s -X "$1" "$BASE/open$2" -H "$J" -H "X-Api-Key: $API_KEY" ${3:+-d "$3"}); [ "$(echo "$out"|jq -r .code)" = "0" ] || { echo "FAIL OPEN $1 $2 -> $out"; exit 1; }; echo "$out"|jq -c .data; }
TS=$(date +%s)

echo "== 1 auth"
test "$(curl -s "$BASE/order/page" | jq -r .code)" = "401"
TOKEN=$(call POST /auth/login "{\"username\":\"admin\",\"password\":\"$PASS\"}" | jq -r .token); test -n "$TOKEN"
test "$(call GET /auth/me | jq -r .role)" = "ADMIN"

echo "== 2 basic data & inventory"
test "$(call GET '/basic/shop/page?size=100' | jq '.records|length')" -ge 4
test "$(call GET '/basic/warehouse/page?size=100' | jq '.records|length')" -ge 3
test "$(call GET '/inventory/page?size=100&sku=SKU001' | jq '.records|length')" -ge 1
GZ0=$(call GET '/inventory/page?size=10&sku=SKU001&warehouseCode=WH-GZ' | jq '.records[0].qtyOnHand')
call POST /inventory/adjust '{"warehouseCode":"WH-GZ","sku":"SKU001","delta":5,"remark":"smoke"}' >/dev/null
GZ1=$(call GET '/inventory/page?size=10&sku=SKU001&warehouseCode=WH-GZ' | jq '.records[0].qtyOnHand'); test "$GZ1" -eq "$((GZ0+5))"
AV=$(call GET '/inventory/available?sku=SKU001&shopCode=SHOP-JD01' | jq -c .); echo "available: $AV"

echo "== 3 channel order (open api)"
if [ -z "$API_KEY" ]; then echo "OMS_OPEN_API_KEY not set, skip open api"; else
test "$(curl -s -X POST "$BASE/open/channel/orders" -H "$J" -H 'X-Api-Key: wrong' -d '{}' | jq -r .code)" = "401"
CO="TM$TS"
O=$(open POST /channel/orders "{\"shopCode\":\"SHOP-JD01\",\"channelOrderNo\":\"$CO\",\"receiverName\":\"张三\",\"receiverPhone\":\"13800000001\",\"province\":\"广东省\",\"city\":\"深圳市\",\"address\":\"南山区1号\",\"payStatus\":\"PAID\",\"items\":[{\"sku\":\"SKU001\",\"qty\":2,\"price\":199},{\"sku\":\"SKU004\",\"qty\":1,\"price\":29}]}")
ON=$(echo "$O"|jq -r .orderNo); test "$(echo "$O"|jq -r .status)" = "CREATED"
test "$(open GET "/channel/orders/SHOP-JD01/$CO" | jq -r .orderNo)" = "$ON"
test "$(open GET '/channel/inventory?shopCode=SHOP-JD01&skus=SKU001,SKU004' | jq 'length')" -eq 2

echo "== 4 audit -> allocate -> push"
test "$(call POST "/order/$ON/audit" '{}' | jq -r .status)" = "AUDITED"
A=$(call POST "/order/$ON/allocate"); test "$(echo "$A"|jq -r '.[0].status')" = "ALLOCATED"; WH=$(echo "$A"|jq -r '.[0].warehouseCode'); test "$WH" = "WH-GZ"
P=$(call POST "/order/$ON/push"); test "$(echo "$P"|jq -r .status)" = "PUSHED"; test "$(echo "$P"|jq -r .wmsOrderNo)" != "null"

echo "== 5 wms shipped callback -> tms signed callback"
S=$(open POST /wms/shipped "{\"orderNo\":\"$ON\",\"wmsOrderNo\":\"WMS-$ON\",\"carrierCode\":\"SF\",\"trackingNo\":\"SF$TS\",\"items\":{\"SKU001\":2,\"SKU004\":1}}")
test "$(echo "$S"|jq -r .status)" = "SHIPPED"; test "$(echo "$S"|jq -r .tmsOrderNo)" != "null"
GZ2=$(call GET '/inventory/page?size=10&sku=SKU001&warehouseCode=WH-GZ' | jq '.records[0].qtyOnHand'); test "$GZ2" -eq "$((GZ1-2))"
open POST /tms/track "{\"orderNo\":\"$ON\",\"event\":\"IN_TRANSIT\",\"location\":\"深圳转运中心\"}" >/dev/null
test "$(open POST /tms/signed "{\"orderNo\":\"$ON\",\"trackingNo\":\"SF$TS\"}" | jq -r .status)" = "COMPLETED"
test "$(call GET "/order/$ON" | jq '.logs|length')" -ge 6

echo "== 6 after-sale return -> receive -> refund"
R=$(call POST /aftersale "{\"orderNo\":\"$ON\",\"type\":\"RETURN\",\"reason\":\"不喜欢\",\"items\":[{\"sku\":\"SKU001\",\"qty\":1}]}")
RN=$(echo "$R"|jq -r .returnNo); test "$(echo "$R"|jq -r .refundAmount)" = "199.00" || test "$(echo "$R"|jq -r .refundAmount)" = "199"
test "$(call POST "/aftersale/$RN/audit" | jq -r .status)" = "AUDITED"
test "$(open POST /wms/return-received "{\"returnNo\":\"$RN\",\"warehouseCode\":\"WH-GZ\",\"items\":{\"SKU001\":1}}" | jq -r .status)" = "RECEIVED"
GZ3=$(call GET '/inventory/page?size=10&sku=SKU001&warehouseCode=WH-GZ' | jq '.records[0].qtyOnHand'); test "$GZ3" -eq "$((GZ2+1))"
test "$(call POST "/aftersale/$RN/refund" '{}' | jq -r .status)" = "COMPLETED"
test "$(call GET "/aftersale/refund/page?returnNo=$RN" | jq '.records|length')" -ge 1

echo "== 7 cancel with release"
O2=$(call POST /order "{\"shopCode\":\"SHOP-OFF01\",\"channelOrderNo\":\"OFF$TS\",\"receiverName\":\"李四\",\"receiverPhone\":\"13900000000\",\"province\":\"上海市\",\"city\":\"上海市\",\"address\":\"浦东1号\",\"payStatus\":\"PAID\",\"items\":[{\"sku\":\"BUNDLE-01\",\"qty\":1,\"price\":329}]}")
ON2=$(echo "$O2"|jq -r .orderNo); test "$(call GET "/order/$ON2" | jq '.items|length')" -eq 3
test "$(call POST "/order/$ON2/auto" | jq -r '.[0].status')" = "PUSHED"
test "$(call POST "/order/$ON2/cancel" '{"reason":"smoke"}' | jq -r .status)" = "CANCELLED"
fi

echo "== 8 dashboard & reports"
call GET /dashboard | jq -c '{todayOrders,pendingAudit,qtyOnHand,lowStock}'
test "$(call GET '/report/sku-sales?days=30' | jq 'type')" = "array"
call GET '/report/lead-time?days=30' | jq -c .
test "$(call GET '/integration/log/page?size=5' | jq '.records|length')" -ge 1
echo "SMOKE OK"
