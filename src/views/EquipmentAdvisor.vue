<template>
  <div class="equipment-advisor">
    <!-- 页头 -->
    <header class="page-header">
      <div>
        <h1 class="page-title">装备推荐</h1>
        <span class="page-subtitle">基于当前面板，计算各槽位装备的属性收益</span>
      </div>
      <div class="panel-badge" v-if="currentPanel">
        <span class="badge-dot"></span>
        <span class="badge-text">{{ currentPanel.panelName || '当前面板' }}</span>
        <span class="badge-switch" @click="showPanelPicker = true">切换</span>
      </div>
    </header>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="slot-tabs">
        <button
          v-for="slot in slots"
          :key="slot.key"
          class="slot-tab"
          :class="{ active: activeSlot === slot.key }"
          @click="activeSlot = slot.key; fetchRecommendations()"
        >
          {{ slot.label }}
        </button>
      </div>
      <div class="sort-select-wrap">
        <span class="sort-label">排序</span>
        <select class="sort-select" v-model="sortBy" @change="sortResults">
          <option value="score">综合得分</option>
          <option value="damage">伤害提升</option>
          <option value="tank">坦度提升</option>
        </select>
      </div>
    </div>

    <!-- 主内容：装备列表 + 详情 -->
    <div class="main-layout">
      <!-- 左：装备列表 -->
      <div class="equipment-list">
        <div class="list-skeleton" v-if="loading">
          <div class="skeleton-item" v-for="i in 6" :key="i"></div>
        </div>

        <template v-else>
          <div
            v-for="(item, idx) in displayList"
            :key="item.id"
            class="equip-card"
            :class="{ selected: selectedId === item.id }"
            @click="selectEquipment(item)"
          >
            <div class="card-rank">
              <span class="rank-num" :class="idx < 3 ? `rank-${idx+1}` : ''">{{ idx + 1 }}</span>
            </div>
            <div class="card-img-wrap">
              <img :src="item.imageUrl || '/placeholder-equip.png'" :alt="item.name" class="card-img" />
            </div>
            <div class="card-info">
              <div class="card-name">{{ item.name }}</div>
              <div class="card-slot-tag">{{ slotLabel(item.slot) }}</div>
            </div>
            <div class="card-metrics">
              <div class="metric" :class="item.damageDeltaPct >= 0 ? 'positive' : 'negative'">
                <span class="metric-icon">⚔</span>
                <span class="metric-val">{{ formatDelta(item.damageDeltaPct) }}</span>
              </div>
              <div class="metric" :class="item.tankDeltaPct >= 0 ? 'positive' : 'negative'">
                <span class="metric-icon">🛡</span>
                <span class="metric-val">{{ formatDelta(item.tankDeltaPct) }}</span>
              </div>
            </div>
            <div class="card-score">
              <span class="score-num">{{ item.totalScore?.toFixed(1) }}</span>
              <span class="score-unit">分</span>
            </div>
          </div>

          <div class="list-empty" v-if="displayList.length === 0">
            <p>暂无推荐数据</p>
            <span>请先保存面板数据</span>
          </div>
        </template>
      </div>

      <!-- 右：装备详情 -->
      <div class="equip-detail" v-if="selectedEquip">
        <div class="detail-card">
          <div class="detail-header">
            <img :src="selectedEquip.imageUrl || '/placeholder-equip.png'" class="detail-img" />
            <div class="detail-title-wrap">
              <h2 class="detail-name">{{ selectedEquip.name }}</h2>
              <span class="detail-slot">{{ slotLabel(selectedEquip.slot) }}</span>
            </div>
          </div>

          <!-- 收益可视化 -->
          <div class="gain-bars">
            <div class="gain-row">
              <span class="gain-label">伤害变化</span>
              <div class="gain-track">
                <div
                  class="gain-fill damage"
                  :style="{ width: Math.abs(selectedEquip.damageDeltaPct) * 5 + 'px' }"
                  :class="selectedEquip.damageDeltaPct >= 0 ? 'up' : 'down'"
                ></div>
              </div>
              <span class="gain-num" :class="selectedEquip.damageDeltaPct >= 0 ? 'up' : 'down'">
                {{ formatDelta(selectedEquip.damageDeltaPct) }}
              </span>
            </div>
            <div class="gain-row">
              <span class="gain-label">坦度变化</span>
              <div class="gain-track">
                <div
                  class="gain-fill tank"
                  :style="{ width: Math.abs(selectedEquip.tankDeltaPct) * 5 + 'px' }"
                  :class="selectedEquip.tankDeltaPct >= 0 ? 'up' : 'down'"
                ></div>
              </div>
              <span class="gain-num" :class="selectedEquip.tankDeltaPct >= 0 ? 'up' : 'down'">
                {{ formatDelta(selectedEquip.tankDeltaPct) }}
              </span>
            </div>
          </div>

          <!-- 综合得分 -->
          <div class="score-block">
            <div class="score-label">综合得分</div>
            <div class="score-value">{{ selectedEquip.totalScore?.toFixed(1) }}</div>
            <div class="score-formula">伤害+{{ selectedEquip.damageDeltaPct?.toFixed(1) }}% × 1分 + 坦度+{{ selectedEquip.tankDeltaPct?.toFixed(1) }}% ÷ 2分</div>
          </div>

          <!-- 属性加成 -->
          <div class="stat-bonuses">
            <div class="bonuses-title">装备属性</div>
            <div class="bonus-list">
              <div class="bonus-item" v-for="(val, key) in selectedEquip.statBonuses" :key="key" v-if="val">
                <span class="bonus-key">{{ statKeyLabel(key) }}</span>
                <span class="bonus-val">+{{ val }}</span>
              </div>
            </div>
          </div>

          <!-- 对比基准 -->
          <div class="compare-block">
            <div class="compare-title">前后对比</div>
            <div class="compare-row">
              <span class="compare-col-label">属性</span>
              <span class="compare-col-label">当前</span>
              <span class="compare-col-label">替换后</span>
              <span class="compare-col-label">变化</span>
            </div>
            <div class="compare-item" v-for="row in compareRows" :key="row.key">
              <span class="c-label">{{ row.label }}</span>
              <span class="c-val">{{ row.before }}</span>
              <span class="c-val">{{ row.after }}</span>
              <span class="c-delta" :class="row.delta >= 0 ? 'up' : 'down'">
                {{ row.delta >= 0 ? '+' : '' }}{{ row.delta }}
              </span>
            </div>
          </div>

          <button class="adopt-btn">加入对比清单</button>
        </div>
      </div>

      <div class="detail-placeholder" v-else>
        <div class="placeholder-inner">
          <div class="placeholder-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#d1d5db" stroke-width="1.5">
              <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
              <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
            </svg>
          </div>
          <span>点击左侧装备查看详情</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

const slots = [
  { key: 'all',  label: '全部' },
  { key: '武器', label: '武器' },
  { key: '头盔', label: '头盔' },
  { key: '胸甲', label: '胸甲' },
  { key: '腿甲', label: '腿甲' },
  { key: '饰品', label: '饰品' },
]
const activeSlot = ref('all')
const sortBy = ref('score')
const loading = ref(false)
const recommendations = ref([])
const selectedId = ref(null)
const selectedEquip = ref(null)
const currentPanel = ref(null)
const showPanelPicker = ref(false)

const displayList = computed(() => {
  let list = activeSlot.value === 'all'
    ? recommendations.value
    : recommendations.value.filter(e => e.slot === activeSlot.value)

  return [...list].sort((a, b) => {
    if (sortBy.value === 'damage') return b.damageDeltaPct - a.damageDeltaPct
    if (sortBy.value === 'tank')   return b.tankDeltaPct - a.tankDeltaPct
    return b.totalScore - a.totalScore
  })
})

const compareRows = computed(() => {
  if (!selectedEquip.value || !currentPanel.value) return []
  const bonuses = selectedEquip.value.statBonuses || {}
  return Object.entries(bonuses).map(([key, delta]) => ({
    key,
    label: statKeyLabel(key),
    before: currentPanel.value.panelData?.[key] ?? '—',
    after:  (currentPanel.value.panelData?.[key] ?? 0) + delta,
    delta
  })).filter(r => r.delta !== 0)
})

async function fetchRecommendations() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/equipment/recommend')
    if (data.code === 200) recommendations.value = data.data
  } catch {}
  loading.value = false
}

async function fetchCurrentPanel() {
  try {
    const { data } = await axios.get('/api/panel/latest')
    if (data.code === 200) currentPanel.value = data.data
  } catch {}
}

function selectEquipment(item) {
  selectedId.value = item.id
  selectedEquip.value = item
}

function sortResults() { /* 依赖 computed displayList */ }

function slotLabel(slot) { return slot || '—' }
function formatDelta(v) {
  if (v == null) return '—'
  return (v >= 0 ? '+' : '') + v.toFixed(2) + '%'
}
function statKeyLabel(key) {
  const map = {
    atk: '外功攻击', maxAtk: '最大攻击', minAtk: '最小攻击',
    def: '防御', hp: '气血', hitRate: '命中', critRate: '会心',
    critDmg: '会心伤害', pierce: '破防', shieldPierce: '破盾',
    elemAtk: '全元素攻', flowElemAtk: '流派元素攻',
    domination: '流派克制', resist: '流派抵御'
  }
  return map[key] || key
}

onMounted(() => {
  fetchCurrentPanel()
  fetchRecommendations()
})
</script>

<style scoped>
* { box-sizing: border-box; margin: 0; padding: 0; }

.equipment-advisor {
  min-height: 100vh;
  background: #f8f9fa;
  padding: 40px 48px;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: #111827;
}

/* 页头 */
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 28px; padding-bottom: 20px; border-bottom: 1px solid #e5e7eb;
}
.page-title { font-size: 26px; font-weight: 700; letter-spacing: -.5px; }
.page-subtitle { display: block; font-size: 13px; color: #9ca3af; margin-top: 4px; }

.panel-badge {
  display: flex; align-items: center; gap: 8px;
  background: #fff; border: 1px solid #e5e7eb;
  border-radius: 20px; padding: 6px 14px;
}
.badge-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: #059669; flex-shrink: 0;
}
.badge-text { font-size: 13px; color: #374151; }
.badge-switch { font-size: 12px; color: #1d4ed8; cursor: pointer; margin-left: 4px; }

/* 筛选栏 */
.filter-bar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
}
.slot-tabs { display: flex; gap: 6px; }
.slot-tab {
  padding: 6px 16px; border-radius: 20px;
  border: 1px solid #e5e7eb; background: #fff;
  font-size: 13px; color: #6b7280; cursor: pointer;
  transition: all .15s;
}
.slot-tab.active, .slot-tab:hover {
  background: #111827; color: #fff; border-color: #111827;
}
.sort-select-wrap { display: flex; align-items: center; gap: 8px; }
.sort-label { font-size: 13px; color: #9ca3af; }
.sort-select {
  border: 1px solid #e5e7eb; border-radius: 6px;
  padding: 6px 12px; font-size: 13px; background: #fff;
  color: #374151; outline: none; cursor: pointer;
}

/* 主布局 */
.main-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 20px;
  align-items: start;
}

/* 装备列表 */
.equipment-list { display: flex; flex-direction: column; gap: 8px; }

.skeleton-item {
  height: 72px; border-radius: 10px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 400% 100%;
  animation: shimmer 1.4s infinite;
}

.equip-card {
  display: flex; align-items: center; gap: 14px;
  background: #fff; border: 1px solid #e5e7eb;
  border-radius: 10px; padding: 14px 16px;
  cursor: pointer; transition: all .15s;
}
.equip-card:hover { border-color: #1d4ed8; box-shadow: 0 2px 12px rgba(29,78,216,.08); }
.equip-card.selected { border-color: #1d4ed8; background: #eff6ff; }

.card-rank { width: 24px; text-align: center; }
.rank-num { font-size: 14px; font-weight: 700; color: #9ca3af; }
.rank-num.rank-1 { color: #f59e0b; }
.rank-num.rank-2 { color: #6b7280; }
.rank-num.rank-3 { color: #b45309; }

.card-img-wrap { width: 44px; height: 44px; border-radius: 8px; overflow: hidden; background: #f3f4f6; flex-shrink: 0; }
.card-img { width: 100%; height: 100%; object-fit: cover; }

.card-info { flex: 1; }
.card-name { font-size: 14px; font-weight: 500; color: #111827; margin-bottom: 3px; }
.card-slot-tag { font-size: 11px; color: #9ca3af; }

.card-metrics { display: flex; flex-direction: column; gap: 4px; align-items: flex-end; }
.metric { display: flex; align-items: center; gap: 5px; font-size: 12px; }
.metric.positive .metric-val { color: #059669; }
.metric.negative .metric-val { color: #dc2626; }
.metric-icon { font-size: 11px; }

.card-score { text-align: right; min-width: 60px; }
.score-num { font-size: 20px; font-weight: 700; color: #111827; }
.score-unit { font-size: 11px; color: #9ca3af; margin-left: 2px; }

.list-empty {
  text-align: center; padding: 60px 0;
  color: #9ca3af;
}
.list-empty p { font-size: 15px; margin-bottom: 6px; }
.list-empty span { font-size: 12px; }

/* 装备详情 */
.detail-card {
  background: #fff; border: 1px solid #e5e7eb;
  border-radius: 12px; overflow: hidden; position: sticky; top: 20px;
}
.detail-header {
  display: flex; align-items: center; gap: 14px;
  padding: 20px; border-bottom: 1px solid #f3f4f6;
}
.detail-img {
  width: 56px; height: 56px; border-radius: 10px;
  background: #f3f4f6; object-fit: cover;
}
.detail-name { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.detail-slot { font-size: 12px; color: #9ca3af; }

/* 收益条 */
.gain-bars { padding: 16px 20px; border-bottom: 1px solid #f3f4f6; }
.gain-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.gain-row:last-child { margin-bottom: 0; }
.gain-label { font-size: 12px; color: #6b7280; width: 52px; flex-shrink: 0; }
.gain-track {
  flex: 1; height: 6px; background: #f3f4f6;
  border-radius: 3px; overflow: hidden;
}
.gain-fill {
  height: 100%; border-radius: 3px; min-width: 4px;
  transition: width .4s cubic-bezier(.4,0,.2,1);
}
.gain-fill.damage.up   { background: #059669; }
.gain-fill.damage.down { background: #dc2626; }
.gain-fill.tank.up     { background: #1d4ed8; }
.gain-fill.tank.down   { background: #f59e0b; }
.gain-num { font-size: 12px; font-weight: 600; width: 54px; text-align: right; }
.gain-num.up   { color: #059669; }
.gain-num.down { color: #dc2626; }

/* 综合得分块 */
.score-block {
  padding: 16px 20px; text-align: center;
  background: #fafafa; border-bottom: 1px solid #f3f4f6;
}
.score-label { font-size: 11px; color: #9ca3af; letter-spacing: .5px; margin-bottom: 4px; }
.score-value { font-size: 36px; font-weight: 800; color: #111827; line-height: 1; margin-bottom: 6px; }
.score-formula { font-size: 11px; color: #9ca3af; }

/* 属性加成 */
.stat-bonuses { padding: 16px 20px; border-bottom: 1px solid #f3f4f6; }
.bonuses-title { font-size: 11px; font-weight: 600; letter-spacing: .8px; color: #9ca3af; text-transform: uppercase; margin-bottom: 10px; }
.bonus-list { display: flex; flex-wrap: wrap; gap: 6px; }
.bonus-item {
  display: flex; align-items: center; gap: 4px;
  background: #f3f4f6; border-radius: 5px; padding: 4px 10px;
}
.bonus-key { font-size: 11px; color: #6b7280; }
.bonus-val { font-size: 12px; font-weight: 600; color: #059669; }

/* 对比 */
.compare-block { padding: 14px 20px; border-bottom: 1px solid #f3f4f6; }
.compare-title { font-size: 11px; font-weight: 600; letter-spacing: .8px; color: #9ca3af; text-transform: uppercase; margin-bottom: 10px; }
.compare-row, .compare-item {
  display: grid; grid-template-columns: 1fr 1fr 1fr 1fr;
  font-size: 12px; padding: 5px 0; border-bottom: 1px solid #f9fafb;
}
.compare-col-label { color: #9ca3af; font-size: 11px; }
.c-label { color: #374151; }
.c-val { color: #374151; }
.c-delta { font-weight: 600; }
.c-delta.up { color: #059669; }
.c-delta.down { color: #dc2626; }

.adopt-btn {
  width: 100%; padding: 13px;
  background: #111827; color: #fff;
  border: none; font-size: 13px; font-weight: 500;
  cursor: pointer; transition: background .15s;
}
.adopt-btn:hover { background: #1f2937; }

/* 占位 */
.detail-placeholder {
  background: #fff; border: 1px solid #e5e7eb;
  border-radius: 12px; display: flex;
  align-items: center; justify-content: center;
  min-height: 300px;
  position: sticky; top: 20px;
}
.placeholder-inner { text-align: center; color: #d1d5db; }
.placeholder-icon { margin-bottom: 12px; }
.placeholder-inner span { font-size: 13px; }

/* 动画 */
@keyframes shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}
</style>
