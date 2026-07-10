<script setup>
import { ref, reactive, computed, watch, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { config } from '@/config.js';
import { useLanguageStore } from '@/stores/language.js';
import { useStudyStore } from '@/stores/studyStore.js';
import { useRecaptcha } from "@/toolbox/useRecaptcha.js";
import Activity from "@/components/Activity.vue";
import { useFocusMonitorStore } from "@/stores/focusMonitor.js";

const store = useLanguageStore();
const studyStore = useStudyStore();
const focusMonitorStore = useFocusMonitorStore();
const router = useRouter();
const API_BASE = config.API_BASE;
const { executeRecaptchaV3, showRecaptchaV2Dialog } = useRecaptcha(config.RECAPTCHA_V3_SITE_KEY, config.RECAPTCHA_V2_SITE_KEY, studyStore.testing);

const props = defineProps({ activity: [Object, Array] });

const questionsPerActivity = ref([]);
const questions = reactive({ q: [] });
const activities = reactive({ a: [] });
const currentActivity = ref(0);
const buttonsState = reactive({ state: [] });
const attentionButtonsState = reactive({ state: [] });

const activitiesWithAttentionChecks = reactive({ indexes: [] });
const attentionChecksIndexes = reactive({ indexes: {} });
const attentionChecks = reactive({ checks: [] });
const currentQuestionRange = reactive({ range: [0,1,2,3,4,5,6] });

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

function highlightQuestions(question) {
  const words = ["udito", "olfatto", "vista", "gusto", "tatto", "hearing", "smell", "sight", "taste", "touch"];
  const regex = new RegExp(`(${words.join('|')})`, 'i');
  return question.replace(regex, '<strong>$1</strong>');
}

async function getActivityQuestions() {
  const response = await fetch(API_BASE + '/activity?' + new URLSearchParams({ action: "get_questions" }), {
    method: 'GET', headers: { 'Content-Type': 'application/json' },
  });
  if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
  return await response.json();
}

async function getActivities() {
  if (!props.activity) return [];
  return Array.isArray(props.activity) ? props.activity : [props.activity];
}

function generateAttentionChecks() {
  attentionChecks.checks = [];
  for (let i = 0; i < activities.a.length; i++) {
    const answer = getRndInteger(1, 5);
    attentionChecks.checks.push({
      textItalian: store.getSpecificAttentionChecks("it").text.replace('#', answer),
      textEnglish: store.getSpecificAttentionChecks("en").text.replace('#', answer),
      answer: answer - 1
    });
  }
}

onMounted(async () => {
  const resultActivities = await getActivities();
  activities.a = resultActivities;
  const resultQuestions = await getActivityQuestions();
  questions.q = resultQuestions;
  questionsPerActivity.value = resultActivities.map(() => resultQuestions.length);
  const totalQuestions = questionsPerActivity.value.reduce((sum, n) => sum + n, 0);
  buttonsState.state = Array(totalQuestions).fill(-1);

  activitiesWithAttentionChecks.indexes = resultActivities.map((_, i) => i);
  attentionButtonsState.state = Array(resultActivities.length).fill(-1);

  generateAttentionChecks();
  if (resultQuestions.length >= 3) {
    activitiesWithAttentionChecks.indexes.forEach(ai => {
      attentionChecksIndexes.indexes[ai] = getRndInteger(1, resultQuestions.length - 2);
    });
  }
});

function getQuestionGlobalIndex(activityIndex, questionIndex) {
  let offset = 0;
  for (let i = 0; i < activityIndex; i++) offset += questionsPerActivity.value[i];
  return offset + questionIndex;
}

function handleButtonClick(activityIndex, questionIndex, buttonIndex) {
  const idx = getQuestionGlobalIndex(activityIndex, questionIndex);
  buttonsState.state[idx] = buttonsState.state[idx] === buttonIndex ? -1 : buttonIndex;
}

function handleAttentionClick(qIndex, attentionIndex, bIndex) {
  attentionButtonsState.state[attentionIndex] = attentionButtonsState.state[attentionIndex] === bIndex ? -1 : bIndex;
}

function buildActivityAnswers() {
  return activities.a.map((act, activityIndex) => {
    if (!act || !act.id) return null;
    const numQ = questionsPerActivity.value[activityIndex];
    const start = getQuestionGlobalIndex(activityIndex, 0);
    return { activityId: act.id, values: buttonsState.state.slice(start, start + numQ) };
  }).filter(Boolean);
}

function buildAttentionAnswers() {
  return attentionChecks.checks.map((check, i) => ({
    correctAnswer: check.answer,
    userAnswer: attentionButtonsState.state[i]
  }));
}

async function sendActivityResultsCall(answers, attentionAnswers) {
  const token = studyStore.testing ? 'TESTING_BYPASS' : await executeRecaptchaV3('submit_activity');
  const data = { answers, attentionChecks: attentionAnswers, recaptcha_version: 'v3', recaptcha_token: token };

  const response = await fetch(API_BASE + '/activity?' + new URLSearchParams({ action: "send_results" }), {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data)
  });
  const result = await response.json();

  if (!studyStore.testing && result.requireV2Verification) {
    await showRecaptchaV2Dialog(focusMonitorStore.getFocusMonitor);
  }
  return result;
}

const emit = defineEmits(['finished', 'progress']);

const progress = computed(() => {
  const total = buttonsState.state.length;
  if (!total) return 0;
  const answered = buttonsState.state.filter(v => v !== -1).length;
  return (answered * 100) / total;
});

watch(progress, val => emit('progress', val));

function skipAll() {
  buttonsState.state.fill(0);
  attentionChecks.checks.forEach((check, i) => {
    attentionButtonsState.state[i] = check.answer;
  });
  logQuestions();
}

function logQuestions() {
  studyStore.incrementStep();
  const answers = buildActivityAnswers();
  const attentionAnswers = buildAttentionAnswers();
  sendActivityResultsCall(answers, attentionAnswers)
    .then(result => {
      if (result && result.error) {
        sessionStorage.clear();
        router.push({ path: '/error', query: { message: 'We are sorry. There was an error while saving your score' } });
      } else {
        emit('finished');
      }
    })
    .catch(() => {
      sessionStorage.clear();
      router.push({ path: '/error', query: { message: 'We are sorry. There was an error while saving your score' } });
    });
}

const questionLanguage = computed(() => store.currentLanguage === 'it' ? "textItalian" : "textEnglish");

const senseNAVariant = ref(false);

const senseKeyMap = {
  vista: 'visionScore', sight: 'visionScore',
  udito: 'hearingScore', hearing: 'hearingScore',
  olfatto: 'smellScore', smell: 'smellScore',
  gusto: 'tasteScore', taste: 'tasteScore',
  tatto: 'touchScore', touch: 'touchScore',
};

function getActivitySpq() {
  const act = Array.isArray(props.activity) ? props.activity[0] : props.activity;
  return act?.spqEvaluation || null;
}

function isSenseQuestion(q) {
  const text = (q[questionLanguage.value] || '').toLowerCase();
  return Object.keys(senseKeyMap).some(k => text.includes(k));
}

function isSenseZeroScore(q) {
  const text = (q[questionLanguage.value] || '').toLowerCase();
  const spq = getActivitySpq();
  if (!spq) return false;
  for (const [keyword, scoreKey] of Object.entries(senseKeyMap)) {
    if (text.includes(keyword)) return Number(spq[scoreKey]) === 0;
  }
  return false;
}

const hasAnyZeroSense = computed(() => {
  const spq = getActivitySpq();
  if (!spq) return false;
  return Object.values(senseKeyMap).some(key => Number(spq[key]) === 0);
});

const senseOrderIt = ["vista", "udito", "olfatto", "gusto", "tatto"];
const senseOrderEn = ["sight", "hearing", "smell", "taste", "touch"];

const orderedQuestions = computed(() => {
  const senseOrder = store.currentLanguage === 'it' ? senseOrderIt : senseOrderEn;
  const overall = [];
  const textDesc = [];
  const sensory = [];
  const rest = [];

  questions.q.forEach(q => {
    const text = (q[questionLanguage.value] || '').toLowerCase();
    const itText = (q.textItalian || '').toLowerCase();
    const enText = (q.textEnglish || '').toLowerCase();
    if (senseOrder.some(k => text.includes(k))) {
      sensory.push(q);
    } else if (overall.length === 0) {
      overall.push(q);
    } else if (itText.includes('informazioni testuali') || enText.includes('textual information')) {
      textDesc.push(q);
    } else {
      rest.push(q);
    }
  });

  sensory.sort((a, b) => {
    const aIdx = senseOrder.findIndex(k => (a[questionLanguage.value] || '').toLowerCase().includes(k));
    const bIdx = senseOrder.findIndex(k => (b[questionLanguage.value] || '').toLowerCase().includes(k));
    return aIdx - bIdx;
  });

  return [...overall, ...textDesc, ...sensory, ...rest];
});

function questionDisplayNumber(questionIndex) {
  const acPos = attentionChecksIndexes.indexes[currentActivity.value];
  return acPos != null && questionIndex > acPos ? questionIndex + 2 : questionIndex + 1;
}

const allQuestionsAnswered = computed(() => {
  const activityIndex = currentActivity.value;
  const numQuestions = questionsPerActivity.value[activityIndex] || questions.q.length;
  const startIndex = getQuestionGlobalIndex(activityIndex, 0);
  const regularAnswered = buttonsState.state.slice(startIndex, startIndex + numQuestions).every(val => val !== -1);
  const attentionIndex = activitiesWithAttentionChecks.indexes.indexOf(activityIndex);
  const attentionAnswered = attentionIndex === -1 || attentionButtonsState.state[attentionIndex] !== -1;
  return regularAnswered && attentionAnswered;
});
</script>

<template>
  <div class="container-fluid mt-3">
    <div class="d-flex flex-column flex-lg-row justify-content-center gap-3" style="min-height: calc(100vh - 200px);">
      <div class="scrollable-col left-col">
        <Activity :activity="activities.a[currentActivity]" :activityIndex="currentActivity"/>
      </div>

      <div class="scrollable-col right-col">
        <h2 style="font-weight: normal; font-size: 1.1rem; margin-bottom: 1rem;">
          {{ store.translationSPQChart["rightTitle"] }}
        </h2>

        <!-- Global N/A variant toggle (testing mode, when at least one sense score is 0) -->
        <div v-if="studyStore.testing && hasAnyZeroSense" class="sense-na-banner">
          <span class="sense-na-label">SVILUPPATORI – Sensi con punteggio 0: variante opzione N/A:</span>
          <button class="sense-na-btn" :class="{ active: !senseNAVariant }" @click="senseNAVariant = false">Standard</button>
          <button class="sense-na-btn" :class="{ active: senseNAVariant }" @click="senseNAVariant = true">Con N/A</button>
        </div>

        <div v-for="(question, questionIndex) in orderedQuestions" :key="questionIndex">
          <div class="card shadow-sm mt-1 mb-3" style="scroll-margin-top: 75px;" :id="'question_' + questionIndex">
            <div class="card-body text-center">
              <h6 class="card-title mb-3">
                {{ questionDisplayNumber(questionIndex) }}.
                <span v-html="highlightQuestions(question[questionLanguage])"></span>
              </h6>
              <div class="d-flex justify-content-center gap-2 flex-wrap">
                <button
                  v-for="(buttonText, buttonIndex) in store.translationActivity['buttons']"
                  :key="buttonIndex"
                  class="btn btn-sm"
                  @click="handleButtonClick(currentActivity, questionIndex, buttonIndex)"
                  :style="{
                    color: buttonsState.state[getQuestionGlobalIndex(currentActivity, questionIndex)] === buttonIndex ? 'white' : '#007ACC',
                    backgroundColor: buttonsState.state[getQuestionGlobalIndex(currentActivity, questionIndex)] === buttonIndex ? '#007ACC' : 'white',
                    borderColor: '#007ACC'
                  }"
                >{{ buttonText }}</button>
                <!-- N/A button for zero-score sense questions in testing mode -->
                <button
                  v-if="studyStore.testing && senseNAVariant && isSenseZeroScore(question)"
                  class="btn btn-sm"
                  @click="handleButtonClick(currentActivity, questionIndex, 5)"
                  :style="{
                    color: buttonsState.state[getQuestionGlobalIndex(currentActivity, questionIndex)] === 5 ? 'white' : '#6b7280',
                    backgroundColor: buttonsState.state[getQuestionGlobalIndex(currentActivity, questionIndex)] === 5 ? '#6b7280' : 'white',
                    borderColor: '#6b7280'
                  }"
                >N/A</button>
              </div>
            </div>
          </div>

          <!-- Attention check after this question -->
          <div v-if="activitiesWithAttentionChecks.indexes.includes(currentActivity) && attentionChecksIndexes.indexes[currentActivity] === questionIndex" class="card shadow-sm mt-1 mb-3" style="scroll-margin-top: 75px;" :id="'attentionCheck_' + questionIndex">
            <div class="card-body text-center">
              <h6 class="card-title mb-3">
                {{ attentionChecksIndexes.indexes[currentActivity] + 2 }}.
                {{ attentionChecks.checks[activitiesWithAttentionChecks.indexes.indexOf(currentActivity)][questionLanguage] }}
              </h6>
              <div class="d-flex justify-content-center gap-2 flex-wrap">
                <button
                  v-for="(buttonText, buttonIndex) in store.translationActivity['buttons']"
                  :key="buttonIndex"
                  class="btn btn-sm"
                  @click="handleAttentionClick(questionIndex, activitiesWithAttentionChecks.indexes.indexOf(currentActivity), buttonIndex)"
                  :style="{
                    color: attentionButtonsState.state[activitiesWithAttentionChecks.indexes.indexOf(currentActivity)] === buttonIndex ? 'white' : '#007ACC',
                    backgroundColor: attentionButtonsState.state[activitiesWithAttentionChecks.indexes.indexOf(currentActivity)] === buttonIndex ? '#007ACC' : 'white',
                    borderColor: '#007ACC'
                  }"
                >{{ buttonText }}</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-3 d-flex justify-content-between pb-3">
      <button v-if="studyStore.testing" class="btn btn-outline-secondary px-3" @click="skipAll()">
        Skip (visibile solo per sviluppatori)
      </button>
      <button v-if="allQuestionsAnswered" class="btn btn-success px-5 py-2 ms-auto" @click="logQuestions">
        {{ store.currentLanguage === 'it' ? "Conferma valutazione →" : "Confirm rating →" }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.scrollable-col { overflow-y: auto; }
.left-col, .right-col { width: 100%; }

@media (min-width: 992px) {
  .left-col, .right-col { width: 50%; max-width: 50%; }
}

.card { border-radius: 10px; border: 1px solid #e9ecef; }
.card-body { padding: 0.75rem; }
.card-title { font-size: 0.9rem; line-height: 1.4; }
.btn-sm { min-width: 48px; padding: 0.3rem 0.5rem; font-size: 0.85rem; border-radius: 6px; }

.sense-na-banner {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  background: #fff8e1;
  border: 1px solid #ffc107;
  border-radius: 6px;
  padding: 0.4rem 0.75rem;
  margin-bottom: 0.75rem;
  font-size: 0.78rem;
}
.sense-na-label { color: #856404; font-weight: 600; flex: 1 1 auto; }
.sense-na-btn {
  padding: 0.2rem 0.6rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: white;
  font-size: 0.78rem;
  cursor: pointer;
  color: #374151;
  white-space: nowrap;
}
.sense-na-btn.active { background: #007ACC; border-color: #007ACC; color: white; }
</style>
