<template>
  <CrudPage title="店铺" :api="basic.shop" :columns="columns" :option-sources="options" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
import { useOptions, statusCol } from '../../composables/useOptions'

const { options } = useOptions(['channel', 'warehouse'])
const columns = [
  { prop: 'code', label: '店铺编码', required: true, readonlyOnEdit: true, width: 120 },
  { prop: 'name', label: '店铺名称', required: true, minWidth: 160 },
  { prop: 'channelCode', label: '所属渠道', type: 'select', options: 'channel', required: true, filter: true, width: 130 },
  { prop: 'defaultWarehouseCode', label: '默认发货仓', type: 'select', options: 'warehouse', width: 140 },
  { prop: 'autoAudit', label: '自动审核', type: 'select', width: 90, default: 0, options: [{ label: '是', value: 1 }, { label: '否', value: 0 }] },
  { prop: 'contact', label: '联系人', width: 100 },
  { prop: 'phone', label: '电话', width: 130 },
  statusCol
]
</script>
