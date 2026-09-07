<template>
  <div class="page" v-loading="loading">
    <div class="card" v-if="r.returnNo">
      <div class="toolbar" style="justify-content: space-between">
        <div style="display: flex; align-items: center; gap: 10px">
          <el-button @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回</el-button>
          <h3 style="margin: 0">售后单 {{ r.returnNo }}</h3>
          <StatusTag :value="r.type" /><StatusTag :value="r.status" />
        </div>
        <div v-if="canWrite()">
          <el-button v-if="r.status === 'CREATED'" type="primary" @click="run(() => aftersale.audit(r.returnNo), '审核')">审核</el-button>
          <el-button v-if="['CREATED', 'AUDITED'].includes(r.status)" type="danger" @click="reject">驳回</el-button>
          <el-button v-if="r.status === 'CREATED'" type="warning" @click="run(() => aftersale.cancel(r.returnNo), '取消')">取消</el-button>
          <el-button v-if="r.status === 'AUDITED' && r.type !== 'REFUND_ONLY'" type="success" @click="openReceive">退货入库</el-button>
          <el-button v-if="r.status === 'RECEIVED' && r.type === 'RETURN'" type="success" @click="refundVisible = true">退款</el-button>
        </div>
      </div>
      <el-descriptions :column="4" border size="small" style="margin-top: 12px">
        <el-descriptions-item label="原订单"><el-link type="primary" @click="$router.push(`/order/detail/${r.orderNo}`)">{{ r.orderNo }}</el-link></el-descriptions-item>
        <el-descriptions-item label="渠道/店铺">{{ r.channelCode }} / {{ r.shopCode }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ r.customerCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="退款金额">{{ r.refundAmount }}</el-descriptions-item>
        <el-descriptions-item label="原因" :span="2">{{ r.reason }}</el-descriptions-item>
        <el-descriptions-item label="退货仓">{{ r.warehouseCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="退货运单">{{ r.returnTrackingNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="换货新单">
          <el-link v-if="r.exchangeOrderNo" type="primary" @click="$router.push(`/order/detail/${r.exchangeOrderNo}`)">{{ r.exchangeOrderNo }}</el-link><span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="审核">{{ fmt(r.auditedAt) }} {{ r.auditedBy }}</el-descriptions-item>
        <el-descriptions-item label="收货">{{ fmt(r.receivedAt) }}</el-descriptions-item>
        <el-descriptions-item label="退款/完成">{{ fmt(r.refundedAt) }} / {{ fmt(r.completedAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="4">{{ r.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4>售后明细</h4>
      <el-table :data="d.items" border size="small">
        <el-table-column prop="sku" label="SKU" width="140" />
        <el-table-column prop="productName" label="商品" min-width="180" />
        <el-table-column prop="qty" label="申请数量" width="90" align="right" />
        <el-table-column prop="receivedQty" label="实收" width="80" align="right" />
        <el-table-column prop="amount" label="金额" width="100" align="right" />
        <el-table-column prop="exchangeSku" label="换货SKU" width="140" />
      </el-table>

      <h4>退款记录</h4>
      <el-table :data="d.refunds" border size="small">
        <el-table-column prop="refundNo" label="退款单号" width="170" />
        <el-table-column prop="amount" label="金额" width="100" align="right" />
        <el-table-column prop="method" label="方式" width="110" />
        <el-table-column prop="status" label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column prop="paidAt" label="时间" width="160"><template #default="{ row }">{{ fmt(row.paidAt) }}</template></el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" />
      </el-table>
    </div>

    <el-dialog v-model="receiveVisible" title="退货入库" width="560px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="入库仓" required>
          <el-select v-model="recv.warehouseCode" filterable style="width: 100%">
            <el-option v-for="w in options.warehouse" :key="w.value" :label="w.label" :value="w.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-table :data="d.items" border size="small">
        <el-table-column prop="sku" label="SKU" width="140" />
        <el-table-column prop="qty" label="申请" width="70" />
        <el-table-column label="实收数量"><template #default="{ row }"><el-input-number v-model="recv.items[row.sku]" :min="0" size="small" /></template></el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="receiveVisible = false">取消</el-button>
        <el-button type="primary" @click="run(() => aftersale.receive(r.returnNo, recv), '入库').then(() => (receiveVisible = false))">确认入库</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="refundVisible" title="退款" width="400px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="金额"><el-input-number v-model="refund.amount" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="方式">
          <el-select v-model="refund.method" style="width: 100%">
            <el-option label="原路退回" value="ORIGINAL" /><el-option label="余额" value="BALANCE" /><el-option label="线下" value="OFFLINE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" @click="run(() => aftersale.refund(r.returnNo, refund), '退款').then(() => (refundVisible = false))">确认退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aftersale } from '../../api'
import { useOptions } from '../../composables/useOptions'
import StatusTag from '../../components/StatusTag.vue'
import { canWrite } from '../../auth'
import { fmt } from '../../utils'

const route = useRoute()
const { options } = useOptions(['warehouse'])
const d = ref({})
const r = computed(() => d.value.returnOrder || {})
const loading = ref(false)
const receiveVisible = ref(false)
const refundVisible = ref(false)
const recv = reactive({ warehouseCode: '', items: {} })
const refund = reactive({ amount: undefined, method: 'ORIGINAL' })

async function load() {
  loading.value = true
  try {
    d.value = await aftersale.get(route.params.returnNo)
    refund.amount = Number(r.value.refundAmount || 0)
  } finally {
    loading.value = false
  }
}
async function run(fn, name) {
  await fn()
  ElMessage.success(`${name}成功`)
  await load()
}
async function reject() {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回', { inputValidator: (v) => (v && v.trim() ? true : '原因必填') })
  await run(() => aftersale.reject(r.value.returnNo, value), '驳回')
}
function openReceive() {
  recv.warehouseCode = r.value.warehouseCode || ''
  recv.items = Object.fromEntries((d.value.items || []).map((i) => [i.sku, i.qty]))
  receiveVisible.value = true
}
onMounted(load)
</script>

<style scoped>
h4 { margin: 18px 0 8px; }
</style>
