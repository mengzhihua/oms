<template>
  <CrudPage title="承运商" :api="basic.carrier" :columns="columns" :option-sources="options" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
import { statusCol } from '../../composables/useOptions'

const options = {}
const columns = [
  { prop: 'code', label: '编码', required: true, readonlyOnEdit: true, width: 100 },
  { prop: 'name', label: '名称', required: true, minWidth: 140 },
  { prop: 'type', label: '类型', type: 'select', width: 90, default: 'EXPRESS', options: [{ label: '快递', value: 'EXPRESS' }, { label: '快运/零担', value: 'LTL' }, { label: '自营车队', value: 'SELF' }] },
  { prop: 'tmsCode', label: 'TMS 编码', width: 110 },
  { prop: 'contact', label: '联系人', width: 90 },
  { prop: 'phone', label: '电话', width: 130 },
  statusCol
]
</script>
