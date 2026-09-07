<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.warehouseCode" placeholder="仓库" clearable filterable @change="load" style="width: 180px">
          <el-option v-for="o in options.warehouse" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-input v-model="query.sku" placeholder="SKU" clearable @keyup.enter="load" @clear="load" style="width: 160px" />
        <el-checkbox v-model="query.lowStock" @change="load">仅看低于安全库存</el-checkbox>
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
        <el-button v-if="canWrite()" type="success" @click="openAdjust()">库存调整</el-button>
        <el-button @click="downloadCsv('/inventory/export', { warehouseCode: query.warehouseCode }, 'inventory.csv')">导出 CSV</el-button>
        <el-button @click="openAvail">渠道可售查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column prop="warehouseCode" label="仓库" width="120" />
        <el-table-column prop="sku" label="SKU" width="140" />
        <el-table-column prop="qtyOnHand" label="在库" width="90" align="right" />
        <el-table-column prop="qtyReserved" label="预占" width="90" align="right" />
        <el-table-column prop="qtyAvailable" label="可用" width="90" align="right">
          <template #default="{ row }"><span :class="{ danger: row.qtyAvailable <= row.safetyQty }">{{ row.qtyAvailable }}</span></template>
        </el-table-column>
        <el-table-column prop="safetyQty" label="安全库存" width="90" align="right" />
        <el-table-column prop="updatedAt" label="更新时间" width="160"><template #default="{ row }">{{ fmt(row.updatedAt) }}</template></el-table-column>
        <el-table-column label="操作" width="100" fixed="right" v-if="canWrite()">
          <template #default="{ row }"><el-button link type="primary" size="small" @click="openAdjust(row)">调整</el-button></template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="adjVisible" title="库存调整" width="460px" destroy-on-close>
      <el-form :model="adj" label-width="100px">
        <el-form-item label="仓库" required>
          <el-select v-model="adj.warehouseCode" filterable style="width: 100%">
            <el-option v-for="o in options.warehouse" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="SKU" required>
          <el-select v-model="adj.sku" filterable style="width: 100%">
            <el-option v-for="o in options.product" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="调整数量"><el-input-number v-model="adj.delta" :min="-99999" style="width: 100%" /><div class="hint">正数增加，负数减少（0 仅修改安全库存）</div></el-form-item>
        <el-form-item label="安全库存"><el-input-number v-model="adj.safetyQty" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="adj.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAdjust">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="availVisible" title="渠道可售库存查询" width="460px" destroy-on-close>
      <el-form :model="avail" label-width="100px">
        <el-form-item label="SKU" required>
          <el-select v-model="avail.sku" filterable style="width: 100%">
            <el-option v-for="o in options.product" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="店铺">
          <el-select v-model="avail.shopCode" filterable clearable style="width: 100%">
            <el-option v-for="o in options.shop" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-descriptions v-if="availResult" :column="2" border size="small">
        <el-descriptions-item label="全网可用">{{ availResult.total }}</el-descriptions-item>
        <el-descriptions-item label="店铺可售">{{ availResult.channel ?? '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="availVisible = false">关闭</el-button>
        <el-button type="primary" @click="queryAvail">查询</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { inventory, downloadCsv } from '../../api'
import { useOptions } from '../../composables/useOptions'
import { canWrite } from '../../auth'
import { fmt } from '../../utils'

const { options } = useOptions(['warehouse', 'product', 'shop'])
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ current: 1, size: 20, warehouseCode: '', sku: '', lowStock: false })

const adjVisible = ref(false)
const adj = reactive({ warehouseCode: '', sku: '', delta: 0, safetyQty: undefined, remark: '' })
const availVisible = ref(false)
const avail = reactive({ sku: '', shopCode: '' })
const availResult = ref(null)

async function load() {
  loading.value = true
  try {
    const p = await inventory.page({ ...query, lowStock: query.lowStock || undefined })
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}

function openAdjust(row) {
  Object.assign(adj, { warehouseCode: row?.warehouseCode || '', sku: row?.sku || '', delta: 0, safetyQty: row?.safetyQty, remark: '' })
  adjVisible.value = true
}

async function saveAdjust() {
  if (!adj.warehouseCode || !adj.sku) return ElMessage.warning('请选择仓库与 SKU')
  saving.value = true
  try {
    await inventory.adjust(adj)
    ElMessage.success('调整成功')
    adjVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openAvail() {
  availResult.value = null
  availVisible.value = true
}
async function queryAvail() {
  if (!avail.sku) return ElMessage.warning('请选择 SKU')
  availResult.value = await inventory.available({ sku: avail.sku, shopCode: avail.shopCode || undefined })
}

onMounted(load)
</script>

<style scoped>
.danger { color: var(--el-color-danger); font-weight: 600; }
.hint { font-size: 12px; color: #909399; }
</style>
