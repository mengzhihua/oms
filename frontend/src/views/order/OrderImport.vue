<template>
  <div class="page">
    <div class="card">
      <h3 style="margin: 0 0 12px">订单批量导入（CSV）</h3>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        <p>列：shopCode, channelOrderNo, receiverName, receiverPhone, province, city, district, address, sku, qty, price, buyerRemark</p>
        <p>同一 店铺+渠道单号 的多行合并为一个订单的多条明细；渠道单号重复的订单会被跳过（幂等）。</p>
      </el-alert>
      <div class="toolbar">
        <el-button @click="downloadCsv('/order/import/template', {}, 'order_import_template.csv')">下载模板</el-button>
        <el-upload v-if="canWrite()" :show-file-list="false" :auto-upload="false" accept=".csv" :on-change="onImport">
          <el-button type="primary" :loading="importing">选择 CSV 导入</el-button>
        </el-upload>
      </div>
      <template v-if="result">
        <el-divider content-position="left">导入结果</el-divider>
        <p>成功创建 <b>{{ result.created.length }}</b> 单，失败 <b>{{ failedRows.length }}</b> 单</p>
        <el-table v-if="result.created.length" :data="result.created.map((n) => ({ n }))" size="small" border style="margin-bottom: 12px; max-width: 400px">
          <el-table-column label="订单号"><template #default="{ row }"><el-link type="primary" @click="$router.push(`/order/detail/${row.n}`)">{{ row.n }}</el-link></template></el-table-column>
        </el-table>
        <el-table v-if="failedRows.length" :data="failedRows" size="small" border>
          <el-table-column prop="key" label="店铺|渠道单号" width="240" />
          <el-table-column prop="error" label="错误" />
        </el-table>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { order, downloadCsv } from '../../api'
import { canWrite } from '../../auth'

const importing = ref(false)
const result = ref(null)
const failedRows = computed(() => Object.entries(result.value?.failed || {}).map(([key, error]) => ({ key, error })))

async function onImport(file) {
  importing.value = true
  try {
    result.value = await order.importCsv(file.raw)
    ElMessage.success('导入完成')
  } finally {
    importing.value = false
  }
}
</script>
