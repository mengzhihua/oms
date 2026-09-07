import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'
import { auth, isAdmin } from '../auth'

export const menus = [
  { path: '/dashboard', name: '工作台', icon: 'Odometer', component: () => import('../views/Dashboard.vue') },
  {
    path: '/order', name: '订单中心', icon: 'Document',
    children: [
      { path: 'list', name: '销售订单', component: () => import('../views/order/OrderList.vue') },
      { path: 'create', name: '手工下单', component: () => import('../views/order/OrderCreate.vue') },
      { path: 'import', name: '订单导入', component: () => import('../views/order/OrderImport.vue') }
    ]
  },
  {
    path: '/aftersale', name: '售后管理', icon: 'RefreshLeft',
    children: [
      { path: 'list', name: '售后单', component: () => import('../views/aftersale/ReturnList.vue') },
      { path: 'refund', name: '退款记录', component: () => import('../views/aftersale/RefundList.vue') }
    ]
  },
  {
    path: '/inventory', name: '库存中心', icon: 'Box',
    children: [
      { path: 'stock', name: '多仓库存', component: () => import('../views/inventory/Stock.vue') },
      { path: 'policy', name: '渠道库存策略', component: () => import('../views/inventory/Policy.vue') },
      { path: 'txn', name: '库存流水', component: () => import('../views/inventory/Txn.vue') }
    ]
  },
  {
    path: '/basic', name: '基础数据', icon: 'Setting',
    children: [
      { path: 'channel', name: '销售渠道', component: () => import('../views/basic/Channel.vue') },
      { path: 'shop', name: '店铺', component: () => import('../views/basic/Shop.vue') },
      { path: 'customer', name: '客户', component: () => import('../views/basic/Customer.vue') },
      { path: 'product', name: '商品', component: () => import('../views/basic/Product.vue') },
      { path: 'bundle', name: '组合商品(BOM)', component: () => import('../views/basic/Bundle.vue') },
      { path: 'warehouse', name: '仓库', component: () => import('../views/basic/Warehouse.vue') },
      { path: 'carrier', name: '承运商', component: () => import('../views/basic/Carrier.vue') },
      { path: 'routing', name: '分仓路由规则', component: () => import('../views/basic/RoutingRule.vue') }
    ]
  },
  {
    path: '/integration', name: '系统集成', icon: 'Connection',
    children: [
      { path: 'log', name: '接口日志', component: () => import('../views/integration/IntegrationLog.vue') },
      { path: 'openapi', name: '开放接口说明', component: () => import('../views/integration/OpenApiDoc.vue') }
    ]
  },
  { path: '/report', name: '报表分析', icon: 'DataAnalysis', component: () => import('../views/report/Report.vue') },
  {
    path: '/system', name: '系统管理', icon: 'Tools', adminOnly: true,
    children: [
      { path: 'user', name: '用户管理', component: () => import('../views/system/User.vue') },
      { path: 'oplog', name: '操作日志', component: () => import('../views/system/OpLog.vue') }
    ]
  }
]

/** 当前用户可见菜单（adminOnly 菜单仅管理员可见；后端同样做了鉴权） */
export const visibleMenus = () => menus.filter((m) => !m.adminOnly || isAdmin())

const routes = [
  { path: '/login', name: '登录', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      ...menus.flatMap((m) =>
        m.children
          ? m.children.map((c) => ({ path: `${m.path}/${c.path}`, name: c.name, component: c.component }))
          : [{ path: m.path, name: m.name, component: m.component }]
      ),
      { path: '/order/detail/:orderNo', name: '订单详情', component: () => import('../views/order/OrderDetail.vue') },
      { path: '/aftersale/detail/:returnNo', name: '售后详情', component: () => import('../views/aftersale/ReturnDetail.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (to.path === '/login') return auth.token ? '/dashboard' : true
  if (!auth.token) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.path.startsWith('/system') && !isAdmin()) return '/dashboard'
  return true
})

export default router
