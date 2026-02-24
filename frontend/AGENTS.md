# Repository Guidelines

## Project Structure & Module Organization
- `src/main.ts` bootstraps Vue 3 + Vuetify; `src/App.vue` holds the root layout.
- UI pieces live in `src/components`; global styles in `src/style.css` and static assets in `src/assets`/`public`.
- API helpers are in `src/services` (see `api.ts` for request wrapper and base URL handling).
- Shared types reside in `src/types.ts`; stateful logic goes in `src/stores` (e.g., `useGameStore.ts`).

## Build, Test, and Development Commands
- `npm install` to restore dependencies.
- `npm run dev` starts Vite with hot reload at localhost (default 5173).
- `npm run build` runs `vue-tsc -b` for type checking then Vite production build to `dist/`.
- `npm run preview` serves the built assets for final verification.

## Coding Style & Naming Conventions
- Use TypeScript with Vue Composition API; keep components in `<script setup>` when possible.
- Components/files: PascalCase for Vue components (`GameList.vue`), camelCase for functions/vars, and `useName.ts` for composables/stores.
- Favor single-responsibility components and keep API calls in `src/services` rather than inside views.
- Follow 2-space indentation; keep imports sorted logically (Vue/Vuetify, shared types, local modules).

## Testing Guidelines
- No automated test suite is present; use `npm run build` as a pre-flight to catch type and bundling issues.
- Add tests alongside new features (suggested: Vitest/unit tests or component tests) and document how to run them in the PR.
- For UI changes, include manual verification steps (browsers used, responsive checks) and screenshots when possible.

## Commit & Pull Request Guidelines
- Write imperative, focused commit messages; align with Conventional Commits (`feat:`, `fix:`, `chore:`) when reasonable.
- Keep PRs scoped: explain the user-facing change, note risks/unknowns, and list how to reproduce or verify.
- Link to related tickets/issues; attach screenshots or recordings for UI work and note any API assumptions (e.g., required endpoints).

## Configuration & Environment
- API base URL comes from `VITE_API_BASE_URL` (defaults to `http://localhost:5191` in `src/services/api.ts`). Set it in a local `.env` when pointing to another backend.
- Avoid committing secrets; use `.env.local` for machine-specific values and validate env requirements in PR descriptions.
