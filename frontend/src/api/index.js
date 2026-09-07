import http from './request'

export const crud = (base) => ({
  page: (params) => http.get(`${base}/page`, { params }),
  list: (params) => http.get(`${base}/list`, { params }),
  get: (id) => http.get(`${base}/${id}`),
  create: (data) => http.post(base, data),
  update: (id, data) => http.put(`${base}/${id}`, data),
  remove: (id) => http.delete(`${base}/${id}`)
})

export const basic = {
  channel: crud('/basic/channel'),
  shop: crud('/basic/shop'),
  customer: crud('/basic/customer'),
  product: crud('/basic/product'),
  bundleItem: crud('/basic/bundle-item'),
  warehouse: crud('/basic/warehouse'),
  carrier: crud('/basic/carrier'),
  routingRule: crud('/basic/routing-rule')
}

export const inventory = {
  page: (params) => http.get('/inventory/page', { params }),
  available: (params) => http.get('/inventory/available', { params }),
  adjust: (data) => http.post('/inventory/adjust', data),
  txnPage: (params) => http.get('/inventory/txn/page', { params }),
  policy: crud('/inventory/policy')
}

export const order = {
  page: (params) => http.get('/order/page', { params }),
  statusCount: () => http.get('/order/status-count'),
  get: (orderNo) => http.get(`/order/${orderNo}`),
  create: (data) => http.post('/order', data),
  audit: (orderNo, remark) => http.post(`/order/${orderNo}/audit`, { remark }),
  hold: (orderNo, reason) => http.post(`/order/${orderNo}/hold`, { reason }),
  unhold: (orderNo) => http.post(`/order/${orderNo}/unhold`),
  allocate: (orderNo) => http.post(`/order/${orderNo}/allocate`),
  push: (orderNo) => http.post(`/order/${orderNo}/push`),
  auto: (orderNo) => http.post(`/order/${orderNo}/auto`),
  ship: (orderNo, data) => http.post(`/order/${orderNo}/ship`, data),
  complete: (orderNo, remark) => http.post(`/order/${orderNo}/complete`, { remark }),
  cancel: (orderNo, reason) => http.post(`/order/${orderNo}/cancel`, { reason }),
  remark: (orderNo, data) => http.post(`/order/${orderNo}/remark`, data),
  split: (orderNo, items) => http.post(`/order/${orderNo}/split`, { items }),
  batch: (data) => http.post('/order/batch', data),
  importCsv: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/order/import', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  }
}

export const aftersale = {
  page: (params) => http.get('/aftersale/page', { params }),
  get: (returnNo) => http.get(`/aftersale/${returnNo}`),
  create: (data) => http.post('/aftersale', data),
  audit: (returnNo) => http.post(`/aftersale/${returnNo}/audit`),
  reject: (returnNo, reason) => http.post(`/aftersale/${returnNo}/reject`, { reason }),
  cancel: (returnNo) => http.post(`/aftersale/${returnNo}/cancel`),
  receive: (returnNo, data) => http.post(`/aftersale/${returnNo}/receive`, data || {}),
  refund: (returnNo, data) => http.post(`/aftersale/${returnNo}/refund`, data || {}),
  refundPage: (params) => http.get('/aftersale/refund/page', { params })
}

export const integration = {
  logPage: (params) => http.get('/integration/log/page', { params })
}

export const report = {
  dashboard: () => http.get('/dashboard'),
  orderDaily: (days) => http.get('/report/order-daily', { params: { days } }),
  channelSales: (params) => http.get('/report/channel-sales', { params }),
  skuSales: (days) => http.get('/report/sku-sales', { params: { days } }),
  warehouseShip: (days) => http.get('/report/warehouse-ship', { params: { days } }),
  afterSale: (days) => http.get('/report/aftersale', { params: { days } }),
  leadTime: (days) => http.get('/report/lead-time', { params: { days } })
}

export const system = {
  user: crud('/system/user'),
  oplogPage: (params) => http.get('/system/oplog/page', { params })
}

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  me: () => http.get('/auth/me'),
  logout: () => http.post('/auth/logout'),
  changePassword: (data) => http.post('/auth/password', data)
}

/** 带 token 下载后端 CSV（使用 blob，避开 URL 中传 token） */
export async function downloadCsv(url, params, filename) {
  const blob = await http.get(url, { params, responseType: 'blob' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = filename
  a.click()
  URL.revokeObjectURL(a.href)
}
