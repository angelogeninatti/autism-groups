<script setup>
import {useRouter} from "vue-router";
import {useLanguageStore} from "@/stores/language.js";
import {onMounted, ref} from "vue";
import {config} from "@/config.js";

const store = useLanguageStore();
const API_BASE = config.API_BASE;
const props = defineProps({
  errorMessage: {
    type: String,
    default: null
  }
});

const router = useRouter();
function handleHomeButton() {
  router.push({
    path: '/',
  });
}

async function getCompletionCodeCall() {
  try {
    const response = await fetch(API_BASE + '/completionCode?' + new URLSearchParams({
      action: "error"
    }), {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error getActivityQuestions:', error);
    throw error;
  }
}

function redirectToProlific() {
  window.location.href = "https://app.prolific.com/submissions/complete?cc=" + completionCode.value;
}

const completionCode = ref("");

onMounted(() => {
  getCompletionCodeCall()
      .then(result => {
        if(result !== "not_prolific" && result !== "no_session") {
          completionCode.value = result;
          redirectToProlific();
        }
      })
      .catch(error => {
        sessionStorage.clear();
      });
});


</script>

<template>
  <div class="error-container text-center p-4">
    <h1 class="mb-3">{{store.translationError["message"]}}</h1>
    <p v-if="errorMessage" class="text-danger">{{ errorMessage }}</p>
    <p v-else class="text-muted">
      We couldn't process your request. Please try again later.
    </p>
    <button
        class="btn btn-primary mt-4"
        @click="handleHomeButton()"
    >
      Home
    </button>
  </div>
</template>


<style scoped>
.error-container {
  max-width: 600px;
  margin: 2rem auto;
  padding: 2rem;
  text-align: center;
  border: 1px solid #ff6b6b;
  border-radius: 8px;
  background-color: #fff5f5;
}

h1 {
  color: #ff6b6b;
  margin-bottom: 1rem;
}

p {
  color: #495057;
  margin-bottom: 1.5rem;
}

</style>