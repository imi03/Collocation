<template>
  <div class="recommend-visual">
    <!-- 页头 -->
    <header class="page-header">
      <div>
        <h1 class="page-title">收益可视化</h1>
        <span class="page-subtitle">直观呈现各养成方案的伤害与坦度变化率及综合得分</span>
      </div>
      <div class="header-controls">
        <div class="type-switch">
          <button :class="{ active: viewType === 'equipment' }" @click="viewType = 'equipment'; refresh()">装备</button>
          <button :class="{ active: viewType === 'trait' }"    @click="viewType = 'trait'; refresh()">特质</button>
        </div>
      </div>
    </header>

    <!-- 概览卡片 -->
    <div class="summary-cards">
      <div class="summary-card" v-for="card in summaryCards" :key="card.label">
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value" :class="card.color">{{ card.value }}</div>
        <div class="summary-sub">{{ card.sub }}</div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="charts-grid">
      <!-- 图表1：柱状图 - 伤害 vs 坦度提升率对比 -->
      <div class="chart-card chart-wide">
        <div class="chart-header">
          <span class="chart-title">各方案收益对比</span>
          <div class="chart-legend">
            <span class="legend-item damage">伤害提升率</span>
            <span class="legend-item tank">坦度提升率</span>
          </div>
        </div>
        <div ref="barChartEl" class="chart-body"></div>
      </div>

      <!-- 图表2：雷达图 - 当前面板 vs 推荐后面板 -->
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">面板雷达对比</span>
          <span class="chart-sub">当前 vs 推荐替换后</span>
        </div>
        <div ref="radarChartEl" class="chart-body"></div>
      </div>

      <!-- 图表3：散点图 - 伤害 vs 坦度权衡 -->
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">伤害·坦度权衡</span>
          <span class="chart-sub">气泡大小代表综合得分</span>
        </div>
        <div ref="scatterChartEl" class="chart-body"></div>
      </div>
    </div>

    <!-- 得分排行榜 -->
    <div class="ranking-section">
      <div class="ranking-header">
        <h2 class="ranking-title">推荐排行</h2>
        <span class="ranking-hint">综合得分 = 伤害提升 × 1分/1% + 坦度提升 × 1分/2%</span>
      </div>

      <div class="ranking-list">
        <div
          class="ranking-item"
          v-for="(item, idx) in rankedItems"
          :key="item.id"
          @click="highlightItem(item)"
        >
          <div class="rank-badge" :class="`r${idx < 3 ? idx + 1 : ''}`">{{ idx + 1 }}</div>
          <div class="rank-thumb">
            <img :src="item.imageUrl || '/placeholder-equip.png'" :alt="item.name" />
          </div>
          <div class="rank-info">
            <div class="rank-name">{{ item.name }}</div>
            <div class="rank-slot">{{ item.slot }}</div>
          </div>
          <div class="rank-metrics">
            <span class="rm damage" :class="item.damageDeltaPct >= 0 ? 'up' : 'down'">
              ⚔ {{ formatDelta(item.damageDeltaPct) }}
            </span>
            <span class="rm tank" :class="item.tankDeltaPct >= 0 ? 'up' : 'down'">
              🛡 {{ formatDelta(item.tankDeltaPct) }}
            </span>
          </div>
          <div class="rank-score-bar-wrap">
            <div class="rank-score-bar">
              <div
                class="rank-score-fill"
                :style="{ width: (item.totalScore / maxScore * 100) + '%' }"
              ></div>
            </div>
            <span class="rank-score-num">{{ item.totalScore?.toFixed(1) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'

const viewType = ref('equipment')
const recommendations = ref([])
const barChartEl    = ref(null)
const radarChartEl  = ref(null)
const scatterChartEl= ref(null)
let barChart = null, radarChart = null, scatterChart = null

// 示例数据（实际调接口替换）
const mockData = [
  { id: 1, name: '天煞轰雷斧', slot: '武器', imageUrl: '', damageDeltaPct: 3.24, tankDeltaPct: -0.5,  totalScore: 3.0,  statBonuses: { atk: 180, critRate: 30 } },
  { id: 2, name: '裂魂破·剑',  slot: '武器', imageUrl: '', damageDeltaPct: 2.88, tankDeltaPct: 0.2,   totalScore: 2.98, statBonuses: { atk: 150, pierce: 60 } },
  { id: 3, name: '紫霞战甲',   slot: '胸甲', imageUrl: '', damageDeltaPct: 0.5,  tankDeltaPct: 4.2,   totalScore: 2.6,  statBonuses: { def: 200, hp: 800 } },
  { id: 4, name: '苍龙战靴',   slot: '腿甲', imageUrl: '', damageDeltaPct: 1.2,  tankDeltaPct: 2.4,   totalScore: 2.4,  statBonuses: { atk: 60, def: 80 } },
  { id: 5, name: '流光护盾',   slot: '饰品', imageUrl: '', damageDeltaPct: 0.8,  tankDeltaPct: 3.0,   totalScore: 2.3,  statBonuses: { hp: 1200 } },
  { id: 6, name: '玄铁头盔',   slot: '头盔', imageUrl: '', damageDeltaPct: -0.3, tankDeltaPct: 5.6,   totalScore: 2.5,  statBonuses: { def: 160, hp: 600 } },
]

const rankedItems = computed(() =>
  [...(recommendations.value.length ? recommendations.value : mockData)]
    .sort((a, b) => b.totalScore - a.totalScore)
)

const maxScore = computed(() =>
  Math.max(...rankedItems.value.map(i => i.totalScore), 1)
)

const summaryCards = computed(() => {
  const list = rankedItems.value
  const best = list[0]
  const avgDmg = list.reduce((s, i) => s + i.damageDeltaPct, 0) / list.length
  return [
    { label: '最高综合得分', value: best?.totalScore?.toFixed(1) ?? '—', sub: best?.name ?? '—', color: 'gold' },
    { label: '最大伤害提升', value: formatDelta(Math.max(...list.map(i => i.damageDeltaPct))), sub: '全槽位最优', color: 'up' },
    { label: '最大坦度提升', value: formatDelta(Math.max(...list.map(i => i.tankDeltaPct))), sub: '全槽位最优', color: 'blue' },
    { label: '平均伤害提升', value: formatDelta(avgDmg), sub: `共 ${list.length} 件候选`, color: 'neutral' },
  ]
})

function formatDelta(v) {
  if (v == null) return '—'
  return (v >= 0 ? '+' : '') + v.toFixed(2) + '%'
}

// ─── ECharts 初始化 ───
function initCharts() {
  const list = rankedItems.value.slice(0, 8)

  // 柱状图
  if (barChartEl.value) {
    barChart = echarts.init(barChartEl.value)
    barChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { top: 16, bottom: 40, left: 16, right: 16, containLabel: true },
      xAxis: {
        type: 'category',
        data: list.map(i => i.name.length > 5 ? i.name.slice(0, 5) + '…' : i.name),
        axisLabel: { fontSize: 11, color: '#6b7280' },
        axisLine: { lineStyle: { color: '#e5e7eb' } },
      },
      yAxis: {
        type: 'value',
        axisLabel: { formatter: v => v + '%', fontSize: 11, color: '#6b7280' },
        splitLine: { lineStyle: { color: '#f3f4f6' } },
        axisLine: { show: false },
      },
      series: [
        {
          name: '伤害提升率',
          type: 'bar',
          barMaxWidth: 28,
          data: list.map(i => ({
            value: i.damageDeltaPct,
            itemStyle: { color: i.damageDeltaPct >= 0 ? '#059669' : '#dc2626', borderRadius: [4, 4, 0, 0] }
          })),
        },
        {
          name: '坦度提升率',
          type: 'bar',
          barMaxWidth: 28,
          data: list.map(i => ({
            value: i.tankDeltaPct,
            itemStyle: { color: i.tankDeltaPct >= 0 ? '#1d4ed8' : '#f59e0b', borderRadius: [4, 4, 0, 0] }
          })),
        }
      ]
    })
  }

  // 雷达图
  if (radarChartEl.value) {
    radarChart = echarts.init(radarChartEl.value)
    radarChart.setOption({
      tooltip: {},
      radar: {
        indicator: [
          { name: '攻击', max: 3000 },
          { name: '防御', max: 2000 },
          { name: '气血', max: 20000 },
          { name: '会心', max: 800 },
          { name: '破防', max: 2000 },
          { name: '命中', max: 700 },
        ],
        axisName: { color: '#6b7280', fontSize: 11 },
        splitArea: { areaStyle: { color: ['#f9fafb', '#fff'] } },
        splitLine: { lineStyle: { color: '#e5e7eb' } },
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        radius: '68%',
      },
      series: [{
        type: 'radar',
        data: [
          {
            value: [1500, 900, 12000, 400, 800, 400],
            name: '当前面板',
            lineStyle: { color: '#9ca3af', width: 1.5 },
            itemStyle: { color: '#9ca3af' },
            areaStyle: { color: 'rgba(156,163,175,0.1)' },
          },
          {
            value: [1680, 900, 12000, 430, 860, 400],
            name: '替换后',
            lineStyle: { color: '#1d4ed8', width: 2 },
            itemStyle: { color: '#1d4ed8' },
            areaStyle: { color: 'rgba(29,78,216,0.08)' },
          }
        ]
      }]
    })
  }

  // 散点图
  if (scatterChartEl.value) {
    scatterChart = echarts.init(scatterChartEl.value)
    scatterChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: p => `${p.data[3]}<br/>伤害 ${formatDelta(p.data[0])}<br/>坦度 ${formatDelta(p.data[1])}<br/>得分 ${p.data[2]?.toFixed(1)}`
      },
      grid: { top: 16, bottom: 40, left: 16, right: 16, containLabel: true },
      xAxis: {
        name: '伤害提升率(%)', nameTextStyle: { color: '#9ca3af', fontSize: 11 },
        axisLabel: { fontSize: 11, color: '#6b7280', formatter: v => v + '%' },
        splitLine: { lineStyle: { color: '#f3f4f6' } },
        axisLine: { lineStyle: { color: '#e5e7eb' } },
      },
      yAxis: {
        name: '坦度提升率(%)', nameTextStyle: { color: '#9ca3af', fontSize: 11 },
        axisLabel: { fontSize: 11, color: '#6b7280', formatter: v => v + '%' },
        splitLine: { lineStyle: { color: '#f3f4f6' } },
        axisLine: { show: false },
      },
      series: [{
        type: 'scatter',
        data: list.map(i => [i.damageDeltaPct, i.tankDeltaPct, i.totalScore, i.name]),
        symbolSize: d => Math.max(d[2] * 8, 10),
        itemStyle: {
          color: p => {
            const d = p.data[0], t = p.data[1]
            if (d > 0 && t > 0) return '#059669'
            if (d > 0 && t < 0) return '#f59e0b'
            if (d < 0 && t > 0) return '#1d4ed8'
            return '#9ca3af'
          },
          opacity: 0.8,
        },
        label: {
          show: true,
          formatter: p => p.data[3]?.length > 4 ? p.data[3].slice(0, 4) + '…' : p.data[3],
          position: 'top', fontSize: 10, color: '#374151',
        }
      }]
    })
  }
}

function refresh() { /* 实际调接口后重绘 */ }

function highlightItem(item) {
  if (!barChart) return
  const list = rankedItems.value.slice(0, 8)
  const idx = list.findIndex(i => i.id === item.id)
  if (idx >= 0) barChart.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex: idx })
}

const resizeObserver = ref(null)

onMounted(async () => {
  try {
    const { data } = await axios.get('/api/equipment/recommend')
    if (data.code === 200) recommendations.value = data.data
  } catch {}
  setTimeout(initCharts, 100)

  resizeObserver.value = new ResizeObserver(() => {
    barChart?.resize(); radarChart?.resize(); scatterChart?.resize()
  })
  if (barChartEl.value) resizeObserver.value.observe(barChartEl.value.parentElement)
})

onBeforeUnmount(() => {
  barChart?.dispose(); radarChart?.dispose(); scatterChart?.dispose()
  resizeObserver.value?.disconnect()
})
</script>

<style scoped>
* { box-sizing: border-box; margin: 0; padding: 0; }

.recommend-visual {
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

.type-switch {
  display: flex; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 3px;
}
.type-switch button {
  padding: 6px 18px; border: none; border-radius: 6px;
  font-size: 13px; cursor: pointer; background: transparent;
  color: #6b7280; transition: all .15s;
}
.type-switch button.active { background: #111827; color: #fff; }

/* 概览卡片 */
.summary-cards {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 24px;
}
.summary-card {
  background: #fff; border: 1px solid #e5e7eb; border-radius: 10px;
  padding: 16px 20px;
}
.summary-label { font-size: 11px; color: #9ca3af; margin-bottom: 8px; letter-spacing: .3px; }
.summary-value { font-size: 22px; font-weight: 700; line-height: 1; margin-bottom: 4px; }
.summary-value.gold    { color: #d97706; }
.summary-value.up      { color: #059669; }
.summary-value.blue    { color: #1d4ed8; }
.summary-value.neutral { color: #374151; }
.summary-sub { font-size: 12px; color: #9ca3af; }

/* 图表区 */
.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: auto auto;
  gap: 16px;
  margin-bottom: 28px;
}
.chart-card {
  background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden;
}
.chart-wide { grid-column: 1 / -1; }
.chart-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid #f3f4f6;
}
.chart-title { font-size: 14px; font-weight: 600; color: #111827; }
.chart-sub { font-size: 12px; color: #9ca3af; }
.chart-legend { display: flex; gap: 16px; }
.legend-item {
  font-size: 12px; color: #6b7280;
  display: flex; align-items: center; gap: 6px;
}
.legend-item::before {
  content: ''; width: 10px; height: 10px; border-radius: 2px; flex-shrink: 0;
}
.legend-item.damage::before { background: #059669; }
.legend-item.tank::before   { background: #1d4ed8; }

.chart-body { height: 280px; }
.chart-wide .chart-body { height: 240px; }

/* 排行榜 */
.ranking-section { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden; }
.ranking-header {
  display: flex; align-items: baseline; justify-content: space-between;
  padding: 18px 24px; border-bottom: 1px solid #f3f4f6;
}
.ranking-title { font-size: 15px; font-weight: 600; }
.ranking-hint  { font-size: 12px; color: #9ca3af; }

.ranking-list { padding: 8px 16px; }
.ranking-item {
  display: flex; align-items: center; gap: 14px;
  padding: 12px 8px; border-radius: 8px; cursor: pointer;
  transition: background .15s; border-bottom: 1px solid #f9fafb;
}
.ranking-item:last-child { border-bottom: none; }
.ranking-item:hover { background: #f9fafb; }

.rank-badge {
  width: 28px; height: 28px; border-radius: 50%;
  background: #f3f4f6; color: #9ca3af;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; flex-shrink: 0;
}
.rank-badge.r1 { background: #fef3c7; color: #d97706; }
.rank-badge.r2 { background: #f3f4f6; color: #6b7280; }
.rank-badge.r3 { background: #fff7ed; color: #b45309; }

.rank-thumb {
  width: 40px; height: 40px; border-radius: 8px;
  background: #f3f4f6; overflow: hidden; flex-shrink: 0;
}
.rank-thumb img { width: 100%; height: 100%; object-fit: cover; }

.rank-info { flex: 1; }
.rank-name { font-size: 14px; font-weight: 500; margin-bottom: 3px; }
.rank-slot { font-size: 11px; color: #9ca3af; }

.rank-metrics { display: flex; gap: 12px; }
.rm { font-size: 12px; font-weight: 500; }
.rm.up   { color: #059669; }
.rm.down { color: #dc2626; }

.rank-score-bar-wrap {
  display: flex; align-items: center; gap: 10px; width: 180px;
}
.rank-score-bar {
  flex: 1; height: 6px; background: #f3f4f6; border-radius: 3px; overflow: hidden;
}
.rank-score-fill {
  height: 100%; background: linear-gradient(90deg, #1d4ed8, #3b82f6);
  border-radius: 3px; transition: width .6s cubic-bezier(.4,0,.2,1);
}
.rank-score-num { font-size: 13px; font-weight: 700; color: #111827; min-width: 36px; text-align: right; }
</style>
