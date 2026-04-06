import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'

// Redirect sasquatsh.web.app to sasquatsh.com (SEO: avoid duplicate content)
// Skip redirect if returning from Firebase Auth OAuth flow (has __/auth in path or auth params in hash)
const isAuthCallback = window.location.pathname.includes('__/auth') ||
  window.location.hash.includes('state=') ||
  window.location.search.includes('mode=')
if (window.location.hostname === 'sasquatsh.web.app' && !isAuthCallback) {
  window.location.replace(`https://sasquatsh.com${window.location.pathname}${window.location.search}`)
} else {
  createApp(App).use(router).mount('#app')
}
