<template>
  <div class="page">
    <div class="card">
      <el-radio-group v-model="query.status" size="small" @change="reload" style="margin-bottom: 10px; flex-wrap: wrap">
        <el-radio-button value="">全部 {{ sum }}</el-radio-button>
        <el-radio-button v-for="s in STATUSES" :key="s" :value="s">{{ label(s) }} {{ counts[s] || 0 }}</el-radio-button>
      </el-radio-group>
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="订单号/渠道单号/收件人/电话/运单号" clearable @keyup.enter="reload" @clear="reload" style="width: 260px" />
        <el-select v-model="query.shopCode" placeholder="店铺" clearable filterable @change="reload" style="width: 160px">
          <el-option v-for="o in options.shop" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.warehouseCode" placeholder="发货仓" clearable @change="reload" style="width: 150px">
          <el-option v-for="o in options.warehouse" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="下单起" end-placeholder="下单止" @change="reload" style="width: 240px" />
        <el-button type="primary" @click="reload"><el-icon><Search /></el-icon>查询</el-button>
        <el-button type="success" @click="$router.push('/order/create')" v-if="canWrite()"><el-icon><Plus /></el-icon>手工下单</el-button>
        <el-button @click="downloadCsv('/order/export', { status: query.status || undefined }, 'orders.csv')">导出 CSV</el-button>
        <el-dropdown v-if="canWrite()" @command="batch" trigger="click">
          <el-button :disabled="!selected.length">批量操作({{ selected.length }})<el-icon><ArrowDown /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="audit">批量审核</el-dropdown-item>
              <el-dropdown-item command="allocate">批量分仓预占</el-dropdown-item>
              <el-dropdown-item command="push">批量推送 WMS</el-dropdown-item>
              <el-dropdown-item command="auto">一键处理(审核→分仓→推送)</el-dropdown-item>
              <el-dropdown-item command="cancel" divided>批量取消</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe size="small" @selection-change="(v) => (selected = v)">
        <el-table-column type="selection" width="40" />
        <el-table-column prop="orderNo" label="订单号" width="170">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/order/detail/${row.orderNo}`)">{{ row.orderNo }}</el-link>
            <el-tag v-if="row.parentOrderNo" size="small" type="info" style="margin-left: 4px">子</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column prop="shopCode" label="店铺" width="110" />
        <el-table-column prop="channelOrderNo" label="渠道单号" width="150" show-overflow-tooltip />
        <el-table-column prop="receiverName" label="收件人" width="90" />
        <el-table-column label="收货地" min-width="160" show-overflow-tooltip><template #default="{ row }">{{ row.province }}{{ row.city }}{{ row.district }}</template></el-table-column>
        <el-table-column prop="payAmount" label="实付" width="90" align="right" />
        <el-table-column prop="warehouseCode" label="发货仓" width="100" />
        <el-table-column prop="carrierCode" label="承运商" width="80" />
        <el-table-column prop="trackingNo" label="运单号" width="140" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先" width="60" align="center" />
        <el-table-column prop="orderTime" label="下单时间" width="150"><template #default="{ row }">{{ fmt(row.orderTime) }}</template></el-table-column>
        <el-table-column label="操作" width="230" fixed="right" v-if="canWrite()">
          <template #default="{ row }">
            <el-button v-if="row.status === 'CREATED'" link type="primary" size="small" @click="act(order.audit, row, '审核')">审核</el-button>
            <el-button v-if="['CREATED', 'AUDITED'].includes(row.status)" link type="warning" size="small" @click="withReason('挂起原因', (r) => order.hold(row.orderNo, r))">挂起</el-button>
            <el-button v-if="row.status === 'HOLD'" link type="primary" size="small" @click="act(order.unhold, row, '解挂')">解挂</el-button>
            <el-button v-if="row.status === 'AUDITED'" link type="primary" size="small" @click="act(order.allocate, row, '分仓预占')">分仓</el-button>
            <el-button v-if="row.status === 'ALLOCATED'" link type="primary" size="small" @click="act(order.push, row, '推送WMS')">推WMS</el-button>
            <el-button v-if="['CREATED', 'AUDITED'].includes(row.status)" link type="success" size="small" @click="act(order.auto, row, '一键处理')">一键</el-button>
            <el-button v-if="['ALLOCATED', 'PUSHED'].includes(row.status)" link type="success" size="small" @click="openShip(row)">发货</el-button>
            <el-button v-if="row.status === 'SHIPPED'" link type="success" size="small" @click="act(order.complete, row, '确认完成')">完成</el-button>
            <el-button v-if="['CREATED', 'AUDITED', 'HOLD', 'ALLOCATED', 'PUSHED'].includes(row.status)" link type="danger" size="small" @click="withReason('取消原因', (r) => order.cancel(row.orderNo, r))">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50, 100]" @change="load" />
      </div>
    </div>

    <el-dialog v-model="shipVisible" title="人工登记发货" width="420px" destroy-on-close>
      <el-form :model="ship" label-width="90px">
        <el-form-item label="订单号">{{ ship.orderNo }}</el-form-item>
        <el-form-item label="承运商">
          <el-select v-model="ship.carrierCode" filterable style="width: 100%">
            <el-option v-for="o in options.carrier" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="运单号" required><el-input v-model="ship.trackingNo" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" @click="doShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { order, downloadCsv } from '../../api'
import { useOptions } from '../../composables/useOptions'
import StatusTag from '../../components/StatusTag.vue'
import { canWrite } from '../../auth'
import { fmt } from '../../utils'
import { useRoute } from 'vue-router'

const STATUSES = ['CREATED', 'HOLD', 'AUDITED', 'ALLOCATED', 'PUSHED', 'SHIPPED', 'COMPLETED', 'CANCELLED']
const LABEL = { CREATED: '待审核', HOLD: '挂起', AUDITED: '待分仓', ALLOCATED: '待推送', PUSHED: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
const label = (s) => LABEL[s] || s

const route = useRoute()
const { options } = useOptions(['shop', 'warehouse', 'carrier'])
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const counts = ref({})
const range = ref([])
const query = reactive({ current: 1, size: 20, keyword: '', status: route.query.status || '', shopCode: '', warehouseCode: '' })
const sum = computed(() => STATUSES.reduce((a, s) => a + (counts.value[s] || 0), 0))

async function load() {
  loading.value = true
  try {
    const params = { ...query, from: range.value?.[0] || undefined, to: range.value?.[1] || undefined }
    const [p, c] = await Promise.all([order.page(params), order.statusCount()])
    rows.value = p.records
    total.value = p.total
    counts.value = c
  } finally {
    loading.value = false
  }
}
function reload() { query.current = 1; load() }

async function act(fn, row, name) {
  await fn(row.orderNo)
  ElMessage.success(`${name}成功`)
  load()
}

async function withReason(title, fn) {
  const { value } = await ElMessageBox.prompt('请输入原因', title, { inputValidator: (v) => (v && v.trim() ? true : '原因必填') })
  await fn(value)
  ElMessage.success('操作成功')
  load()
}

const shipVisible = ref(false)
const ship = reactive({ orderNo: '', carrierCode: '', trackingNo: '' })
function openShip(row) {
  Object.assign(ship, { orderNo: row.orderNo, carrierCode: row.carrierCode || '', trackingNo: '' })
  shipVisible.value = true
}
async function doShip() {
  if (!ship.trackingNo) return ElMessage.warning('运单号必填')
  await order.ship(ship.orderNo, { carrierCode: ship.carrierCode, trackingNo: ship.trackingNo })
  ElMessage.success('发货成功')
  shipVisible.value = false
  load()
}

async function batch(action) {
  let reason
  if (action === 'cancel') {
    reason = (await ElMessageBox.prompt('请输入取消原因', '批量取消', { inputValidator: (v) => (v && v.trim() ? true : '原因必填') })).value
  }
  const r = await order.batch({ orderNos: selected.value.map((o) => o.orderNo), action, reason })
  const failed = Object.entries(r.failed || {})
  if (failed.length) {
    ElMessageBox.alert(failed.map(([no, err]) => `${no}: ${err}`).join('<br/>'), `成功 ${r.success.length}，失败 ${failed.length}`, { dangerouslyUseHTMLString: true })
  } else {
    ElMessage.success(`全部成功：${r.success.length} 单`)
  }
  load()
}

onMounted(load)
</script>
