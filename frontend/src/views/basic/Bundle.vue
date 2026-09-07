<template>
  <CrudPage title="组合明细" :api="basic.bundleItem" :columns="columns" :option-sources="options" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
import { useOptions } from '../../composables/useOptions'
import { computed } from 'vue'
const { options: raw } = useOptions(['product'])
const options = computed(() => ({ product: raw.value.product || [], bundle: (raw.value.product || []).filter((p) => p.type === 'BUNDLE') }))
const columns = [
  { prop: 'bundleSku', label: '组合SKU', type: 'select', options: 'bundle', required: true, filter: true, width: 160 },
  { prop: 'sku', label: '子SKU', type: 'select', options: 'product', required: true, minWidth: 200 },
  { prop: 'qty', label: '数量', type: 'number', min: 1, default: 1, width: 90 }
]
</script>
