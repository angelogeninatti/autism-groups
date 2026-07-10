<script setup>
import { RouterView, useRoute } from 'vue-router'
import NavBar from "@/components/NavBar.vue";
import { onMounted } from "vue";
import { useLanguageStore } from '@/stores/language.js';
import { useStudyStore } from '@/stores/studyStore.js';
import { useFocusMonitorStore } from '@/stores/focusMonitor.js';

const langStore = useLanguageStore();
const studyStore = useStudyStore();
const focusMonitorStore = useFocusMonitorStore();
const route = useRoute();

onMounted(() => {
  const urlParams = new URLSearchParams(window.location.search);

  // Reject mobile users immediately
  focusMonitorStore.checkMobileAndRedirect();

  // Auto-select language from ?lang= parameter
  const lang = urlParams.get('lang') ?? route.query.lang;
  if (lang === 'en' || lang === 'it') {
    langStore.setLanguage(lang);
  }

  // Enable testing mode from ?testing= parameter (read from native query string, before the hash)
  const testing = urlParams.get('testing') ?? route.query.testing;
  if (testing === 'true' || testing === '1') {
    studyStore.setTesting(true);
    sessionStorage.setItem('testing', 'true');
  } else if (sessionStorage.getItem('testing') === 'true') {
    studyStore.setTesting(true);
  }

  // Start focus monitor as soon as PROLIFIC_PID is known (either from URL or a previous step)
  const prolificId = urlParams.get('PROLIFIC_PID') ?? sessionStorage.getItem('prolificId');
  if (prolificId) {
    focusMonitorStore.initMonitor(!studyStore.testing);
  }
});
</script>

<template>
  <div class="app-container">
    <main class="content">
      <NavBar />
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.content {
  flex: 1;
  padding-top: 75px;
  position: relative;
}
</style>

<style>
* { caret-color: transparent; }
</style>
