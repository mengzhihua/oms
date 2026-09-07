<template>
  <CrudPage title="商品" :api="basic.product" :columns="columns" :option-sources="options" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
import { statusCol } from '../../composables/useOptions'

const options = {}
const columns = [
  { prop: 'sku', label: 'SKU', required: true, readonlyOnEdit: true, width: 120 },
  { prop: 'name', label: '商品名称', required: true, minWidth: 180 },
  { prop: 'type', label: '类型', type: 'select', width: 90, default: 'NORMAL', filter: true, options: [{ label: '普通', value: 'NORMAL' }, { label: '组合', value: 'BUNDLE' }] },
  { prop: 'category', label: '分类', width: 100 },
  { prop: 'spec', label: '规格', width: 120 },
  { prop: 'unit', label: '单位', width: 70, default: '个' },
  { prop: 'barcode', label: '条码', width: 140 },
  { prop: 'price', label: '售价', type: 'number', precision: 2, width: 90 },
  { prop: 'weightKg', label: '重量(kg)', type: 'number', precision: 3, width: 90 },
  statusCol
]
</script>
