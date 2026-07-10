<script setup>
import { ref, computed, reactive, onMounted, nextTick } from 'vue'
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
const { executeRecaptchaV3, showRecaptchaV2Dialog } = useRecaptcha(config.RECAPTCHA_V3_SITE_KEY, config.RECAPTCHA_V2_SITE_KEY, studyStore.testing)

async function getSPQQuestions() {
  const response = await fetch(API_BASE + '/spq?' + new URLSearchParams({ action: "get_questions" }), {
    method: 'GET', headers: { 'Content-Type': 'application/json' },
  });
  if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
  return await response.json();
}

function shuffleArray(array) {
  for (let i = array.length - 1; i >= 1; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
  return array;
}

function getRndInteger(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

const numAttentionChecks = 2;

const attentionChecksIndexes = reactive({ indexes: [] })
const questions = reactive({ q: [] });
const pageIndexes = reactive({ indexes: shuffleArray([1, 2, 3, 4, 5]).slice(0, 2).sort() });
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
    const answerIndex = getRndInteger(0, store.translationAttentionChecks.fourOptions.length - 1);
    return {
      text: store.translationAttentionChecks.text.replace("#", store.translationAttentionChecks.fourOptions[answerIndex]),
      answer: answerIndex
    };
  });
});

function addAttentionChecks() {
  for (let i = 0; i < numAttentionChecks; i++) {
    const page = pageIndexes.indexes[i];
    const pos = getRndInteger((page * 5) + i + 1, (page * 5) + i + 3);
    attentionChecksIndexes.indexes.push(pos);
    questions.q.splice(pos, 0, attentionChecks.value[i]);
  }
}

onMounted(() => {
  getSPQQuestions().then(result => {
    questions.q = result;
    addAttentionChecks();
    for (let i = 0; i < questions.q.length; i++) { buttonsState.state.push(-1); }
  });
})

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
    sendSPQResults();
  }
}

function scrollToQuestion(index) {
  nextTick(() => {
    if (index <= currentQuestionRange.range[currentQuestionRange.range.length - 1]) {
      document.getElementById('question_' + index)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  });
}

async function sendSPQResultsCall() {
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

  const token = studyStore.testing ? 'TESTING_BYPASS' : await executeRecaptchaV3('submit_spq');
  const data = { answers, attentionChecks: attentionChecksAnswered, recaptcha_version: 'v3', recaptcha_token: token };

  const response = await fetch(API_BASE + '/spq?' + new URLSearchParams({ action: "send_results" }), {
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
  sendSPQResults();
}

const submitting = ref(false)

function sendSPQResults() {
  if (submitting.value) return
  submitting.value = true
  sendSPQResultsCall()
    .then(result => {
      if (result && result.error) {
        sessionStorage.clear();
        router.push({ path: '/error', query: { message: "We are sorry. There was an error while saving your score." } });
      } else {
        studyStore.setUserSpqResult(result)
        router.push("/bigF")
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
  <TestLayout :progress="progress" :title="store.translationHome.spq.title">
    <div v-for="questionIndex in currentQuestionRange.range" :key="questionIndex">
      <div class="card shadow-sm mt-1 mb-4" style="scroll-margin-top: 75px;" :id="'question_' + questionIndex">
        <div class="card-body text-center">
          <div v-if="questions.q[questionIndex] && questions.q[questionIndex]['id'] != null">
            <h5 class="card-title mb-4">{{ questions.q[questionIndex]['id'] }}. {{ questions.q[questionIndex][questionLanguage] }}</h5>
          </div>
          <div v-else-if="questions.q[questionIndex]">
            <h5 class="card-title mb-4">{{ questions.q[questionIndex]['text'] }}</h5>
          </div>
          <div class="d-flex justify-content-center gap-2 flex-wrap">
            <button
              v-for="(buttonText, buttonIndex) in store.translationSPQ['buttons']"
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
</style>
