<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  size?: number  // Size in pixels (default 100)
  opacity?: number  // Opacity 0-1 (default 0.15)
  duration?: number  // Animation duration in seconds (default 20)
}>()

// Check if on mobile phone (not tablet) - phones are typically < 640px wide
const isMobilePhone = ref(false)

function checkMobile() {
  // Check both width and if it's a touch device with small screen
  // Tablets are typically 768px+ in at least one dimension
  isMobilePhone.value = window.innerWidth < 640 && window.innerHeight < 900
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

const size = computed(() => props.size || 100)
const opacity = computed(() => props.opacity || 0.15)
const duration = computed(() => props.duration || 20)

// Pip positions for each face (1-6)
const pipPositions = {
  1: [[50, 50]],
  2: [[25, 25], [75, 75]],
  3: [[25, 25], [50, 50], [75, 75]],
  4: [[25, 25], [75, 25], [25, 75], [75, 75]],
  5: [[25, 25], [75, 25], [50, 50], [25, 75], [75, 75]],
  6: [[25, 25], [75, 25], [25, 50], [75, 50], [25, 75], [75, 75]],
}

const containerStyle = computed(() => ({
  width: `${size.value}px`,
  height: `${size.value}px`,
  opacity: opacity.value,
}))

const cubeStyle = computed(() => ({
  width: `${size.value}px`,
  height: `${size.value}px`,
  animationDuration: `${duration.value}s`,
}))

const faceSize = computed(() => size.value)
const translateZ = computed(() => size.value / 2)
const pipSize = computed(() => Math.max(8, size.value * 0.12))
</script>

<template>
  <!-- Don't render on mobile phones to prevent performance issues -->
  <div v-if="!isMobilePhone" class="d6-container" :style="containerStyle">
    <div class="d6-cube" :style="cubeStyle">
      <!-- Face 1 (front) -->
      <div
        class="d6-face face-1"
        :style="{ width: `${faceSize}px`, height: `${faceSize}px`, transform: `rotateY(0deg) translateZ(${translateZ}px)` }"
      >
        <div
          v-for="(pos, i) in pipPositions[1]"
          :key="i"
          class="pip"
          :style="{ left: `${pos[0]}%`, top: `${pos[1]}%`, width: `${pipSize}px`, height: `${pipSize}px` }"
        />
      </div>

      <!-- Face 6 (back) -->
      <div
        class="d6-face face-6"
        :style="{ width: `${faceSize}px`, height: `${faceSize}px`, transform: `rotateY(180deg) translateZ(${translateZ}px)` }"
      >
        <div
          v-for="(pos, i) in pipPositions[6]"
          :key="i"
          class="pip"
          :style="{ left: `${pos[0]}%`, top: `${pos[1]}%`, width: `${pipSize}px`, height: `${pipSize}px` }"
        />
      </div>

      <!-- Face 3 (right) -->
      <div
        class="d6-face face-3"
        :style="{ width: `${faceSize}px`, height: `${faceSize}px`, transform: `rotateY(90deg) translateZ(${translateZ}px)` }"
      >
        <div
          v-for="(pos, i) in pipPositions[3]"
          :key="i"
          class="pip"
          :style="{ left: `${pos[0]}%`, top: `${pos[1]}%`, width: `${pipSize}px`, height: `${pipSize}px` }"
        />
      </div>

      <!-- Face 4 (left) -->
      <div
        class="d6-face face-4"
        :style="{ width: `${faceSize}px`, height: `${faceSize}px`, transform: `rotateY(-90deg) translateZ(${translateZ}px)` }"
      >
        <div
          v-for="(pos, i) in pipPositions[4]"
          :key="i"
          class="pip"
          :style="{ left: `${pos[0]}%`, top: `${pos[1]}%`, width: `${pipSize}px`, height: `${pipSize}px` }"
        />
      </div>

      <!-- Face 2 (top) -->
      <div
        class="d6-face face-2"
        :style="{ width: `${faceSize}px`, height: `${faceSize}px`, transform: `rotateX(90deg) translateZ(${translateZ}px)` }"
      >
        <div
          v-for="(pos, i) in pipPositions[2]"
          :key="i"
          class="pip"
          :style="{ left: `${pos[0]}%`, top: `${pos[1]}%`, width: `${pipSize}px`, height: `${pipSize}px` }"
        />
      </div>

      <!-- Face 5 (bottom) -->
      <div
        class="d6-face face-5"
        :style="{ width: `${faceSize}px`, height: `${faceSize}px`, transform: `rotateX(-90deg) translateZ(${translateZ}px)` }"
      >
        <div
          v-for="(pos, i) in pipPositions[5]"
          :key="i"
          class="pip"
          :style="{ left: `${pos[0]}%`, top: `${pos[1]}%`, width: `${pipSize}px`, height: `${pipSize}px` }"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.d6-container {
  perspective: 800px;
  perspective-origin: 50% 50%;
}

.d6-cube {
  position: relative;
  transform-style: preserve-3d;
  animation: d6-roll linear infinite;
}

.d6-face {
  position: absolute;
  background: linear-gradient(135deg, rgba(45, 90, 61, 0.9) 0%, rgba(34, 67, 46, 0.9) 100%);
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 12%;
  box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.2);
  backface-visibility: visible;
}

.pip {
  position: absolute;
  background: radial-gradient(circle, #fff 0%, #e0e0e0 100%);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  box-shadow:
    inset 0 -2px 4px rgba(0, 0, 0, 0.2),
    0 1px 2px rgba(0, 0, 0, 0.3);
}

@keyframes d6-roll {
  0% {
    transform: rotateX(0deg) rotateY(0deg) rotateZ(0deg);
  }
  100% {
    transform: rotateX(360deg) rotateY(540deg) rotateZ(360deg);
  }
}
</style>
