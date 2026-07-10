<script setup>
import { computed, onMounted, onBeforeUnmount, watch, ref } from 'vue'
import ApexCharts from 'apexcharts'
import { useLanguageStore } from '@/stores/language.js'

const props = defineProps({
  group: { type: Object, required: true },
  randomUsers: { type: Array, required: true }
})

const store = useLanguageStore()
const chartEl = ref(null)
let chart = null

const senses = ['vision', 'hearing', 'smell', 'taste', 'touch']
const senseLabels = {
  it: { vision: 'Vista', hearing: 'Udito', smell: 'Olfatto', taste: 'Gusto', touch: 'Tatto' },
  en: { vision: 'Sight', hearing: 'Hearing', smell: 'Smell', taste: 'Taste', touch: 'Touch' }
}

// Filter randomUsers to only those in this group
const groupUsers = computed(() => {
  const ids = new Set(props.group.users || [])
  return props.randomUsers.filter(u => ids.has(u.id))
})

// Per-user scores for each sense, normalised 0-100 (max SPQ per sense = 21)
const perUserData = computed(() =>
  groupUsers.value.map(u => {
    const spq = u.SPQResult || {}
    return senses.map(s => Math.round(((spq[s] || 0) / 21) * 100))
  })
)

const groupAvg = computed(() =>
  senses.map((s, i) => {
    const sum = perUserData.value.reduce((acc, u) => acc + u[i], 0)
    return Math.round(sum / Math.max(1, perUserData.value.length))
  })
)

const labels = computed(() => {
  const l = senseLabels[store.currentLanguage] || senseLabels.en
  return senses.map(s => l[s])
})

function buildOptions() {
  const series = [
    { name: store.currentLanguage === 'it' ? 'Media gruppo' : 'Group avg', data: groupAvg.value },
    ...perUserData.value.map((d, i) => ({ name: `P${i + 1}`, data: d }))
  ]
  return {
    chart: { type: 'radar', height: 320, toolbar: { show: false }, animations: { enabled: false } },
    series,
    xaxis: { categories: labels.value },
    yaxis: { min: 0, max: 100, tickAmount: 4, labels: { show: false } },
    stroke: { width: [3, ...perUserData.value.map(() => 1)], dashArray: [0, ...perUserData.value.map(() => 4)] },
    fill: { opacity: [0.2, ...perUserData.value.map(() => 0)] },
    markers: { size: 3 },
    legend: { show: true, position: 'bottom' },
    title: {
      text: store.currentLanguage === 'it' ? 'Sensibilità agli stimoli sensoriali del gruppo (0–100)' : 'Group sensitivity to sensory stimuli (0–100)',
      align: 'center', style: { fontSize: '13px' }
    },
    colors: ['#007ACC', '#33a474', '#f59e0b', '#ef4444']
  }
}

onMounted(() => {
  chart = new ApexCharts(chartEl.value, buildOptions())
  chart.render()
})

onBeforeUnmount(() => { chart?.destroy() })

watch([() => props.group, () => store.currentLanguage], () => {
  chart?.updateOptions(buildOptions())
})
</script>

<template>
  <div ref="chartEl"></div>
</template>
