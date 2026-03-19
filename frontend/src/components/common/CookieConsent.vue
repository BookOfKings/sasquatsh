<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const showBanner = ref(false)

const CONSENT_KEY = 'sasquatsh_cookie_consent'

type CookieConsentLevel = 'all' | 'essential'

onMounted(() => {
  const consent = localStorage.getItem(CONSENT_KEY)
  if (!consent) {
    showBanner.value = true
  }
})

function saveConsent(level: CookieConsentLevel) {
  localStorage.setItem(CONSENT_KEY, JSON.stringify({
    level,
    timestamp: new Date().toISOString(),
  }))
  showBanner.value = false
}

function acceptAll() {
  saveConsent('all')
}

function essentialOnly() {
  saveConsent('essential')
}

function goToCookiePolicy() {
  router.push('/cookies')
}
</script>

<template>
  <Transition
    enter-active-class="transition-transform duration-300 ease-out"
    enter-from-class="translate-y-full"
    enter-to-class="translate-y-0"
    leave-active-class="transition-transform duration-200 ease-in"
    leave-from-class="translate-y-0"
    leave-to-class="translate-y-full"
  >
    <div
      v-if="showBanner"
      class="fixed bottom-0 left-0 right-0 z-50 bg-gray-900 text-white p-4 shadow-lg"
    >
      <div class="container-wide">
        <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div class="flex-1">
            <p class="text-sm">
              We use cookies and similar technologies to enhance your experience, analyze site usage, and assist in our marketing efforts. By continuing to use this site, you consent to our use of cookies.
              <button
                @click="goToCookiePolicy"
                class="underline hover:text-primary-300 ml-1"
              >
                Learn more
              </button>
            </p>
          </div>
          <div class="flex items-center gap-2 sm:gap-3 flex-shrink-0 flex-wrap">
            <button
              @click="goToCookiePolicy"
              class="px-3 py-2 text-sm text-gray-300 hover:text-white transition-colors"
            >
              Cookie Policy
            </button>
            <button
              @click="essentialOnly"
              class="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white text-sm font-medium rounded-lg transition-colors"
            >
              Essential Only
            </button>
            <button
              @click="acceptAll"
              class="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white text-sm font-medium rounded-lg transition-colors"
            >
              Accept All
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>
