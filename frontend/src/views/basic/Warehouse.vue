<template>
  <CrudPage title="仓库" :api="basic.warehouse" :columns="columns" :option-sources="options" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
import { statusCol } from '../../composables/useOptions'

const options = {}
const columns = [
  { prop: 'code', label: '仓库编码', required: true, readonlyOnEdit: true, width: 110 },
  { prop: 'name', label: '仓库名称', required: true, minWidth: 140 },
  { prop: 'type', label: '类型', type: 'select', width: 90, default: 'OWN', options: [{ label: '自营', value: 'OWN' }, { label: '三方(3PL)', value: '3PL' }, { label: '门店', value: 'STORE' }] },
  { prop: 'wmsCode', label: 'WMS 仓编码', width: 110 },
  { prop: 'province', label: '省', width: 90 },
  { prop: 'city', label: '市', width: 90 },
  { prop: 'address', label: '地址', hideInTable: true, span: 24 },
  { prop: 'contact', label: '联系人', width: 90 },
  { prop: 'phone', label: '电话', width: 130 },
  { prop: 'priority', label: '优先级', type: 'number', width: 80, default: 100 },
  statusCol
]
</script>
