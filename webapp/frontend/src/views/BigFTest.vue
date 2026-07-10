<script setup>
import { ref, computed, reactive, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import TestLayout from '../components/TestLayout.vue'
import { config } from '@/config.js'
import { useLanguageStore } from '@/stores/language.js'
import { useRecaptcha } from "@/toolbox/useRecaptcha.js";
import { useFocusMonitorStore } from "@/stores/focusMonitor.js";
import { useStudyStore } from "@/stores/studyStore.js";

const store = useLanguageStore();
const focusMonitorStore = useFocusMonitorStore();
const studyStore = useStudyStore();
const router = useRouter()
const API_BASE = config.API_BASE

const phase = ref('test')
const profileResult = ref(null)

const BIG5_MAX = 10
const SPQ_MAX = { vision: 18, hearing: 15, touch: 30, smell: 27, taste: 15 }

const big5ProfileData = computed(() => {
  if (!profileResult.value) return []
  const r = profileResult.value
  const labels = store.currentLanguage === 'it'
    ? { openness: 'Apertura', conscientiousness: 'Coscienziosità', emotionalStability: 'Stabilità emotiva', extroversion: 'Estroversione', agreeableness: 'Amichevolezza' }
    : { openness: 'Openness', conscientiousness: 'Conscientiousness', emotionalStability: 'Emot. Stability', extroversion: 'Extroversion', agreeableness: 'Agreeableness' }
  return ['openness', 'conscientiousness', 'emotionalStability', 'extroversion', 'agreeableness'].map(key => ({
    key,
    label: labels[key],
    pct: Math.round((r[key] / BIG5_MAX) * 100),
  }))
})

const spqProfileData = computed(() => {
  const spq = studyStore.userSpqResult
  if (!spq) return []
  const labels = store.currentLanguage === 'it'
    ? { vision: 'Vista', hearing: 'Udito', smell: 'Olfatto', taste: 'Gusto', touch: 'Tatto' }
    : { vision: 'Vision', hearing: 'Hearing', smell: 'Smell', taste: 'Taste', touch: 'Touch' }
  return ['vision', 'hearing', 'smell', 'taste', 'touch'].map(key => ({
    key,
    label: labels[key],
    pct: Math.round((spq[key] / SPQ_MAX[key]) * 100),
  }))
})

const { executeRecaptchaV3, showRecaptchaV2Dialog } = useRecaptcha(config.RECAPTCHA_V3_SITE_KEY, config.RECAPTCHA_V2_SITE_KEY, studyStore.testing)

watch(() => store.currentLanguage, async (newLang, oldLang) => {
  if (newLang !== oldLang) {
    try {
      const result = await getBigFQuestions();
      questions.q = result;
      buttonsState.state = [];
      addAttentionChecks();
      for (let i = 0; i < questions.q.length; i++) buttonsState.state.push(-1);
      currentQuestionRange.range = [0, 1, 2, 3, 4];
    } catch (error) {
      console.error("Error reloading questions:", error);
    }
  }
});

async function getBigFQuestions() {
  const response = await fetch(API_BASE + '/bigF?' + new URLSearchParams({ lang: store.currentLanguage, action: "get_questions" }), {
    method: 'GET', headers: { 'Content-Type': 'application/json' },
  });
  if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
  return await response.json();
}

function getRndInteger(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

const numAttentionChecks = 1;
const attentionChecksIndexes = reactive({ indexes: [] });
const questions = reactive({ q: [] });
const currentQuestionRange = reactive({ range: [0, 1, 2, 3, 4] });
const buttonsState = reactive({ state: [] });

const progress = computed(() => {
  const total = questions.q.length;
  if (!total) return 0;
  const countAnswer = buttonsState.state.filter(item => item !== -1).length;
  return (countAnswer * 100) / total;
});

const attentionChecks = computed(() => {
  return Array.from({ length: numAttentionChecks }, () => {
    const answerIndex = getRndInteger(0, store.translationAttentionChecks.fiveOptions.length - 1);
    return {
      text: store.translationAttentionChecks.text.replace("#", store.translationAttentionChecks.fiveOptions[answerIndex]),
      answer: answerIndex
    };
  });
});

function addAttentionChecks() {
  attentionChecksIndexes.indexes = [];
  for (let i = 0; i < numAttentionChecks; i++) {
    const page = 1;
    const pos = getRndInteger((page * 5) + i + 1, (page * 5) + i + 3);
    attentionChecksIndexes.indexes.push(pos);
    questions.q.splice(pos, 0, attentionChecks.value[i]);
  }
}

onMounted(() => {
  getBigFQuestions().then(result => {
    questions.q = result;
    addAttentionChecks();
    for (let i = 0; i < questions.q.length; i++) buttonsState.state.push(-1);
  });
});

function handleButtonClick(qIndex, bIndex) {
  buttonsState.state[qIndex] = buttonsState.state[qIndex] === bIndex ? -1 : bIndex;
  scrollToQuestion(qIndex + 1);
}

const checkNext = computed(() => {
  const visible = buttonsState.state.slice(currentQuestionRange.range[0], currentQuestionRange.range[4] + 1);
  return !visible.includes(-1);
});

function goNext() {
  studyStore.incrementStep();
  let hasAttentionCheck = false;
  if (currentQuestionRange.range[currentQuestionRange.range.length - 1] !== (questions.q.length - 1)) {
    if (currentQuestionRange.range.length > 5) currentQuestionRange.range.splice(0, 1);
    for (let i = 0; i < currentQuestionRange.range.length; i++) {
      currentQuestionRange.range[i] += 5;
      if (questions.q[currentQuestionRange.range[i]] && questions.q[currentQuestionRange.range[i]]["id"] == null) {
        hasAttentionCheck = true;
      }
    }
    if (hasAttentionCheck) {
      currentQuestionRange.range.push(currentQuestionRange.range[currentQuestionRange.range.length - 1] + 1);
    }
    scrollToQuestion(currentQuestionRange.range[0]);
  } else {
    sendBigFResults();
  }
}

function scrollToQuestion(index) {
  nextTick(() => {
    if (index <= currentQuestionRange.range[currentQuestionRange.range.length - 1]) {
      document.getElementById('question_' + index)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  });
}

async function sendBigFResultsCall() {
  let answers = buttonsState.state.map(x => x);
  let attentionChecksAnswered = [];

  for (let i = 0; i < attentionChecksIndexes.indexes.length; i++) {
    attentionChecksAnswered.push({
      userAnswer: buttonsState.state[attentionChecksIndexes.indexes[i]],
      correctAnswer: questions.q[attentionChecksIndexes.indexes[i]]["answer"]
    });
  }
  for (let i = 0; i < attentionChecksIndexes.indexes.length; i++) {
    answers.splice(attentionChecksIndexes.indexes[i] - i, 1);
  }

  const token = studyStore.testing ? 'TESTING_BYPASS' : await executeRecaptchaV3('submit_bigF');
  const data = { answers, attentionChecks: attentionChecksAnswered, recaptcha_version: 'v3', recaptcha_token: token };

  const response = await fetch(API_BASE + '/bigF?' + new URLSearchParams({ action: "send_results" }), {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data)
  });
  const result = await response.json();

  if (!studyStore.testing && result["requireV2Verification"]) {
    await showRecaptchaV2Dialog(focusMonitorStore.getFocusMonitor);
  }
  return result;
}

function skipAll() {
  buttonsState.state.fill(0);
  sendBigFResults();
}

const submitting = ref(false)

function sendBigFResults() {
  if (submitting.value) return
  submitting.value = true
  sendBigFResultsCall()
    .then(result => {
      if (result && !result.error) {
        studyStore.setUserBigfResult(result)
        profileResult.value = typeof result === 'string' ? JSON.parse(result) : result
        phase.value = 'profile'
        window.scrollTo({ top: 0, behavior: 'smooth' })
      } else {
        router.push("/activity_valuation")
      }
    })
    .catch(() => {
      sessionStorage.clear();
      router.push({ path: '/error', query: { message: "We are sorry. There was an error while saving your score." } });
    });
}

const questionLanguage = computed(() => store.currentLanguage === 'en' ? "textEnglish" : "textItalian");
</script>

<template>
  <!-- Combined profile screen shown after Big5 completion -->
  <div v-if="phase === 'profile'" class="profile-wrapper">
    <div class="profile-card">
      <h4 class="profile-title">
        {{ store.currentLanguage === 'it' ? 'Il tuo profilo sensoriale e di personalità' : 'Your Sensory & Personality Profile' }}
      </h4>
      <p class="profile-subtitle">
        {{ store.currentLanguage === 'it'
          ? 'Basato sulle tue risposte ai questionari SPQ e Big Five, ecco il tuo profilo di sensibilità sensoriale e di personalità. Tienilo a mente mentre valuti i candidati e i gruppi nelle prossime fasi dello studio.'
          : 'Based on your answers to the SPQ and Big Five questionnaires, here is your sensory sensitivity and personality profile. Keep it in mind as you evaluate candidates and groups in the upcoming phases of the study.' }}
      </p>

      <div class="profile-cols">
        <!-- Personality -->
        <div class="profile-section">
          <p class="profile-section-title">{{ store.currentLanguage === 'it' ? 'Personalità' : 'Personality traits' }}</p>
          <div v-for="item in big5ProfileData" :key="item.key" class="profile-trait-row">
            <span class="profile-trait-label">{{ item.label }}</span>
            <div class="profile-bar-bg">
              <div class="profile-bar-fill" :style="{ width: item.pct + '%' }"></div>
            </div>
          </div>
        </div>

        <!-- Sensory -->
        <div class="profile-section">
          <p class="profile-section-title">{{ store.currentLanguage === 'it' ? 'Sensibilità sensoriali' : 'Sensory sensitivities' }}</p>
          <div v-for="item in spqProfileData" :key="item.key" class="profile-trait-row">
            <span class="profile-trait-label">{{ item.label }}</span>
            <div class="profile-bar-bg">
              <div class="profile-bar-fill" :style="{ width: item.pct + '%' }"></div>
            </div>
          </div>
        </div>
      </div>

      <div class="d-flex justify-content-end mt-4">
        <button class="btn btn-primary px-4" @click="router.push('/activity_valuation')">
          {{ store.currentLanguage === 'it' ? 'Continua →' : 'Continue →' }}
        </button>
      </div>
    </div>
  </div>

  <TestLayout v-else-if="phase === 'test'" :progress="progress" :title="store.translationHome.bigF.title">
    <div v-for="questionIndex in currentQuestionRange.range" :key="questionIndex">
      <div class="card shadow-sm mt-1 mb-4" style="scroll-margin-top: 75px;" :id="'question_' + questionIndex">
        <div class="card-body text-center">
          <div v-if="questions.q[questionIndex] && questions.q[questionIndex]['id'] != null">
            <h5 class="card-title mb-4">{{ questions.q[questionIndex]['id'] }}. {{ questions.q[questionIndex][questionLanguage] }}</h5>
          </div>
          <div v-else-if="questions.q[questionIndex]">
            <h5 class="card-title mb-4">{{ questions.q[questionIndex]['text'] }}</h5>
          </div>
          <div class="d-flex justify-content-center align-items-center gap-2 flex-wrap">
            <button
              v-for="(buttonText, buttonIndex) in store.translationBigF['buttons']"
              :key="buttonIndex"
              class="btn"
              @click="handleButtonClick(questionIndex, buttonIndex)"
              :style="{
                color: buttonsState.state[questionIndex] === buttonIndex ? 'white' : '#007ACC',
                backgroundColor: buttonsState.state[questionIndex] === buttonIndex ? '#007ACC' : 'white',
                borderColor: '#007ACC'
              }"
            >{{ buttonText }}</button>
          </div>
        </div>
      </div>
    </div>

    <div class="d-flex justify-content-between mt-3">
      <button
        v-if="studyStore.testing"
        class="btn btn-outline-secondary px-3"
        @click="skipAll()"
      >
        Skip (visibile solo per sviluppatori)
      </button>
      <button
        class="btn btn-primary px-4 ms-auto"
        @click="goNext()"
        :disabled="!checkNext || submitting"
        :class="{ disabled: !checkNext || submitting }"
        :style="{ backgroundColor: checkNext && !submitting ? '#33a474' : '#007ACC' }"
      >
        {{ store.currentLanguage === 'it' ? "Prossimo" : "Next" }}
      </button>
    </div>
  </TestLayout>
</template>

<style scoped>
.card { border-radius: 15px; border: none; margin: 0.5rem; padding: 1rem; }
.card-body { padding: 1rem; }
.card-title { font-size: 1rem; }
button.btn { min-width: 80px; padding: 0.5rem 0.75rem; font-size: 0.9rem; border-radius: 8px; transition: all 0.2s ease-in-out; }

@media (max-width: 768px) {
  .card { margin: 0.4rem; padding: 0.8rem; }
  button.btn { font-size: 0.8rem; min-width: 70px; padding: 0.4rem 0.6rem; }
}

/* Combined profile screen */
.profile-wrapper {
  display: flex;
  justify-content: center;
  padding: 2rem 1rem;
}
.profile-card {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  max-width: 760px;
  width: 100%;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  border: 1px solid #e9ecef;
}
.profile-title {
  text-align: center;
  color: #007ACC;
  font-weight: 600;
  margin-bottom: 0.75rem;
  font-size: 1.2rem;
}
.profile-subtitle {
  font-size: 0.9rem;
  color: #374151;
  margin-bottom: 1.5rem;
  text-align: center;
}
.profile-cols {
  display: flex;
  gap: 2rem;
  flex-wrap: wrap;
}
.profile-section {
  flex: 1;
  min-width: 240px;
}
.profile-section-title {
  font-size: 0.8rem;
  font-weight: 700;
  color: #007ACC;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 0.5rem;
}
.profile-trait-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.35rem;
}
.profile-trait-label {
  font-size: 0.78rem;
  color: #6b7280;
  width: 120px;
  flex-shrink: 0;
}
.profile-bar-bg {
  flex: 1;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}
.profile-bar-fill {
  height: 100%;
  border-radius: 4px;
  background-color: #008FFB;
  transition: width 0.3s;
}
</style>
