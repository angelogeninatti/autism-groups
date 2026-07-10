<script setup>
import { useLanguageStore } from '@/stores/language.js';
import { useStudyStore } from '@/stores/studyStore.js';
import { ref, computed, onMounted } from 'vue';
import { config } from '@/config.js';

const API_BASE = config.API_BASE;
const store = useLanguageStore();
const studyStore = useStudyStore();
const emit = defineEmits(['finished']);

const randomActivities = ref([]);
const selectedCategory = ref("");
const liked_activity = ref([]);

const MAX_ACTIVITIES = 2;

async function fetchRandomActivities() {
  try {
    const res = await fetch(`${API_BASE}/activity?action=fetch_random_activities&lang=${store.currentLanguage}`);
    const data = await res.json();
    randomActivities.value = Array.isArray(data) ? data : (Array.isArray(data.activities) ? data.activities : []);
  } catch (e) {
    console.error('Error fetchRandomActivities:', e);
  }
}

onMounted(async () => { await fetchRandomActivities(); });

const uniqueCategories = computed(() => {
  const lang = store.currentLanguage === 'it' ? 'categoryItalian' : 'categoryEnglish';
  return [...new Set(randomActivities.value.map(a => a[lang]))];
});

function toggleActivity(activity) {
  const index = liked_activity.value.findIndex(a => a.id === activity.id);
  if (index !== -1) {
    liked_activity.value.splice(index, 1);
  } else if (liked_activity.value.length < MAX_ACTIVITIES) {
    liked_activity.value.push(activity);
  }
}

function isSelected(activity) {
  return liked_activity.value.some(a => a.id === activity.id);
}

function nextStep() {
  if (liked_activity.value.length === MAX_ACTIVITIES) {
    studyStore.incrementStep();
    emit('finished', liked_activity.value);
  }
}

const filteredActivities = computed(() => {
  if (!selectedCategory.value) return randomActivities.value;
  const lang = store.currentLanguage === 'it' ? 'categoryItalian' : 'categoryEnglish';
  return randomActivities.value.filter(a => a[lang] === selectedCategory.value);
});

// Selected activities that belong to a different category than the active filter
const selectedFromOtherCategories = computed(() => {
  if (!selectedCategory.value) return [];
  return liked_activity.value.filter(a => !filteredActivities.value.some(f => f.id === a.id));
});

const remaining = computed(() => MAX_ACTIVITIES - liked_activity.value.length);

const t = computed(() => ({
  instructions: store.currentLanguage === 'it'
    ? `Seleziona esattamente ${MAX_ACTIVITIES} attività di gruppo che ti piacciono cliccandoci sopra. Puoi filtrare per categoria usando il menu a tendina. Ogni attività ha una sensibilità agli stimoli sensoriali che descrive quanto i sensi vengono coinvolti durante l'attività.`
    : `Select exactly ${MAX_ACTIVITIES} group activities you like the most by clicking on them. You can filter by category using the dropdown menu. Each activity has a sensitivity to sensory stimuli that describes how much the senses are engaged during the activity.`,
  remaining: store.currentLanguage === 'it'
    ? (remaining.value === 0 ? 'Hai selezionato tutte le attività!' : `${remaining.value} ${remaining.value === 1 ? 'attività rimasta' : 'attività rimanenti'}`)
    : (remaining.value === 0 ? 'All activities selected!' : `${remaining.value} ${remaining.value === 1 ? 'activity remaining' : 'activities remaining'}`),
  selectCategory: store.currentLanguage === 'it' ? 'Filtra per categoria' : 'Filter by category',
  allCategories: store.currentLanguage === 'it' ? 'Tutte le categorie' : 'All categories',
  next: store.currentLanguage === 'it' ? 'Avanti →' : 'Next →',
  maxReached: store.currentLanguage === 'it'
    ? `Hai già selezionato ${MAX_ACTIVITIES} attività. Deseleziona una per cambiare.`
    : `You've already selected ${MAX_ACTIVITIES} activities. Deselect one to change.`,
}));
</script>

<template>
  <div class="activity-liked-wrapper">
    <!-- Instructions box -->
    <div class="instructions-box">
      <p class="instructions-text">{{ t.instructions }}</p>

      <div class="status-row">
        <span class="status-badge" :class="remaining === 0 ? 'badge-done' : 'badge-pending'">
          {{ t.remaining }}
        </span>
        <button class="btn-next" :disabled="remaining !== 0" @click="nextStep">{{ t.next }}</button>
      </div>

      <p v-if="remaining === 0 && liked_activity.length === MAX_ACTIVITIES" class="max-reached-note">
        {{ t.maxReached }}
      </p>
    </div>

    <!-- Category filter -->
    <div class="category-filter">
      <label>{{ t.selectCategory }}</label>
      <select v-model="selectedCategory">
        <option value="">{{ t.allCategories }}</option>
        <option v-for="cat in uniqueCategories" :key="cat" :value="cat">{{ cat }}</option>
      </select>
    </div>

    <!-- Section: selected activities from other categories (only when a filter is active) -->
    <div v-if="selectedFromOtherCategories.length > 0" class="other-selection-section">
      <p class="section-label">{{ store.currentLanguage === 'it' ? 'Le tue scelte selezionate da altre categorie:' : 'Your selections from other categories:' }}</p>
      <div class="activity-grid">
        <div
          v-for="activity in selectedFromOtherCategories"
          :key="activity.id"
          class="activity-card card-selected"
          @click="toggleActivity(activity)"
        >
          <div class="card-header-row">
            <h3>{{ store.currentLanguage === 'it' ? activity.titleItalian : activity.titleEnglish }}</h3>
            <span class="select-icon">✓</span>
          </div>
          <p class="category-line">
            {{ store.currentLanguage === 'it' ? activity.categoryItalian : activity.categoryEnglish }} · {{ activity.city }}
          </p>
          <p class="description-text">
            {{ store.currentLanguage === 'it' ? activity.descriptionItalian : activity.descriptionEnglish }}
          </p>
          <div v-if="activity.spqEvaluation" class="spq-profile">
            <p class="spq-label">{{ store.currentLanguage === 'it' ? 'Profilo sensoriale dell\'attività (Indica in che misura gli stimoli sensoriali sono presenti durante l\'attività)' : 'Activity sensory profile (Indicates the extent to which sensory stimuli are present during the activity)' }}</p>
            <p class="spq-subtitle">{{ store.currentLanguage === 'it' ? '(descrive quanto i sensi vengono coinvolti durante l\'esecuzione dell\'attività)' : '(describes how much the senses are engaged during the activity)' }}</p>
            <div class="spq-bars">
              <div v-for="(sense, key) in {
                [store.currentLanguage === 'it' ? 'Vista' : 'Sight']: activity.spqEvaluation.visionScore,
                [store.currentLanguage === 'it' ? 'Udito' : 'Hearing']: activity.spqEvaluation.hearingScore,
                [store.currentLanguage === 'it' ? 'Olfatto' : 'Smell']: activity.spqEvaluation.smellScore,
                [store.currentLanguage === 'it' ? 'Gusto' : 'Taste']: activity.spqEvaluation.tasteScore,
                [store.currentLanguage === 'it' ? 'Tatto' : 'Touch']: activity.spqEvaluation.touchScore,
              }" :key="key" class="spq-bar-row">
                <span class="spq-sense-label">{{ key }}</span>
                <div class="spq-bar-bg">
                  <div class="spq-bar-fill" :style="{ width: Math.min(100, sense) + '%' }"></div>
                </div>
                <span class="spq-score">{{ sense }}/100</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Divider between sections -->
    <div v-if="selectedFromOtherCategories.length > 0 && filteredActivities.length > 0" class="section-divider">
      <span>{{ store.currentLanguage === 'it' ? `Attività nella categoria selezionata:` : `Activities in selected category:` }}</span>
    </div>

    <!-- Section: activities matching the current filter -->
    <div v-if="filteredActivities.length > 0" class="activity-grid">
      <div
        v-for="activity in filteredActivities"
        :key="activity.id"
        class="activity-card"
        :class="{
          'card-selected': isSelected(activity),
          'card-disabled': !isSelected(activity) && remaining === 0
        }"
        @click="toggleActivity(activity)"
      >
        <div class="card-header-row">
          <h3>{{ store.currentLanguage === 'it' ? activity.titleItalian : activity.titleEnglish }}</h3>
          <span class="select-icon">{{ isSelected(activity) ? '✓' : '+' }}</span>
        </div>
        <p class="category-line">
          {{ store.currentLanguage === 'it' ? activity.categoryItalian : activity.categoryEnglish }} · {{ activity.city }}
        </p>
        <p class="description-text">
          {{ store.currentLanguage === 'it' ? activity.descriptionItalian : activity.descriptionEnglish }}
        </p>
        <div v-if="activity.spqEvaluation" class="spq-profile">
          <p class="spq-label">{{ store.currentLanguage === 'it' ? 'Profilo sensoriale dell\'attività (Indica in che misura gli stimoli sensoriali sono presenti durante l\'attività)' : 'Activity sensory profile (Indicates the extent to which sensory stimuli are present during the activity)' }}</p>
          <p class="spq-subtitle">{{ store.currentLanguage === 'it' ? '(descrive quanto i sensi vengono coinvolti durante l\'esecuzione dell\'attività)' : '(describes how much the senses are engaged during the activity)' }}</p>
          <div class="spq-bars">
            <div v-for="(sense, key) in {
              [store.currentLanguage === 'it' ? 'Vista' : 'Sight']: activity.spqEvaluation.visionScore,
              [store.currentLanguage === 'it' ? 'Udito' : 'Hearing']: activity.spqEvaluation.hearingScore,
              [store.currentLanguage === 'it' ? 'Olfatto' : 'Smell']: activity.spqEvaluation.smellScore,
              [store.currentLanguage === 'it' ? 'Gusto' : 'Taste']: activity.spqEvaluation.tasteScore,
              [store.currentLanguage === 'it' ? 'Tatto' : 'Touch']: activity.spqEvaluation.touchScore,
            }" :key="key" class="spq-bar-row">
              <span class="spq-sense-label">{{ key }}</span>
              <div class="spq-bar-bg">
                <div class="spq-bar-fill" :style="{ width: Math.min(100, sense) + '%' }"></div>
              </div>
              <span class="spq-score">{{ sense }}/100</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="selectedFromOtherCategories.length === 0" class="empty-state">
      <p>{{ store.currentLanguage === 'it' ? 'Caricamento attività...' : 'Loading activities...' }}</p>
    </div>
  </div>
</template>

<style scoped>
.activity-liked-wrapper {
  padding: 1rem 1.5rem;
  max-width: 1400px;
  margin: 0 auto;
}

.instructions-box {
  background: #f0f7ff;
  border-left: 4px solid #007ACC;
  border-radius: 8px;
  padding: 0.6rem 1rem;
  margin-bottom: 1.25rem;
}

.instructions-text {
  margin: 0 0 0.5rem 0;
  color: #1e3a5f;
  font-size: 0.9rem;
  line-height: 1.4;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.status-badge {
  padding: 0.3rem 0.85rem;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 600;
}

.badge-pending { background: #fff3cd; color: #856404; }
.badge-done { background: #d1e7dd; color: #0f5132; }

.max-reached-note {
  margin: 0.5rem 0 0 0;
  font-size: 0.82rem;
  color: #6c757d;
}

.btn-next {
  padding: 0.5rem 1.5rem;
  background: #33a474;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;
}

.btn-next:hover:not(:disabled) { background: #27835c; }
.btn-next:disabled { background: #9ca3af; cursor: not-allowed; opacity: 0.7; }

.category-filter {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.25rem;
  max-width: 380px;
}

.category-filter label {
  font-weight: 600;
  white-space: nowrap;
  color: #374151;
  font-size: 0.9rem;
}

.category-filter select {
  flex: 1;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  border: 2px solid #e5e7eb;
  font-size: 0.9rem;
  background: white;
  color: #374151;
}

.other-selection-section {
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #f0fdf4;
  border: 1.5px solid #33a474;
  border-radius: 10px;
}

.section-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #0f5132;
  margin-bottom: 0.75rem;
}

.section-divider {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 1.25rem 0;
  color: #6b7280;
  font-size: 0.85rem;
  font-weight: 600;
}

.section-divider::before,
.section-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e5e7eb;
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.25rem;
}

@media (max-width: 1024px) { .activity-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) { .activity-grid { grid-template-columns: 1fr; } }

.activity-card {
  border-radius: 10px;
  padding: 1.25rem;
  background: #fff;
  border: 2px solid #e5e7eb;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.activity-card:hover:not(.card-disabled) {
  border-color: #007ACC;
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.1);
}

.card-selected {
  border-color: #33a474 !important;
  background: #f0fdf4 !important;
  box-shadow: 0 4px 14px rgba(51,164,116,0.2) !important;
}

.card-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.5rem;
  margin-bottom: 0.4rem;
}

.card-header-row h3 {
  font-size: 1.05rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
  line-height: 1.3;
}

.select-icon {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  font-weight: 700;
  flex-shrink: 0;
  background: #e5e7eb;
  color: #374151;
  transition: all 0.2s ease;
}

.card-selected .select-icon {
  background: #33a474;
  color: white;
}

.category-line {
  font-size: 0.82rem;
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.description-text {
  font-size: 0.9rem;
  color: #374151;
  line-height: 1.5;
  margin-bottom: 0.75rem;
}

.spq-profile {
  margin-top: 0.5rem;
  padding-top: 0.75rem;
  border-top: 1px solid #e5e7eb;
}

.spq-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 0.1rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.spq-subtitle {
  font-size: 0.73rem;
  color: #9ca3af;
  font-style: italic;
  margin-bottom: 0.4rem;
}

.spq-bars { display: flex; flex-direction: column; gap: 4px; }

.spq-bar-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.spq-sense-label {
  font-size: 0.75rem;
  color: #6b7280;
  width: 48px;
  flex-shrink: 0;
}

.spq-bar-bg {
  flex: 1;
  height: 8px;
  background: #e9ecef;
  border-radius: 4px;
  overflow: hidden;
}

.spq-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #007ACC, #33a474);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.spq-score {
  font-size: 0.72rem;
  color: #9ca3af;
  width: 36px;
  flex-shrink: 0;
  text-align: right;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: #6b7280;
}

.other-selection-section .activity-card {
  padding: 0.5rem 0.75rem;
}

.other-selection-section .card-header-row {
  margin-bottom: 0.2rem;
}

.other-selection-section .card-header-row h3 {
  font-size: 0.85rem;
  line-height: 1.2;
}

.other-selection-section .category-line {
  font-size: 0.72rem;
  margin-bottom: 0.2rem;
}

.other-selection-section .description-text {
  font-size: 0.78rem;
  line-height: 1.3;
  margin-bottom: 0.3rem;
}

.other-selection-section .spq-profile {
  padding-top: 0.4rem;
  margin-top: 0.3rem;
}

.other-selection-section .spq-label {
  font-size: 0.66rem;
  margin-bottom: 0;
}

.other-selection-section .spq-subtitle {
  font-size: 0.63rem;
  margin-bottom: 0.25rem;
}

.other-selection-section .spq-bars {
  gap: 2px;
}

.other-selection-section .spq-bar-bg {
  height: 5px;
}

.other-selection-section .spq-bar-row {
  gap: 0.3rem;
}

.other-selection-section .spq-sense-label {
  font-size: 0.65rem;
}

.other-selection-section .spq-score {
  font-size: 0.62rem;
}
</style>
