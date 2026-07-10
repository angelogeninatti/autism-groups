<script setup>
/**
 * Iterative group formation component.
 * 3 rounds per activity:
 *   - Each round: 3 candidates (similar / complementary / random) rated on 6 dimensions, all on one page
 *   - One is randomly assigned to the group
 * After 3 rounds: final 5-question group evaluation.
 */
import { ref, computed, onMounted } from 'vue'
import { useLanguageStore } from '@/stores/language.js'
import { useStudyStore } from '@/stores/studyStore.js'

const props = defineProps({
  activity: Object,
  allSimulatedUsers: Array,  // full pool of simulated users
  userProfile: Object,        // user's own {id, BigFResult, SPQResult}
  activityIndex: { type: Number, default: 0 },  // 0-based; drives progressive letter naming
})

const emit = defineEmits(['submit'])

const store = useLanguageStore()
const studyStore = useStudyStore()

// ─── Constants ────────────────────────────────────────────────────────────────
const NUM_ROUNDS = 3
const NUM_CANDIDATE_QUESTIONS = 7
const NUM_FINAL_QUESTIONS = 7

// ─── State ────────────────────────────────────────────────────────────────────
const phase = ref('rounds')       // 'rounds' | 'final_group'
const roundIndex = ref(0)
const showAssignedScreen = ref(false)
const roundCandidates = ref([])
const allCandidateRatings = ref([
  new Array(NUM_CANDIDATE_QUESTIONS).fill(-1),
  new Array(NUM_CANDIDATE_QUESTIONS).fill(-1),
  new Array(NUM_CANDIDATE_QUESTIONS).fill(-1),
])
const allRoundsData = ref([])
const currentGroupMembers = ref([])
const currentGroupMemberLetters = ref([null])  // null = user ("Tu"/"You"); letter strings for joined members
const alreadyShownIds = ref(new Set())
const finalAnswers = ref(new Array(NUM_FINAL_QUESTIONS).fill(-1))

// Round phase: 2 attention checks assigned to 2 randomly chosen candidates each round
const roundAttentionCheckCandidates = ref([0, 2])  // 2 of [0,1,2], chosen in initRound
const roundAttentionCheckRatings = ref([-1, -1])
const roundAttentionCheckCorrectAnswers = ref([1, 1])
const roundAttentionCheckPositions = ref([0, 0])  // position within each candidate's questions

// Final phase: 1 attention check
const attentionCheckRating = ref(-1)
const attentionCheckCorrectAnswer = ref(1)
const attentionCheckPosition = ref(0)
const attentionCheckPassed = computed(() => {
  if (attentionCheckRating.value === -1) return null
  return attentionCheckRating.value === attentionCheckCorrectAnswer.value ? 'passed' : 'failed'
})

// ─── Translations ─────────────────────────────────────────────────────────────
const candidateQuestions = computed(() => {
  const it = [
    'Quanto questa persona sembra compatibile con te per svolgere questa attività?',
    'Quanto questa persona sembra adatta all\'attività in termini di sensibilità sensoriali?',
    'Quanto questa persona sembra compatibile con il gruppo attuale?',
    'Quanto questa persona aggiungerebbe qualcosa che manca al gruppo?',
    'Quanto ti sentiresti a tuo agio con questa persona?',
    'Quanto questa persona sembra compatibile con te in termini di tratti di personalità?',
    'Quanto questa persona sembra compatibile con te in termini di profilo sensoriale?',
  ]
  const en = [
    'How compatible does this person seem with you for this activity?',
    'How suitable does this person seem for this activity in terms of sensory sensitivities?',
    'How compatible does this person seem with the current group?',
    'How much would this person add something the group is missing?',
    'How comfortable would you feel with this person?',
    'How compatible does this person seem with you in terms of personality traits?',
    'How compatible does this person seem with you in terms of sensory profile?',
  ]
  return store.currentLanguage === 'it' ? it : en
})

const finalQuestions = computed(() => {
  const it = [
    'Parteciperesti a questa attività con questo gruppo?',
    'Quanto il gruppo sembra efficace per svolgere questa attività?',
    'Quanto il gruppo sembra compatibile con te?',
    'Quanto il gruppo sembra equilibrato?',
    'Quanto ti sentiresti a tuo agio in questo gruppo?',
    'Quanto il gruppo sembra compatibile con te in termini di tratti di personalità?',
    'Quanto il gruppo sembra compatibile con te in termini di profilo sensoriale?',
  ]
  const en = [
    'Would you participate in this activity with this group?',
    'How effective does the group seem for this activity?',
    'How compatible does the group seem with you?',
    'How balanced does the group seem?',
    'How comfortable would you feel in this group?',
    'How compatible does the group seem with you in terms of personality traits?',
    'How compatible does the group seem with you in terms of sensory profile?',
  ]
  return store.currentLanguage === 'it' ? it : en
})

const t = computed(() => store.currentLanguage === 'it' ? {
  roundLabel: (r) => `Round ${r} di ${NUM_ROUNDS}`,
  assignedTitle: (letter) => `Persona ${letter} ha deciso di unirsi al gruppo!`,
  groupNow: 'Il tuo gruppo ora:',
  youLabel: 'Tu',
  memberLabel: (i) => `Membro ${i}`,
  next: 'Avanti →',
  nextRound: 'Prossimo round →',
  finalGroup: 'Valuta il gruppo →',
  submitFinal: 'Invia valutazioni →',
  scale1: 'Per niente',
  scale5: 'Moltissimo',
  personalityTitle: 'Caratteristiche di personalità',
  sensoryTitle: 'Sensibilità agli stimoli sensoriali',
  groupPersonalityTitle: 'Media delle caratteristiche di personalità dei membri del gruppo',
  groupSensoryTitle: 'Media della sensibilità agli stimoli sensoriali dei membri del gruppo',
  finalTitle: 'Valutazione finale del gruppo',
  finalInstructions: (activityName) => `Abbiamo completato la formazione del gruppo. Valuta la sua qualità complessiva per l'attività "${activityName}".`,
  groupSizeLabel: 'Il gruppo è formato da:',
  groupAvgTitle: 'Profilo del gruppo (incluso te)',
  candidateProfileTitle: (letter) => `Profilo di ${letter}`,
  candidateName: (name) => `Persona ${name}`,
  roundIntro: (letters, activityName) => `Le persone ${letters.slice(0, -1).join(', ')} e ${letters[letters.length - 1]} stanno valutando se entrare nel gruppo per partecipare all'attività "${activityName}".`,
} : {
  roundLabel: (r) => `Round ${r} of ${NUM_ROUNDS}`,
  assignedTitle: (letter) => `Person ${letter} has decided to join the group!`,
  groupNow: 'Your group now:',
  youLabel: 'You',
  memberLabel: (i) => `Member ${i}`,
  next: 'Next →',
  nextRound: 'Next round →',
  finalGroup: 'Evaluate group →',
  submitFinal: 'Submit ratings →',
  scale1: 'Not at all',
  scale5: 'Very much',
  personalityTitle: 'Personality traits',
  sensoryTitle: 'Sensitivity to sensory stimuli',
  groupPersonalityTitle: 'Average personality traits of group members',
  groupSensoryTitle: 'Average sensitivity to sensory stimuli of group members',
  finalTitle: 'Final group evaluation',
  finalInstructions: (activityName) => `We have completed the group formation. Please evaluate its overall quality for the activity "${activityName}".`,
  groupSizeLabel: 'The group is formed by:',
  groupAvgTitle: 'Group profile (including yourself)',
  candidateProfileTitle: (letter) => `Profile of ${letter}`,
  candidateName: (name) => `Person ${name}`,
  roundIntro: (letters, activityName) => `Persons ${letters.slice(0, -1).join(', ')} and ${letters[letters.length - 1]} are considering whether to join the group for the activity "${activityName}".`,
})

function getAttentionLabel(correctAnswer) {
  return store.currentLanguage === 'it'
    ? `Seleziona ${correctAnswer} per confermare che stai prestando attenzione`
    : `Select ${correctAnswer} to confirm you are paying attention`
}

const activitySpqDisplay = computed(() => {
  const spq = props.activity?.spqEvaluation
  if (!spq) return null
  return {
    vision:  Number(spq.visionScore  ?? 0),
    hearing: Number(spq.hearingScore ?? 0),
    smell:   Number(spq.smellScore   ?? 0),
    taste:   Number(spq.tasteScore   ?? 0),
    touch:   Number(spq.touchScore   ?? 0),
  }
})

// ─── Big5 / SPQ max values ────────────────────────────────────────────────────
const BIG5_MAX = 10
const SPQ_MAX = { vision: 18, hearing: 15, touch: 30, smell: 27, taste: 15 }

// ─── Candidate selection ──────────────────────────────────────────────────────
function setRating(ci, qi, n) {
  const copy = [...allCandidateRatings.value[ci]]
  copy[qi] = n
  allCandidateRatings.value[ci] = copy
}

function getBig5(user) {
  const r = user?.BigFResult
  if (!r) return null
  return {
    openness:           Number(r.openness          ?? 0),
    conscientiousness:  Number(r.conscientiousness ?? 0),
    emotionalStability: Number(r.emotionalStability ?? 0),
    extroversion:       Number(r.extroversion       ?? 0),
    agreeableness:      Number(r.agreeableness      ?? 0),
  }
}

function getSpq(user) {
  const r = user?.SPQResult
  if (!r) return null
  return {
    vision:  Number(r.vision  ?? 0),
    hearing: Number(r.hearing ?? 0),
    smell:   Number(r.smell   ?? 0),
    taste:   Number(r.taste   ?? 0),
    touch:   Number(r.touch   ?? 0),
  }
}

function getActivitySpqWeights() {
  const spq = props.activity?.spqEvaluation
  if (!spq) return { vision: 0.2, hearing: 0.2, smell: 0.2, taste: 0.2, touch: 0.2 }
  const raw = {
    vision:  Number(spq.visionScore  ?? 0),
    hearing: Number(spq.hearingScore ?? 0),
    smell:   Number(spq.smellScore   ?? 0),
    taste:   Number(spq.tasteScore   ?? 0),
    touch:   Number(spq.touchScore   ?? 0),
  }
  const total = Object.values(raw).reduce((a, b) => a + b, 0)
  if (total === 0) return { vision: 0.2, hearing: 0.2, smell: 0.2, taste: 0.2, touch: 0.2 }
  return Object.fromEntries(Object.entries(raw).map(([k, v]) => [k, v / total]))
}

function normalizeUser(user, spqWeights) {
  const b5 = getBig5(user)
  const spq = getSpq(user)
  if (!b5 || !spq) return null
  return [
    b5.openness           / BIG5_MAX,
    b5.conscientiousness  / BIG5_MAX,
    b5.emotionalStability / BIG5_MAX,
    b5.extroversion       / BIG5_MAX,
    b5.agreeableness      / BIG5_MAX,
    spq.vision  / SPQ_MAX.vision  * spqWeights.vision  * 5,
    spq.hearing / SPQ_MAX.hearing * spqWeights.hearing * 5,
    spq.smell   / SPQ_MAX.smell   * spqWeights.smell   * 5,
    spq.taste   / SPQ_MAX.taste   * spqWeights.taste   * 5,
    spq.touch   / SPQ_MAX.touch   * spqWeights.touch   * 5,
  ]
}

function groupCentroid(vectors) {
  const n = vectors.length
  return vectors[0].map((_, i) => vectors.reduce((s, v) => s + v[i], 0) / n)
}

function groupVariance(vectors) {
  if (vectors.length <= 1) return 0
  const means = groupCentroid(vectors)
  return means.reduce((sum, _, i) =>
    sum + vectors.reduce((s, v) => s + (v[i] - means[i]) ** 2, 0) / vectors.length, 0
  ) / vectors.length
}

function selectCandidates(groupMembers, pool, shownIds, spqWeights) {
  const eligible = pool.filter(u =>
    !shownIds.has(u.id) && !groupMembers.find(g => g.id === u.id)
  )
  if (eligible.length < 3) {
    console.warn('Not enough eligible candidates, reusing pool')
    return { similar: eligible[0] || pool[0], complementary: eligible[1] || pool[1], random: eligible[2] || pool[2] }
  }

  const groupVectors = groupMembers.map(u => normalizeUser(u, spqWeights)).filter(Boolean)
  const currentVar = groupVectors.length > 0 ? groupVariance(groupVectors) : 0

  const eligibleWithVec = eligible
    .map(u => ({ user: u, vec: normalizeUser(u, spqWeights) }))
    .filter(x => x.vec !== null)

  if (eligibleWithVec.length < 3) {
    return { similar: eligible[0], complementary: eligible[1], random: eligible[2] }
  }

  let bestSim = null, bestSimScore = Infinity
  for (const x of eligibleWithVec) {
    const newVectors = [...groupVectors, x.vec]
    const newVar = groupVariance(newVectors)
    const score = currentVar === 0 ? newVar : (newVar - currentVar) / (newVar || 1)
    if (score < bestSimScore) { bestSimScore = score; bestSim = x.user }
  }
  const similar = bestSim

  let bestComp = null, bestScore = -Infinity
  for (const x of eligibleWithVec) {
    if (x.user.id === similar.id) continue
    const newVectors = [...groupVectors, x.vec]
    const newVar = groupVariance(newVectors)
    const score = currentVar === 0 ? newVar : (newVar - currentVar) / (newVar || 1)
    if (score > bestScore) { bestScore = score; bestComp = x.user }
  }
  if (!bestComp) bestComp = eligibleWithVec.find(x => x.user.id !== similar.id)?.user || similar

  const pool_remaining = eligible.filter(u => u.id !== similar.id && u.id !== bestComp.id)
  const random = pool_remaining[Math.floor(Math.random() * pool_remaining.length)] || eligible[2]

  return { similar, complementary: bestComp, random }
}

// ─── Progressive letter naming ────────────────────────────────────────────────
// Candidates are lettered A, B, C, D… across all rounds and activities.
// activityIndex * NUM_ROUNDS * 3 gives the offset for this activity.
function getCandidateLetter(candidateIdx) {
  const offset = (props.activityIndex ?? 0) * NUM_ROUNDS * 3
  return String.fromCharCode(65 + offset + roundIndex.value * 3 + candidateIdx)
}

// ─── Round initialization ──────────────────────────────────────────────────────
function initRound() {
  const spqWeights = getActivitySpqWeights()
  const { similar, complementary, random } = selectCandidates(
    currentGroupMembers.value, props.allSimulatedUsers, alreadyShownIds.value, spqWeights
  )
  roundCandidates.value = [
    { ...similar,       type: 'similar' },
    { ...complementary, type: 'complementary' },
    { ...random,        type: 'random' },
  ]
  roundCandidates.value.sort(() => Math.random() - 0.5)
  showAssignedScreen.value = false
  allCandidateRatings.value = [
    new Array(NUM_CANDIDATE_QUESTIONS).fill(-1),
    new Array(NUM_CANDIDATE_QUESTIONS).fill(-1),
    new Array(NUM_CANDIDATE_QUESTIONS).fill(-1),
  ]
  // Randomly assign 2 of the 3 candidates to receive attention checks
  const shuffled = [0, 1, 2].sort(() => Math.random() - 0.5)
  roundAttentionCheckCandidates.value = shuffled.slice(0, 2).sort((a, b) => a - b)
  roundAttentionCheckRatings.value = [-1, -1]
  roundAttentionCheckCorrectAnswers.value = [
    Math.floor(Math.random() * 5) + 1,
    Math.floor(Math.random() * 5) + 1,
  ]
  roundAttentionCheckPositions.value = [
    Math.floor(Math.random() * (NUM_CANDIDATE_QUESTIONS - 2)) + 1,
    Math.floor(Math.random() * (NUM_CANDIDATE_QUESTIONS - 2)) + 1,
  ]
}

onMounted(() => {
  currentGroupMembers.value = [props.userProfile]
  currentGroupMemberLetters.value = [null]
  initRound()
})

// ─── Computed ──────────────────────────────────────────────────────────────────
const isLastRound = computed(() => roundIndex.value === NUM_ROUNDS - 1)

const canSubmitRound = computed(() =>
  allCandidateRatings.value.every(ratings => ratings.every(r => r !== -1)) &&
  roundAttentionCheckRatings.value.every(r => r !== -1)
)

const canSubmitFinal = computed(() => finalAnswers.value.every(r => r !== -1) && attentionCheckPassed.value !== null)

// ─── Attention check helpers ───────────────────────────────────────────────────
function shouldShowRoundAttentionCheck(candidateIdx, questionIdx) {
  const checkIdx = roundAttentionCheckCandidates.value.indexOf(candidateIdx)
  return checkIdx >= 0 && questionIdx === roundAttentionCheckPositions.value[checkIdx]
}

function getRoundAttentionCheckIdx(candidateIdx) {
  return roundAttentionCheckCandidates.value.indexOf(candidateIdx)
}

// ─── Profile labels ────────────────────────────────────────────────────────────
const big5Labels = computed(() => ({
  it: { openness: 'Apertura', conscientiousness: 'Coscienziosità', emotionalStability: 'Stabilità emotiva', extroversion: 'Estroversione', agreeableness: 'Amichevolezza' },
  en: { openness: 'Openness', conscientiousness: 'Conscientiousness', emotionalStability: 'Emotional Stability', extroversion: 'Extroversion', agreeableness: 'Agreeableness' },
}[store.currentLanguage]))

const spqLabels = computed(() => ({
  it: { vision: 'Vista', hearing: 'Udito', smell: 'Olfatto', taste: 'Gusto', touch: 'Tatto' },
  en: { vision: 'Vision', hearing: 'Hearing', smell: 'Smell', taste: 'Taste', touch: 'Touch' },
}[store.currentLanguage]))

function traitBar(value, maxValue) {
  return Math.round((value / maxValue) * 100)
}

function big5Color(_value) {
  return '#008FFB'
}

function spqColor(_pct) {
  return '#008FFB'
}

function barTooltip(value, maxValue) {
  return `${Math.round((value / maxValue) * 100)} / 100`
}

// ─── Group averages ────────────────────────────────────────────────────────────
const groupAvgBig5 = computed(() => {
  const members = currentGroupMembers.value
  if (!members.length) return null
  const keys = ['openness', 'conscientiousness', 'emotionalStability', 'extroversion', 'agreeableness']
  const result = {}
  keys.forEach(k => {
    result[k] = Math.round(members.reduce((s, m) => s + Number(m?.BigFResult?.[k] ?? 0), 0) / members.length)
  })
  return result
})

const groupAvgSpq = computed(() => {
  const members = currentGroupMembers.value
  if (!members.length) return null
  const keys = ['vision', 'hearing', 'smell', 'taste', 'touch']
  const result = {}
  keys.forEach(k => {
    result[k] = Math.round(members.reduce((s, m) => s + Number(m?.SPQResult?.[k] ?? 0), 0) / members.length)
  })
  return result
})

// ─── Handlers ──────────────────────────────────────────────────────────────────
function submitAllCandidates() {
  studyStore.incrementStep()

  const savedRatings = roundCandidates.value.map((c, i) => ({
    id:      c.id,
    type:    c.type,
    ratings: [...allCandidateRatings.value[i]],
  }))

  roundCandidates.value.forEach(c => {
    alreadyShownIds.value = new Set([...alreadyShownIds.value, c.id])
  })

  const assignedIdx = Math.floor(Math.random() * roundCandidates.value.length)
  const assignedCandidate = roundCandidates.value[assignedIdx]
  const assignedLetter = getCandidateLetter(assignedIdx)

  const roundAttentionFailed = roundAttentionCheckRatings.value.filter(
    (r, i) => r !== roundAttentionCheckCorrectAnswers.value[i]
  ).length

  allRoundsData.value.push({
    roundNumber: roundIndex.value + 1,
    candidates: savedRatings,
    assignedCandidateId: assignedCandidate.id,
    attentionChecksFailed: roundAttentionFailed,
  })

  currentGroupMembers.value = [...currentGroupMembers.value, assignedCandidate]
  currentGroupMemberLetters.value = [...currentGroupMemberLetters.value, assignedLetter]
  showAssignedScreen.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function nextRound() {
  studyStore.incrementStep()
  if (!isLastRound.value) {
    roundIndex.value++
    initRound()
  } else {
    phase.value = 'final_group'
    finalAnswers.value = new Array(NUM_FINAL_QUESTIONS).fill(-1)
    attentionCheckRating.value = -1
    attentionCheckCorrectAnswer.value = Math.floor(Math.random() * 5) + 1
    attentionCheckPosition.value = Math.floor(Math.random() * (NUM_FINAL_QUESTIONS - 2)) + 1
  }
}

function submitFinal() {
  studyStore.incrementStep()
  emit('submit', {
    rounds: allRoundsData.value,
    finalGroupAnswers: [...finalAnswers.value],
    attentionCheckPassed: attentionCheckPassed.value === 'passed',
  })
}

function skipRound() {
  if (!showAssignedScreen.value) {
    const savedRatings = roundCandidates.value.map((c) => ({
      id: c.id, type: c.type, ratings: new Array(NUM_CANDIDATE_QUESTIONS).fill(3),
    }))
    roundCandidates.value.forEach(c => {
      alreadyShownIds.value = new Set([...alreadyShownIds.value, c.id])
    })
    const assigned = roundCandidates.value[0]
    allRoundsData.value.push({
      roundNumber: roundIndex.value + 1,
      candidates: savedRatings,
      assignedCandidateId: assigned.id,
    })
    currentGroupMembers.value = [...currentGroupMembers.value, assigned]
    currentGroupMemberLetters.value = [...currentGroupMemberLetters.value, getCandidateLetter(0)]
    showAssignedScreen.value = true
  } else {
    nextRound()
  }
}

function skipFinal() {
  finalAnswers.value.fill(3)
  attentionCheckRating.value = attentionCheckCorrectAnswer.value
  submitFinal()
}
</script>

<template>
  <div class="gf-wrapper">

    <!-- ─── ROUNDS PHASE ─────────────────────────────────────────────────────── -->
    <div v-if="phase === 'rounds'">

      <!-- Progress header -->
      <div class="gf-header">
        <div class="gf-round-indicator">
          <span v-for="r in NUM_ROUNDS" :key="r"
            class="gf-dot"
            :class="{ 'done': r - 1 < roundIndex || (r - 1 === roundIndex && showAssignedScreen), 'active': r - 1 === roundIndex && !showAssignedScreen }"
          ></span>
          <span class="gf-round-label">{{ t.roundLabel(roundIndex + 1) }}</span>
        </div>
      </div>

      <!-- Current group card (always visible) -->
      <div class="gf-group-card">
        <div class="gf-group-header-row">
          <span class="gf-group-title">{{ t.groupSizeLabel }}</span>
          <div v-for="letter in currentGroupMemberLetters" :key="letter ?? 'you'" class="gf-member-badge">
            <span class="gf-member-icon">👤</span>
            <span>{{ letter === null ? t.youLabel : t.candidateName(letter) }}</span>
          </div>
        </div>
        <p v-if="currentGroupMembers.length === 1" class="gf-you-first-note">
          {{ store.currentLanguage === 'it' ? 'Ti sei iscritto all\'attività per primo.' : 'You signed up for this activity first.' }}
        </p>
        <template v-if="currentGroupMembers.length > 1">
          <div v-if="groupAvgBig5 || groupAvgSpq" class="gf-group-avg-title">{{ t.groupAvgTitle }}</div>
          <div v-if="groupAvgBig5">
            <p class="gf-group-section-subtitle">{{ t.groupPersonalityTitle }}</p>
            <div class="gf-group-traits">
              <div v-for="(val, key) in groupAvgBig5" :key="key" class="gf-trait-mini">
                <span class="gf-trait-name">{{ big5Labels[key] }}</span>
                <div class="gf-mini-bar-tooltip-wrapper" :data-value="barTooltip(val, BIG5_MAX)">
                  <div class="gf-mini-bar-bg">
                    <div class="gf-mini-bar-fill" :style="{ width: traitBar(val, BIG5_MAX) + '%', backgroundColor: big5Color(val) }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="groupAvgSpq">
            <p class="gf-group-section-subtitle">{{ t.groupSensoryTitle }}</p>
            <div class="gf-group-traits">
              <div v-for="(val, key) in groupAvgSpq" :key="key" class="gf-trait-mini">
                <span class="gf-trait-name">{{ spqLabels[key] }}</span>
                <div class="gf-mini-bar-tooltip-wrapper" :data-value="barTooltip(val, SPQ_MAX[key])">
                  <div class="gf-mini-bar-bg">
                    <div class="gf-mini-bar-fill" :style="{ width: traitBar(val, SPQ_MAX[key]) + '%', backgroundColor: spqColor(traitBar(val, SPQ_MAX[key])) }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
        <div v-if="activitySpqDisplay" class="gf-activity-spq">
          <p class="gf-activity-spq-title">{{ store.currentLanguage === 'it' ? 'Profilo sensoriale dell\'attività (Indica in che misura gli stimoli sensoriali sono presenti durante l\'attività)' : 'Activity sensory profile (Indicates the extent to which sensory stimuli are present during the activity)' }}</p>
          <div class="gf-group-traits">
            <div v-for="(pct, key) in activitySpqDisplay" :key="key" class="gf-trait-mini">
              <span class="gf-trait-name">{{ spqLabels[key] }}</span>
              <div class="gf-mini-bar-tooltip-wrapper" :data-value="barTooltip(pct, 100)">
                <div class="gf-mini-bar-bg">
                  <div class="gf-mini-bar-fill" :style="{ width: pct + '%', backgroundColor: spqColor(pct) }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Assignment screen -->
      <div v-if="showAssignedScreen" class="gf-assigned-screen">
        <div class="gf-assigned-icon">🎉</div>
        <h3>{{ t.assignedTitle(currentGroupMemberLetters[currentGroupMemberLetters.length - 1]) }}</h3>
        <div class="gf-next-row">
          <button v-if="studyStore.testing" class="btn btn-outline-secondary btn-sm me-2" @click="skipRound">
            Skip (dev)
          </button>
          <button class="gf-btn-primary" @click="nextRound">
            {{ isLastRound ? t.finalGroup : t.nextRound }}
          </button>
        </div>
      </div>

      <!-- All 3 candidates on a single page -->
      <div v-else class="gf-all-candidates">
        <p class="gf-round-intro">{{ t.roundIntro(roundCandidates.map((_, ci) => getCandidateLetter(ci)), store.currentLanguage === 'it' ? (activity?.titleItalian ?? '') : (activity?.titleEnglish ?? '')) }}</p>
        <div v-for="(candidate, ci) in roundCandidates" :key="candidate.id">
          <hr v-if="ci > 0" class="gf-candidate-divider" />

          <h4 class="gf-candidate-name-title">{{ t.candidateName(getCandidateLetter(ci)) }}</h4>

          <div class="gf-content-cols">
            <!-- Left: candidate profile -->
            <div class="gf-profile-col">
              <p class="gf-candidate-profile-title">{{ t.candidateProfileTitle(getCandidateLetter(ci)) }}</p>
              <div class="gf-profile-section">
                <p class="gf-section-title">{{ t.personalityTitle }}</p>
                <div v-for="(val, key) in getBig5(candidate)" :key="key" class="gf-trait-row">
                  <span class="gf-trait-label">{{ big5Labels[key] }}</span>
                  <div class="gf-bar-tooltip-wrapper" :data-value="barTooltip(val, BIG5_MAX)">
                    <div class="gf-bar-bg">
                      <div class="gf-bar-fill"
                        :style="{ width: traitBar(val, BIG5_MAX) + '%', backgroundColor: big5Color(val) }">
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="gf-profile-section">
                <p class="gf-section-title">{{ t.sensoryTitle }}</p>
                <div v-for="(val, key) in getSpq(candidate)" :key="key" class="gf-trait-row">
                  <span class="gf-trait-label">{{ spqLabels[key] }}</span>
                  <div class="gf-bar-tooltip-wrapper" :data-value="barTooltip(val, SPQ_MAX[key])">
                    <div class="gf-bar-bg">
                      <div class="gf-bar-fill"
                        :style="{ width: traitBar(val, SPQ_MAX[key]) + '%', backgroundColor: spqColor(traitBar(val, SPQ_MAX[key])) }">
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Right: rating questions -->
            <div class="gf-rating-col">
              <template v-for="(question, qi) in candidateQuestions" :key="qi">
                <div class="gf-question-block">
                  <p class="gf-question-text">{{ qi + 1 }}. {{ question }}</p>
                  <div class="gf-scale">
                    <span class="gf-scale-label">{{ t.scale1 }}</span>
                    <div class="gf-rating-btns">
                      <button
                        v-for="n in 5" :key="n"
                        class="gf-rating-btn"
                        :class="{ selected: allCandidateRatings[ci][qi] === n }"
                        @click="setRating(ci, qi, n)"
                      >{{ n }}</button>
                    </div>
                    <span class="gf-scale-label">{{ t.scale5 }}</span>
                  </div>
                </div>

                <!-- Attention check for this candidate (if applicable) -->
                <div v-if="shouldShowRoundAttentionCheck(ci, qi)" class="gf-question-block">
                  <p class="gf-question-text">{{ getAttentionLabel(roundAttentionCheckCorrectAnswers[getRoundAttentionCheckIdx(ci)]) }}</p>
                  <div class="gf-scale">
                    <span class="gf-scale-label">{{ t.scale1 }}</span>
                    <div class="gf-rating-btns">
                      <button
                        v-for="n in 5" :key="n"
                        class="gf-rating-btn"
                        :class="{ selected: roundAttentionCheckRatings[getRoundAttentionCheckIdx(ci)] === n }"
                        @click="roundAttentionCheckRatings[getRoundAttentionCheckIdx(ci)] = n"
                      >{{ n }}</button>
                    </div>
                    <span class="gf-scale-label">{{ t.scale5 }}</span>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>

        <div class="gf-next-row">
          <button v-if="studyStore.testing" class="btn btn-outline-secondary btn-sm me-2"
            @click="() => { allCandidateRatings.forEach(r => r.fill(3)); roundAttentionCheckRatings[0] = roundAttentionCheckCorrectAnswers[0]; roundAttentionCheckRatings[1] = roundAttentionCheckCorrectAnswers[1]; submitAllCandidates() }">
            Skip (dev)
          </button>
          <button class="gf-btn-primary" :disabled="!canSubmitRound" @click="submitAllCandidates">
            {{ t.next }}
          </button>
        </div>
      </div>
    </div>

    <!-- ─── FINAL GROUP EVALUATION ─────────────────────────────────────────── -->
    <div v-else-if="phase === 'final_group'">
      <div class="gf-final-header">
        <h3>{{ t.finalTitle }}</h3>
        <p class="gf-final-instructions">{{ t.finalInstructions(store.currentLanguage === 'it' ? (activity?.titleItalian ?? '') : (activity?.titleEnglish ?? '')) }}</p>
      </div>

      <!-- Final group card -->
      <div class="gf-group-card gf-group-card-large">
        <div class="gf-group-header-row">
          <span class="gf-group-title">{{ t.groupSizeLabel }}</span>
          <div v-for="letter in currentGroupMemberLetters" :key="letter ?? 'you'" class="gf-member-badge">
            <span class="gf-member-icon">👤</span>
            <span>{{ letter === null ? t.youLabel : t.candidateName(letter) }}</span>
          </div>
        </div>
        <div v-if="groupAvgBig5 || groupAvgSpq" class="gf-group-avg-title">{{ t.groupAvgTitle }}</div>
        <div v-if="groupAvgBig5">
          <p class="gf-group-section-subtitle">{{ t.groupPersonalityTitle }}</p>
          <div class="gf-group-traits">
            <div v-for="(val, key) in groupAvgBig5" :key="key" class="gf-trait-mini">
              <span class="gf-trait-name">{{ big5Labels[key] }}</span>
              <div class="gf-mini-bar-tooltip-wrapper" :data-value="barTooltip(val, BIG5_MAX)">
                <div class="gf-mini-bar-bg">
                  <div class="gf-mini-bar-fill" :style="{ width: traitBar(val, BIG5_MAX) + '%', backgroundColor: big5Color(val) }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="groupAvgSpq">
          <p class="gf-group-section-subtitle">{{ t.groupSensoryTitle }}</p>
          <div class="gf-group-traits">
            <div v-for="(val, key) in groupAvgSpq" :key="key" class="gf-trait-mini">
              <span class="gf-trait-name">{{ spqLabels[key] }}</span>
              <div class="gf-mini-bar-tooltip-wrapper" :data-value="barTooltip(val, SPQ_MAX[key])">
                <div class="gf-mini-bar-bg">
                  <div class="gf-mini-bar-fill" :style="{ width: traitBar(val, SPQ_MAX[key]) + '%', backgroundColor: spqColor(traitBar(val, SPQ_MAX[key])) }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="activitySpqDisplay" class="gf-activity-spq">
          <p class="gf-activity-spq-title">{{ store.currentLanguage === 'it' ? 'Profilo sensoriale dell\'attività (Indica in che misura gli stimoli sensoriali sono presenti durante l\'attività)' : 'Activity sensory profile (Indicates the extent to which sensory stimuli are present during the activity)' }}</p>
          <div class="gf-group-traits">
            <div v-for="(pct, key) in activitySpqDisplay" :key="key" class="gf-trait-mini">
              <span class="gf-trait-name">{{ spqLabels[key] }}</span>
              <div class="gf-mini-bar-tooltip-wrapper" :data-value="barTooltip(pct, 100)">
                <div class="gf-mini-bar-bg">
                  <div class="gf-mini-bar-fill" :style="{ width: pct + '%', backgroundColor: spqColor(pct) }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Final questions -->
      <div class="gf-final-questions">
        <template v-for="(question, qi) in finalQuestions" :key="qi">
          <div class="gf-question-block">
            <p class="gf-question-text">{{ qi + 1 }}. {{ question }}</p>
            <div class="gf-scale">
              <span class="gf-scale-label">{{ t.scale1 }}</span>
              <div class="gf-rating-btns">
                <button
                  v-for="n in 5" :key="n"
                  class="gf-rating-btn"
                  :class="{ selected: finalAnswers[qi] === n }"
                  @click="finalAnswers[qi] = n"
                >{{ n }}</button>
              </div>
              <span class="gf-scale-label">{{ t.scale5 }}</span>
            </div>
          </div>

          <div v-if="qi === attentionCheckPosition" class="gf-question-block">
            <p class="gf-question-text">{{ getAttentionLabel(attentionCheckCorrectAnswer) }}</p>
            <div class="gf-scale">
              <span class="gf-scale-label">{{ t.scale1 }}</span>
              <div class="gf-rating-btns">
                <button
                  v-for="n in 5" :key="n"
                  class="gf-rating-btn"
                  :class="{ selected: attentionCheckRating === n }"
                  @click="attentionCheckRating = n"
                >{{ n }}</button>
              </div>
              <span class="gf-scale-label">{{ t.scale5 }}</span>
            </div>
          </div>
        </template>
      </div>

      <div class="gf-next-row">
        <button v-if="studyStore.testing" class="btn btn-outline-secondary btn-sm me-2" @click="skipFinal">
          Skip (dev)
        </button>
        <button class="gf-btn-primary" :disabled="!canSubmitFinal" @click="submitFinal">
          {{ t.submitFinal }}
        </button>
      </div>
    </div>

  </div>
</template>

<style scoped>
.gf-wrapper { padding: 0 1rem 2rem; max-width: 900px; margin: 0 auto; }

.gf-header {
  display: flex; align-items: center; gap: 1.5rem;
  background: #f0f7ff; border-left: 4px solid #007ACC;
  padding: 0.6rem 1rem; margin-bottom: 1rem; border-radius: 0 6px 6px 0;
}

.gf-round-indicator {
  display: flex; align-items: center; gap: 0.4rem; font-size: 0.9rem; color: #1e3a5f;
}

.gf-dot {
  width: 14px; height: 14px; border-radius: 50%;
  background: #d1d5db; transition: background 0.2s;
}
.gf-dot.done { background: #33a474; }
.gf-dot.active { background: #007ACC; transform: scale(1.3); box-shadow: 0 0 0 3px rgba(0,122,204,0.2); }
.gf-round-label { margin-left: 0.3rem; font-weight: 600; color: #1e3a5f; }

.gf-group-card {
  background: #f8f9fa; border: 1px solid #dee2e6; border-radius: 10px;
  padding: 0.75rem 1rem; margin-bottom: 1rem;
}
.gf-group-card-large { padding: 1rem 1.25rem; }
.gf-group-header-row {
  display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem;
}
.gf-group-title { font-weight: 600; font-size: 0.88rem; color: #374151; white-space: nowrap; }
.gf-group-avg-title {
  font-size: 0.78rem; font-weight: 700; color: #374151;
  margin: 0.6rem 0 0.25rem; text-transform: uppercase; letter-spacing: 0.03em;
}
.gf-group-section-subtitle {
  font-size: 0.72rem; font-weight: 600; color: #007ACC;
  text-transform: uppercase; letter-spacing: 0.04em;
  margin: 0.3rem 0 0.25rem;
}
.gf-member-badge {
  display: flex; align-items: center; gap: 0.3rem;
  background: white; border: 1px solid #007ACC; border-radius: 20px;
  padding: 0.2rem 0.6rem; font-size: 0.82rem; color: #007ACC; font-weight: 600;
}
.gf-member-icon { font-size: 0.9rem; }
.gf-group-traits { display: grid; grid-template-columns: repeat(5, 1fr); gap: 0.3rem 0.5rem; margin-bottom: 0.2rem; }
.gf-trait-mini { display: flex; align-items: center; gap: 2.5rem; }
.gf-trait-name { font-size: 0.72rem; color: #6b7280; width: 70px; flex-shrink: 0; text-align: right; }

/* Mini bar with hover tooltip */
.gf-mini-bar-tooltip-wrapper { flex: 1; position: relative; }
.gf-mini-bar-tooltip-wrapper:hover::after {
  content: attr(data-value);
  position: absolute;
  top: -22px; left: 50%;
  transform: translateX(-50%);
  background: #374151; color: white;
  padding: 2px 5px; border-radius: 3px; font-size: 10px;
  white-space: nowrap; z-index: 10; pointer-events: none;
}
.gf-mini-bar-bg { height: 6px; background: #e5e7eb; border-radius: 3px; overflow: hidden; }
.gf-mini-bar-fill { height: 100%; border-radius: 3px; transition: width 0.3s; }

.gf-assigned-screen { text-align: center; padding: 2rem 1rem; }
.gf-assigned-icon { font-size: 3rem; margin-bottom: 0.5rem; }
.gf-assigned-screen h3 { color: #007ACC; margin-bottom: 0.5rem; }
.gf-assigned-screen p { color: #374151; margin-bottom: 1.5rem; }
.gf-round-intro {
  font-size: 0.92rem; color: #374151; margin-bottom: 1.25rem;
  background: #f0f7ff; border-radius: 6px; padding: 0.6rem 0.9rem;
  border-left: 3px solid #007ACC;
}

.gf-candidate-divider {
  border: none; border-top: 2px solid #dee2e6;
  margin: 2rem 0;
}
.gf-candidate-name-title {
  font-size: 1.4rem; font-weight: 800; color: #007ACC;
  text-align: center; margin-bottom: 1rem;
  letter-spacing: 0.03em;
}

.gf-content-cols { display: flex; gap: 1.5rem; align-items: flex-start; flex-wrap: wrap; }
.gf-profile-col { flex: 0 0 260px; min-width: 220px; }
.gf-rating-col { flex: 1; min-width: 280px; }

.gf-candidate-profile-title {
  font-size: 0.78rem; font-weight: 700; color: #374151;
  text-transform: uppercase; letter-spacing: 0.03em; margin-bottom: 0.5rem;
}
.gf-profile-section { margin-bottom: 1rem; }
.gf-section-title { font-size: 0.8rem; font-weight: 700; color: #007ACC; text-transform: uppercase; letter-spacing: 0.04em; margin-bottom: 0.4rem; }
.gf-trait-row { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.3rem; }
.gf-trait-label { font-size: 0.78rem; color: #6b7280; width: 120px; flex-shrink: 0; }

/* Full bar with hover tooltip */
.gf-bar-tooltip-wrapper { flex: 1; position: relative; }
.gf-bar-tooltip-wrapper:hover::after {
  content: attr(data-value);
  position: absolute;
  top: -24px; left: 50%;
  transform: translateX(-50%);
  background: #374151; color: white;
  padding: 2px 6px; border-radius: 4px; font-size: 11px;
  white-space: nowrap; z-index: 10; pointer-events: none;
}
.gf-bar-bg { height: 8px; background: #e5e7eb; border-radius: 4px; overflow: hidden; }
.gf-bar-fill { height: 100%; border-radius: 4px; transition: width 0.3s; }

.gf-question-block { margin-bottom: 1.25rem; }
.gf-question-text { font-size: 0.92rem; font-weight: 600; color: #1f2937; margin-bottom: 0.5rem; }
.gf-scale { display: flex; align-items: center; gap: 0.6rem; flex-wrap: wrap; }
.gf-scale-label { font-size: 0.78rem; color: #9ca3af; white-space: nowrap; }
.gf-rating-btns { display: flex; gap: 0.4rem; }
.gf-rating-btn {
  width: 42px; height: 42px; border-radius: 8px;
  border: 2px solid #d1d5db; background: white;
  font-size: 1rem; font-weight: 700; color: #374151; cursor: pointer; transition: all 0.15s;
}
.gf-rating-btn:hover { border-color: #007ACC; color: #007ACC; }
.gf-rating-btn.selected { background: #007ACC !important; border-color: #007ACC !important; color: white !important; }

.gf-next-row { display: flex; justify-content: flex-end; align-items: center; margin-top: 1.5rem; }
.gf-btn-primary {
  padding: 0.75rem 2rem; background: #007ACC; color: white;
  border: none; border-radius: 8px; font-size: 1rem; font-weight: 600;
  cursor: pointer; transition: background 0.2s;
}
.gf-btn-primary:hover:not(:disabled) { background: #005fa3; }
.gf-btn-primary:disabled { opacity: 0.45; cursor: not-allowed; }

.gf-final-header { margin-bottom: 1rem; }
.gf-final-header h3 { color: #007ACC; font-size: 1.3rem; }
.gf-final-instructions { font-size: 0.9rem; color: #374151; }
.gf-final-questions { margin-top: 1rem; }

.gf-you-first-note {
  font-size: 0.82rem; color: #6b7280; font-style: italic;
  margin: 0.25rem 0 0.25rem; padding: 0;
}

.gf-activity-spq { margin-top: 0.5rem; border-top: 1px dashed #d1d5db; padding-top: 0.4rem; }
.gf-activity-spq-title { font-size: 0.68rem; font-weight: 700; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.04em; margin-bottom: 0.3rem; }

@media (max-width: 640px) {
  .gf-profile-col { flex: 0 0 100%; }
  .gf-content-cols { gap: 0.75rem; }
  .gf-group-traits { grid-template-columns: repeat(2, 1fr); }
}
</style>
