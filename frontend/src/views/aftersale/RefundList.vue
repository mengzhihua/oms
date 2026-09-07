<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="退款单号/售后单号/订单号" clearable @keyup.enter="load" @clear="load" style="width: 260px" />
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column prop="refundNo" label="退款单号" width="170" />
        <el-table-column prop="returnNo" label="售后单" width="170">
          <template #default="{ row }"><el-link type="primary" @click="$router.push(`/aftersale/detail/${row.returnNo}`)">{{ row.returnNo }}</el-link></template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单" width="170">
          <template #default="{ row }"><el-link type="primary" @click="$router.push(`/order/detail/${row.orderNo}`)">{{ row.orderNo }}</el-link></template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="100" align="right" />
        <el-table-column prop="method" label="方式" width="100" />
        <el-table-column prop="status" label="状态" width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column prop="paidAt" label="退款时间" width="160"><template #default="{ row }">{{ fmt(row.paidAt) }}</template></el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="remark" label="备注" min-width="140" />
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { aftersale } from '../../api'
import StatusTag from '../../components/StatusTag.vue'
import { fmt } from '../../utils'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 20, keyword: '' })
async function load() {
  loading.value = true
  try {
    const p = await aftersale.refundPage(query)
    rows.value = p.records
    total.value = p.total
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
