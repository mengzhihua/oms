import { ref } from 'vue'
import { basic } from '../api'

/** Loads master-data drop-down options once per page. */
export function useOptions(kinds) {
  const options = ref({})
  const loaders = {
    channel: async () => (await basic.channel.list()).map((c) => ({ label: `${c.code} ${c.name}`, value: c.code })),
    shop: async () => (await basic.shop.list()).map((s) => ({ label: `${s.code} ${s.name}`, value: s.code, channelCode: s.channelCode })),
    customer: async () => (await basic.customer.list()).map((c) => ({ label: `${c.code} ${c.name}`, value: c.code })),
    product: async () => (await basic.product.list()).map((p) => ({ label: `${p.sku} ${p.name}`, value: p.sku, price: p.price, type: p.type })),
    warehouse: async () => (await basic.warehouse.list()).map((w) => ({ label: `${w.code} ${w.name}`, value: w.code })),
    carrier: async () => (await basic.carrier.list()).map((c) => ({ label: `${c.code} ${c.name}`, value: c.code }))
  }
  async function reload() {
    const out = {}
    await Promise.all(kinds.map(async (k) => { out[k] = await loaders[k]() }))
    options.value = out
  }
  reload()
  return { options, reload }
}

export const statusCol = { prop: 'status', label: '状态', type: 'status', width: 80, default: 1 }
