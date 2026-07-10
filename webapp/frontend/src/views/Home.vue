<script setup>
import NavBar from "@/components/NavBar.vue";
import InformedConsent from "@/views/InformedConsent.vue";
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { onMounted, ref } from "vue";
import { config } from "@/config.js";
import { useLanguageStore } from '@/stores/language.js'
import { useFocusMonitorStore } from "@/stores/focusMonitor.js";
import { useStudyStore } from "@/stores/studyStore.js";

const store = useLanguageStore();
const studyStore = useStudyStore();

const route = useRoute();
const prolificId = new URLSearchParams(window.location.search).get('PROLIFIC_PID') ?? route.query.PROLIFIC_PID ?? null;
const neurotype = new URLSearchParams(window.location.search).get('type') ?? route.query.type ?? null;
const router = useRouter()
const API_BASE = config.API_BASE
const focusMonitorStore = useFocusMonitorStore();

const consentGiven = ref(sessionStorage.getItem('consentGiven') === 'true');

function onConsentSubmit() {
  consentGiven.value = true;
}

async function checkSession() {
  const response = await fetch(API_BASE + '/checkSession', {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
  });
  if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
  return await response.text();
}

async function createSession(id) {
  const params = { prolificId: id };
  if (neurotype) params.type = neurotype;
  const response = await fetch(API_BASE + '/createSession?' + new URLSearchParams(params), {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
  });
  if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
  return await response.text();
}

function startTest() {
  checkSession()
    .then(result => {
      if (result === "nok") {
        sessionStorage.clear();
        if (studyStore.testing) sessionStorage.setItem('testing', 'true');
        const effectiveId = prolificId ?? 'undefined'
        createSession(String(effectiveId))
          .then(result => {
            if (result === "nok") {
              sessionStorage.clear();
              router.push({ path: '/error', query: { message: 'Your prolificId is already registered. You can only take the tests one time.' } });
            } else {
              sessionStorage.setItem('specificId', String(result));
              if (prolificId != null) sessionStorage.setItem('prolificId', prolificId);
              focusMonitorStore.initMonitor(prolificId != null && !studyStore.testing);
              router.push({ path: '/spq' });
            }
          })
          .catch(() => {
            sessionStorage.clear();
            router.push({ path: '/error', query: { message: 'Error in creating session. Please try again later.' } });
          });
      } else {
        result = result.split(",");
        const progress = result[1].trim();
        const specificId = result[0];
        sessionStorage.setItem('specificId', specificId);
        if (prolificId != null) sessionStorage.setItem('prolificId', prolificId);
        focusMonitorStore.initMonitor(prolificId != null && !studyStore.testing);
        if (progress === "undefined") router.push({ path: '/spq' });
        else if (progress === "spq") router.push({ path: '/bigF' });
        else if (progress === "bigF") router.push({ path: '/activity_valuation' });
        else router.push({ path: '/error', query: { message: 'You probably already completed this test.' } });
      }
    })
    .catch(() => {
      sessionStorage.clear();
      router.push({ path: '/error', query: { message: 'Error while checking for an existing session. Please try again later.' } });
    });
}
</script>

<template>
  <div class="container pt-4 text-center">
    <!-- Informed consent gate -->
    <div v-if="!consentGiven" class="card p-4 mb-4 mx-auto" style="max-width: 800px;">
      <InformedConsent @submit="onConsentSubmit" />
    </div>

    <!-- Study description + start -->
    <div v-else class="card p-4 mb-4 mx-auto" style="max-width: 800px;">
      <h3 class="mb-3">{{ store.translationHome.title }}</h3>
      <p class="lead">{{ store.translationHome.subTitle }}</p>

      <div class="d-flex flex-column gap-3 text-start mb-4">
        <div class="test-card p-3">
          <h5>{{ store.translationHome.spq.title }}</h5>
          <p>{{ store.translationHome.spq.desc }}</p>
        </div>
        <div class="test-card p-3">
          <h5>{{ store.translationHome.bigF.title }}</h5>
          <p>{{ store.translationHome.bigF.desc }}</p>
        </div>
        <div class="test-card p-3">
          <h5>{{ store.translationHome.activityValuation.title }}</h5>
          <p>{{ store.translationHome.activityValuation.desc }}</p>
        </div>
      </div>

      <div v-if="studyStore.testing" class="alert alert-warning mb-3" style="font-size:0.85rem;">
        ⚙️ Testing mode active — captcha and focus monitor disabled
      </div>

      <div class="btn btn-primary border px-4 text-decoration-none" @click="startTest">
        {{ store.translationHome.button }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.test-card {
  background-color: #f8f9fa;
  border-left: 4px solid #007ACC;
}

.test-card:hover {
  background-color: #e9ecef;
  transform: translateX(5px);
  transition: all 0.1s ease;
}
</style>
