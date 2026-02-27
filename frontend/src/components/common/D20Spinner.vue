<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  size?: 'sm' | 'md' | 'lg' | 'xl'
  color?: 'primary' | 'white' | 'gray'
}>()

// Numbers on each face of the D20
const faceNumbers = [20, 8, 14, 2, 17, 1, 13, 7, 19, 4, 16, 10, 6, 18, 12, 5, 11, 15, 3, 9]

// Scale factor based on size (base is 70px wrapper)
const scale = computed(() => {
  switch (props.size) {
    case 'sm': return 0.5    // ~35px
    case 'lg': return 1.2    // ~84px
    case 'xl': return 1.5    // ~105px
    default: return 0.85     // ~60px
  }
})

const wrapStyle = computed(() => ({
  transform: `rotate(-29deg) scale(${scale.value})`,
}))
</script>

<template>
  <div
    class="d20-container"
    :class="[
      size === 'sm' ? 'container-sm' : size === 'lg' ? 'container-lg' : size === 'xl' ? 'container-xl' : 'container-md',
      color === 'white' ? 'color-white' : color === 'gray' ? 'color-gray' : 'color-primary'
    ]"
  >
    <div class="d20-wrap" :style="wrapStyle">
      <div class="d20">
        <div
          v-for="(num, i) in faceNumbers"
          :key="i"
          :class="`tri tri${i + 1}`"
          :data-n="num"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Container to control the final size */
.d20-container {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.container-sm { width: 35px; height: 35px; }
.container-md { width: 60px; height: 60px; }
.container-lg { width: 84px; height: 84px; }
.container-xl { width: 105px; height: 105px; }

/* D20 wrapper - fixed size, scaled via transform */
.d20-wrap {
  perspective: 750px;
  width: 70px;
  height: 70px;
  flex-shrink: 0;
}

.d20 {
  width: 60px;
  height: 60px;
  margin: 5px;
  position: relative;
  transform-style: preserve-3d;
  animation: d20-roll 3s linear infinite;
}

.tri {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  border-left: 32px solid transparent;
  border-right: 32px solid transparent;
  border-bottom: 60px solid var(--d20-face-color, rgba(124, 131, 255, 0.35));
  filter: drop-shadow(0 0 0.5px var(--d20-glow-color, rgba(124, 131, 255, 0.8)));
}

/* Face numbers */
.tri::after {
  content: attr(data-n);
  position: absolute;
  font-size: 11px;
  font-weight: bold;
  color: var(--d20-number-color, rgba(210, 215, 255, 0.9));
  text-shadow: 0 0 3px rgba(0, 0, 0, 0.8);
  width: 20px;
  text-align: center;
  left: -10px;
}

/* Number position: upward-pointing triangles (centroid at ~2/3 height) */
.tri1::after, .tri2::after, .tri3::after, .tri4::after, .tri5::after,
.tri16::after, .tri17::after, .tri18::after, .tri19::after, .tri20::after {
  top: 30px;
}

/* Number position: inverted triangles (centroid at ~1/3 from top) */
.tri6::after, .tri7::after, .tri8::after, .tri9::after, .tri10::after,
.tri11::after, .tri12::after, .tri13::after, .tri14::after, .tri15::after {
  top: -42px;
}

/* Top cap: 5 upright triangles */
.tri1  { transform: rotateY(-72deg)  rotateX(-45deg) translateZ(-30px); top: -30px; }
.tri2  { transform: rotateY(-144deg) rotateX(-45deg) translateZ(-30px); top: -30px; }
.tri3  { transform: rotateY(-216deg) rotateX(-45deg) translateZ(-30px); top: -30px; }
.tri4  { transform: rotateY(-288deg) rotateX(-45deg) translateZ(-30px); top: -30px; }
.tri5  { transform: rotateY(-360deg) rotateX(-45deg) translateZ(-30px); top: -30px; }

/* Bottom cap: 5 inverted triangles */
.tri6  { transform: rotateY(-432deg) rotateX(-45deg) translateZ(30px); top: 30px; border-bottom: none; border-top: 60px solid var(--d20-face-color-alt, rgba(124, 131, 255, 0.3)); }
.tri7  { transform: rotateY(-504deg) rotateX(-45deg) translateZ(30px); top: 30px; border-bottom: none; border-top: 60px solid var(--d20-face-color-alt, rgba(124, 131, 255, 0.3)); }
.tri8  { transform: rotateY(-576deg) rotateX(-45deg) translateZ(30px); top: 30px; border-bottom: none; border-top: 60px solid var(--d20-face-color-alt, rgba(124, 131, 255, 0.3)); }
.tri9  { transform: rotateY(-648deg) rotateX(-45deg) translateZ(30px); top: 30px; border-bottom: none; border-top: 60px solid var(--d20-face-color-alt, rgba(124, 131, 255, 0.3)); }
.tri10 { transform: rotateY(-720deg) rotateX(-45deg) translateZ(30px); top: 30px; border-bottom: none; border-top: 60px solid var(--d20-face-color-alt, rgba(124, 131, 255, 0.3)); }

/* First ring: 5 inverted triangles */
.tri11 { transform: rotateY(-792deg)  translateZ(-47px) rotateX(-9deg); top: 0; border-bottom: none; border-top: 60px solid var(--d20-face-color-dim, rgba(124, 131, 255, 0.25)); }
.tri12 { transform: rotateY(-864deg)  translateZ(-47px) rotateX(-9deg); top: 0; border-bottom: none; border-top: 60px solid var(--d20-face-color-dim, rgba(124, 131, 255, 0.25)); }
.tri13 { transform: rotateY(-936deg)  translateZ(-47px) rotateX(-9deg); top: 0; border-bottom: none; border-top: 60px solid var(--d20-face-color-dim, rgba(124, 131, 255, 0.25)); }
.tri14 { transform: rotateY(-1008deg) translateZ(-47px) rotateX(-9deg); top: 0; border-bottom: none; border-top: 60px solid var(--d20-face-color-dim, rgba(124, 131, 255, 0.25)); }
.tri15 { transform: rotateY(-1080deg) translateZ(-47px) rotateX(-9deg); top: 0; border-bottom: none; border-top: 60px solid var(--d20-face-color-dim, rgba(124, 131, 255, 0.25)); }

/* Second ring: 5 upright triangles */
.tri16 { transform: rotateY(-1404deg) translateZ(-47px) rotateX(9deg); top: 0; }
.tri17 { transform: rotateY(-1476deg) translateZ(-47px) rotateX(9deg); top: 0; }
.tri18 { transform: rotateY(-1548deg) translateZ(-47px) rotateX(9deg); top: 0; }
.tri19 { transform: rotateY(-1620deg) translateZ(-47px) rotateX(9deg); top: 0; }
.tri20 { transform: rotateY(-1692deg) translateZ(-47px) rotateX(9deg); top: 0; }

/* Color variants */
.color-primary {
  --d20-face-color: rgba(99, 102, 241, 0.45);
  --d20-face-color-alt: rgba(99, 102, 241, 0.35);
  --d20-face-color-dim: rgba(99, 102, 241, 0.28);
  --d20-glow-color: rgba(99, 102, 241, 0.8);
  --d20-number-color: rgba(255, 255, 255, 0.9);
}

.color-white {
  --d20-face-color: rgba(255, 255, 255, 0.5);
  --d20-face-color-alt: rgba(255, 255, 255, 0.4);
  --d20-face-color-dim: rgba(255, 255, 255, 0.3);
  --d20-glow-color: rgba(255, 255, 255, 0.8);
  --d20-number-color: rgba(255, 255, 255, 0.95);
}

.color-gray {
  --d20-face-color: rgba(156, 163, 175, 0.45);
  --d20-face-color-alt: rgba(156, 163, 175, 0.35);
  --d20-face-color-dim: rgba(156, 163, 175, 0.28);
  --d20-glow-color: rgba(156, 163, 175, 0.8);
  --d20-number-color: rgba(255, 255, 255, 0.9);
}

@keyframes d20-roll {
  0%   { transform: rotateY(0deg)   rotateX(-20deg); }
  100% { transform: rotateY(360deg) rotateX(-20deg); }
}
</style>
