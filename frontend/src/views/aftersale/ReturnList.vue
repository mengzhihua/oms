<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="售后单号/订单号/退货运单" clearable @keyup.enter="reload" @clear="reload" style="width: 240px" />
        <el-select v-model="query.type" placeholder="类型" clearable @change="reload" style="width: 130px">
          <el-option label="退货退款" value="RETURN" /><el-option label="仅退款" value="REFUND_ONLY" /><el-option label="换货" value="EXCHANGE" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable @change="reload" style="width: 130px">
          <el-option v-for="s in STATUSES" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="query.shopCode" placeholder="店铺" clearable filterable @change="reload" style="width: 160px">
          <el-option v-for="o in options.shop" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-button type="primary" @click="reload"><el-icon><Search /></el-icon>查询</el-button>
        <el-button type="success" v-if="canWrite()" @click="openCreate()"><el-icon><Plus /></el-icon>创建售后单</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column prop="returnNo" label="售后单号" width="170">
          <template #default="{ row }"><el-link type="primary" @click="$router.push(`/aftersale/detail/${row.returnNo}`)">{{ row.returnNo }}</el-link></template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100"><template #default="{ row }"><StatusTag :value="row.type" /></template></el-table-column>
        <el-table-column prop="status" label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column prop="orderNo" label="原订单" width="170">
          <template #default="{ row }"><el-link type="primary" @click="$router.push(`/order/detail/${row.orderNo}`)">{{ row.orderNo }}</el-link></template>
        </el-table-column>
        <el-table-column prop="shopCode" label="店铺" width="110" />
        <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
        <el-table-column prop="refundAmount" label="退款金额" width="100" align="right" />
        <el-table-column prop="warehouseCode" label="退货仓" width="100" />
        <el-table-column prop="returnTrackingNo" label="退货运单" width="140" />
        <el-table-column prop="exchangeOrderNo" label="换货新单" width="170" />
        <el-table-column prop="createdAt" label="创建时间" width="150"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="200" fixed="right" v-if="canWrite()">
          <template #default="{ row }">
            <el-button v-if="row.status === 'CREATED'" link type="primary" size="small" @click="act(() => aftersale.audit(row.returnNo), '审核')">审核</el-button>
            <el-button v-if="['CREATED', 'AUDITED'].includes(row.status)" link type="danger" size="small" @click="reject(row)">驳回</el-button>
            <el-button v-if="row.status === 'CREATED'" link type="warning" size="small" @click="act(() => aftersale.cancel(row.returnNo), '取消')">取消</el-button>
            <el-button v-if="row.status === 'AUDITED' && row.type !== 'REFUND_ONLY'" link type="success" size="small" @click="act(() => aftersale.receive(row.returnNo), '退货入库')">入库</el-button>
            <el-button v-if="row.status === 'RECEIVED' && row.type === 'RETURN'" link type="success" size="small" @click="act(() => aftersale.refund(row.returnNo), '退款')">退款</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="createVisible" title="创建售后单" width="720px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="原订单号" required>
              <el-input v-model="form.orderNo" placeholder="输入订单号后回车加载明细" @keyup.enter="loadOrder" @blur="loadOrder" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="售后类型">
              <el-radio-group v-model="form.type">
                <el-radio-button value="RETURN">退货退款</el-radio-button>
                <el-radio-button value="REFUND_ONLY">仅退款</el-radio-button>
                <el-radio-button value="EXCHANGE">换货</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="原因" required><el-input v-model="form.reason" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="退货仓">
              <el-select v-model="form.warehouseCode" clearable filterable style="width: 100%">
                <el-option v-for="w in options.warehouse" :key="w.value" :label="w.label" :value="w.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="退款金额"><el-input-number v-model="form.refundAmount" :min="0" :precision="2" style="width: 100%" placeholder="留空按明细金额" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="退货运单"><el-input v-model="form.returnTrackingNo" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item></el-col>
        </el-row>
        <el-table v-if="form.type !== 'REFUND_ONLY'" :data="orderItems" border size="small">
          <el-table-column prop="sku" label="SKU" width="130" />
          <el-table-column prop="productName" label="商品" min-width="150" />
          <el-table-column prop="qty" label="购买数" width="70" />
          <el-table-column label="售后数量" width="140"><template #default="{ row }"><el-input-number v-model="row.rqty" :min="0" :max="row.qty" size="small" /></template></el-table-column>
          <el-table-column v-if="form.type === 'EXCHANGE'" label="换货SKU" min-width="180">
            <template #default="{ row }">
              <el-select v-model="row.exchangeSku" filterable clearable size="small" placeholder="默认同 SKU" style="width: 100%">
                <el-option v-for="p in options.product" :key="p.value" :label="p.label" :value="p.value" />
              </el-select>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="create">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aftersale, order } from '../../api'
import { useOptions } from '../../composables/useOptions'
import StatusTag from '../../components/StatusTag.vue'
import { canWrite } from '../../auth'
import { fmt } from '../../utils'

const STATUSES = ['CREATED', 'AUDITED', 'RECEIVED', 'REFUNDED', 'COMPLETED', 'REJECTED', 'CANCELLED']
const route = useRoute()
const { options } = useOptions(['shop', 'warehouse', 'product'])
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ current: 1, size: 20, keyword: '', type: '', status: '', shopCode: '' })

const createVisible = ref(false)
const form = reactive({ orderNo: '', type: 'RETURN', reason: '', warehouseCode: '', refundAmount: undefined, returnTrackingNo: '', remark: '' })
const orderItems = ref([])

async function load() {
  loading.value = true
  try {
    const p = await aftersale.page(query)
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}
function reload() { query.current = 1; load() }

async function act(fn, name) {
  await fn()
  ElMessage.success(`${name}成功`)
  load()
}
async function reject(row) {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回', { inputValidator: (v) => (v && v.trim() ? true : '原因必填') })
  await act(() => aftersale.reject(row.returnNo, value), '驳回')
}

function openCreate(orderNo) {
  Object.assign(form, { orderNo: orderNo || '', type: 'RETURN', reason: '', warehouseCode: '', refundAmount: undefined, returnTrackingNo: '', remark: '' })
  orderItems.value = []
  createVisible.value = true
  if (orderNo) loadOrder()
}
async function loadOrder() {
  if (!form.orderNo) return
  const d = await order.get(form.orderNo.trim())
  form.orderNo = d.order.orderNo
  form.warehouseCode = form.warehouseCode || d.order.warehouseCode || ''
  orderItems.value = d.items.map((i) => ({ ...i, rqty: i.qty, exchangeSku: '' }))
}
async function create() {
  if (!form.orderNo || !form.reason) return ElMessage.warning('订单号与原因必填')
  const items = form.type === 'REFUND_ONLY' ? [] : orderItems.value.filter((i) => i.rqty > 0).map((i) => ({ sku: i.sku, qty: i.rqty, exchangeSku: i.exchangeSku || undefined }))
  saving.value = true
  try {
    const r = await aftersale.create({ ...form, items })
    ElMessage.success(`售后单 ${r.returnNo} 已创建`)
    createVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  load()
  if (route.query.create) openCreate(route.query.create)
})
</script>
