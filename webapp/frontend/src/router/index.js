import {createRouter, createWebHashHistory} from 'vue-router'
import SPQTest from '@/views/SPQTest.vue'
import Home from "@/views/Home.vue";
import BigFTest from "@/views/BigFTest.vue";
import ActivityValuation from "@/views/ActivityValuation.vue";
import Error from "@/views/Error.vue";
import {config} from "@/config.js";
import EndTest from "@/views/EndTest.vue";

const API_BASE = config.API_BASE
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/spq',                name: 'spq',                component: SPQTest },
    { path: '/bigF',               name: 'bigF',               component: BigFTest },
    { path: '/activity_valuation', name: 'activity_valuation', component: ActivityValuation },
    { path: '/',                   name: 'home',               component: Home },
    { path: '/endTest',            name: 'endTest',            component: EndTest },
    {
      path: '/error',
      name: 'error',
      component: Error,
      props: (route) => ({ errorMessage: route.query.message }),
    },
    { path: '/', redirect: '/home' },
    {
      path: '/:pathMatch(.*)*',
      redirect: { path: '/error', query: { message: 'Page not found!' } }
    }
  ],
});

async function checkSession() {
  try {
    const response = await fetch(API_BASE + '/checkSession', {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    return await response.text();
  } catch (error) {
    console.error('Error checking session:', error);
    throw error;
  }
}

router.beforeEach((to, from, next) => {
  const protectedPath = ["/bigF", "/spq", "/activity_valuation", "/endTest"];
  if (protectedPath.includes(to.path)) {
    checkSession()
      .then(result => {
        if (result === "nok") {
          sessionStorage.clear();
          next({ path: '/error', query: { message: 'Session does not exist.' } });
        } else {
          result = result.split(",");
          const progress = result[1].trim();
          const specificId = result[0];
          if (
            (to.path === "/spq"                && progress === "undefined") ||
            (to.path === "/bigF"               && progress === "spq") ||
            (to.path === "/activity_valuation" && progress === "bigF") ||
            (to.path === "/endTest"            && progress === "activity")
          ) {
            sessionStorage.setItem('specificId', specificId);
            next();
          } else {
            next({ path: '/error', query: { message: "You can't access this page" } });
          }
        }
      })
      .catch(() => {
        sessionStorage.clear();
        next({ path: '/error', query: { message: 'Error while checking for an existing session. Please try again later.' } });
      });
  } else {
    next();
  }
});

export default router
