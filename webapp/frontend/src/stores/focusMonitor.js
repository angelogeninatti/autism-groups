import { defineStore } from 'pinia'
import {computed, reactive, ref} from 'vue'
import FocusMonitor from "@/toolbox/FocusMonitor.js";

export const useFocusMonitorStore  = defineStore('focusMonitor', () => {
    // State
    const focusMonitor = ref(null);
    // Getters
    const getFocusMonitor = computed(() => focusMonitor.value);

    // Actions
    function redirectToProlific() {
        window.location.href = "https://app.prolific.com/submissions/complete?cc=C19X81NG";
    }

    function isMobileDevice() {
        const mobileRegex = /android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini|mobile|tablet/i;
        const hasTouchPoints = navigator.maxTouchPoints > 1;
        const isSmallScreen = window.screen.width < 768;
        return mobileRegex.test(navigator.userAgent) || (hasTouchPoints && isSmallScreen);
    }

    function checkMobileAndRedirect() {
        if (isMobileDevice()) {
            window.location.href = "https://app.prolific.com/submissions/complete?cc=CCXKVBZB";
            return true;
        }
        return false;
    }

    function initMonitor(shouldMonitor) {
        if (shouldMonitor && !focusMonitor.value) {
            focusMonitor.value = new FocusMonitor(
                () => console.log('Focus lost'),
                () => console.log('Focus regained'),
                () => redirectToProlific()
            );
        }
    }


    return { initMonitor, getFocusMonitor, checkMobileAndRedirect }
});
