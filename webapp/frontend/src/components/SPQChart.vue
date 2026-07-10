<script setup>
import { onMounted, onBeforeUnmount, watch, computed, ref } from 'vue';
import ApexCharts from 'apexcharts';
import { useLanguageStore } from "@/stores/language.js";

const store = useLanguageStore();

const props = defineProps({
  SPQValue: Object,
  chartIdPrefix: {
    type: String,
    default: () => `chart-${Math.random().toString(36).substr(2, 5)}`
  },
  spq: {
    type: Boolean,
    default: true
  }
});

watch(
  () => store.currentLanguage,
  () => {
    if (charts['vertical']) charts['vertical'].updateOptions(getChartOptionsVertical());

    senses.value.forEach(sense => {
      if (charts[sense.key]) charts[sense.key].updateOptions(getChartOptions(sense));
    });
  }
);


let charts = {}; 
const colors = ['#008FFB'];

const senses = computed(() => {
  if (!props.SPQValue || typeof props.SPQValue !== 'object') return [];

  if (props.spq) {
    const requiredProps = [
      'visionScore', 'hearingScore', 'smellScore', 'tasteScore', 'touchScore',
      'visionDescription', 'hearingDescription', 'smellDescription', 'tasteDescription', 'touchDescription'
    ];
    const hasAllProps = requiredProps.every(prop => props.SPQValue.hasOwnProperty(prop));
    if (!hasAllProps) return [];

    return [
      { key: 'vision', score: props.SPQValue.visionScore || 0, desc: props.SPQValue.visionDescription || '' },
      { key: 'hearing', score: props.SPQValue.hearingScore || 0, desc: props.SPQValue.hearingDescription || '' },
      { key: 'smell', score: props.SPQValue.smellScore || 0, desc: props.SPQValue.smellDescription || '' },
      { key: 'taste', score: props.SPQValue.tasteScore || 0, desc: props.SPQValue.tasteDescription || '' },
      { key: 'touch', score: props.SPQValue.touchScore || 0, desc: props.SPQValue.touchDescription || '' }
    ];
  } else {
    return [
      { key: 'agreeableness', score: props.SPQValue.agreeablenessScore || 0, desc: props.SPQValue.agreeablenessDescription || '' },
      { key: 'conscientiousness', score: props.SPQValue.conscientiousnessScore || 0, desc: props.SPQValue.conscientiousnessDescription || '' },
      { key: 'openness', score: props.SPQValue.opennessScore || 0, desc: props.SPQValue.opennessDescription || '' },
      { key: 'extroversion', score: props.SPQValue.extroversionScore || 0, desc: props.SPQValue.extroversionDescription || '' },
      { key: 'emotionalStability', score: props.SPQValue.emotionalStabilityScore || 0, desc: props.SPQValue.emotionalStabilityDescription || '' }
    ];
  }
});

const getChartOptions = (sense) => ({
  yaxis: { labels: { style: { fontSize: '14px' } } },
  xaxis: { min: 0, max: 100, labels: { style: { fontSize: '12px' } } },
  chart: { toolbar: { show: false }, type: 'bar', height: 110 },
  plotOptions: { bar: { horizontal: true } },
  series: [{
    data: [{
      x: store.translationSPQChart?.[sense.key] || sense.key,
      y: sense.score,
      desc: sense.desc
    }]
  }],
  tooltip: {
    enabled: true,
    custom: ({ seriesIndex, dataPointIndex, w }) => {
      const title = w.config.series[seriesIndex].data[dataPointIndex].x;
      const description = w.config.series[seriesIndex].data[dataPointIndex].desc || '';
      return `
        <div class="bg-white border rounded p-1 shadow-sm">
          <div class="fw-bold text-dark mb-1" style="font-size: 14px;">${title}</div>
          <div class="text-muted" style="font-size: 12px;">${description}</div>
        </div>
      `;
    }
  }
});

const getChartOptionsVertical = () => {
  const categories = props.spq
    ? [
        store.currentLanguage === 'it' ? "Vista" : "Vision",
        store.currentLanguage === 'it' ? "Udito" : "Hearing",
        store.currentLanguage === 'it' ? "Olfatto" : 'Smell',
        store.currentLanguage === 'it' ? "Gusto" : 'Taste',
        store.currentLanguage === 'it' ? "Tatto" : 'Touch'
      ]
    : [
        store.currentLanguage === 'it' ? "Amicalità" : "Agreeableness",
        store.currentLanguage === 'it' ? "Coscienziosità" : "Conscientiousness",
        store.currentLanguage === 'it' ? "Apertura" : "Openness",
        store.currentLanguage === 'it' ? "Estroversione" : "Extroversion",
        store.currentLanguage === 'it' ? "Stabilità Emotiva" : "Emotional Stability"
      ];

  const data = props.spq
    ? [
        props.SPQValue.visionScore,
        props.SPQValue.hearingScore,
        props.SPQValue.smellScore,
        props.SPQValue.tasteScore,
        props.SPQValue.touchScore
      ]
    : [
        props.SPQValue.agreeablenessScore,
        props.SPQValue.conscientiousnessScore,
        props.SPQValue.opennessScore,
        props.SPQValue.extroversionScore,
        props.SPQValue.emotionalStabilityScore
      ];

  return {
    series: [{ data }],
    chart: {
      type: 'bar',
      height: 400,
      toolbar: { show: false },
      selection: { enabled: false },
      events: {
        dataPointSelection: function(event, chartContext, config) {
          return false;
        }
      },
    },
    states: {
      normal: {
        filter: { type: 'none' }
      },
      hover: {
        filter: { type: 'none' },
        opacity: 1
      },
      active: {
        allowMultipleDataPointsSelection: false,
        filter: { type: 'none' },
        opacity: 1
      }
    },
    colors,
    plotOptions: {
      bar: {
        columnWidth: '45%',
        distributed: false,
        borderRadius: 20,
        borderRadiusApplication: 'end'
      }
    },
    dataLabels: { enabled: false },
    legend: { show: false },
    xaxis: {
      categories,
      labels: {
        rotate: -30,
        style: {
          colors: 'black',
          fontSize: '11px'
        },
        offsetY: 5,
      },
      crosshairs: {
        show: false,
        width: 0,
      },
    },
    yaxis: {
      max: 100,
      stepSize: 10
    },
    tooltip: { 
      enabled: true,
      x: { show: false },
      y: {
        formatter: function (value, { dataPointIndex, w }) {
          const label = w.globals.labels[dataPointIndex];
          return label + ": " + value; 
        },
        title: { formatter: function () { return ''; } }
      }
    },
    fill: {
      type: 'gradient',
      gradient: {
        shade: 'light',
        type: "horizontal",
        shadeIntensity: 0.25,
        gradientToColors: undefined,
        inverseColors: true,
        opacityFrom: 0.85,
        opacityTo: 0.85,
        stops: [50, 0, 100]
      },
    }
  };
};

onMounted(() => {
  const hasDescription = senses.value.every(s => s.desc);

  if (props.spq && hasDescription) {
    senses.value.forEach(sense => {
      const id = `#${props.chartIdPrefix}-${sense.key}`;
      const chart = new ApexCharts(document.querySelector(id), getChartOptions(sense));
      chart.render();
      charts[sense.key] = chart;
    });
  } else {
    const id = `#${props.chartIdPrefix}-vertical`;
    const chart = new ApexCharts(document.querySelector(id), getChartOptionsVertical());
    chart.render();
    charts['vertical'] = chart;
  }
});

watch(senses, (newSenses) => {
  Object.keys(charts).forEach(key => {
    if (key === 'vertical') {
      charts[key].updateOptions(getChartOptionsVertical());
    } else {
      charts[key].updateOptions(getChartOptions(newSenses.find(s => s.key === key)));
    }
  });
}, { deep: true });

onBeforeUnmount(() => {
  Object.values(charts).forEach(chart => chart.destroy());
});
</script>

<template>
  <div v-if="senses.length">
    <div v-if="props.spq && senses.every(s => s.desc)">
      <div v-for="sense in senses" :key="sense.key" class="chart-container mb-4 border-top border-2">
        <div :id="`${props.chartIdPrefix}-${sense.key}`"></div>
        <div class="text-sm ms-2">{{ sense.desc }}</div>
      </div>
    </div>
    <div v-else class="chart-container mb-4">
      <div :id="`${props.chartIdPrefix}-vertical`"></div>
    </div>
  </div>
  <div v-else class="text-center p-3">
    <div class="text-muted">No chart data available</div>
  </div>
</template>

<style scoped>
.chart-container > div {
  min-height: 0 !important;
}
</style>
