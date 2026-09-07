<template>
  <el-tag :type="meta.type" size="small" effect="light">{{ meta.text }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

const DICT = {
  CREATED: ['待审核', 'info'],
  HOLD: ['挂起', 'danger'],
  AUDITED: ['已审核', 'primary'],
  ALLOCATED: ['已分仓', 'primary'],
  PUSHED: ['已推WMS', 'warning'],
  SHIPPED: ['已发货', 'success'],
  COMPLETED: ['已完成', 'success'],
  CANCELLED: ['已取消', 'danger'],
  SPLIT: ['已拆单', 'info'],
  RECEIVED: ['已收货', 'primary'],
  REFUNDED: ['已退款', 'success'],
  REJECTED: ['已驳回', 'danger'],
  PAID: ['已支付', 'success'],
  UNPAID: ['未支付', 'warning'],
  RETURN: ['退货退款', 'warning'],
  REFUND_ONLY: ['仅退款', 'info'],
  EXCHANGE: ['换货', 'primary'],
  SYNC: ['同步', 'info'],
  ADJUST: ['调整', 'warning'],
  RESERVE: ['预占', 'primary'],
  RELEASE: ['释放', 'info'],
  DEDUCT: ['发货扣减', 'danger'],
  MANUAL: ['手工', 'info'],
  IMPORT: ['导入', 'primary'],
  API: ['接口', 'success'],
  IN: ['入站', 'success'],
  OUT: ['出站', 'primary'],
  WMS: ['WMS', 'primary'],
  TMS: ['TMS', 'warning'],
  CHANNEL: ['渠道', 'success'],
  NORMAL: ['普通', 'info'],
  BUNDLE: ['组合', 'warning']
}

const props = defineProps({ value: String })
const meta = computed(() => {
  const d = DICT[props.value]
  return d ? { text: d[0], type: d[1] } : { text: props.value, type: 'info' }
})
</script>
