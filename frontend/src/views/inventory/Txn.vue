<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.warehouseCode" placeholder="仓库" clearable filterable @change="load" style="width: 170px">
          <el-option v-for="o in options.warehouse" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-input v-model="query.sku" placeholder="SKU" clearable @keyup.enter="load" @clear="load" style="width: 150px" />
        <el-select v-model="query.type" placeholder="类型" clearable @change="load" style="width: 130px">
          <el-option v-for="t in TYPES" :key="t" :label="t" :value="t" />
        </el-select>
        <el-input v-model="query.refNo" placeholder="关联单号" clearable @keyup.enter="load" @clear="load" style="width: 170px" />
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column prop="createdAt" label="时间" width="160"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
        <el-table-column prop="warehouseCode" label="仓库" width="110" />
        <el-table-column prop="sku" label="SKU" width="130" />
        <el-table-column prop="type" label="类型" width="100"><template #default="{ row }"><StatusTag :value="row.type" /></template></el-table-column>
        <el-table-column prop="qty" label="数量" width="80" align="right" />
        <el-table-column prop="onHandAfter" label="在库(后)" width="90" align="right" />
        <el-table-column prop="reservedAfter" label="预占(后)" width="90" align="right" />
        <el-table-column prop="refNo" label="关联单号" width="180" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="remark" label="备注" min-width="160" />
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { inventory } from '../../api'
import { useOptions } from '../../composables/useOptions'
import StatusTag from '../../components/StatusTag.vue'
import { fmt } from '../../utils'

const TYPES = ['SYNC', 'ADJUST', 'RESERVE', 'RELEASE', 'DEDUCT', 'RETURN']
const { options } = useOptions(['warehouse'])
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 20, warehouseCode: '', sku: '', type: '', refNo: '' })

async function load() {
  loading.value = true
  try {
    const p = await inventory.txnPage(query)
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
