<script setup>
defineProps({
  progress: { type: Number, required: true },
  title: { type: String, default: null }
})
</script>

<template>
  <div class="test-container">
    <h4 v-if="title" class="test-title">{{ title }}</h4>
    <slot />
  </div>
</template>

<style scoped>
.test-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}
.test-title {
  text-align: center;
  color: #007ACC;
  font-weight: 600;
  margin-bottom: 1.25rem;
}
</style>
