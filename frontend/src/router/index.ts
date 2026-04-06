import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/signup',
    name: 'signup',
    component: () => import('@/views/SignupView.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: () => import('@/views/ForgotPasswordView.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/games',
    name: 'games',
    component: () => import('@/views/EventsView.vue'),
  },
  {
    path: '/games/create',
    name: 'create-game',
    component: () => import('@/views/CreateEventView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/games/:id',
    name: 'game-detail',
    component: () => import('@/views/EventDetailView.vue'),
  },
  {
    path: '/games/:id/edit',
    name: 'edit-game',
    component: () => import('@/views/EditEventView.vue'),
    meta: { requiresAuth: true },
  },
  // Redirect old /events routes to /games for backwards compatibility
  {
    path: '/events',
    redirect: '/games',
  },
  {
    path: '/events/create',
    redirect: '/games/create',
  },
  {
    path: '/events/:id',
    redirect: (to) => `/games/${to.params.id}`,
  },
  // Groups
  {
    path: '/groups',
    name: 'groups',
    component: () => import('@/views/GroupsView.vue'),
  },
  {
    path: '/groups/create',
    name: 'create-group',
    component: () => import('@/views/CreateGroupView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/groups/invite/:code',
    name: 'group-invite',
    component: () => import('@/views/GroupInviteView.vue'),
  },
  {
    path: '/groups/:slug',
    name: 'group-detail',
    component: () => import('@/views/GroupDetailView.vue'),
  },
  {
    path: '/groups/:slug/plan',
    name: 'plan-game-night',
    component: () => import('@/views/PlanGameNightView.vue'),
    meta: { requiresAuth: true },
  },
  // Planning Sessions
  {
    path: '/planning/:id',
    name: 'planning-session',
    component: () => import('@/views/PlanningSessionView.vue'),
    meta: { requiresAuth: true },
  },
  // Looking for Players
  {
    path: '/looking-for-players',
    name: 'looking-for-players',
    component: () => import('@/views/LookingForPlayersView.vue'),
  },
  // Admin
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/AdminView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
  // Pricing & Billing
  {
    path: '/pricing',
    name: 'pricing',
    component: () => import('@/views/PricingView.vue'),
  },
  {
    path: '/billing',
    name: 'billing',
    component: () => import('@/views/BillingView.vue'),
    meta: { requiresAuth: true },
  },
  // Invitations
  {
    path: '/invite/:code',
    name: 'invite',
    component: () => import('@/views/InviteView.vue'),
  },
  // Legal pages
  {
    path: '/terms',
    name: 'terms',
    component: () => import('@/views/TermsView.vue'),
  },
  {
    path: '/privacy',
    name: 'privacy',
    component: () => import('@/views/PrivacyView.vue'),
  },
  {
    path: '/cookies',
    name: 'cookies',
    component: () => import('@/views/CookiesView.vue'),
  },
  {
    path: '/contact',
    name: 'contact',
    component: () => import('@/views/ContactView.vue'),
  },
  // MTG Event Creation
  {
    path: '/mtg/events/create',
    name: 'create-mtg-event',
    component: () => import('@/views/CreateMtgEventView.vue'),
    meta: { requiresAuth: true },
  },
  // MTG Event Edit
  {
    path: '/mtg/events/:id/edit',
    name: 'edit-mtg-event',
    component: () => import('@/views/EditMtgEventView.vue'),
    meta: { requiresAuth: true },
  },
  // Pokemon TCG Event Creation
  {
    path: '/pokemon/events/create',
    name: 'create-pokemon-event',
    component: () => import('@/views/CreatePokemonEventView.vue'),
    meta: { requiresAuth: true },
  },
  // Pokemon TCG Event Edit
  {
    path: '/pokemon/events/:id/edit',
    name: 'edit-pokemon-event',
    component: () => import('@/views/EditPokemonEventView.vue'),
    meta: { requiresAuth: true },
  },
  // Yu-Gi-Oh! Event Creation
  {
    path: '/yugioh/events/create',
    name: 'create-yugioh-event',
    component: () => import('@/views/CreateYugiohEventView.vue'),
    meta: { requiresAuth: true },
  },
  // Yu-Gi-Oh! Event Edit
  {
    path: '/yugioh/events/:id/edit',
    name: 'edit-yugioh-event',
    component: () => import('@/views/EditYugiohEventView.vue'),
    meta: { requiresAuth: true },
  },
  // Warhammer 40k Event Creation
  {
    path: '/warhammer40k/events/create',
    name: 'create-warhammer40k-event',
    component: () => import('@/views/CreateWarhammer40kEventView.vue'),
    meta: { requiresAuth: true },
  },
  // Warhammer 40k Event Edit
  {
    path: '/warhammer40k/events/:id/edit',
    name: 'edit-warhammer40k-event',
    component: () => import('@/views/EditWarhammer40kEventView.vue'),
    meta: { requiresAuth: true },
  },
  // MTG Deck Management
  {
    path: '/mtg/decks',
    name: 'my-decks',
    component: () => import('@/views/MyDecksView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/mtg/decks/new',
    name: 'create-deck',
    component: () => import('@/views/MtgDeckBuilderView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/mtg/decks/:id',
    name: 'view-deck',
    component: () => import('@/views/MtgDeckBuilderView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/mtg/decks/:id/edit',
    name: 'edit-deck',
    component: () => import('@/views/MtgDeckBuilderView.vue'),
    meta: { requiresAuth: true },
  },
  // Catch-all redirect to home
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // Wait for auth to initialize
  if (!auth.isInitialized.value) {
    await auth.initializeAuth()
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated.value) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.guestOnly && auth.isAuthenticated.value) {
    return { name: 'dashboard' }
  }

  // Admin route guard
  if (to.meta.requiresAdmin && !auth.isAdmin.value) {
    return { name: 'home' }
  }

  // Allow navigation
  return true
})

// Update canonical URL on each navigation
router.afterEach((to) => {
  const canonical = document.querySelector('link[rel="canonical"]') as HTMLLinkElement | null
  if (canonical) {
    canonical.href = `https://sasquatsh.com${to.path}`
  }
})

export default router
