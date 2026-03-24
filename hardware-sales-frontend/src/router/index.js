import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const Layout = () => import('@/layout/index.vue')

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' },
      },
    ],
  },
  {
    path: '/product',
    component: Layout,
    meta: { title: '商品管理', icon: 'Goods' },
    children: [
      {
        path: 'category',
        name: 'Category',
        component: () => import('@/views/category/index.vue'),
        meta: { title: '商品分类' },
      },
      {
        path: 'list',
        name: 'Product',
        component: () => import('@/views/product/index.vue'),
        meta: { title: '商品列表' },
      },
    ],
  },
  {
    path: '/supplier',
    component: Layout,
    meta: { title: '供应商管理', icon: 'UserFilled' },
    children: [
      {
        path: 'list',
        name: 'Supplier',
        component: () => import('@/views/supplier/index.vue'),
        meta: { title: '供应商列表' },
      },
      {
        path: 'product',
        name: 'SupplierProduct',
        component: () => import('@/views/supplier-product/index.vue'),
        meta: { title: '供应商商品' },
      },
      {
        path: 'message',
        name: 'Message',
        component: () => import('@/views/message/index.vue'),
        meta: { title: '站内消息' },
      },
    ],
  },
  {
    path: '/trade',
    component: Layout,
    meta: { title: '进销存', icon: 'ShoppingCart' },
    children: [
      {
        path: 'purchase',
        name: 'Purchase',
        component: () => import('@/views/purchase/index.vue'),
        meta: { title: '采购管理' },
      },
      {
        path: 'sales',
        name: 'Sales',
        component: () => import('@/views/sales/index.vue'),
        meta: { title: '销售管理' },
      },
      {
        path: 'inventory',
        name: 'Inventory',
        component: () => import('@/views/inventory/index.vue'),
        meta: { title: '库存流水' },
      },
    ],
  },
  {
    path: '/finance',
    component: Layout,
    children: [
      {
        path: '',
        name: 'Finance',
        component: () => import('@/views/finance/index.vue'),
        meta: { title: '财务管理', icon: 'Money' },
      },
    ],
  },
  {
    path: '/system',
    component: Layout,
    meta: { title: '系统管理', icon: 'Setting' },
    children: [
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'db',
        name: 'Database',
        component: () => import('@/views/system/db.vue'),
        meta: { title: '数据库备份' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/** 导航守卫 */
router.beforeEach(async (to) => {
  const userStore = useUserStore()
  if (to.path === '/login') {
    if (!userStore.isLoggedIn) return true
    try {
      if (!userStore.userInfo) {
        await userStore.fetchUserInfo()
      }
      return '/'
    } catch {
      return '/login'
    }
  }

  if (!userStore.isLoggedIn) return '/login'

  try {
    if (!userStore.userInfo) {
      await userStore.fetchUserInfo()
    }
    if (userStore.userInfo?.role !== 'ADMIN') {
      await userStore.logout()
      return '/login'
    }
    return true
  } catch {
    return '/login'
  }
})

export default router
