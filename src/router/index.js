import { createRouter, createWebHistory } from 'vue-router'

// 导入所有页面组件
import Login from '../views/Login.vue'
import PanelCapture from '../views/PanelCapture.vue'
import EquipmentAdvisor from '../views/EquipmentAdvisor.vue'
import RecommendVisual from '../views/RecommendVisual.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { 
      path: '/login', 
      component: Login 
    },
    { 
      path: '/', 
      component: PanelCapture, 
      meta: { requiresAuth: true } 
    },
    { 
      path: '/equipment', 
      component: EquipmentAdvisor, 
      meta: { requiresAuth: true } 
    },
    { 
      path: '/visual', 
      component: RecommendVisual, 
      meta: { requiresAuth: true } 
    },
  ]
})

// ==================== 路由守卫 ====================
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  // 需要登录但没有 token → 跳转登录页
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } 
  // 已登录却访问登录页 → 跳回首页
  else if (to.path === '/login' && token) {
    next('/')
  } 
  else {
    next()
  }
})

export default router