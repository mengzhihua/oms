<template>
  <CrudPage title="渠道库存策略" :api="inventory.policy" :columns="columns" :option-sources="options">
    <template #toolbar>
      <el-alert type="info" :closable="false" show-icon style="flex: 1 1 100%">
        店铺可售 = 匹配仓库可用量(在库-预占-安全库存) × 比例 或 固定值；SKU 级策略优先于店铺级（SKU 留空）。
      </el-alert>
    </template>
  </CrudPage>
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { inventory } from '../../api'
import { useOptions, statusCol } from '../../composables/useOptions'

const { options } = useOptions(['shop', 'product', 'warehouse'])
const columns = [
  { prop: 'shopCode', label: '店铺', type: 'select', options: 'shop', required: true, filter: true, width: 150 },
  { prop: 'sku', label: 'SKU(空=店铺级)', type: 'select', options: 'product', width: 160 },
  { prop: 'warehouseCode', label: '限定仓库', type: 'select', options: 'warehouse', width: 130 },
  { prop: 'mode', label: '模式', type: 'select', width: 90, default: 'RATIO', options: [{ label: '比例', value: 'RATIO' }, { label: '固定值', value: 'FIXED' }] },
  { prop: 'ratio', label: '比例(%)', type: 'number', width: 90, default: 100 },
  { prop: 'fixedQty', label: '固定数量', type: 'number', width: 90, default: 0 },
  statusCol
]
</script>
