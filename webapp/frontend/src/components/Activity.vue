<script setup>
import {useLanguageStore} from "@/stores/language.js";
import {computed} from "vue";

const props = defineProps({
  activity: Object,
  activityIndex: Number,
});

const store = useLanguageStore();

const activityTranslated = computed(() => {
  if (store.currentLanguage === 'en') {
    return {
      id: props.activity.id,
      title: props.activity["titleEnglish"],
      description: props.activity["descriptionEnglish"],
      category: props.activity["categoryEnglish"],
      city: props.activity["city"],
      spqEvaluation: {
        visionScore: props.activity["spqEvaluation"]["visionScore"],
        hearingScore: props.activity["spqEvaluation"]["hearingScore"],
        smellScore: props.activity["spqEvaluation"]["smellScore"],
        tasteScore: props.activity["spqEvaluation"]["tasteScore"],
        touchScore: props.activity["spqEvaluation"]["touchScore"],
        visionDescription: props.activity["spqEvaluation"]["visionDescriptionEnglish"],
        hearingDescription: props.activity["spqEvaluation"]["hearingDescriptionEnglish"],
        smellDescription: props.activity["spqEvaluation"]["smellDescriptionEnglish"],
        tasteDescription: props.activity["spqEvaluation"]["tasteDescriptionEnglish"],
        touchDescription: props.activity["spqEvaluation"]["touchDescriptionEnglish"],
      }
    };
  } else if (store.currentLanguage === 'it') {
    return {
      id: props.activity.id,
      title: props.activity["titleItalian"],
      description: props.activity["descriptionItalian"],
      category: props.activity["categoryItalian"],
      city: props.activity["city"],
      spqEvaluation: {
        visionScore: props.activity["spqEvaluation"]["visionScore"],
        hearingScore: props.activity["spqEvaluation"]["hearingScore"],
        smellScore: props.activity["spqEvaluation"]["smellScore"],
        tasteScore: props.activity["spqEvaluation"]["tasteScore"],
        touchScore: props.activity["spqEvaluation"]["touchScore"],
        visionDescription: props.activity["spqEvaluation"]["visionDescriptionItalian"],
        hearingDescription: props.activity["spqEvaluation"]["hearingDescriptionItalian"],
        smellDescription: props.activity["spqEvaluation"]["smellDescriptionItalian"],
        tasteDescription: props.activity["spqEvaluation"]["tasteDescriptionItalian"],
        touchDescription: props.activity["spqEvaluation"]["touchDescriptionItalian"],
      }
    };
  }
})

</script>

<template>
  <div v-if="activityTranslated && activityTranslated.title">
    <div class="card shadow-sm mb-4" style="border-radius: 10px; border: none;">
      <div class="card-body p-4">
        <div class="card-title fw-bold fs-3">{{ activityTranslated.title }}</div>

        <div class="badge bg-primary mb-2" style="background-color: #3498db !important;">
          {{ activityTranslated.category }}
        </div>
        <div class="mt-2">
          <font-awesome-icon icon="map-marker-alt" /> {{ activityTranslated.city }}
        </div>

        <p class="card-text my-1" style="line-height: 1.6;">
          {{ activityTranslated.description }}
        </p>

        <div class="mt-3" v-if="activityTranslated.spqEvaluation">
          <div class="fw-bold mb-1">{{ store.translationSPQChart["evaluationTitle"] }}</div>
          <div class="spq-subtitle mb-2">{{ store.translationSPQChart["evaluationSubtitle"] }}</div>
          <div class="spq-bars">
            <div v-for="(sense, key) in {
              [store.currentLanguage === 'it' ? 'Vista' : 'Sight']: activityTranslated.spqEvaluation.visionScore,
              [store.currentLanguage === 'it' ? 'Udito' : 'Hearing']: activityTranslated.spqEvaluation.hearingScore,
              [store.currentLanguage === 'it' ? 'Olfatto' : 'Smell']: activityTranslated.spqEvaluation.smellScore,
              [store.currentLanguage === 'it' ? 'Gusto' : 'Taste']: activityTranslated.spqEvaluation.tasteScore,
              [store.currentLanguage === 'it' ? 'Tatto' : 'Touch']: activityTranslated.spqEvaluation.touchScore,
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

  <div v-else class="text-center py-5">
    <span>Loading...</span>
  </div>
</template>

<style scoped>
.spq-subtitle { font-size: 0.78rem; color: #6b7280; font-style: italic; margin-top: 0; }
.spq-bars { display: flex; flex-direction: column; gap: 4px; }
.spq-bar-row { display: flex; align-items: center; gap: 8px; font-size: 0.82rem; }
.spq-sense-label { min-width: 52px; color: #555; }
.spq-bar-bg { flex: 1; height: 10px; background: #e9ecef; border-radius: 5px; overflow: hidden; }
.spq-bar-fill { height: 100%; background: linear-gradient(90deg, #007ACC, #33a474); border-radius: 5px; }
.spq-score { min-width: 40px; text-align: right; color: #777; }
</style>
