<template>
  <CrudPage title="渠道" :api="basic.channel" :columns="columns" :option-sources="options" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
import { statusCol } from '../../composables/useOptions'

const options = {}
const columns = [
  { prop: 'code', label: '渠道编码', required: true, readonlyOnEdit: true, width: 120 },
  { prop: 'name', label: '渠道名称', required: true, minWidth: 160 },
  { prop: 'type', label: '类型', type: 'select', width: 110, default: 'PLATFORM', options: [{ label: '电商平台', value: 'PLATFORM' }, { label: '线下/经销', value: 'OFFLINE' }, { label: 'API 直连', value: 'API' }] },
  { prop: 'remark', label: '备注', minWidth: 160 },
  statusCol
]
</script>
