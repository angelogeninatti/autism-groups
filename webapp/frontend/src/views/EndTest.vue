<script setup>
import {useRouter} from "vue-router";
import {useLanguageStore} from "@/stores/language.js";
import {onMounted, ref} from "vue";
import {config} from "@/config.js";

const store = useLanguageStore();

const router = useRouter();

const API_BASE = config.API_BASE

function redirectToProlific() {
  window.location.href = "https://app.prolific.com/submissions/complete?cc=" + completionCode.value;
}

const completionCode = ref("");

async function getCompletionCodeCall() {
  try {
    const response = await fetch(API_BASE + '/completionCode?' + new URLSearchParams({
      action: "get_completion_code"
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

onMounted(() => {
  getCompletionCodeCall()
      .then(result => {
        completionCode.value = result;
      })
      .catch(error => {
        sessionStorage.clear();
        router.push({
          path: '/error',
          query: { message: 'We are sorry. There was an error!' },
        });
      });
});
</script>

<template>
  <div class="success-container text-center p-4">
    <h1 class="mb-3">{{ store.translationEnd["title"] }}</h1>
    <p class="text-muted">
      {{ store.translationEnd["subTitle"] }}
    </p>
    <p v-if="completionCode">{{ store.translationEnd["completionBefore"] }} <strong class="fs-5"> {{ completionCode }} </strong>{{ store.translationEnd["completionAfter"] }}</p>
    <button
        class="btn btn-success mt-4"
        @click="redirectToProlific()"
    >
      {{ store.translationEnd["button"] }}
    </button>
  </div>
</template>

<style scoped>
.success-container {
  max-width: 600px;
  margin: 2rem auto;
  padding: 2rem;
  text-align: center;
  border: 1px solid #28a745;
  border-radius: 8px;
  background-color: #e6ffed;
}

h1 {
  color: #28a745;
  margin-bottom: 1rem;
}

p {
  color: #2f4f4f;
  margin-bottom: 1.5rem;
}

.btn-success {
  background-color: #28a745;
  border-color: #28a745;
  color: white;
}

.btn-success:hover {
  background-color: #218838;
  border-color: #1e7e34;
}
</style>