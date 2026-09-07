<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="4" v-for="s in stats" :key="s.label">
        <div class="stat">
          <div class="label">{{ s.label }}</div>
          <div class="value" :style="{ color: s.color }">{{ s.value }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="card" style="margin-top: 12px">
      <h3 style="margin: 0 0 12px">待办事项</h3>
      <el-row :gutter="12">
        <el-col :span="4" v-for="t in todos" :key="t.label">
          <div class="todo" @click="$router.push(t.to)">
            <div class="todo-value" :style="{ color: t.color }">{{ t.value ?? 0 }}</div>
            <div class="todo-label">{{ t.label }}</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :span="14">
        <div class="card">
          <h3 style="margin: 0 0 12px">近 7 日订单趋势</h3>
          <div class="bars">
            <div v-for="t in trend" :key="t.order_day" class="bar-col">
              <div class="bar-num">{{ t.orders }}</div>
              <div class="bar" :style="{ height: barH(t.orders) + 'px' }" :title="`${t.orders} 单 / ¥${t.amount}`"></div>
              <div class="bar-day">{{ String(t.order_day).slice(5, 10) }}</div>
            </div>
            <el-empty v-if="!trend.length" description="暂无数据" :image-size="60" />
          </div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="card">
          <h3 style="margin: 0 0 12px">今日渠道订单</h3>
          <el-table :data="data.channelToday || []" size="small" stripe>
            <el-table-column prop="channel_code" label="渠道" width="120" />
            <el-table-column prop="orders" label="订单数" width="90" align="right" />
            <el-table-column prop="amount" label="销售额" align="right" />
          </el-table>
        </div>
        <div class="card" style="margin-top: 12px">
          <h3 style="margin: 0 0 12px">订单状态分布</h3>
          <div class="status-grid">
            <div v-for="(v, k) in data.orderStatus || {}" :key="k" class="status-cell" @click="$router.push({ path: '/order/list', query: { status: k } })">
              <StatusTag :value="k" /><b>{{ v }}</b>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { report } from '../api'
import StatusTag from '../components/StatusTag.vue'

const data = ref({})
const trend = computed(() => data.value.trend || [])
const maxOrders = computed(() => Math.max(1, ...trend.value.map((t) => Number(t.orders))))
const barH = (n) => Math.round((Number(n) / maxOrders.value) * 120)

const stats = computed(() => [
  { label: '今日订单', value: data.value.todayOrders ?? '-', color: '#409eff' },
  { label: '今日销售额', value: data.value.todayAmount ?? '-', color: '#67c23a' },
  { label: '今日发货', value: data.value.todayShipped ?? '-' },
  { label: '在库 / 预占', value: `${data.value.qtyOnHand ?? '-'} / ${data.value.qtyReserved ?? '-'}` },
  { label: '有货 SKU', value: data.value.skuInStock ?? '-' },
  { label: '低库存 SKU', value: data.value.lowStock ?? '-', color: '#f56c6c' }
])
const todos = computed(() => [
  { label: '待审核', value: data.value.pendingAudit, color: '#409eff', to: { path: '/order/list', query: { status: 'CREATED' } } },
  { label: '待分仓', value: data.value.pendingAllocate, color: '#409eff', to: { path: '/order/list', query: { status: 'AUDITED' } } },
  { label: '待推送 WMS', value: data.value.pendingPush, color: '#e6a23c', to: { path: '/order/list', query: { status: 'ALLOCATED' } } },
  { label: '待发货', value: data.value.pendingShip, color: '#e6a23c', to: { path: '/order/list', query: { status: 'PUSHED' } } },
  { label: '挂起', value: data.value.hold, color: '#f56c6c', to: { path: '/order/list', query: { status: 'HOLD' } } },
  { label: '待处理售后', value: data.value.pendingAfterSale, color: '#f56c6c', to: '/aftersale/list' },
  { label: '在途', value: data.value.inTransit, to: { path: '/order/list', query: { status: 'SHIPPED' } } },
  { label: '接口失败(7日)', value: data.value.integrationFailed, color: '#f56c6c', to: '/integration/log' }
])
onMounted(async () => { data.value = await report.dashboard() })
</script>

<style scoped>
.todo { text-align: center; padding: 10px 0; border-radius: 6px; cursor: pointer; background: #f5f7fa; margin-bottom: 8px; }
.todo:hover { background: #ecf5ff; }
.todo-value { font-size: 24px; font-weight: 600; }
.todo-label { color: #909399; font-size: 13px; }
.bars { display: flex; align-items: flex-end; gap: 12px; height: 170px; padding: 0 8px; }
.bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; }
.bar { width: 60%; background: #409eff; border-radius: 3px 3px 0 0; min-height: 2px; }
.bar-num { font-size: 12px; color: #606266; }
.bar-day { font-size: 12px; color: #909399; margin-top: 4px; }
.status-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.status-cell { display: flex; justify-content: space-between; align-items: center; padding: 6px 10px; background: #f5f7fa; border-radius: 4px; cursor: pointer; }
</style>
