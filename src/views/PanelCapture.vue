<template>
  <div class="panel-capture">
    <NavBar />

    <div class="page-content">
      <div class="content-grid">
        <!-- 左栏：上传区 -->
        <div class="upload-section">
          <div
            class="upload-zone"
            :class="{ 'dragging': isDragging, 'has-image': previewUrl }"
            @dragover.prevent="isDragging = true"
            @dragleave="isDragging = false"
            @drop.prevent="handleDrop"
            @click="triggerUpload"
          >
            <input ref="fileInput" type="file" accept="image/*" hidden @change="handleFileChange" />

            <transition name="fade" mode="out-in">
              <div v-if="!previewUrl" class="upload-placeholder" key="placeholder">
                <div class="upload-icon">
                  <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="17 8 12 3 7 8"/>
                    <line x1="12" y1="3" x2="12" y2="15"/>
                  </svg>
                </div>
                <p class="upload-hint-primary">拖拽截图至此处</p>
                <p class="upload-hint-secondary">或点击选择文件 · 支持 PNG / JPG</p>
              </div>

              <div v-else class="upload-preview" key="preview">
                <img :src="previewUrl" alt="面板截图" class="preview-img" />
                <button class="re-upload-btn" @click.stop="resetUpload">重新上传</button>
              </div>
            </transition>
          </div>

          <!-- OCR 状态 -->
          <div class="ocr-status" v-if="ocrState !== 'idle'">
            <div class="status-bar">
              <div class="status-indicator" :class="ocrState"></div>
              <span class="status-text">{{ statusText }}</span>
            </div>
            <div class="progress-track" v-if="ocrState === 'loading'">
              <div class="progress-fill"></div>
            </div>
          </div>

          <button
            class="ocr-btn"
            :disabled="!previewUrl || ocrState === 'loading'"
            @click="runOcr"
          >
            <span v-if="ocrState !== 'loading'">识别属性</span>
            <span v-else class="loading-dots">识别中<span>.</span><span>.</span><span>.</span></span>
          </button>
        </div>

        <!-- 右栏：数值编辑区 -->
        <div class="stats-section">
          <div class="stats-header">
            <h2 class="stats-title">属性数值</h2>
            <span class="stats-hint">识别后可手动修正各项数值</span>
          </div>

          <div class="stats-panel" :class="{ 'highlighted': ocrState === 'success' }">
            <div class="stat-group" v-for="group in statGroups" :key="group.label">
              <div class="group-label">{{ group.label }}</div>
              <div class="stat-rows">
                <div class="stat-row" v-for="stat in group.stats" :key="stat.key">
                  <label class="stat-label">{{ stat.label }}</label>
                  <div class="stat-input-wrap">
                    <input
                      type="number"
                      class="stat-input"
                      v-model.number="panelData[stat.key]"
                      :placeholder="stat.placeholder || '—'"
                      min="0"
                    />
                    <span class="stat-unit" v-if="stat.unit">{{ stat.unit }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 面板命名 & 确认 -->
          <div class="confirm-section">
            <div class="panel-name-wrap">
              <label class="panel-name-label">面板名称</label>
              <input
                type="text"
                class="panel-name-input"
                v-model="panelName"
                placeholder="如：毕业面板 / 攻击流配装"
                maxlength="20"
              />
            </div>
            <button class="confirm-btn" @click="savePanel" :disabled="!hasData">
              确认保存
            </button>
          </div>
        </div>
      </div>

      <!-- 历史记录抽屉 -->
      <transition name="drawer">
        <div class="history-drawer" v-if="showHistory">
          <div class="drawer-overlay" @click="showHistory = false"></div>
          <div class="drawer-panel">
            <div class="drawer-header">
              <h3>历史面板</h3>
              <button class="drawer-close" @click="showHistory = false">✕</button>
            </div>
            <div class="history-list">
              <div class="history-item" v-for="item in historyPanels" :key="item.id" @click="loadHistory(item)">
                <div class="history-name">{{ item.panelName || '未命名面板' }}</div>
                <div class="history-time">{{ formatTime(item.createdAt) }}</div>
                <div class="history-stats">
                  攻击 {{ item.panelData.atk }} · 防御 {{ item.panelData.def }} · 气血 {{ item.panelData.hp }}
                </div>
              </div>
              <div class="history-empty" v-if="historyPanels.length === 0">暂无历史记录</div>
            </div>
          </div>
        </div>
      </transition>

      <!-- 保存成功提示 -->
      <transition name="toast">
        <div class="toast" v-if="toastVisible">{{ toastMsg }}</div>
      </transition>
    </div>   <!-- ← 这里关闭 page-content -->
  </div>     <!-- ← 这里关闭 panel-capture -->
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import axios from 'axios'
import NavBar from '../components/NavBar.vue'

// ───────────── 上传状态 ─────────────
const fileInput = ref(null)
const previewUrl = ref('')
const isDragging = ref(false)
const uploadedFile = ref(null)

// ───────────── OCR 状态 ─────────────
const ocrState = ref('idle') // idle | loading | success | error
const statusText = computed(() => ({
  loading: '正在识别，请稍候…',
  success: '识别完成，请核对数值',
  error:   '识别失败，请手动填写'
})[ocrState.value] || '')

// ───────────── 面板数据 ─────────────
const panelData = reactive({
  atk: null, maxAtk: null, minAtk: null,
  def: null, hp: null,
  hitRate: null, critRate: null, critDmg: null,
  pierce: null, shieldPierce: null,
  elemAtk: null, flowElemAtk: null,
  domination: null, resist: null
})

const hasData = computed(() => Object.values(panelData).some(v => v !== null && v !== ''))
const panelName = ref('')
const showHistory = ref(false)
const historyPanels = ref([])

// 属性分组配置
const statGroups = [
  {
    label: '攻击',
    stats: [
      { key: 'atk',       label: '外功攻击'   },
      { key: 'maxAtk',    label: '最大攻击'   },
      { key: 'minAtk',    label: '最小攻击'   },
      { key: 'elemAtk',   label: '全元素攻'   },
      { key: 'flowElemAtk', label: '流派元素攻' },
    ]
  },
  {
    label: '命中·会心',
    stats: [
      { key: 'hitRate',   label: '命中'   },
      { key: 'critRate',  label: '会心'   },
      { key: 'critDmg',   label: '会心伤害' },
    ]
  },
  {
    label: '破防·破盾',
    stats: [
      { key: 'pierce',       label: '破防' },
      { key: 'shieldPierce', label: '破盾' },
    ]
  },
  {
    label: '防御·生存',
    stats: [
      { key: 'def',       label: '防御' },
      { key: 'hp',        label: '气血' },
    ]
  },
  {
    label: '克制',
    stats: [
      { key: 'domination', label: '流派克制' },
      { key: 'resist',     label: '流派抵御' },
    ]
  }
]

// ───────────── 文件处理 ─────────────
function triggerUpload() { fileInput.value?.click() }

function handleFileChange(e) {
  const file = e.target.files[0]
  if (file) loadFile(file)
}

function handleDrop(e) {
  isDragging.value = false
  const file = e.dataTransfer.files[0]
  if (file && file.type.startsWith('image/')) loadFile(file)
}

function loadFile(file) {
  uploadedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
  ocrState.value = 'idle'
}

function resetUpload() {
  previewUrl.value = ''
  uploadedFile.value = null
  ocrState.value = 'idle'
  if (fileInput.value) fileInput.value.value = ''
}

// ───────────── OCR ─────────────
async function runOcr() {
  if (!uploadedFile.value) return
  ocrState.value = 'loading'

  const form = new FormData()
  form.append('file', uploadedFile.value)

  try {
    const { data } = await axios.post('/api/panel/ocr', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (data.code === 200) {
      Object.assign(panelData, data.data)
      ocrState.value = 'success'
    } else {
      ocrState.value = 'error'
    }
  } catch {
    ocrState.value = 'error'
  }
}

// ───────────── 保存 ─────────────
const toastVisible = ref(false)
const toastMsg = ref('')

async function savePanel() {
  try {
    await axios.post('/api/panel/save', {
      panelData,
      panelName: panelName.value || '未命名面板',
      source: ocrState.value === 'success' ? 'ocr' : 'manual'
    })
    showToast('面板保存成功！')
  } catch {
    showToast('保存失败，请重试')
  }
}

function showToast(msg) {
  toastMsg.value = msg
  toastVisible.value = true
  setTimeout(() => (toastVisible.value = false), 2500)
}

function formatTime(ts) {
  return new Date(ts).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function loadHistory(item) {
  Object.assign(panelData, item.panelData)
  panelName.value = item.panelName
  showHistory.value = false
}
</script>

<style scoped>
/* ── 设计令牌 ── */
:root {
  --white:      #ffffff;
  --bg:         #f8f9fa;
  --border:     #e5e7eb;
  --text-primary:   #111827;
  --text-secondary: #6b7280;
  --accent:     #1d4ed8;
  --accent-light: #eff6ff;
  --success:    #059669;
  --error:      #dc2626;
  --radius-sm:  6px;
  --radius-md:  10px;
  --radius-lg:  16px;
}

* { box-sizing: border-box; margin: 0; padding: 0; }

.panel-capture {
  min-height: 100vh;
  background: #f8f9fa;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: #111827;
}

/* ── 页头 ── */
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 36px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e5e7eb;
}
.page-title {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: #111827;
}
.page-subtitle {
  display: block;
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
}
.page-content {
  padding: 40px 48px;
}
.history-link {
  font-size: 13px;
  color: #1d4ed8;
  cursor: pointer;
  transition: opacity .15s;
}
.history-link:hover { opacity: .7; }

/* ── 主布局 ── */
.content-grid {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 28px;
  align-items: start;
}

/* ── 上传区 ── */
.upload-section { display: flex; flex-direction: column; gap: 16px; }

.upload-zone {
  background: #ffffff;
  border: 1.5px dashed #d1d5db;
  border-radius: 12px;
  cursor: pointer;
  transition: border-color .2s, background .2s;
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}
.upload-zone:hover,
.upload-zone.dragging {
  border-color: #1d4ed8;
  background: #eff6ff;
}
.upload-zone.has-image {
  border-style: solid;
  border-color: #d1d5db;
  background: #000;
}

.upload-placeholder {
  text-align: center;
  color: #9ca3af;
  padding: 40px 24px;
}
.upload-icon {
  display: inline-flex;
  margin-bottom: 16px;
  color: #d1d5db;
}
.upload-hint-primary {
  font-size: 15px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}
.upload-hint-secondary {
  font-size: 12px;
  color: #9ca3af;
}

.upload-preview {
  width: 100%;
  height: 100%;
  position: relative;
}
.preview-img {
  width: 100%;
  height: 260px;
  object-fit: contain;
  display: block;
}
.re-upload-btn {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 6px 14px;
  font-size: 12px;
  cursor: pointer;
  transition: background .15s;
}
.re-upload-btn:hover { background: rgba(0,0,0,0.85); }

/* ── OCR 状态 ── */
.ocr-status { background: #fff; border-radius: 8px; padding: 12px 16px; border: 1px solid #e5e7eb; }
.status-bar { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.status-indicator {
  width: 8px; height: 8px; border-radius: 50%;
}
.status-indicator.loading { background: #f59e0b; animation: pulse 1s infinite; }
.status-indicator.success { background: #059669; }
.status-indicator.error   { background: #dc2626; }
.status-text { font-size: 13px; color: #374151; }

.progress-track {
  height: 3px;
  background: #e5e7eb;
  border-radius: 99px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: #1d4ed8;
  border-radius: 99px;
  animation: progress 1.8s ease-in-out infinite;
}

/* ── 按钮 ── */
.ocr-btn {
  width: 100%;
  height: 44px;
  background: #111827;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background .15s, opacity .15s;
  letter-spacing: 0.5px;
}
.ocr-btn:hover:not(:disabled) { background: #1f2937; }
.ocr-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.loading-dots span {
  animation: blink 1.2s infinite;
}
.loading-dots span:nth-child(2) { animation-delay: .2s; }
.loading-dots span:nth-child(3) { animation-delay: .4s; }

/* ── 属性区 ── */
.stats-section {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}
.stats-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f3f4f6;
}
.stats-title { font-size: 15px; font-weight: 600; color: #111827; }
.stats-hint  { font-size: 12px; color: #9ca3af; }

.stats-panel {
  padding: 8px 0 0;
  transition: background .3s;
}
.stats-panel.highlighted { background: #f0fdf4; }

.stat-group { padding: 12px 24px; border-bottom: 1px solid #f3f4f6; }
.stat-group:last-child { border-bottom: none; }

.group-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.8px;
  text-transform: uppercase;
  color: #9ca3af;
  margin-bottom: 10px;
}
.stat-rows { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }

.stat-row { display: flex; align-items: center; gap: 8px; }
.stat-label {
  flex: 0 0 72px;
  font-size: 13px;
  color: #374151;
  white-space: nowrap;
}
.stat-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
  transition: border-color .15s;
}
.stat-input-wrap:focus-within {
  border-color: #1d4ed8;
  background: #fff;
}
.stat-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 7px 10px;
  font-size: 13px;
  color: #111827;
  outline: none;
  width: 100%;
}
.stat-input::placeholder { color: #d1d5db; }
.stat-input::-webkit-outer-spin-button,
.stat-input::-webkit-inner-spin-button { -webkit-appearance: none; }
.stat-unit {
  padding: 0 10px 0 4px;
  font-size: 11px;
  color: #9ca3af;
}

/* ── 确认栏 ── */
.confirm-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #f3f4f6;
  background: #fafafa;
}
.panel-name-wrap { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.panel-name-label { font-size: 11px; color: #9ca3af; }
.panel-name-input {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 13px;
  background: #fff;
  outline: none;
  transition: border-color .15s;
}
.panel-name-input:focus { border-color: #1d4ed8; }
.panel-name-input::placeholder { color: #d1d5db; }

.confirm-btn {
  flex: 0 0 100px;
  height: 38px;
  background: #1d4ed8;
  color: #fff;
  border: none;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background .15s, opacity .15s;
}
.confirm-btn:hover:not(:disabled) { background: #1e40af; }
.confirm-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* ── 历史抽屉 ── */
.history-drawer {
  position: fixed; inset: 0; z-index: 100;
  display: flex; justify-content: flex-end;
}
.drawer-overlay {
  position: absolute; inset: 0;
  background: rgba(0,0,0,0.3);
  backdrop-filter: blur(2px);
}
.drawer-panel {
  position: relative;
  width: 360px;
  background: #fff;
  height: 100%;
  overflow-y: auto;
  box-shadow: -4px 0 24px rgba(0,0,0,0.08);
  display: flex; flex-direction: column;
}
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px;
  border-bottom: 1px solid #f3f4f6;
  position: sticky; top: 0;
  background: #fff;
}
.drawer-header h3 { font-size: 15px; font-weight: 600; }
.drawer-close {
  background: none; border: none;
  font-size: 14px; color: #9ca3af;
  cursor: pointer; width: 28px; height: 28px;
  border-radius: 4px; transition: background .15s;
}
.drawer-close:hover { background: #f3f4f6; }

.history-list { padding: 12px; }
.history-item {
  padding: 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: background .15s;
  margin-bottom: 6px;
}
.history-item:hover { background: #f3f4f6; }
.history-name { font-size: 14px; font-weight: 500; color: #111827; margin-bottom: 4px; }
.history-time { font-size: 11px; color: #9ca3af; margin-bottom: 6px; }
.history-stats { font-size: 12px; color: #6b7280; }
.history-empty { text-align: center; color: #9ca3af; font-size: 13px; padding: 40px 0; }

/* ── Toast ── */
.toast {
  position: fixed;
  bottom: 32px; left: 50%; transform: translateX(-50%);
  background: #111827;
  color: #fff;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 13px;
  box-shadow: 0 4px 16px rgba(0,0,0,.15);
  z-index: 200;
}

/* ── 过渡 ── */
.fade-enter-active, .fade-leave-active { transition: opacity .2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.drawer-enter-active .drawer-panel,
.drawer-leave-active .drawer-panel {
  transition: transform .3s cubic-bezier(.4,0,.2,1);
}
.drawer-enter-from .drawer-panel,
.drawer-leave-to   .drawer-panel { transform: translateX(100%); }
.drawer-enter-active .drawer-overlay,
.drawer-leave-active .drawer-overlay { transition: opacity .3s; }
.drawer-enter-from .drawer-overlay,
.drawer-leave-to   .drawer-overlay { opacity: 0; }

.toast-enter-active, .toast-leave-active { transition: opacity .25s, transform .25s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(8px); }

/* ── 动画 ── */
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.4} }
@keyframes progress { 0%{width:0;margin-left:0} 50%{width:60%;margin-left:20%} 100%{width:0;margin-left:100%} }
@keyframes blink { 0%,80%,100%{opacity:1} 40%{opacity:0} }
</style>
