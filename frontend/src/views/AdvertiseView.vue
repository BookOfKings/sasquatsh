<script setup lang="ts">
import { ref } from 'vue'

const form = ref({
  name: '',
  email: '',
  company: '',
  adType: 'banner',
  message: '',
})
const submitted = ref(false)
const loading = ref(false)
const error = ref('')

async function handleSubmit() {
  if (!form.value.name.trim() || !form.value.email.trim() || !form.value.message.trim()) {
    error.value = 'Please fill in all required fields'
    return
  }

  loading.value = true
  error.value = ''

  try {
    // Use the contact form endpoint
    const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
    const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

    const response = await fetch(`${FUNCTIONS_URL}/contact`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: form.value.name.trim(),
        email: form.value.email.trim(),
        subject: `Advertising Inquiry - ${form.value.company || 'Individual'}`,
        message: `Ad Type: ${form.value.adType}\nCompany: ${form.value.company || 'N/A'}\n\n${form.value.message}`,
        recaptchaToken: 'advertising-inquiry',
      }),
    })

    if (!response.ok) throw new Error('Failed to send')

    submitted.value = true
  } catch {
    error.value = 'Failed to send your inquiry. Please try again or email us at contact@sasquatsh.com'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 py-12">
    <div class="max-w-4xl mx-auto px-4">
      <!-- Hero -->
      <div class="text-center mb-12">
        <h1 class="text-4xl font-bold text-gray-900 mb-4">Advertise on Sasquatsh</h1>
        <p class="text-xl text-gray-600 max-w-2xl mx-auto">
          Reach active tabletop gamers who host and attend game nights every week.
        </p>
      </div>

      <!-- Who Should Advertise -->
      <div class="card p-8 mb-8">
        <h2 class="text-2xl font-bold text-gray-900 mb-6">Who Should Advertise?</h2>
        <div class="grid md:grid-cols-2 gap-6">
          <div class="flex items-start gap-3">
            <svg class="w-6 h-6 text-primary-500 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
            </svg>
            <div>
              <h3 class="font-semibold text-gray-900">Game Stores</h3>
              <p class="text-sm text-gray-600">Promote your store to local players looking for game nights and supplies.</p>
            </div>
          </div>
          <div class="flex items-start gap-3">
            <svg class="w-6 h-6 text-primary-500 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
            </svg>
            <div>
              <h3 class="font-semibold text-gray-900">Conventions & Expos</h3>
              <p class="text-sm text-gray-600">Get your gaming convention in front of players who actively attend events.</p>
            </div>
          </div>
          <div class="flex items-start gap-3">
            <svg class="w-6 h-6 text-primary-500 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5Z"/>
            </svg>
            <div>
              <h3 class="font-semibold text-gray-900">Game Publishers</h3>
              <p class="text-sm text-gray-600">Promote new releases directly to people who buy and play board games.</p>
            </div>
          </div>
          <div class="flex items-start gap-3">
            <svg class="w-6 h-6 text-primary-500 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,3L1,9L12,15L21,10.09V17H23V9M5,13.18V17.18L12,21L19,17.18V13.18L12,17L5,13.18Z"/>
            </svg>
            <div>
              <h3 class="font-semibold text-gray-900">Gaming Accessories</h3>
              <p class="text-sm text-gray-600">Dice, sleeves, playmats, storage — reach buyers who game weekly.</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Ad Types -->
      <div class="card p-8 mb-8">
        <h2 class="text-2xl font-bold text-gray-900 mb-6">Ad Placements</h2>
        <div class="space-y-4">
          <div class="flex items-center gap-4 p-4 bg-gray-50 rounded-lg">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center flex-shrink-0">
              <svg class="w-6 h-6 text-primary-600" viewBox="0 0 24 24" fill="currentColor">
                <path d="M13,3V9H21V3M13,21H21V11H13M3,21H11V15H3M3,13H11V3H3V13Z"/>
              </svg>
            </div>
            <div>
              <h3 class="font-semibold">Dashboard Banner</h3>
              <p class="text-sm text-gray-600">Shown on every user's dashboard — the first thing they see when they log in.</p>
            </div>
          </div>
          <div class="flex items-center gap-4 p-4 bg-gray-50 rounded-lg">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center flex-shrink-0">
              <svg class="w-6 h-6 text-primary-600" viewBox="0 0 24 24" fill="currentColor">
                <path d="M9.5,3A6.5,6.5 0 0,1 16,9.5C16,11.11 15.41,12.59 14.44,13.73L14.71,14H15.5L20.5,19L19,20.5L14,15.5V14.71L13.73,14.44C12.59,15.41 11.11,16 9.5,16A6.5,6.5 0 0,1 3,9.5A6.5,6.5 0 0,1 9.5,3Z"/>
              </svg>
            </div>
            <div>
              <h3 class="font-semibold">Browse Games & Groups</h3>
              <p class="text-sm text-gray-600">Shown when users browse games, groups, and looking-for-players posts.</p>
            </div>
          </div>
          <div class="flex items-center gap-4 p-4 bg-gray-50 rounded-lg">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center flex-shrink-0">
              <svg class="w-6 h-6 text-primary-600" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
              </svg>
            </div>
            <div>
              <h3 class="font-semibold">Location-Targeted</h3>
              <p class="text-sm text-gray-600">Target ads to users in specific cities or states. Perfect for local game stores.</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Contact Form -->
      <div class="card p-8">
        <h2 class="text-2xl font-bold text-gray-900 mb-2">Get Started</h2>
        <p class="text-gray-600 mb-6">Fill out the form below and we'll get back to you with pricing and availability.</p>

        <div v-if="submitted" class="text-center py-8">
          <svg class="w-16 h-16 mx-auto text-green-500 mb-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
          </svg>
          <h3 class="text-xl font-bold text-gray-900 mb-2">Thanks for your interest!</h3>
          <p class="text-gray-600">We'll review your inquiry and get back to you within 1-2 business days.</p>
        </div>

        <form v-else @submit.prevent="handleSubmit" class="space-y-4">
          <div v-if="error" class="alert-error">{{ error }}</div>

          <div class="grid md:grid-cols-2 gap-4">
            <div>
              <label class="label">Your Name *</label>
              <input v-model="form.name" type="text" class="input" required />
            </div>
            <div>
              <label class="label">Email *</label>
              <input v-model="form.email" type="email" class="input" required />
            </div>
          </div>

          <div>
            <label class="label">Company / Store Name</label>
            <input v-model="form.company" type="text" class="input" placeholder="Optional" />
          </div>

          <div>
            <label class="label">What are you looking to advertise?</label>
            <select v-model="form.adType" class="input">
              <option value="game_store">Game Store</option>
              <option value="convention">Convention / Expo</option>
              <option value="product">Game / Product</option>
              <option value="tournament">Tournament / League</option>
              <option value="other">Other</option>
            </select>
          </div>

          <div>
            <label class="label">Tell us about your ad *</label>
            <textarea v-model="form.message" class="input" rows="4" placeholder="What would you like to promote? Any specific locations or timeframes?" required></textarea>
          </div>

          <button type="submit" class="btn-primary w-full py-3" :disabled="loading">
            <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Send Inquiry
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
