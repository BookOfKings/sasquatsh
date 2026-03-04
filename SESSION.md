# Session Log

Work sessions organized newest to oldest.

---

## 2026-03-04 - Nearby Radius Search, E2E Tests, UI Fixes

### Work Completed

1. **Nearby Radius Search Feature (25-mile vicinity)**
   - Created `zip_codes` table with lat/long for US zip codes
   - Created Haversine distance calculation function (`calculate_distance_miles`)
   - Created `get_zips_within_radius()` RPC function
   - Created seed script to populate zip codes from public dataset
   - Added `nearbyZip` and `radiusMiles` parameters to events browse endpoint
   - Added "Nearby" toggle button with radius selector (10/25/50/100 miles)
   - Nearby search uses user's postal code from their profile
   - **Requires activation**: Run migration + seed zip codes

2. **Comprehensive E2E Tests (Playwright)**
   - Created full CRUD tests for events (create, view, edit, delete)
   - Created games management tests (add/remove BGG games from events)
   - Created groups CRUD tests (create, join, leave)
   - Created test fixtures and authentication helpers
   - Test credentials configured: `TestAccount334`

3. **UI Fixes**
   - Removed GitHub link from contact page
   - Removed large pricing tier cards section from home page
   - Home page now shows simple Get Started/Sign In buttons + Browse link

### Files Created

| File | Description |
|------|-------------|
| `supabase/migrations/038_zip_codes.sql` | Zip codes table with distance functions |
| `supabase/scripts/seed-zip-codes.ts` | Script to populate zip codes from public dataset |
| `frontend/e2e/events-full.spec.ts` | Full events CRUD tests |
| `frontend/e2e/groups-full.spec.ts` | Full groups CRUD tests |
| `frontend/e2e/games-management.spec.ts` | Add/remove games from events tests |
| `frontend/e2e/fixtures/test-helpers.ts` | Test helper utilities |

### Files Modified

| File | Changes |
|------|---------|
| `frontend/src/types/events.ts` | Added `nearbyZip`, `radiusMiles` to EventSearchFilter |
| `frontend/src/services/eventsApi.ts` | Added `browseEvents()` for authenticated radius search |
| `frontend/src/stores/useEventStore.ts` | Uses authenticated browse when logged in |
| `frontend/src/views/EventsView.vue` | Added Nearby toggle, radius selector |
| `frontend/src/views/ContactView.vue` | Removed GitHub link section |
| `frontend/src/views/HomeView.vue` | Removed large pricing tier cards |
| `supabase/functions/events/index.ts` | Added radius search support to browse |

### To Activate Nearby Search

1. Run migration: `038_zip_codes.sql`
2. Seed zip codes:
   ```bash
   SUPABASE_URL=xxx SUPABASE_SERVICE_ROLE_KEY=xxx npx tsx supabase/scripts/seed-zip-codes.ts
   ```
3. Users need postal code in their profile for "Nearby" button to appear

### E2E Test Commands

```bash
# Set test credentials
set TEST_USER_EMAIL=TestAccount334@...
set TEST_USER_PASSWORD=<password>

# Install browsers (first time)
npx playwright install

# Run tests
npm run test:e2e
npx playwright test e2e/events-full.spec.ts
```

---

## 2026-03-03 - Game Thumbnails, Admin Fixes, UI Updates

### Work Completed

1. **Fixed Admin Panel API Errors**
   - Fixed "Cannot read properties of undefined (reading 'length')" in Ads tab
   - Changed all `data?.map()` to `(data || []).map()` in admin-stats
   - Added `|| []` fallbacks in frontend adsApi.ts

2. **Game Thumbnails on Event Cards**
   - Event cards now show board game box art with host avatar as overlay badge
   - Added `primaryGameThumbnail` field to EventSummary type
   - Added `games:event_games(thumbnail_url, is_primary)` join to events queries
   - Created admin "Refresh Thumbnails" button to fetch missing thumbnails from BGG
   - Created migration to backfill thumbnails from BGG cache

3. **UI Text Updates**
   - Changed "Plan a Game Night" to "Host a Game" on group detail page
   - Changed minimum dates for planning sessions from 2 to 1

### Files Created

| File | Description |
|------|-------------|
| `supabase/migrations/037_backfill_event_game_thumbnails.sql` | Backfill thumbnails from cache |

### Files Modified

| File | Changes |
|------|---------|
| `frontend/src/components/events/EventCard.vue` | Game thumbnail with host avatar overlay |
| `frontend/src/services/eventsApi.ts` | Added games join to public events query |
| `frontend/src/services/adminApi.ts` | Added refreshBggThumbnails function |
| `frontend/src/services/adsApi.ts` | Added null checks for API responses |
| `frontend/src/types/events.ts` | Added primaryGameThumbnail field |
| `frontend/src/views/AdminView.vue` | Added "Refresh Thumbnails" button, fixed ads |
| `frontend/src/views/GroupDetailView.vue` | Changed "Plan a Game Night" to "Host a Game" |
| `frontend/src/views/PlanGameNightView.vue` | Allow single date option |
| `supabase/functions/admin-stats/index.ts` | Fixed null arrays, added refresh-bgg-cache |
| `supabase/functions/events/index.ts` | Added games join and primaryGameThumbnail |

---

## 2026-03-03 - SendGrid Email Integration

### Work Completed

1. **SendGrid Email Service**
   - Created shared email utility (`_shared/email.ts`) with SendGrid API integration
   - Beautiful HTML email templates for invitations and contact notifications
   - Plain text fallbacks for all emails

2. **Invitation Emails**
   - When creating an invitation with an email address, recipient gets styled email
   - Includes event title, host name, date/time, location
   - "Join the Game" CTA button with invite link

3. **Contact Form Notifications**
   - Admin receives email notification at ray@sasquatsh.com when contact form submitted
   - Includes sender name, email, subject, and full message
   - Reply-to link for easy response

4. **Home Page CTA Repositioned**
   - Moved upgrade CTA under Dashboard/Browse buttons for free tier users

### Secrets Configured

- `SENDGRID_API_KEY` - SendGrid API key for sending emails
- `ADMIN_EMAIL` - Contact form notifications recipient
- `APP_URL` - Base URL for invite links

### Files Created

| File | Description |
|------|-------------|
| `supabase/functions/_shared/email.ts` | SendGrid email utility and templates |

### Files Modified

| File | Changes |
|------|---------|
| `supabase/functions/invitations/index.ts` | Added email sending on invite creation |
| `supabase/functions/contact/index.ts` | Added admin notification emails |
| `frontend/src/views/HomeView.vue` | Moved upgrade CTA position |

---

## 2026-03-03 - Bug Fixes, Time Zones, Home Page Tiers, Self-Hosted Ads System

### Work Completed

1. **Bug Fixes (all 6 open bugs resolved)**
   - Bug #1: Free accounts can add items - Already fixed via items feature gating
   - Bug #2: Games page defaults to profile location - Added profile loading to EventsView
   - Bug #3: "I am playing" toggle - Added `hostIsPlaying` field to events
   - Bug #4: LFP only shows active games - Changed to time-based filtering (upcoming games)
   - Bug #5: Create sitemap - Created `sitemap.xml` and `robots.txt`
   - Bug #6: Image upload slow - Added client-side image compression before upload

2. **Time Zone Support**
   - Added `timezone` column to users, events, and event_locations tables
   - Added timezone dropdown to Create/Edit Event forms
   - Defaults to user's profile timezone
   - Updates to venue timezone when selecting a venue
   - Set all existing users to `America/Phoenix` (MST)

3. **Home Page Subscription Tiers**
   - Added tier comparison section for non-authenticated users
   - Shows Free/Basic ($7.99)/Pro ($14.99) with feature lists
   - Added upgrade CTA banner for free tier authenticated users

4. **Self-Hosted Advertising System**
   - Created `ads`, `ad_impressions`, `ad_clicks` tables with `ad_stats` view
   - Created ads edge function for serving and tracking ads
   - Created `AdBanner.vue` component (shows only to free tier users)
   - Created `adsApi.ts` admin service for CRUD operations
   - Added full ads management to Admin panel with stats
   - Added ad placements to Dashboard, Events, and Groups pages
   - Initial house ads for "Upgrade to Basic" and "Upgrade to Pro"

### Files Created

| File | Description |
|------|-------------|
| `frontend/public/sitemap.xml` | SEO sitemap |
| `frontend/public/robots.txt` | Robots configuration |
| `frontend/src/components/ads/AdBanner.vue` | Ad display component |
| `frontend/src/services/adsApi.ts` | Admin ads API service |
| `supabase/functions/ads/index.ts` | Public ads API |
| `supabase/migrations/031_host_is_playing.sql` | Host playing field |
| `supabase/migrations/032_timezones.sql` | Timezone columns |
| `supabase/migrations/033_set_mst_timezone.sql` | Set MST timezone |
| `supabase/migrations/034_set_phoenix_timezone.sql` | Set Phoenix timezone |
| `supabase/migrations/035_ads_system.sql` | Ads database schema |
| `supabase/migrations/036_mark_ads_implemented.sql` | Mark ads note complete |

### Files Modified

| File | Changes |
|------|---------|
| `frontend/src/views/AdminView.vue` | Added ads management tab |
| `frontend/src/views/CreateEventView.vue` | Added timezone field, profile default |
| `frontend/src/views/EditEventView.vue` | Added timezone field, venue watch |
| `frontend/src/views/EventsView.vue` | Profile location default, ad placement |
| `frontend/src/views/DashboardView.vue` | Added ad placement |
| `frontend/src/views/GroupsView.vue` | Added ad placement |
| `frontend/src/views/HomeView.vue` | Added tier comparison section |
| `frontend/src/views/ProfileView.vue` | Added timezone field |
| `frontend/src/views/LookingForPlayersView.vue` | Time-based filtering |
| `frontend/src/views/EventDetailView.vue` | Show host playing status |
| `frontend/src/views/PlanningSessionView.vue` | Show host playing in attendees |
| `frontend/src/types/events.ts` | Added hostIsPlaying, timezone fields |
| `frontend/src/types/profile.ts` | Added timezone field |
| `frontend/src/services/profileApi.ts` | Added image compression |
| `frontend/src/services/eventsApi.ts` | Added timezone to DTOs |
| `supabase/functions/admin-stats/index.ts` | Added ads CRUD endpoints |
| `supabase/functions/events/index.ts` | Added hostIsPlaying support |
| `supabase/functions/profile/index.ts` | Added timezone support |

### Ads System Details

- **Tracking**: Impressions deduplicated by IP hash (1-hour window), clicks tracked individually
- **Targeting**: Supports city/state targeting, date scheduling, priority weighting
- **Admin UI**: Full CRUD with stats (impressions, clicks, CTR)
- **House Ads**: Initial self-promo ads for tier upgrades

---

## 2026-03-02 - E2E Testing, Header Plan Badge, Admin Enhancements

### Work Completed

1. **Playwright E2E Testing Setup**
   - Installed `@playwright/test` and browsers
   - Created `playwright.config.ts` with multi-browser support (Chromium, Firefox, WebKit)
   - Created test files: `home.spec.ts`, `auth.spec.ts`, `navigation.spec.ts`, `groups.spec.ts`, `events.spec.ts`, `profile.spec.ts`
   - Added test scripts to `package.json`
   - 37 tests passing, 4 skipped (require auth credentials)

2. **Header Plan Indicator**
   - Added plan badge next to user avatar showing current tier (Free/Basic/Pro/Premium)
   - "Upgrade" link for free tier users linking to /pricing
   - Plan info added to user dropdown menu and mobile menu
   - Admins don't see upgrade prompt (they have full access)

3. **Admin Edit User Dialog Enhancement**
   - Added subscription tier dropdown to Edit User dialog
   - Added "Override Reason" field for admin notes
   - Tier changes saved when dialog is submitted
   - Integrated with existing tier override system

4. **Admin Group Member Management**
   - Added "Members" button on each group row
   - Members dialog shows all members with avatar, name, email, role
   - Role dropdown to change member roles (owner/admin/member)
   - Remove button to kick members from group
   - "Add Member" dialog with user search
   - Backend endpoints: `group-members`, `add-group-member`, `remove-group-member`, `change-member-role`

5. **Bug Fix: GroupDetailView**
   - Fixed planning sessions loading for non-members
   - Now only loads planning sessions if user is a member of the group
   - Eliminates 403 error for non-members viewing public groups

### Files Created

| File | Description |
|------|-------------|
| `frontend/playwright.config.ts` | Playwright configuration |
| `frontend/e2e/home.spec.ts` | Home page tests |
| `frontend/e2e/auth.spec.ts` | Authentication tests |
| `frontend/e2e/navigation.spec.ts` | Navigation & responsive tests |
| `frontend/e2e/groups.spec.ts` | Groups page tests |
| `frontend/e2e/events.spec.ts` | Events page tests |
| `frontend/e2e/profile.spec.ts` | Profile tests (auth required) |
| `frontend/e2e/global-setup.ts` | Global test setup |
| `frontend/e2e/fixtures/auth.fixture.ts` | Auth fixture for tests |

### Files Modified

| File | Changes |
|------|---------|
| `frontend/package.json` | Added Playwright and test scripts |
| `frontend/.gitignore` | Added Playwright output folders |
| `frontend/src/App.vue` | Added plan indicator to header |
| `frontend/src/views/AdminView.vue` | Added tier to edit dialog, group member management |
| `frontend/src/services/adminApi.ts` | Added group member API functions |
| `frontend/src/views/GroupDetailView.vue` | Fixed planning sessions for non-members |
| `supabase/functions/admin-stats/index.ts` | Added group member management endpoints |

### Test Commands

```bash
npm run test          # Run all tests
npm run test:ui       # Interactive UI mode
npm run test:headed   # Run with browser visible
npm run test:chromium # Run only Chromium
```

---

## 2026-03-02 - Admin Controls (Phase 7)

### Work Completed

1. **Backend tier management and ban actions** (`admin-stats/index.ts`)
   - `set-tier` action: Set user subscription override tier with reason
   - `ban-user` action: Permanently ban a user (different from suspend)
   - `unban-user` action: Remove permanent ban
   - Updated `users` query to include subscription and ban fields
   - All actions logged to `subscription_events` table

2. **Updated AdminUser interface** (`adminApi.ts`)
   - Added subscription fields: `subscriptionTier`, `subscriptionOverrideTier`, `effectiveTier`
   - Added account status fields: `accountStatus`, `bannedAt`, `banReason`
   - Added new API functions: `banUser()`, `unbanUser()`, `setUserTier()`

3. **Enhanced AdminView user management**
   - User rows now show tier badge with override indicator
   - User rows show account status (Active/Suspended/Banned)
   - Added "Set Tier" button with dialog to set override
   - Added "Ban" button with confirmation dialog
   - Added "Unban" button for banned users
   - Added filter checkboxes for suspended/banned users
   - Ban/Suspend are separate actions (suspend=temporary, ban=permanent)

### Files Modified

| File | Changes |
|------|---------|
| `supabase/functions/admin-stats/index.ts` | Added set-tier, ban-user, unban-user actions |
| `frontend/src/services/adminApi.ts` | Added new admin functions and types |
| `frontend/src/views/AdminView.vue` | Added tier/ban management UI |

### Current Plan Progress

- [x] **Phase 1**: Database migration, types update
- [x] **Phase 2**: Stripe account setup, environment variables
- [x] **Phase 3**: Stripe Edge Functions (checkout, webhook, portal, billing)
- [x] **Phase 4**: Pricing page (public)
- [x] **Phase 5**: Billing section in Profile, BillingView page
- [x] **Phase 6**: Feature gating (backend + frontend)
- [x] **Phase 7**: Admin controls (tier management, ban/suspend)

**Subscription System Complete!**

---

## 2026-03-02 - Feature Gating (Tier Limits)

### Work Completed

1. **Backend enforcement for game creation limits**
   - Updated `events/index.ts` to check user's subscription tier
   - Free users limited to 1 active (upcoming) game at a time
   - Basic: 5 games, Pro: 10 games, Premium: unlimited
   - Returns `TIER_LIMIT_REACHED` error with details when limit hit

2. **Backend enforcement for group creation limits**
   - Updated `groups/index.ts` to check user's subscription tier
   - Free users limited to 1 group they own
   - Basic: 5 groups, Pro: 10 groups, Premium: unlimited
   - Returns `TIER_LIMIT_REACHED` error with details when limit hit

3. **Created `UpgradePrompt.vue` component**
   - Reusable modal for both game and group limit scenarios
   - Shows current tier and limit
   - Recommends upgrade with feature comparison
   - Links to pricing page

4. **Updated `CreateEventView.vue`**
   - Checks active event count on mount
   - Shows upgrade prompt if already at limit
   - Handles `TIER_LIMIT_REACHED` error from backend gracefully

5. **Updated `CreateGroupView.vue`**
   - Checks owned group count on mount
   - Shows upgrade prompt if already at limit
   - Handles `TIER_LIMIT_REACHED` error from backend gracefully

6. **Deployed all changes**
   - Frontend: https://sasquatsh.web.app
   - Backend functions: events, groups

### Files Created

| File | Description |
|------|-------------|
| `frontend/src/components/billing/UpgradePrompt.vue` | Reusable upgrade prompt modal |

### Files Modified

| File | Changes |
|------|---------|
| `supabase/functions/events/index.ts` | Added tier limit check on event creation |
| `supabase/functions/groups/index.ts` | Added tier limit check on group creation |
| `frontend/src/views/CreateEventView.vue` | Integrated upgrade prompt |
| `frontend/src/views/CreateGroupView.vue` | Integrated upgrade prompt |

### Current Plan Progress

- [x] **Phase 1**: Database migration, types update
- [x] **Phase 2**: Stripe account setup, environment variables
- [x] **Phase 3**: Stripe Edge Functions (checkout, webhook, portal, billing)
- [x] **Phase 4**: Pricing page (public)
- [x] **Phase 5**: Billing section in Profile, BillingView page
- [x] **Phase 6**: Feature gating (backend + frontend)
- [ ] **Phase 7**: Admin controls (tier management, ban/suspend)

---

## 2026-03-02 - Frontend Billing UI

### Work Completed

1. Created `BillingView.vue` - Full billing management page with:
   - Current plan display with features
   - Payment method display
   - Invoice history with pagination (15 per page)
   - Invoice detail modal with download links
   - Cancel/reactivate subscription functionality
   - Stripe portal integration for payment management

2. Created `PricingView.vue` - Public pricing page with:
   - Three plan cards (Free, Basic, Pro)
   - Feature comparison checklist
   - Stripe Checkout integration for upgrades
   - Enterprise "Contact Us" section
   - FAQ section

3. Added billing routes to `router/index.ts`:
   - `/pricing` - Public pricing page
   - `/billing` - Authenticated billing management

4. Updated `ProfileView.vue` with billing section:
   - Shows current tier badge
   - Upgrade button for free users
   - Manage button linking to billing page

5. Created `billingApi.ts` service with:
   - `getSubscriptionInfo()` - Get current subscription
   - `getInvoices()` - Paginated invoice history
   - `createCheckoutSession()` - Stripe Checkout
   - `createPortalSession()` - Stripe Portal
   - `cancelSubscription()` / `reactivateSubscription()`
   - Helper functions for formatting

6. Built and deployed frontend to https://sasquatsh.web.app

### Files Created

| File | Description |
|------|-------------|
| `frontend/src/views/PricingView.vue` | Public pricing page |
| `frontend/src/views/BillingView.vue` | Billing management page |
| `frontend/src/services/billingApi.ts` | Billing API service |

### Files Modified

| File | Changes |
|------|---------|
| `frontend/src/router/index.ts` | Added /pricing and /billing routes |
| `frontend/src/views/ProfileView.vue` | Added billing section |

### Current Plan Progress

- [x] **Phase 1**: Database migration, types update
- [x] **Phase 2**: Stripe account setup, environment variables
- [x] **Phase 3**: Stripe Edge Functions (checkout, webhook, portal, billing)
- [x] **Phase 4**: Pricing page (public)
- [x] **Phase 5**: Billing section in Profile, BillingView page
- [x] **Phase 6**: Feature gating (backend + frontend)
- [ ] **Phase 7**: Admin controls (tier management, ban/suspend)

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

- [x] **Phase 1**: Database migration, types update
- [x] **Phase 2**: Stripe account setup, environment variables
- [x] **Phase 3**: Stripe Edge Functions (checkout, webhook, portal, billing)
- [x] **Phase 4**: Pricing page (public)
- [x] **Phase 5**: Billing section in Profile, BillingView page
- [ ] **Phase 6**: Feature gating (backend + frontend)
- [ ] **Phase 7**: Admin controls (tier management, ban/suspend)

### Current Progress

**Status**: Phase 5 Complete - Ready for Phase 6 (Feature Gating)

### Work Completed This Session

1. Planned subscription system architecture
2. Defined tier limits and features
3. Designed database schema (users table updates, stripe_invoices, subscription_events)
4. Outlined Stripe integration approach
5. Created SESSION.md for tracking

### Files to Create

| File | Status |
|------|--------|
| `supabase/migrations/030_subscription_system.sql` | Complete |
| `frontend/src/config/subscriptionLimits.ts` | Complete |
| `supabase/functions/stripe-checkout/index.ts` | Complete |
| `supabase/functions/stripe-webhook/index.ts` | Complete |
| `supabase/functions/stripe-portal/index.ts` | Complete |
| `supabase/functions/billing/index.ts` | Complete |
| `frontend/src/views/PricingView.vue` | Complete |
| `frontend/src/views/BillingView.vue` | Complete |
| `frontend/src/services/billingApi.ts` | Complete |

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
