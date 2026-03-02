# Session Log

Work sessions organized newest to oldest.

---

## 2026-03-01 - Subscription & Billing System

### Current Plan: Subscription Tiers with Stripe

Implementing a tiered subscription system:

| Feature | Free ($0) | Basic ($7.99/mo) | Pro ($14.99/mo) | Enterprise |
|---------|-----------|------------------|-----------------|------------|
| Games per event | 1 | 5 | 10 | Contact Us |
| Groups | 1 | 5 | 10 | Contact Us |
| Table info per game | - | Yes | Yes | - |
| Host planning | - | Yes | Yes | - |
| Items list | - | - | Yes | - |
| Ads | Yes | No | No | - |

### Implementation Phases

- [ ] **Phase 1**: Database migration, types update
- [ ] **Phase 2**: Stripe account setup, environment variables
- [ ] **Phase 3**: Stripe Edge Functions (checkout, webhook, portal, billing)
- [ ] **Phase 4**: Pricing page (public)
- [ ] **Phase 5**: Billing section in Profile, BillingView page
- [ ] **Phase 6**: Feature gating (backend + frontend)
- [ ] **Phase 7**: Admin controls (tier management, ban/suspend)

### Current Progress

**Status**: Starting Phase 1 - Database Migration

### Work Completed This Session

1. Planned subscription system architecture
2. Defined tier limits and features
3. Designed database schema (users table updates, stripe_invoices, subscription_events)
4. Outlined Stripe integration approach
5. Created SESSION.md for tracking

### Files to Create

| File | Status |
|------|--------|
| `supabase/migrations/030_subscription_system.sql` | Pending |
| `supabase/functions/stripe-checkout/index.ts` | Pending |
| `supabase/functions/stripe-webhook/index.ts` | Pending |
| `supabase/functions/stripe-portal/index.ts` | Pending |
| `supabase/functions/billing/index.ts` | Pending |
| `frontend/src/views/PricingView.vue` | Pending |
| `frontend/src/views/BillingView.vue` | Pending |
| `frontend/src/config/subscriptionLimits.ts` | Pending |

### Full Plan Reference

See: `C:\Users\Ray\.claude\plans\groovy-squishing-crescent.md`

---

## 2026-03-01 - Avatar Upload Feature

### Completed

- Added avatar upload functionality to profile
- Created Supabase storage bucket for avatars
- Updated profile function with upload/delete actions
- Fixed auth-sync to preserve uploaded avatars on login
- Header now shows user's avatar immediately after upload

### Files Modified

- `supabase/migrations/029_avatar_storage.sql` (created)
- `supabase/functions/profile/index.ts`
- `supabase/functions/auth-sync/index.ts`
- `frontend/src/services/profileApi.ts`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/stores/useAuthStore.ts`
- `supabase/config.toml`

---
