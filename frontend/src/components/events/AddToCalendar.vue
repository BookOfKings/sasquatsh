<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  title: string
  date: string // YYYY-MM-DD
  startTime?: string | null // HH:MM:SS
  durationMinutes?: number | null
  location?: string | null
  description?: string | null
}>()

const showDropdown = ref(false)

// Detect device type
const isAndroid = /Android/i.test(navigator.userAgent)
const isIOS = /iPhone|iPad|iPod/i.test(navigator.userAgent)
const isMobile = isAndroid || isIOS

// Format dates for calendar URLs
function getStartEnd() {
  const date = props.date.replace(/-/g, '')
  if (props.startTime) {
    const time = props.startTime.slice(0, 5).replace(':', '') + '00'
    const start = `${date}T${time}`
    // Calculate end time
    const dur = props.durationMinutes || 120
    const startDate = new Date(`${props.date}T${props.startTime}`)
    const endDate = new Date(startDate.getTime() + dur * 60000)
    const endStr = endDate.toISOString().replace(/[-:]/g, '').split('.')[0]
    return { start, end: endStr }
  }
  // All-day event
  return { start: date, end: date }
}

const locationStr = computed(() => props.location || '')

const googleUrl = computed(() => {
  const { start, end } = getStartEnd()
  const params = new URLSearchParams({
    action: 'TEMPLATE',
    text: props.title,
    dates: `${start}/${end}`,
    location: locationStr.value,
    details: props.description || `Game night: ${props.title}`,
  })
  return `https://calendar.google.com/calendar/render?${params}`
})

const outlookUrl = computed(() => {
  const startDt = props.startTime
    ? `${props.date}T${props.startTime}`
    : props.date
  const dur = props.durationMinutes || 120
  const startDate = new Date(startDt)
  const endDate = new Date(startDate.getTime() + dur * 60000)
  const params = new URLSearchParams({
    subject: props.title,
    startdt: startDate.toISOString(),
    enddt: endDate.toISOString(),
    location: locationStr.value,
    body: props.description || `Game night: ${props.title}`,
    path: '/calendar/action/compose',
  })
  return `https://outlook.live.com/calendar/0/action/compose?${params}`
})

function downloadICS() {
  const { start, end } = getStartEnd()
  const now = new Date().toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z'
  const loc = locationStr.value.replace(/,/g, '\\,')
  const desc = (props.description || `Game night: ${props.title}`).replace(/\n/g, '\\n')

  const ics = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//Sasquatsh//Game Night//EN',
    'BEGIN:VEVENT',
    `DTSTART:${start}`,
    `DTEND:${end}`,
    `DTSTAMP:${now}`,
    `SUMMARY:${props.title}`,
    `LOCATION:${loc}`,
    `DESCRIPTION:${desc}`,
    'END:VEVENT',
    'END:VCALENDAR',
  ].join('\r\n')

  const blob = new Blob([ics], { type: 'text/calendar;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${props.title.replace(/[^a-zA-Z0-9]/g, '_')}.ics`
  a.click()
  URL.revokeObjectURL(url)
  showDropdown.value = false
}

function handlePrimaryClick() {
  if (isAndroid) {
    window.open(googleUrl.value, '_blank')
  } else if (isIOS) {
    downloadICS()
  } else {
    showDropdown.value = !showDropdown.value
  }
}

function handleOptionClick(option: 'google' | 'outlook' | 'ics') {
  if (option === 'google') {
    window.open(googleUrl.value, '_blank')
  } else if (option === 'outlook') {
    window.open(outlookUrl.value, '_blank')
  } else {
    downloadICS()
  }
  showDropdown.value = false
}

// Close dropdown on click outside
if (typeof document !== 'undefined') {
  document.addEventListener('click', (e: Event) => {
    const target = e.target as HTMLElement
    if (!target.closest('.calendar-dropdown')) {
      showDropdown.value = false
    }
  })
}
</script>

<template>
  <div class="relative calendar-dropdown inline-block" @click.stop>
    <button
      class="btn-outline text-sm flex items-center gap-2"
      @click="handlePrimaryClick"
    >
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z"/>
      </svg>
      <span v-if="isAndroid">Add to Google Calendar</span>
      <span v-else-if="isIOS">Add to Calendar</span>
      <span v-else>Add to Calendar</span>
      <svg v-if="!isMobile" class="w-3 h-3 ml-1" viewBox="0 0 24 24" fill="currentColor">
        <path d="M7,10L12,15L17,10H7Z"/>
      </svg>
    </button>

    <!-- Desktop dropdown -->
    <div
      v-if="showDropdown && !isMobile"
      class="absolute left-0 mt-1 w-56 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50"
    >
      <button
        class="w-full px-4 py-2.5 text-sm text-left hover:bg-gray-50 flex items-center gap-3"
        @click="handleOptionClick('google')"
      >
        <svg class="w-5 h-5 text-blue-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M21.35,11.1H12.18V13.83H18.69C18.36,17.64 15.19,19.27 12.19,19.27C8.36,19.27 5,16.25 5,12C5,7.9 8.2,4.73 12.2,4.73C15.29,4.73 17.1,6.7 17.1,6.7L19,4.72C19,4.72 16.56,2 12.1,2C6.42,2 2.03,6.8 2.03,12C2.03,17.05 6.16,22 12.25,22C17.6,22 21.5,18.33 21.5,12.91C21.5,11.76 21.35,11.1 21.35,11.1Z"/>
        </svg>
        Google Calendar
      </button>
      <button
        class="w-full px-4 py-2.5 text-sm text-left hover:bg-gray-50 flex items-center gap-3"
        @click="handleOptionClick('outlook')"
      >
        <svg class="w-5 h-5 text-blue-600" viewBox="0 0 24 24" fill="currentColor">
          <path d="M7.88,12.04Q7.88,10.93 8.42,10.27Q8.96,9.61 9.86,9.61Q10.76,9.61 11.3,10.27Q11.84,10.93 11.84,12.04Q11.84,13.15 11.3,13.82Q10.76,14.49 9.86,14.49Q8.96,14.49 8.42,13.82Q7.88,13.15 7.88,12.04M24,12L24,12L24,12L20,12C20,7.72 16.27,4 12,4C7.73,4 4,7.72 4,12C4,16.27 7.73,20 12,20L12,20L12,20L12,16.91C9.37,16.91 7.24,14.84 7.24,12.26C7.24,9.68 9.37,7.61 12,7.61C14.63,7.61 16.76,9.68 16.76,12.26L20,12.26Z"/>
        </svg>
        Outlook Calendar
      </button>
      <button
        class="w-full px-4 py-2.5 text-sm text-left hover:bg-gray-50 flex items-center gap-3"
        @click="handleOptionClick('ics')"
      >
        <svg class="w-5 h-5 text-gray-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M5,20H19V18H5M19,9H15V3H9V9H5L12,16L19,9Z"/>
        </svg>
        Download .ics File
      </button>
    </div>
  </div>
</template>
