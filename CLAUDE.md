# Sasquatsh - Game Night Planning App

## Project Overview
Sasquatsh is a board game night planning platform built with Vue 3 + TypeScript frontend and Supabase Edge Functions backend.

## Tech Stack

### Frontend
- **Framework**: Vue 3 with Composition API
- **Language**: TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS
- **State Management**: Pinia-style composable stores
- **Router**: Vue Router

### Backend
- **Database**: Supabase PostgreSQL
- **API**: Supabase Edge Functions (Deno)
- **Authentication**: Firebase Auth (tokens passed via `X-Firebase-Token` header)

### Hosting
- **Frontend**: Firebase Hosting
- **Domain**: https://sasquatsh.com
- **Backend**: Supabase (hosted)

## Project Structure

```
gamenightapp/
├── frontend/                 # Vue 3 frontend app
│   ├── src/
│   │   ├── components/       # Reusable Vue components
│   │   ├── views/            # Page components
│   │   ├── stores/           # State management (useAuthStore, useEventStore, useGroupStore)
│   │   ├── services/         # API service functions
│   │   ├── types/            # TypeScript interfaces
│   │   └── router/           # Vue Router configuration
│   ├── dist/                 # Built output (gitignored)
│   ├── firebase.json         # Firebase Hosting config
│   └── package.json
├── supabase/
│   ├── functions/            # Edge Functions (Deno)
│   │   ├── _shared/          # Shared utilities (firebase.ts for auth)
│   │   ├── events/           # Events API
│   │   ├── groups/           # Groups API
│   │   ├── users/            # Users API
│   │   └── ...
│   └── migrations/           # Database migrations
└── CLAUDE.md                 # This file
```

## Deployment

### Frontend Deployment
The frontend is deployed to Firebase Hosting. To deploy:

```bash
cd frontend
npm run build
npx firebase deploy --only hosting
```

The site will be live at **https://sasquatsh.com**

**DO NOT use Netlify, Vercel, or other hosting services.**

### Backend Deployment (Edge Functions)
Edge Functions are deployed to Supabase. To deploy a function:

```bash
npx supabase functions deploy <function-name> --no-verify-jwt
```

Examples:
```bash
npx supabase functions deploy events --no-verify-jwt
npx supabase functions deploy groups --no-verify-jwt
npx supabase functions deploy users --no-verify-jwt
```

### Database Migrations
Migrations are in `supabase/migrations/`. To apply:

```bash
npx supabase db push
```

## Authentication Pattern

All authenticated API requests use this pattern:
1. Frontend gets Firebase ID token via `authStore.getIdToken()`
2. Token is passed in `X-Firebase-Token` header
3. Edge Functions verify token using `verifyFirebaseToken()` from `_shared/firebase.ts`
4. User is looked up in Supabase `users` table by `firebase_uid`

## Key Features

- **Events/Games**: Create and manage game nights with BGG integration
- **Groups**: Create groups, manage members, join requests, invitations
- **Looking for Players**: Find players in your area
- **User Profiles**: Game collections, preferences

## Environment Variables

### Frontend (.env)
- `VITE_FIREBASE_*` - Firebase config
- `VITE_SUPABASE_URL` - Supabase project URL
- `VITE_SUPABASE_ANON_KEY` - Supabase anon key
- `VITE_SUPABASE_FUNCTIONS_URL` - Edge Functions base URL

### Supabase Edge Functions
- `SUPABASE_URL` - Auto-provided
- `SUPABASE_SERVICE_ROLE_KEY` - Auto-provided
- `FIREBASE_*` - Firebase admin credentials (set in Supabase dashboard)

## Common Commands

```bash
# Frontend development
cd frontend && npm run dev

# Build frontend
cd frontend && npm run build

# FULL DEPLOYMENT (both frontend and backend)
# 1. Deploy backend (Supabase Edge Functions)
cd supabase && npx supabase functions deploy

# 2. Deploy frontend (Firebase Hosting)
cd frontend && npm run build && npx firebase deploy --only hosting

# Deploy a single Edge Function
npx supabase functions deploy <name> --no-verify-jwt

# Run database migrations
npx supabase db push
```

**When user asks to "deploy", always deploy BOTH backend and frontend.**
