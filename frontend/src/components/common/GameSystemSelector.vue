<script setup lang="ts">
import { useRouter } from 'vue-router'

interface GameSystemOption {
  id: string
  label: string
  description: string
  color: string
  hoverBorder: string
  shimmerColor: string
  icon: string | null
}

const router = useRouter()

const systems: GameSystemOption[] = [
  {
    id: 'board-games',
    label: 'Board Games',
    description: 'Classic tabletop games from BGG',
    color: 'primary',
    hoverBorder: 'hover:border-primary-400',
    shimmerColor: '34, 87, 60',
    icon: null,
  },
  {
    id: 'mtg',
    label: 'Magic: The Gathering',
    description: 'Formats, decks, tournaments',
    color: 'purple',
    hoverBorder: 'hover:border-purple-400',
    shimmerColor: '147, 51, 234',
    icon: '/icons/mtg-logo.png',
  },
  {
    id: 'pokemon',
    label: 'Pokemon TCG',
    description: 'League cups, formats, prizes',
    color: 'yellow',
    hoverBorder: 'hover:border-yellow-400',
    shimmerColor: '234, 179, 8',
    icon: '/icons/pokemon-logo.png',
  },
  {
    id: 'yugioh',
    label: 'Yu-Gi-Oh!',
    description: 'Locals, OTS, tournaments',
    color: 'blue',
    hoverBorder: 'hover:border-blue-400',
    shimmerColor: '59, 130, 246',
    icon: '/icons/yugioh-logo.png',
  },
  {
    id: 'warhammer40k',
    label: 'Warhammer 40k',
    description: 'Matched play, crusade, armies',
    color: 'red',
    hoverBorder: 'hover:border-red-400',
    shimmerColor: '220, 38, 38',
    icon: '/icons/warhammer40k-logo.png',
  },
]

function navigate(systemId: string) {
  const routes: Record<string, string> = {
    'board-games': '/games/create',
    'mtg': '/mtg/events/create',
    'pokemon': '/pokemon/events/create',
    'yugioh': '/yugioh/events/create',
    'warhammer40k': '/warhammer40k/events/create',
  }
  router.push(routes[systemId] || `/games/create?system=${systemId}`)
}
</script>

<template>
  <div class="grid grid-cols-2 xs:grid-cols-3 sm:grid-cols-5 gap-3 px-1">
    <button
      v-for="system in systems"
      :key="system.id"
      class="system-card group relative flex flex-col items-center gap-2 px-3 py-4 rounded-xl border-2 border-gray-100 bg-white cursor-pointer text-center"
      :class="system.hoverBorder"
      :style="{ '--shimmer-color': system.shimmerColor }"
      @click="navigate(system.id)"
    >
      <!-- Icon — depth layer 3 (floats highest) -->
      <div class="card-layer-icon w-10 h-10 flex items-center justify-center relative z-10">
        <img
          v-if="system.icon"
          :src="system.icon"
          :alt="system.label"
          class="w-10 h-10 object-contain"
        />
        <svg v-else class="w-8 h-8 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
        </svg>
      </div>

      <!-- Label — depth layer 2 (mid float) -->
      <span class="card-layer-label text-sm font-semibold text-gray-800 leading-tight relative z-10">{{ system.label }}</span>

      <!-- Description — depth layer 1 (near surface) -->
      <span class="card-layer-desc text-xs text-gray-500 leading-tight relative z-10">{{ system.description }}</span>
    </button>
  </div>
</template>

<style scoped>
.system-card {
  transform-style: preserve-3d;
  perspective: 800px;
  transition: transform 0.5s cubic-bezier(0.23, 1, 0.32, 1),
              box-shadow 0.5s cubic-bezier(0.23, 1, 0.32, 1),
              border-color 0.3s ease;
}

/* Each content layer transitions independently for parallax depth */
.card-layer-icon,
.card-layer-label,
.card-layer-desc {
  transition: transform 0.5s cubic-bezier(0.23, 1, 0.32, 1);
  will-change: transform;
}

/* Subtle holo foil on the card surface */
.system-card::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  z-index: 0;
  opacity: 0;
  overflow: hidden;
  transition: opacity 0.5s ease;
  background:
    repeating-conic-gradient(
      from 0deg at 50% 50%,
      rgba(255, 80, 160, 0.22) 0deg,
      rgba(255, 200, 60, 0.18) 60deg,
      rgba(80, 255, 160, 0.22) 120deg,
      rgba(60, 180, 255, 0.18) 180deg,
      rgba(160, 80, 255, 0.22) 240deg,
      rgba(255, 180, 60, 0.18) 300deg,
      rgba(255, 80, 160, 0.22) 360deg
    );
}

/* Hover: 3D tilt + layered depth separation */
.system-card:hover {
  transform: rotateX(3deg) scale(1.02);
  box-shadow:
    0 20px 40px -12px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(var(--shimmer-color), 0.15),
    -4px 8px 20px rgba(var(--shimmer-color), 0.06);
}

/* Icon floats highest — moves opposite to tilt for parallax */
.system-card:hover .card-layer-icon {
  transform: translateZ(30px) translateX(3px) translateY(-4px);
}

/* Label floats mid-height */
.system-card:hover .card-layer-label {
  transform: translateZ(18px) translateX(2px) translateY(-2px);
}

/* Description barely lifts — stays close to card surface */
.system-card:hover .card-layer-desc {
  transform: translateZ(8px) translateX(1px) translateY(-1px);
}

/* Show subtle holo surface on hover */
.system-card:hover::after {
  opacity: 1;
}
</style>
