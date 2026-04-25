<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 新增：图片放在标题上方 -->
      <div class="mascot-container">
        <img src="../assets/mascot.png" alt="逆水寒搭配助手" class="mascot" />
      </div>

      <div class="brand">
        <h1 class="brand-name">简单搭配</h1>
        <p class="brand-sub">专为联赛设计的简单好用的搭配推荐</p>
      </div>

      <div class="tab-switch">
        <button :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
        <button :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
      </div>

      <div class="form">
        <div class="field">
          <label>用户名</label>
          <input v-model="form.username" type="text" placeholder="请输入用户名" @keyup.enter="submit" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="form.password" type="password" placeholder="请输入密码（至少6位）" @keyup.enter="submit" />
        </div>

        <!-- 验证码（仅注册时显示）-->
        <div class="field" v-if="mode === 'register'">
          <label>验证码</label>
          <div class="captcha-row">
            <input v-model="form.captcha" type="text" placeholder="请输入图中验证码" maxlength="4" autocomplete="off" />
            <canvas ref="captchaCanvas" class="captcha-canvas" width="110" height="38"
              @click="refreshCaptcha" title="点击刷新验证码"></canvas>
          </div>
        </div>

        <!-- 职业选择（仅注册时显示）-->
        <div class="field" v-if="mode === 'register'">
          <label>选择职业</label>
          <select v-model="form.profession" class="profession-select">
            <option value="" disabled>请选择你的职业</option>
            <option v-for="p in professions" :key="p" :value="p">{{ p }}</option>
          </select>
        </div>

        <p class="error-msg" v-if="errorMsg">{{ errorMsg }}</p>
        <button class="submit-btn" @click="submit" :disabled="loading">
          {{ loading ? '请稍候…' : (mode === 'login' ? '登录' : '注册') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const mode = ref('login')
const loading = ref(false)
const errorMsg = ref('')

const form = reactive({ username: '', password: '', captcha: '', profession: '' })

const professions = ['神相', '素问', '潮光', '鸿音', '龙吟', '沧澜', '玄机', '铁衣', '碎梦', '九灵', '血河']

// ── 验证码 ──
const captchaCanvas = ref(null)
let captchaText = ''

function refreshCaptcha() {
  const canvas = captchaCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  captchaText = Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join('')

  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.fillStyle = '#f3f4f6'
  ctx.fillRect(0, 0, canvas.width, canvas.height)

  // 干扰线
  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `hsl(${Math.random() * 360}, 50%, 70%)`
    ctx.beginPath()
    ctx.moveTo(Math.random() * canvas.width, Math.random() * canvas.height)
    ctx.lineTo(Math.random() * canvas.width, Math.random() * canvas.height)
    ctx.stroke()
  }

  // 字符
  captchaText.split('').forEach((char, i) => {
    ctx.font = `bold ${19 + Math.random() * 5}px Arial`
    ctx.fillStyle = `hsl(${Math.random() * 360}, 70%, 35%)`
    ctx.save()
    ctx.translate(14 + i * 24, 26 + Math.random() * 6)
    ctx.rotate((Math.random() - 0.5) * 0.35)
    ctx.fillText(char, 0, 0)
    ctx.restore()
  })
}

// 切换到注册时自动生成验证码
watch(() => mode.value, (val) => {
  if (val === 'register') nextTick(() => refreshCaptcha())
})

async function submit() {
  errorMsg.value = ''
  if (!form.username || !form.password) {
    errorMsg.value = '用户名和密码不能为空'
    return
  }
  if (form.password.length < 6) {
    errorMsg.value = '密码长度不能少于6位'
    return
  }
  // 注册专属校验
  if (mode.value === 'register') {
    if (form.captcha.toUpperCase() !== captchaText) {
      errorMsg.value = '验证码错误，请重新输入'
      form.captcha = ''
      refreshCaptcha()
      return
    }
    if (!form.profession) {
      errorMsg.value = '请选择职业'
      return
    }
  }

  loading.value = true
  try {
    const url = mode.value === 'login' ? '/api/auth/login' : '/api/auth/register'
    const { data } = await axios.post(url, form)
    if (data.code === 200) {
      if (mode.value === 'login') {
        localStorage.setItem('token', data.data.accessToken)
        localStorage.setItem('userId', data.data.userId)
        localStorage.setItem('username', data.data.username)
        router.push('/')
      } else {
        errorMsg.value = '注册成功，请登录'
        mode.value = 'login'
      }
    } else {
      errorMsg.value = data.message || '操作失败'
    }
  } catch (e) {
    if (e.response?.data?.message) {
      errorMsg.value = e.response.data.message
    } else {
      errorMsg.value = '网络错误，请稍后重试'
    }
    console.error(e)
  }
  loading.value = false
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.login-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 40px 40px 48px;
  width: 380px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06);
  text-align: center;
}

/* 新增图片样式 - 按比例居中显示 */
.mascot-container {
  margin-bottom: 4px;
  display: flex;
  justify-content: center;
}

.mascot {
  width: 110px;
  height: auto;
  filter: drop-shadow(0 4px 8px rgba(0,0,0,0.1));
}

/* 品牌文字 */
.brand {
  margin-bottom: 32px;
}

.brand-name {
  font-size: 26px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.5px;
  margin: 0;
}

.brand-sub {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 6px;
}

.tab-switch { display: flex; background: #f3f4f6; border-radius: 8px; padding: 3px; margin-bottom: 28px; }
.tab-switch button { flex: 1; padding: 8px; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; background: transparent; color: #6b7280; transition: all .15s; }
.tab-switch button.active { background: #fff; color: #111827; font-weight: 500; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }

.field { margin-bottom: 16px; text-align: left; }
.field label { display: block; font-size: 12px; color: #6b7280; margin-bottom: 6px; }
.field input { width: 100%; padding: 10px 14px; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 14px; outline: none; background: #fff; transition: border-color .15s; }
.field input:focus { border-color: #1d4ed8; }

/* 验证码行 */
.captcha-row { display: flex; gap: 10px; align-items: center; }
.captcha-row input { flex: 1; width: auto; }
.captcha-canvas {
  width: 110px;
  height: 38px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid #e5e7eb;
  flex-shrink: 0;
}

/* 职业选择 */
.profession-select {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  outline: none;
  color: #111827;
  cursor: pointer;
  transition: border-color .15s;
}
.profession-select:focus { border-color: #1d4ed8; }

.error-msg { font-size: 12px; color: #dc2626; margin-bottom: 12px; text-align: center; }

.submit-btn {
  width: 100%;
  padding: 12px;
  background: #111827;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  margin-top: 8px;
  transition: background .15s, opacity .15s;
}
.submit-btn:hover:not(:disabled) { background: #1f2937; }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
