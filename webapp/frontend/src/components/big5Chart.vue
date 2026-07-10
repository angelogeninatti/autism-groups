<script setup>
import { computed } from 'vue';
import { Pie } from 'vue-chartjs';
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  ArcElement,
} from 'chart.js';
import { useLanguageStore } from '@/stores/language.js';

const store = useLanguageStore();

ChartJS.register(Title, Tooltip, Legend, ArcElement);

const props = defineProps({
  openness: { type: Number, required: true },
  conscientiousness: { type: Number, required: true },
  emotionalStability: { type: Number, required: true },
  extroversion: { type: Number, required: true },
  agreeableness: { type: Number, required: true },
});

const chartData = computed(() => {
  const labels = store.currentLanguage === 'it'
    ? ['Apertura', 'Coscienziosità', 'Stabilità emotiva', 'Estroversione', 'Gradevolezza']
    : ['Openness', 'Conscientiousness', 'Emotional Stability', 'Extroversion', 'Agreeableness'];

  const values = [
    props.openness,
    props.conscientiousness,
    props.emotionalStability,
    props.extroversion,
    props.agreeableness
  ];

  return {
    labels,
    datasets: [
      {
        data: values,
        backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF'],
        borderColor: '#fff',
        borderWidth: 2
      }
    ]
  };
});

const chartOptions = {
  responsive: true,
  plugins: {
    legend: {
      position: 'bottom'
    },
    tooltip: {
      callbacks: {
        label: function(context) {
          const dataset = context.dataset;
          const total = dataset.data.reduce((acc, val) => acc + val, 0);
          const currentValue = dataset.data[context.dataIndex];
          const percentage = ((currentValue / total) * 100).toFixed(1);
          return `${context.label}: ${percentage}%`;
        }
      }
    }
  }
};
</script>

<template>
  <div class="chart-container" style="max-width: 400px; margin: auto;">
    <Pie :data="chartData" :options="chartOptions" />
  </div>
</template>

<style scoped>
.chart-container {
  position: relative;
}
</style>
