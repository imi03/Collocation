<template>
  <header class="nav-bar">
    <div class="nav-left">
      <span class="nav-logo">简单搭配</span>
      <nav class="nav-links">
       <RouterLink to="/" class="nav-link">面板录入</RouterLink>
        <RouterLink to="/equipment" class="nav-link">装备推荐</RouterLink>
        <RouterLink to="/visual" class="nav-link">收益可视化</RouterLink>
      </nav>
    </div>
    <div class="nav-right">
      <span class="nav-user" v-if="username">{{ username }}</span>
      <button class="logout-btn" @click="logout">退出登录</button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = computed(() => localStorage.getItem('username'))

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<style scoped>
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 50;
}
.nav-left { display: flex; align-items: center; gap: 32px; }
.nav-logo { font-size: 16px; font-weight: 700; color: #111827; letter-spacing: -0.3px; }
.nav-links { display: flex; gap: 4px; }
.nav-link {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #6b7280;
  text-decoration: none;
  transition: all .15s;
}
.nav-link:hover { background: #f3f4f6; color: #111827; }
.nav-link.router-link-active { background: #f3f4f6; color: #111827; font-weight: 500; }
.nav-right { display: flex; align-items: center; gap: 12px; }
.nav-user { font-size: 13px; color: #6b7280; }
.logout-btn {
  padding: 6px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #6b7280;
  font-size: 12px;
  cursor: pointer;
  transition: all .15s;
}
.logout-btn:hover { border-color: #dc2626; color: #dc2626; background: #fef2f2; }
</style>