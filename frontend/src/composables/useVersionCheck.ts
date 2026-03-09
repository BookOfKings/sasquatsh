import { ref, onMounted, onUnmounted } from 'vue'

const CHECK_INTERVAL = 60000 // Check every 60 seconds
const VERSION_KEY = 'app_version_hash'

export function useVersionCheck() {
  const updateAvailable = ref(false)
  let intervalId: ReturnType<typeof setInterval> | null = null

  async function getVersionHash(): Promise<string | null> {
    try {
      // Fetch the index.html with cache busting
      const response = await fetch(`/?_=${Date.now()}`, {
        cache: 'no-store',
        headers: {
          'Cache-Control': 'no-cache',
        },
      })
      const html = await response.text()

      // Extract script src hashes from the HTML (these change with each build)
      const scriptMatches = html.match(/src="\/assets\/index-[^"]+\.js"/g)
      if (scriptMatches && scriptMatches.length > 0) {
        return scriptMatches.join(',')
      }
      return null
    } catch {
      return null
    }
  }

  async function checkForUpdate() {
    const currentHash = sessionStorage.getItem(VERSION_KEY)
    const newHash = await getVersionHash()

    if (!newHash) return

    if (!currentHash) {
      // First load, store the hash
      sessionStorage.setItem(VERSION_KEY, newHash)
    } else if (currentHash !== newHash) {
      // Version changed!
      updateAvailable.value = true
    }
  }

  function refresh() {
    // Clear the stored version and reload
    sessionStorage.removeItem(VERSION_KEY)
    window.location.reload()
  }

  function dismiss() {
    // Update stored version to current and hide the banner
    getVersionHash().then(hash => {
      if (hash) {
        sessionStorage.setItem(VERSION_KEY, hash)
      }
    })
    updateAvailable.value = false
  }

  onMounted(async () => {
    // Initial check and store
    const hash = await getVersionHash()
    if (hash && !sessionStorage.getItem(VERSION_KEY)) {
      sessionStorage.setItem(VERSION_KEY, hash)
    }

    // Start periodic checking
    intervalId = setInterval(checkForUpdate, CHECK_INTERVAL)
  })

  onUnmounted(() => {
    if (intervalId) {
      clearInterval(intervalId)
    }
  })

  return {
    updateAvailable,
    refresh,
    dismiss,
  }
}
