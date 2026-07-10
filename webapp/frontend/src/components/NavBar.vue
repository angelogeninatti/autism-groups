<script setup>
import LanguageDropdown from "@/components/LanguageDropdown.vue";
import ProgressBar from "@/components/ProgressBar.vue";
import { useRoute } from "vue-router";
import { useStudyStore } from '@/stores/studyStore.js';
import { computed } from "vue";

// Total number of page-advance button presses in the full study:
// SPQ 7 + BigF 2 + ActivityLiked 1 + Instructions 1 +
// ActivityTest×2 2 + GroupFormation×2 (9 candidates + 3 rounds + 1 final)×2 26 = 39
const TOTAL_STEPS = 39;

const route = useRoute();
const studyStore = useStudyStore();

const globalProgress = computed(() => {
  if (route.name === 'endTest') return 100;
  return Math.min(100, (studyStore.currentStep / TOTAL_STEPS) * 100);
});

const showProgress = computed(() => route.name !== 'home');
</script>

<template>
  <nav class="navbar fixed-top border-bottom shadow-sm" style="background-color: white !important;">
    <div class="container-fluid px-3">
      <div class="navbar-content">
        <div class="navbar-brand-section" style="visibility: hidden">
          <span class="navbar-title">aaaaaaa</span>
        </div>
        <div class="navbar-end">
          <LanguageDropdown />
        </div>
      </div>
    </div>
  </nav>

  <ProgressBar v-if="showProgress" :progress="globalProgress" />
</template>

<style scoped>
.navbar {
  height: 70px;
  z-index: 1000;
}

.container-fluid { height: 100%; }

.navbar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 100%;
}

.navbar-brand-section { flex-shrink: 0; }

.navbar-title {
  font-size: 1.75rem;
  font-weight: 500;
  color: #007ACC;
}

.navbar-end {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}
</style>
