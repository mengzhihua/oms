<template>
  <div class="page" v-loading="loading">
    <div class="card" v-if="d.order">
      <div class="toolbar" style="justify-content: space-between">
        <div style="display: flex; align-items: center; gap: 10px">
          <el-button @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回</el-button>
          <h3 style="margin: 0">订单 {{ o.orderNo }}</h3>
          <StatusTag :value="o.status" />
          <el-tag v-if="o.parentOrderNo" type="info" size="small">父单 <el-link type="primary" @click="go(o.parentOrderNo)">{{ o.parentOrderNo }}</el-link></el-tag>
        </div>
        <div v-if="canWrite()">
          <el-button v-if="o.status === 'CREATED'" type="primary" @click="run(() => order.audit(o.orderNo, '人工审核'), '审核')">审核</el-button>
          <el-button v-if="['CREATED', 'AUDITED'].includes(o.status)" type="warning" @click="reason('挂起原因', (r) => order.hold(o.orderNo, r))">挂起</el-button>
          <el-button v-if="o.status === 'HOLD'" type="primary" @click="run(() => order.unhold(o.orderNo), '解挂')">解挂</el-button>
          <el-button v-if="o.status === 'AUDITED'" type="primary" @click="run(() => order.allocate(o.orderNo), '分仓预占')">分仓预占</el-button>
          <el-button v-if="o.status === 'ALLOCATED'" @click="splitVisible = true">手工拆单</el-button>
          <el-button v-if="o.status === 'ALLOCATED'" type="primary" @click="run(() => order.push(o.orderNo), '推送 WMS')">推送 WMS</el-button>
          <el-button v-if="['CREATED', 'AUDITED'].includes(o.status)" type="success" @click="run(() => order.auto(o.orderNo), '一键处理')">一键处理</el-button>
          <el-button v-if="['ALLOCATED', 'PUSHED'].includes(o.status)" type="success" @click="shipVisible = true">登记发货</el-button>
          <el-button v-if="o.status === 'SHIPPED'" type="success" @click="run(() => order.complete(o.orderNo), '确认完成')">确认完成</el-button>
          <el-button v-if="['SHIPPED', 'COMPLETED'].includes(o.status)" @click="$router.push({ path: '/aftersale/list', query: { create: o.orderNo } })">创建售后</el-button>
          <el-button v-if="['CREATED', 'AUDITED', 'HOLD', 'ALLOCATED', 'PUSHED'].includes(o.status)" type="danger" @click="reason('取消原因', (r) => order.cancel(o.orderNo, r))">取消</el-button>
          <el-button @click="openRemark">备注/优先级</el-button>
        </div>
      </div>

      <el-descriptions :column="4" border size="small" style="margin-top: 12px">
        <el-descriptions-item label="渠道/店铺">{{ o.channelCode }} / {{ o.shopCode }}</el-descriptions-item>
        <el-descriptions-item label="渠道单号">{{ o.channelOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ o.source }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ o.customerCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收件人">{{ o.receiverName }} {{ o.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="3">{{ o.province }}{{ o.city }}{{ o.district }} {{ o.address }}</el-descriptions-item>
        <el-descriptions-item label="商品金额">{{ o.goodsAmount }}</el-descriptions-item>
        <el-descriptions-item label="运费/优惠">{{ o.freight }} / {{ o.discount }}</el-descriptions-item>
        <el-descriptions-item label="实付">{{ o.payAmount }}</el-descriptions-item>
        <el-descriptions-item label="支付状态"><StatusTag :value="o.payStatus" /></el-descriptions-item>
        <el-descriptions-item label="发货仓">{{ o.warehouseCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="承运商/运单">{{ o.carrierCode || '-' }} / {{ o.trackingNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="WMS 单号">{{ o.wmsOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="TMS 单号">{{ o.tmsOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ fmt(o.orderTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核">{{ fmt(o.auditedAt) }} {{ o.auditedBy }}</el-descriptions-item>
        <el-descriptions-item label="推送">{{ fmt(o.pushedAt) }}</el-descriptions-item>
        <el-descriptions-item label="发货/签收">{{ fmt(o.shippedAt) }} / {{ fmt(o.signedAt) }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ o.priority }}</el-descriptions-item>
        <el-descriptions-item label="买家留言">{{ o.buyerRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="卖家备注">{{ o.sellerRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="挂起/取消原因">{{ o.holdReason || o.cancelReason || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4>商品明细</h4>
      <el-table :data="d.items" border size="small">
        <el-table-column prop="sku" label="SKU" width="140" />
        <el-table-column prop="productName" label="商品名称" min-width="180" />
        <el-table-column prop="bundleSku" label="所属组合" width="130" />
        <el-table-column prop="qty" label="数量" width="80" align="right" />
        <el-table-column prop="price" label="单价" width="90" align="right" />
        <el-table-column prop="amount" label="金额" width="100" align="right" />
        <el-table-column prop="reservedQty" label="已预占" width="80" align="right" />
        <el-table-column prop="shippedQty" label="已发货" width="80" align="right" />
      </el-table>

      <template v-if="d.children?.length">
        <h4>拆分子单</h4>
        <el-table :data="d.children" border size="small">
          <el-table-column prop="orderNo" label="子单号" width="170"><template #default="{ row }"><el-link type="primary" @click="go(row.orderNo)">{{ row.orderNo }}</el-link></template></el-table-column>
          <el-table-column prop="status" label="状态" width="100"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
          <el-table-column prop="warehouseCode" label="发货仓" width="120" />
          <el-table-column prop="carrierCode" label="承运商" width="100" />
          <el-table-column prop="trackingNo" label="运单号" width="160" />
          <el-table-column prop="payAmount" label="金额" width="100" align="right" />
        </el-table>
      </template>

      <h4>操作日志</h4>
      <el-timeline>
        <el-timeline-item v-for="l in d.logs" :key="l.id" :timestamp="fmt(l.createdAt)" placement="top">
          <b>{{ l.action }}</b>
          <span v-if="l.fromStatus || l.toStatus" style="margin-left: 8px; color: #909399">{{ l.fromStatus }} → {{ l.toStatus }}</span>
          <span style="margin-left: 8px">{{ l.operator }}</span>
          <div style="color: #606266">{{ l.remark }}</div>
        </el-timeline-item>
      </el-timeline>
    </div>

    <el-dialog v-model="shipVisible" title="人工登记发货" width="420px" destroy-on-close>
      <el-form :model="ship" label-width="90px">
        <el-form-item label="承运商">
          <el-select v-model="ship.carrierCode" filterable style="width: 100%">
            <el-option v-for="c in options.carrier" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="运单号" required><el-input v-model="ship.trackingNo" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" @click="run(() => order.ship(o.orderNo, ship), '发货').then(() => (shipVisible = false))">确认发货</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="splitVisible" title="手工拆单（填写拆出到新子单的数量）" width="640px" destroy-on-close>
      <el-table :data="d.items" border size="small">
        <el-table-column prop="sku" label="SKU" width="140" />
        <el-table-column prop="productName" label="商品" min-width="160" />
        <el-table-column prop="qty" label="数量" width="80" />
        <el-table-column label="拆出数量" width="160">
          <template #default="{ row }"><el-input-number v-model="splitQty[row.id]" :min="0" :max="row.qty" size="small" /></template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="splitVisible = false">取消</el-button>
        <el-button type="primary" @click="doSplit">确认拆单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="remarkVisible" title="卖家备注 / 优先级" width="420px" destroy-on-close>
      <el-form :model="remarkForm" label-width="90px">
        <el-form-item label="卖家备注"><el-input v-model="remarkForm.remark" type="textarea" /></el-form-item>
        <el-form-item label="优先级"><el-input-number v-model="remarkForm.priority" :min="0" :max="9" /><span class="hint">越大越优先</span></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="remarkVisible = false">取消</el-button>
        <el-button type="primary" @click="run(() => order.remark(o.orderNo, remarkForm), '保存').then(() => (remarkVisible = false))">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { order } from '../../api'
import { useOptions } from '../../composables/useOptions'
import StatusTag from '../../components/StatusTag.vue'
import { canWrite } from '../../auth'
import { fmt } from '../../utils'

const route = useRoute()
const router = useRouter()
const { options } = useOptions(['carrier'])
const d = ref({})
const o = computed(() => d.value.order || {})
const loading = ref(false)
const shipVisible = ref(false)
const splitVisible = ref(false)
const remarkVisible = ref(false)
const ship = reactive({ carrierCode: '', trackingNo: '' })
const splitQty = reactive({})
const remarkForm = reactive({ remark: '', priority: 0 })

async function load() {
  loading.value = true
  try {
    d.value = await order.get(route.params.orderNo)
    ship.carrierCode = o.value.carrierCode || ''
  } finally {
    loading.value = false
  }
}
function go(no) { router.push(`/order/detail/${no}`) }

async function run(fn, name) {
  await fn()
  ElMessage.success(`${name}成功`)
  await load()
}
async function reason(title, fn) {
  const { value } = await ElMessageBox.prompt('请输入原因', title, { inputValidator: (v) => (v && v.trim() ? true : '原因必填') })
  await run(() => fn(value), title)
}
async function doSplit() {
  const items = Object.fromEntries(Object.entries(splitQty).filter(([, q]) => q > 0))
  if (!Object.keys(items).length) return ElMessage.warning('请填写拆出数量')
  await run(() => order.split(o.value.orderNo, items), '拆单')
  splitVisible.value = false
}
function openRemark() {
  Object.assign(remarkForm, { remark: o.value.sellerRemark || '', priority: o.value.priority || 0 })
  remarkVisible.value = true
}

watch(() => route.params.orderNo, load)
onMounted(load)
</script>

<style scoped>
h4 { margin: 18px 0 8px; }
.hint { margin-left: 8px; font-size: 12px; color: #909399; }
</style>
