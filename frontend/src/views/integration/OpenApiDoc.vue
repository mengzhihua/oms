<template>
  <div class="page">
    <div class="card">
      <h3 style="margin: 0 0 8px">开放接口（Open API）</h3>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        所有 <code>/api/open/**</code> 接口通过请求头 <code>X-Api-Key</code> 鉴权（服务端环境变量 <code>OMS_OPEN_API_KEY</code>）。
        响应统一为 <code>{ code: 0, msg: 'success', data }</code>；非 0 表示业务失败。
      </el-alert>
      <el-table :data="apis" border size="small">
        <el-table-column prop="side" label="调用方" width="90"><template #default="{ row }"><StatusTag :value="row.side" /></template></el-table-column>
        <el-table-column prop="method" label="方法" width="70" />
        <el-table-column prop="path" label="路径" width="330"><template #default="{ row }"><code>{{ row.path }}</code></template></el-table-column>
        <el-table-column prop="desc" label="说明" min-width="220" />
        <el-table-column prop="body" label="请求体 / 参数" min-width="360"><template #default="{ row }"><code style="white-space: pre-wrap">{{ row.body }}</code></template></el-table-column>
      </el-table>
      <h4>示例</h4>
      <pre class="json">curl -X POST http://localhost:8080/api/open/channel/orders \
  -H 'X-Api-Key: $OMS_OPEN_API_KEY' -H 'Content-Type: application/json' \
  -d '{"shopCode":"SHOP-TM01","channelOrderNo":"TM20240001","receiverName":"张三","receiverPhone":"13800000001",
       "province":"广东省","city":"深圳市","address":"南山区科技园1号","payStatus":"PAID","payAmount":228,
       "items":[{"sku":"SKU001","qty":1,"price":199},{"sku":"SKU004","qty":1,"price":29}]}'</pre>
    </div>
  </div>
</template>

<script setup>
import StatusTag from '../../components/StatusTag.vue'

const apis = [
  { side: 'CHANNEL', method: 'POST', path: '/api/open/channel/orders', desc: '渠道下单（按 店铺+渠道单号 幂等；店铺开启自动审核时自动审核→分仓→推 WMS）', body: '{ shopCode, channelOrderNo, receiverName, receiverPhone, province, city, district, address, payStatus, payAmount, items:[{sku, qty, price}] }' },
  { side: 'CHANNEL', method: 'GET', path: '/api/open/channel/orders/{shopCode}/{channelOrderNo}', desc: '查询订单状态/物流', body: '-' },
  { side: 'CHANNEL', method: 'POST', path: '/api/open/channel/orders/cancel', desc: '渠道取消订单（已发货不可取消）', body: '{ shopCode, channelOrderNo, reason }' },
  { side: 'CHANNEL', method: 'GET', path: '/api/open/channel/inventory?shopCode=&skus=a,b', desc: '查询店铺可售库存（按渠道库存策略计算）', body: 'shopCode, skus(逗号分隔)' },
  { side: 'WMS', method: 'POST', path: '/api/open/wms/inventory', desc: 'WMS 库存同步（覆盖在库数量）', body: '[{ warehouseCode, sku, qty }]' },
  { side: 'WMS', method: 'POST', path: '/api/open/wms/shipped', desc: 'WMS 发货回传：扣减库存、订单→SHIPPED、创建 TMS 运单', body: '{ orderNo, wmsOrderNo, carrierCode, trackingNo, items:{sku: qty} }' },
  { side: 'WMS', method: 'POST', path: '/api/open/wms/return-received', desc: 'WMS 退货入库回传：售后单→RECEIVED、库存回增', body: '{ returnNo, warehouseCode, items:{sku: qty} }' },
  { side: 'TMS', method: 'POST', path: '/api/open/tms/signed', desc: 'TMS 签收回传：订单→COMPLETED', body: '{ orderNo, tmsOrderNo, trackingNo, remark }' },
  { side: 'TMS', method: 'POST', path: '/api/open/tms/track', desc: 'TMS 轨迹回传（仅记录日志）', body: '{ orderNo, event, location, remark }' }
]
</script>

<style scoped>
.json { background: #f5f7fa; padding: 10px; border-radius: 4px; font-size: 12px; overflow: auto; }
code { background: #f5f7fa; padding: 1px 4px; border-radius: 3px; font-size: 12px; }
</style>
