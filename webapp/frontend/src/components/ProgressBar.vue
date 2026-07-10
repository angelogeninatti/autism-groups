<script setup>
defineProps({
  progress: {
    type: Number,
    required: true,
    validator: value => value >= 0 && value <= 100
  }
})
</script>

<template>
  <div class="progress-container">
    <div class="progress">
      <div
        class="progress-bar progress-bar-animated"
        role="progressbar"
        :style="{ width: progress + '%' }"
        :aria-valuenow="progress"
        aria-valuemin="0"
        aria-valuemax="100"
      ></div>
    </div>
  </div>
</template>

<style scoped>
.progress-container {
  position: fixed;
  top: 70px;
  left: 0;
  right: 0;
  z-index: 999;
  padding: 0;
  background: transparent;
}

.progress {
  height: 5px;
  border-radius: 0;
  overflow: hidden;
  background: #e9ecef;
}

.progress-bar {
  background: linear-gradient(90deg, #007ACC, #33a474);
  transition: width 0.4s ease;
  border-radius: 0 3px 3px 0;
}
</style>
