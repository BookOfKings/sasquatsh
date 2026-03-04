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

export default router
