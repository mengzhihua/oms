<template>
  <CrudPage title="客户" :api="basic.customer" :columns="columns" :option-sources="options" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
import { statusCol } from '../../composables/useOptions'

const options = {}
const columns = [
  { prop: 'code', label: '客户编码', required: true, readonlyOnEdit: true, width: 110 },
  { prop: 'name', label: '客户名称', required: true, minWidth: 140 },
  { prop: 'type', label: '类型', type: 'select', width: 90, default: 'PERSON', options: [{ label: '个人', value: 'PERSON' }, { label: '企业', value: 'COMPANY' }] },
  { prop: 'level', label: '等级', type: 'select', width: 90, default: 'NORMAL', options: [{ label: '普通', value: 'NORMAL' }, { label: 'VIP', value: 'VIP' }, { label: 'KA', value: 'KA' }] },
  { prop: 'phone', label: '电话', width: 130 },
  { prop: 'email', label: '邮箱', hideInTable: true },
  { prop: 'province', label: '省', width: 90 },
  { prop: 'city', label: '市', width: 90 },
  { prop: 'address', label: '地址', minWidth: 180, span: 24 },
  statusCol
]
</script>
