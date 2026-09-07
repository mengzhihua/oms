<template>
  <CrudPage title="路由规则" :api="basic.routingRule" :columns="columns" :option-sources="options" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
import { useOptions, statusCol } from '../../composables/useOptions'

const { options } = useOptions(['channel', 'shop', 'product', 'warehouse', 'carrier'])
const columns = [
  { prop: 'name', label: '规则名称', required: true, minWidth: 140 },
  { prop: 'priority', label: '优先级(小优先)', type: 'number', width: 120, default: 100 },
  { prop: 'channelCode', label: '渠道', type: 'select', options: 'channel', width: 110 },
  { prop: 'shopCode', label: '店铺', type: 'select', options: 'shop', width: 130 },
  { prop: 'province', label: '省份(模糊)', width: 100 },
  { prop: 'sku', label: 'SKU', type: 'select', options: 'product', width: 140 },
  { prop: 'warehouseCode', label: '发货仓', type: 'select', options: 'warehouse', required: true, filter: true, width: 130 },
  { prop: 'carrierCode', label: '承运商', type: 'select', options: 'carrier', width: 110 },
  statusCol
]
</script>
