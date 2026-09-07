<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <span>统计周期</span>
        <el-radio-group v-model="days" size="small" @change="load">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
          <el-radio-button :value="90">近 90 天</el-radio-button>
        </el-radio-group>
        <el-button @click="downloadCsv('/report/sku-sales/export', { days }, 'sku_sales.csv')">导出 SKU 销量</el-button>
      </div>
      <el-tabs v-model="tab">
        <el-tab-pane label="每日订单" name="daily">
          <el-table :data="daily" size="small" border stripe v-loading="loading">
            <el-table-column prop="order_day" label="日期" width="120"><template #default="{ row }">{{ String(row.order_day).slice(0, 10) }}</template></el-table-column>
            <el-table-column prop="orders" label="订单数" width="100" align="right" />
            <el-table-column prop="cancelled" label="取消" width="100" align="right" />
            <el-table-column prop="shipped" label="已发货" width="100" align="right" />
            <el-table-column prop="amount" label="销售额" align="right" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="渠道/店铺" name="channel">
          <el-table :data="channel" size="small" border stripe v-loading="loading">
            <el-table-column prop="channel_code" label="渠道" width="130" />
            <el-table-column prop="shop_code" label="店铺" width="160" />
            <el-table-column prop="orders" label="订单数" width="100" align="right" />
            <el-table-column prop="cancelled" label="取消" width="100" align="right" />
            <el-table-column prop="amount" label="销售额" align="right" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="SKU 销量" name="sku">
          <el-table :data="sku" size="small" border stripe v-loading="loading">
            <el-table-column type="index" width="50" />
            <el-table-column prop="sku" label="SKU" width="140" />
            <el-table-column prop="product_name" label="商品" min-width="180" />
            <el-table-column prop="qty" label="销量" width="100" align="right" />
            <el-table-column prop="orders" label="订单数" width="100" align="right" />
            <el-table-column prop="amount" label="销售额" align="right" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="仓库发货" name="wh">
          <el-table :data="wh" size="small" border stripe v-loading="loading">
            <el-table-column prop="warehouse_code" label="仓库" width="140" />
            <el-table-column prop="orders" label="分配订单" width="110" align="right" />
            <el-table-column prop="shipped" label="已发货" width="110" align="right" />
            <el-table-column prop="pending" label="待发货" width="110" align="right" />
            <el-table-column label="发货率" align="right"><template #default="{ row }">{{ row.orders ? ((row.shipped / row.orders) * 100).toFixed(1) + '%' : '-' }}</template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="售后" name="as">
          <el-table :data="as" size="small" border stripe v-loading="loading">
            <el-table-column prop="type" label="类型" width="120"><template #default="{ row }"><StatusTag :value="row.type" /></template></el-table-column>
            <el-table-column prop="status" label="状态" width="120"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
            <el-table-column prop="cnt" label="数量" width="100" align="right" />
            <el-table-column prop="refund_amount" label="退款金额" align="right" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="订单时效" name="lt">
          <el-row :gutter="12">
            <el-col :span="4" v-for="s in leadStats" :key="s.label">
              <div class="stat"><div class="label">{{ s.label }}</div><div class="value">{{ s.value ?? '-' }}</div></div>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { report, downloadCsv } from '../../api'
import StatusTag from '../../components/StatusTag.vue'

const days = ref(30)
const tab = ref('daily')
const loading = ref(false)
const daily = ref([]); const channel = ref([]); const sku = ref([]); const wh = ref([]); const as = ref([]); const lead = ref({})
const leadStats = computed(() => [
  { label: '订单数', value: lead.value.orders },
  { label: '平均审核(h)', value: lead.value.audit_hours },
  { label: '平均推送(h)', value: lead.value.push_hours },
  { label: '平均发货(h)', value: lead.value.ship_hours },
  { label: '平均签收(h)', value: lead.value.deliver_hours }
])

async function load() {
  loading.value = true
  try {
    ;[daily.value, channel.value, sku.value, wh.value, as.value, lead.value] = await Promise.all([
      report.orderDaily(days.value), report.channelSales({ from: new Date(Date.now() - (days.value - 1) * 86400000).toISOString().slice(0, 10) }), report.skuSales(days.value), report.warehouseShip(days.value), report.afterSale(days.value), report.leadTime(days.value)
    ])
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
