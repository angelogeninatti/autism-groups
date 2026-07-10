<script setup>
import { useLanguageStore } from '@/stores/language.js'
import { computed, onMounted, onUnmounted, ref } from 'vue'

const store = useLanguageStore();

const windowWidth = ref(window.innerWidth);
const isMobile = computed(() => windowWidth.value <= 768);

const updateWidth = () => {
  windowWidth.value = window.innerWidth;
};

onMounted(() => {
  window.addEventListener('resize', updateWidth);
});

onUnmounted(() => {
  window.removeEventListener('resize', updateWidth);
});
</script>

<template>
  <div class="language-dropdown" :class="{ 'mobile-friendly': isMobile }">
    <button
        class="language-button dropdown-toggle"
        type="button"
        data-bs-toggle="dropdown"
        aria-expanded="false"
        :aria-label="store.currentLanguage === 'en' ? 'Select language - Current: English' : 'Seleziona lingua - Attuale: Italiano'"
    >
      <span v-if="store.currentLanguage === 'en'" class="language-option">
        <img
            src="https://flagcdn.com/16x12/gb.png"
            alt="English" 
            class="flag-icon">
        <span class="language-text">English</span>
      </span>
      <span v-else class="language-option">
        <img
            src="https://flagcdn.com/16x12/it.png"
            alt="Italian" 
            class="flag-icon">
        <span class="language-text">Italiano</span>
      </span>
    </button>
    <ul class="dropdown-menu dropdown-menu-end language-menu" aria-labelledby="languageDropdown">
      <li>
        <a
            @click="store.setLanguage('en')"
            class="dropdown-item language-item"
            :class="{ 'active': store.currentLanguage === 'en' }"
            role="button"
            tabindex="0"
            @keydown.enter="store.setLanguage('en')"
        >
          <img
              src="https://flagcdn.com/16x12/gb.png"
              alt="English" 
              class="flag-icon">
          <span class="language-text">English</span>
        </a>
      </li>
      <li>
        <a
            @click="store.setLanguage('it')"
            class="dropdown-item language-item"
            :class="{ 'active': store.currentLanguage === 'it' }"
            role="button"
            tabindex="0"
            @keydown.enter="store.setLanguage('it')"
        >
          <img
              src="https://flagcdn.com/16x12/it.png"
              alt="Italian" 
              class="flag-icon">
          <span class="language-text">Italiano</span>
        </a>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.language-dropdown {
  position: relative;
  width: 180px;
  margin-right: 0.5rem;
}

.language-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0.5rem 1rem;
  background-color: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #495057;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.language-button:hover {
  background-color: #e9ecef;
  border-color: #adb5bd;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.language-button:focus {
  outline: none;
  border-color: #007ACC;
  box-shadow: 0 0 0 2px rgba(0, 122, 204, 0.25);
}

.language-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
}

.flag-icon {
  width: 16px;
  height: 12px;
  flex-shrink: 0;
  border-radius: 1px;
}

.language-text {
  font-size: 0.875rem;
  white-space: nowrap;
}

.language-menu {
  min-width: 180px;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background-color: white;
  z-index: 1050;
}

.language-item {
  display: flex !important;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  color: #495057;
  text-decoration: none;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  cursor: pointer;
}

.language-item:hover {
  background-color: #f8f9fa;
  color: #343a40;
}

.language-item.active {
  background-color: #007ACC;
  color: white;
}

.language-item.active:hover {
  background-color: #0056b3;
}

@media (max-width: 768px) {
  .language-dropdown {
    width: 100%;
    max-width: 200px;
  }

  .language-button {
    padding: 0.75rem 1rem;
    font-size: 1rem;
    min-height: 44px; 
  }

  .language-text {
    font-size: 1rem;
  }

  .language-menu {
    min-width: 100%;
    max-width: 200px;
    font-size: 1rem;
  }

  .language-item {
    padding: 1rem;
    font-size: 1rem;
    min-height: 44px; 
  }

  .flag-icon {
    width: 20px;
    height: 15px;
  }
}

@media (max-width: 480px) {
  .language-dropdown {
    width: 100%;
    max-width: 150px;
  }

  .language-button {
    padding: 0.625rem 0.875rem;
    font-size: 0.95rem;
  }

  .language-text {
    font-size: 0.95rem;
  }

  .language-menu {
    min-width: 150px;
    font-size: 0.95rem;
  }

  .language-item {
    padding: 0.875rem;
    font-size: 0.95rem;
  }
}

.mobile-friendly {
  width: 100%;
  max-width: none;
}

.mobile-friendly .language-button {
  justify-content: center;
  border: none;
  background-color: transparent;
  padding: 0.75rem;
  font-size: 1rem;
  box-shadow: none;
}

.mobile-friendly .language-button:hover {
  background-color: #f8f9fa;
}

.mobile-friendly .language-menu {
  width: 100%;
  position: static;
  box-shadow: none;
  border: none;
  border-top: 1px solid #e5e7eb;
  border-radius: 0;
  margin-top: 0.5rem;
}
</style>