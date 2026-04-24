<script setup lang="ts">
import { ref, watch } from 'vue'
import HotLocationsBar from '@/components/venues/HotLocationsBar.vue'
import StateSelect from '@/components/common/StateSelect.vue'
import VenueSelector from '@/components/venues/VenueSelector.vue'
import VenueDetailsFields from '@/components/venues/VenueDetailsFields.vue'
import { hasFeature, TIER_NAMES } from '@/config/subscriptionLimits'
import type { EventLocation } from '@/types/social'

const props = defineProps<{
  // Venue mode
  eventLocationId?: string
  venueHall?: string
  venueRoom?: string
  venueTable?: string
  // Custom address mode
  addressLine1?: string
  city?: string
  state?: string
  postalCode?: string
  // Shared
  locationDetails?: string
  // Control
  disabled?: boolean
  currentTier: 'free' | 'basic' | 'pro' | 'premium'
  errors?: {
    location?: string
  }
}>()

const emit = defineEmits<{
  (e: 'update:eventLocationId', value: string | undefined): void
  (e: 'update:venueHall', value: string | undefined): void
  (e: 'update:venueRoom', value: string | undefined): void
  (e: 'update:venueTable', value: string | undefined): void
  (e: 'update:addressLine1', value: string): void
  (e: 'update:city', value: string): void
  (e: 'update:state', value: string): void
  (e: 'update:postalCode', value: string): void
  (e: 'update:locationDetails', value: string): void
  (e: 'showVenueModal'): void
  (e: 'venueSelected', venue: EventLocation): void
}>()

// Location mode: venue or custom address
const locationMode = ref<'venue' | 'custom'>(props.eventLocationId ? 'venue' : 'custom')

// Track selected venue for displaying details
const selectedVenue = ref<EventLocation | null>(null)

// Watch for external changes to eventLocationId
watch(() => props.eventLocationId, (newVal) => {
  if (newVal) {
    locationMode.value = 'venue'
  }
})

function handleVenueSelect(venue: EventLocation) {
  selectedVenue.value = venue
  emit('update:eventLocationId', venue.id)
  emit('venueSelected', venue)
  locationMode.value = 'venue'
}

function handleVenueSelectorSelect(venue: EventLocation | null) {
  selectedVenue.value = venue
  if (venue) {
    emit('venueSelected', venue)
  }
}

function clearVenueSelection() {
  emit('update:eventLocationId', undefined)
  emit('update:venueHall', undefined)
  emit('update:venueRoom', undefined)
  emit('update:venueTable', undefined)
  selectedVenue.value = null
}
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Location</h3>

    <!-- Hot Locations Quick Select -->
    <HotLocationsBar
      :selected-id="eventLocationId"
      @select="handleVenueSelect"
    />

    <!-- Location Mode Toggle -->
    <div class="flex gap-4 mb-4">
      <label class="flex items-center gap-2 cursor-pointer">
        <input
          type="radio"
          v-model="locationMode"
          value="venue"
          class="w-4 h-4 text-primary-500 focus:ring-primary-500"
          :disabled="disabled"
        />
        <span class="text-sm text-gray-700">Select a venue</span>
      </label>
      <label class="flex items-center gap-2 cursor-pointer">
        <input
          type="radio"
          v-model="locationMode"
          value="custom"
          class="w-4 h-4 text-primary-500 focus:ring-primary-500"
          :disabled="disabled"
          @change="clearVenueSelection"
        />
        <span class="text-sm text-gray-700">Enter custom address</span>
      </label>
    </div>

    <!-- Venue Selection Mode -->
    <div v-if="locationMode === 'venue'" class="space-y-4">
      <div>
        <label class="label">Select a Venue</label>
        <VenueSelector
          :model-value="eventLocationId ?? null"
          :disabled="disabled"
          @update:model-value="(v) => $emit('update:eventLocationId', v ?? undefined)"
          @select="handleVenueSelectorSelect"
        />
        <div class="flex items-center gap-2 mt-2">
          <button
            type="button"
            class="text-sm text-primary-500 hover:text-primary-600"
            @click="$emit('showVenueModal')"
          >
            + Submit a new venue
          </button>
        </div>
      </div>

      <!-- Venue Details (Hall/Room/Table) -->
      <div v-if="selectedVenue || eventLocationId">
        <template v-if="hasFeature(currentTier, 'tableInfo')">
          <label class="label">Location Details (optional)</label>
          <VenueDetailsFields
            :hall="venueHall"
            :room="venueRoom"
            :table="venueTable"
            :disabled="disabled"
            @update:hall="(v) => $emit('update:venueHall', v ?? undefined)"
            @update:room="(v) => $emit('update:venueRoom', v ?? undefined)"
            @update:table="(v) => $emit('update:venueTable', v ?? undefined)"
          />
        </template>
        <div v-else class="rounded-lg bg-gray-50 border border-gray-200 p-4 text-center">
          <p class="text-sm text-gray-500">Hall, room, and table details require {{ TIER_NAMES.basic }} plan</p>
          <router-link to="/pricing" class="text-sm text-primary-500 hover:text-primary-600 mt-1">Upgrade</router-link>
        </div>
      </div>

      <div>
        <label for="locationDetails" class="label">Additional Details</label>
        <input
          id="locationDetails"
          :value="locationDetails"
          type="text"
          class="input"
          placeholder="e.g., Meet at the registration desk"
          :disabled="disabled"
          @input="$emit('update:locationDetails', ($event.target as HTMLInputElement).value)"
        />
      </div>
    </div>

    <!-- Custom Address Mode -->
    <div v-else class="space-y-4">
      <div>
        <label for="addressLine1" class="label">Address</label>
        <input
          id="addressLine1"
          :value="addressLine1"
          type="text"
          class="input"
          placeholder="123 Main St"
          :disabled="disabled"
          @input="$emit('update:addressLine1', ($event.target as HTMLInputElement).value)"
        />
      </div>

      <div class="grid grid-cols-12 gap-4">
        <div class="col-span-5">
          <label for="city" class="label">City</label>
          <input
            id="city"
            :value="city"
            type="text"
            class="input"
            :disabled="disabled"
            @input="$emit('update:city', ($event.target as HTMLInputElement).value)"
          />
        </div>
        <div class="col-span-4">
          <label for="state" class="label">State</label>
          <StateSelect
            :model-value="state"
            :disabled="disabled"
            @update:model-value="$emit('update:state', $event)"
          />
        </div>
        <div class="col-span-3">
          <label for="postalCode" class="label">Zip</label>
          <input
            id="postalCode"
            :value="postalCode"
            type="text"
            class="input"
            :disabled="disabled"
            @input="$emit('update:postalCode', ($event.target as HTMLInputElement).value)"
          />
        </div>
      </div>

      <div>
        <label for="locationDetailsCustom" class="label">Location Details</label>
        <input
          id="locationDetailsCustom"
          :value="locationDetails"
          type="text"
          class="input"
          placeholder="e.g., Ring doorbell, upstairs apartment"
          :disabled="disabled"
          @input="$emit('update:locationDetails', ($event.target as HTMLInputElement).value)"
        />
      </div>
    </div>

    <!-- Location Error -->
    <p v-if="errors?.location" class="text-sm text-red-500">{{ errors.location }}</p>
  </div>
</template>
