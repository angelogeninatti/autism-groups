import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useStudyStore = defineStore('study', () => {
  const testing = ref(false)
  const currentStep = ref(0)
  const visualization = ref('heatmap') // 'heatmap' | 'bars' | 'cards'
  const userSpqResult = ref(null)   // {total, vision, hearing, smell, taste, touch}
  const userBigfResult = ref(null)  // {total, agreeableness, conscientiousness, emotionalStability, extroversion, openness}

  function setTesting(val) { testing.value = val }
  function incrementStep() { currentStep.value++ }
  function setVisualization(val) { visualization.value = val }
  function setUserSpqResult(val) {
    try { userSpqResult.value = typeof val === 'string' ? JSON.parse(val) : val }
    catch { userSpqResult.value = val }
  }
  function setUserBigfResult(val) {
    try { userBigfResult.value = typeof val === 'string' ? JSON.parse(val) : val }
    catch { userBigfResult.value = val }
  }

  return { testing, currentStep, visualization, userSpqResult, userBigfResult, setTesting, incrementStep, setVisualization, setUserSpqResult, setUserBigfResult }
})
