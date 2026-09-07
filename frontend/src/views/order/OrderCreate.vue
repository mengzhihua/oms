<template>
  <div class="page">
    <div class="card">
      <h3 style="margin: 0 0 12px">手工下单</h3>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="店铺" prop="shopCode">
              <el-select v-model="form.shopCode" filterable style="width: 100%">
                <el-option v-for="s in options.shop" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="渠道单号" prop="channelOrderNo"><el-input v-model="form.channelOrderNo" placeholder="留空自动生成" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="客户">
              <el-select v-model="form.customerCode" filterable clearable style="width: 100%">
                <el-option v-for="c in options.customer" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="收件人" prop="receiverName"><el-input v-model="form.receiverName" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="电话" prop="receiverPhone"><el-input v-model="form.receiverPhone" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="支付状态">
              <el-select v-model="form.payStatus" style="width: 100%">
                <el-option label="已支付" value="PAID" /><el-option label="未支付" value="UNPAID" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6"><el-form-item label="省" prop="province"><el-input v-model="form.province" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="市"><el-input v-model="form.city" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="区"><el-input v-model="form.district" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="优先级"><el-input-number v-model="form.priority" :min="0" :max="9" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="详细地址" prop="address"><el-input v-model="form.address" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="运费"><el-input-number v-model="form.freight" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="优惠"><el-input-number v-model="form.discount" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="买家留言"><el-input v-model="form.buyerRemark" /></el-form-item></el-col>
        </el-row>

        <el-divider content-position="left">商品明细</el-divider>
        <el-table :data="form.items" border size="small">
          <el-table-column label="SKU" min-width="240">
            <template #default="{ row }">
              <el-select v-model="row.sku" filterable style="width: 100%" @change="(v) => onSku(row, v)">
                <el-option v-for="p in options.product" :key="p.value" :label="p.label" :value="p.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="150"><template #default="{ row }"><el-input-number v-model="row.qty" :min="1" size="small" /></template></el-table-column>
          <el-table-column label="单价" width="170"><template #default="{ row }"><el-input-number v-model="row.price" :min="0" :precision="2" size="small" /></template></el-table-column>
          <el-table-column label="小计" width="100" align="right"><template #default="{ row }">{{ ((row.qty || 0) * (row.price || 0)).toFixed(2) }}</template></el-table-column>
          <el-table-column label="可用" width="90" align="right"><template #default="{ row }">{{ row.avail ?? '-' }}</template></el-table-column>
          <el-table-column width="70"><template #default="{ $index }"><el-button link type="danger" size="small" @click="form.items.splice($index, 1)">删除</el-button></template></el-table-column>
        </el-table>
        <div style="margin: 10px 0; display: flex; justify-content: space-between; align-items: center">
          <el-button @click="form.items.push({ sku: '', qty: 1, price: 0 })"><el-icon><Plus /></el-icon>添加商品</el-button>
          <div>商品金额 <b>{{ goodsAmount.toFixed(2) }}</b>，应付 <b>{{ payAmount.toFixed(2) }}</b></div>
        </div>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit(false)">保存订单</el-button>
          <el-button type="success" :loading="saving" @click="submit(true)">保存并一键处理</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { order, inventory } from '../../api'
import { useOptions } from '../../composables/useOptions'

const router = useRouter()
const { options } = useOptions(['shop', 'customer', 'product'])
const formRef = ref()
const saving = ref(false)
const form = reactive({
  shopCode: '', channelOrderNo: '', customerCode: '', receiverName: '', receiverPhone: '', province: '', city: '', district: '', address: '',
  payStatus: 'PAID', priority: 0, freight: 0, discount: 0, buyerRemark: '', source: 'MANUAL', items: [{ sku: '', qty: 1, price: 0 }]
})
const rules = {
  shopCode: [{ required: true, message: '请选择店铺', trigger: 'change' }],
  receiverName: [{ required: true, message: '必填', trigger: 'blur' }],
  receiverPhone: [{ required: true, message: '必填', trigger: 'blur' }],
  province: [{ required: true, message: '必填', trigger: 'blur' }],
  address: [{ required: true, message: '必填', trigger: 'blur' }]
}
const goodsAmount = computed(() => form.items.reduce((a, i) => a + (i.qty || 0) * (i.price || 0), 0))
const payAmount = computed(() => Math.max(0, goodsAmount.value + (form.freight || 0) - (form.discount || 0)))

async function onSku(row, sku) {
  const p = options.value.product.find((x) => x.value === sku)
  if (p) row.price = Number(p.price || 0)
  const a = await inventory.available({ sku, shopCode: form.shopCode || undefined })
  row.avail = a.channel ?? a.total
}

async function submit(auto) {
  await formRef.value.validate()
  const items = form.items.filter((i) => i.sku)
  if (!items.length) return ElMessage.warning('请添加商品')
  saving.value = true
  try {
    const o = await order.create({ ...form, items: items.map(({ sku, qty, price }) => ({ sku, qty, price })), payAmount: payAmount.value })
    if (auto) await order.auto(o.orderNo)
    ElMessage.success(`订单 ${o.orderNo} 已创建`)
    router.push(`/order/detail/${o.orderNo}`)
  } finally {
    saving.value = false
  }
}
</script>
