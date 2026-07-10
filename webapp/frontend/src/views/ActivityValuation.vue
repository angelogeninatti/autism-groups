<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useLanguageStore } from '@/stores/language.js';
import { useStudyStore } from '@/stores/studyStore.js';
import { config } from '@/config.js';
import ActivityTest from './ActivityTest.vue';
import ActivityLiked from '../components/ActivityLiked.vue';
import GroupFormation from '../components/GroupFormation.vue';

const store = useLanguageStore();
const studyStore = useStudyStore();
const router = useRouter();
const API_BASE = config.API_BASE;

// ─── State machine ───────────────────────────────────────────────────────────
// phases: 'selecting' → 'instructions' → 'testing' → 'group_formation' → (loop) → done
const phase = ref('selecting');
const currentActivityIndex = ref(0);
const selectedActivities = ref([]);
const likedToBeSent = ref([]);
const allFormationData = ref([]);  // one entry per activity

const allSimulatedUsers = ref([]);
const userProfile = ref(null);

const NUM_ACTIVITIES = 2;

const currentActivity = computed(() => selectedActivities.value[currentActivityIndex.value] || null);

async function fetchSimulatedUsers() {
  try {
    const res = await fetch(`${API_BASE}/users?action=fetch_all_simulated_users&lang=${store.currentLanguage}`);
    const data = await res.json();
    allSimulatedUsers.value = Array.isArray(data) ? data : [];
  } catch (e) {
    console.error('Error fetching simulated users:', e);
  }
}

async function fetchUserProfile() {
  try {
    const res = await fetch(`${API_BASE}/users?action=get_my_profile`);
    const data = await res.json();
    userProfile.value = data;
  } catch (e) {
    console.error('Error fetching user profile:', e);
  }
}

onMounted(async () => {
  await Promise.all([fetchSimulatedUsers(), fetchUserProfile()]);

  // If studyStore already has results (same session), set them on userProfile
  if (userProfile.value && studyStore.userSpqResult && !userProfile.value.SPQResult) {
    userProfile.value.SPQResult = studyStore.userSpqResult;
  }
  if (userProfile.value && studyStore.userBigfResult && !userProfile.value.BigFResult) {
    userProfile.value.BigFResult = studyStore.userBigfResult;
  }
});

// ─── Activity selection ──────────────────────────────────────────────────────
function handleLikedActivity(likedActivities) {
  selectedActivities.value = likedActivities;
  likedToBeSent.value = likedActivities.map(a => a.id);
  phase.value = 'instructions';
}

function startEvaluation() {
  studyStore.incrementStep();
  currentActivityIndex.value = 0;
  phase.value = 'testing';
}

// ─── Flow handlers ───────────────────────────────────────────────────────────
function handleActivityFinished() {
  phase.value = 'group_formation';
}

async function handleGroupFormationDone(formationData) {
  const activity = currentActivity.value;

  // Save this activity's group formation to backend
  try {
    const payload = {
      activityId: activity.id,
      rounds: formationData.rounds,
      finalGroupAnswers: formationData.finalGroupAnswers,
      attentionCheckPassed: formationData.attentionCheckPassed,
    };
    await fetch(`${API_BASE}/activity?action=save_group_formation`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
  } catch (e) {
    console.error('Error saving group formation:', e);
  }

  allFormationData.value.push({ activityId: activity.id, ...formationData });

  const nextIndex = currentActivityIndex.value + 1;
  if (nextIndex < NUM_ACTIVITIES) {
    currentActivityIndex.value = nextIndex;
    phase.value = 'testing';
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } else {
    await finishStudy();
  }
}

// ─── Submission ──────────────────────────────────────────────────────────────
async function finishStudy() {
  try {
    await sendLikedActivity();
    router.push({ path: '/endTest' });
  } catch (e) {
    console.error('Error finishing study:', e);
    router.push({ path: '/error', query: { message: 'Unexpected error occurred' } });
  }
}

async function sendLikedActivity() {
  const data = { liked_activities_id: likedToBeSent.value, user_id: sessionStorage.getItem('specificId') };
  const response = await fetch(`${API_BASE}/activity?action=save_liked_activity`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data),
  });
  const result = await response.json();
  if (result.error) throw new Error(result.error);
  return result;
}

// ─── Translations ─────────────────────────────────────────────────────────────
const instructionsText = computed(() => store.currentLanguage === 'it'
  ? {
    title: 'Istruzioni per la fase di valutazione',
    body: `Hai selezionato ${NUM_ACTIVITIES} attività. Per ciascuna di esse:`,
    steps: [
      '1. Risponderai a domande sull\'esperienza che ti aspetti di vivere facendo l\'attività',
      '2. Formerai un gruppo in 3 round: in ogni round valuterai 3 candidati e uno verrà assegnato al tuo gruppo',
      '3. Al termine valuterai il gruppo complessivo che si è formato',
    ],
    note: 'Le tue valutazioni ci aiutano a capire come le caratteristiche sensoriali e di personalità influenzano la formazione del gruppo.',
    start: 'Inizia la valutazione →'
  }
  : {
    title: 'Instructions for the evaluation phase',
    body: `You have selected ${NUM_ACTIVITIES} activities. For each one:`,
    steps: [
      '1. You will answer questions about the experience you expect to have doing the activity',
      '2. You will form a group over 3 rounds: in each round you rate 3 candidates and one is assigned to your group',
      '3. At the end you will evaluate the overall group that was formed',
    ],
    note: 'Your ratings help us understand how sensory and personality characteristics influence group formation.',
    start: 'Start evaluation →'
  });

const activityLabel = computed(() => {
  if (!currentActivity.value) return '';
  return store.currentLanguage === 'it'
    ? currentActivity.value.titleItalian
    : currentActivity.value.titleEnglish;
});
</script>

<template>
  <div class="activity-valuation-wrapper">

    <!-- Phase: Selecting activities -->
    <div v-if="phase === 'selecting'">
      <ActivityLiked @finished="handleLikedActivity" />
    </div>

    <!-- Phase: Instructions -->
    <div v-else-if="phase === 'instructions'" class="instructions-screen">
      <div class="instructions-card">
        <h2>{{ instructionsText.title }}</h2>
        <p class="instructions-body">{{ instructionsText.body }}</p>
        <ul class="instructions-steps">
          <li v-for="step in instructionsText.steps" :key="step">{{ step }}</li>
        </ul>
        <p class="instructions-note">{{ instructionsText.note }}</p>

        <div class="selected-activities-preview">
          <p class="preview-label">{{ store.currentLanguage === 'it' ? 'Attività selezionate:' : 'Selected activities:' }}</p>
          <div v-for="(act, i) in selectedActivities" :key="act.id" class="preview-activity">
            <span class="preview-num">{{ i + 1 }}.</span>
            <span>{{ store.currentLanguage === 'it' ? act.titleItalian : act.titleEnglish }}</span>
          </div>
        </div>

        <button class="btn-start" @click="startEvaluation">{{ instructionsText.start }}</button>
      </div>
    </div>

    <!-- Phase: Activity questionnaire -->
    <div v-else-if="phase === 'testing' && currentActivity">
      <div class="phase-indicator">
        <span>{{ store.currentLanguage === 'it' ? 'Attività' : 'Activity' }} {{ currentActivityIndex + 1 }}/{{ NUM_ACTIVITIES }}:</span>
        <strong>{{ activityLabel }}</strong>
      </div>
      <ActivityTest :activity="currentActivity" @finished="handleActivityFinished" />
    </div>

    <!-- Phase: Group formation -->
    <div v-else-if="phase === 'group_formation' && currentActivity">
      <div class="phase-indicator">
        <span>{{ store.currentLanguage === 'it' ? 'Forma il gruppo per:' : 'Form group for:' }}</span>
        <strong>{{ activityLabel }}</strong>
      </div>
      <GroupFormation
        :activity="currentActivity"
        :allSimulatedUsers="allSimulatedUsers"
        :userProfile="userProfile"
        :activityIndex="currentActivityIndex"
        @submit="handleGroupFormationDone"
      />
    </div>

  </div>
</template>

<style scoped>
.activity-valuation-wrapper {
  min-height: 60vh;
}

.phase-indicator {
  background: #f0f7ff;
  border-left: 4px solid #007ACC;
  padding: 0.6rem 1rem;
  margin-bottom: 0.5rem;
  font-size: 0.95rem;
  color: #1e3a5f;
  display: flex;
  gap: 0.4rem;
  align-items: center;
}

.instructions-screen {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 2rem 1rem;
}

.instructions-card {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  max-width: 640px;
  width: 100%;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  border: 1px solid #e9ecef;
}

.instructions-card h2 {
  color: #007ACC;
  margin-bottom: 1rem;
  font-size: 1.4rem;
}

.instructions-body {
  font-size: 1rem;
  color: #374151;
  margin-bottom: 0.75rem;
}

.instructions-steps {
  list-style: none;
  padding: 0;
  margin-bottom: 1rem;
}

.instructions-steps li {
  padding: 0.4rem 0.5rem;
  color: #1f2937;
  font-size: 0.97rem;
  border-left: 3px solid #007ACC;
  margin-bottom: 0.4rem;
  padding-left: 0.75rem;
}

.instructions-note {
  font-size: 0.88rem;
  color: #6b7280;
  font-style: italic;
  margin-bottom: 1.5rem;
}

.selected-activities-preview {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 0.75rem 1rem;
  margin-bottom: 1.5rem;
}

.preview-label {
  font-weight: 600;
  font-size: 0.9rem;
  color: #374151;
  margin-bottom: 0.5rem;
}

.preview-activity {
  display: flex;
  gap: 0.5rem;
  padding: 0.3rem 0;
  font-size: 0.95rem;
  color: #1f2937;
}

.preview-num { font-weight: 600; color: #007ACC; }

.btn-start {
  width: 100%;
  padding: 0.875rem 2rem;
  background: #007ACC;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1.05rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;
}

.btn-start:hover { background: #005fa3; }
</style>
