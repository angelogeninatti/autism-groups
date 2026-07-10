import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import en from '@/translations/en.json';
import it from '@/translations/it.json';

export const useLanguageStore = defineStore('language', () => {
    // State
    const currentLanguage = ref('en')

    // Getters
    const translationHome = computed(() => {
            if (currentLanguage.value === 'en') {
                return en.home;
            } else if (currentLanguage.value === 'it') {

                return it.home;
            } else {
                return en.home;
            }
        }
    );

    const translationNavBar = computed(() => {
            if (currentLanguage.value === 'en') {
                return en.navbar;
            } else if (currentLanguage.value === 'it') {
                return it.navbar;
            } else {
                return en.navbar;
            }
        }
    );

    const translationSPQ = computed(() => {
            if (currentLanguage.value === 'en') {
                return en.spq;
            } else if (currentLanguage.value === 'it') {
                return it.spq;
            } else {
                return en.spq;
            }
        }
    );

    const translationBigF = computed(() => {
            if (currentLanguage.value === 'en') {
                return en.bigF;
            } else if (currentLanguage.value === 'it') {
                return it.bigF;
            } else {
                return en.bigF;
            }
        }
    );

    const translationActivity = computed(() => {
            if (currentLanguage.value === 'en') {
                return en.activity;
            } else if (currentLanguage.value === 'it') {
                return it.activity;
            } else {
                return en.activity;
            }
        }
    );

    const translationActivityValuation = computed(() => {
            if (currentLanguage.value === 'en') {
                return en.activityValuation;
            } else if (currentLanguage.value === 'it') {
                return it.activityValuation;
            } else {
                return en.activityValuation;
            }
        }
    );

    const translationSPQChart = computed(() => {
            if (currentLanguage.value === 'en') {
                return en.SPQChart;
            } else if (currentLanguage.value === 'it') {
                return it.SPQChart;
            } else {
                return en.SPQChart;
            }
        }
    );

    const translationError = computed(() => {
            if (currentLanguage.value === 'en') {
                return en.error;
            } else if (currentLanguage.value === 'it') {
                return it.error;
            } else {
                return en.error;
            }
        }
    );

    const translationEnd = computed(() => {
            if (currentLanguage.value === 'en') {
                return en.endTest;
            } else if (currentLanguage.value === 'it') {
                return it.endTest;
            } else {
                return en.endTest;
            }
        }
    );

    //this property is no more in use
    const translationAttentionChecks = computed(() => {
            if (currentLanguage.value === 'en') {
                return en.attentionChecks;
            } else if (currentLanguage.value === 'it') {
                return it.attentionChecks;
            } else {
                return en.attentionChecks;
            }
        }
    );

    // Actions
    function setLanguage(lang) {
        currentLanguage.value = lang
    }

    function getSpecificAttentionChecks(lang) {
        if (lang === "en") {
            return en.attentionChecks;
        } else if (lang === "it") {
            return it.attentionChecks;
        } else {
            return en.attentionChecks;
        }
    }

    return { currentLanguage, translationHome, translationNavBar, translationSPQ, translationBigF, translationActivity,  translationActivityValuation, translationSPQChart, translationError, translationEnd, translationAttentionChecks, setLanguage, getSpecificAttentionChecks}
});
