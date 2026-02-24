# Repository Guidelines

## Project Structure & Module Organization
- `GameNight.Api/` hosts the .NET 8 Web API (controllers, DTOs, entities, services) with MySQL connectivity configured in `appsettings*.json` and registered in `Program.cs`.
- `frontend/` is a Vue 3 + TypeScript + Vite app with Vuetify UI; primary code lives in `src/` (components, stores, services, assets), `public/` holds static assets, and `dist/` is generated builds.
- `GameNightApp.sln` ties backend projects together; `publish/` may contain deployment artifacts created by `dotnet publish`.

## Build, Test, and Development Commands
- Backend: `dotnet restore` then `dotnet build GameNightApp.sln` to verify compilation; run locally with `ASPNETCORE_ENVIRONMENT=Development dotnet run --project GameNight.Api` (defaults to port 5191 and enables Swagger in Development).
- Frontend: from `frontend/`, `npm install` once, `npm run dev -- --host` for live reload on port 5173, `npm run build` to emit production assets to `dist/`, and `npm run preview` to serve the built bundle.

## Coding Style & Naming Conventions
- C#: 4-space indent, nullable enabled; prefer `async`/`await` with `CancellationToken` parameters, log via `ILogger`, and keep MySQL commands parameterized as in `GameRepository` to avoid injection.
- TypeScript/Vue: use `<script setup>` SFCs, PascalCase component filenames, camelCase props and locals, and keep API calls in `src/services` with types from `src/types.ts`. Vuetify styles live alongside components; keep shared styles in `src/style.css` or scoped `<style lang="scss">` blocks.

## Testing Guidelines
- No automated test projects are present yet; add targeted coverage when changing behavior (e.g., create `GameNight.Api.Tests` with xUnit and run via `dotnet test`, or add component/utility specs under `frontend/src/**/__tests__` using Vitest once added to devDependencies).
- Favor fast unit tests for logic (reservation rules, DTO mapping) and lightweight integration tests for API endpoints that exercise MySQL interactions.

## Commit & Pull Request Guidelines
- Write clear, present-tense commit subjects (e.g., `backend: prevent duplicate reservations`, `frontend: show seat counts`).
- Keep PRs focused; include a short summary, testing notes (commands run), linked issues, and UI screenshots or screen captures for visible changes. Document config assumptions (API base URL, DB) in the PR description.

## Security & Configuration Tips
- Do not commit secrets; override `ConnectionStrings__DefaultConnection` and other settings via environment variables or user-secrets instead of editing `appsettings.json`.
- Adjust CORS origins in `appsettings*.json` carefully to match deployed frontends.
- Frontend uses `VITE_API_BASE_URL` (default `http://localhost:5191`) to reach the API; align this with the backend host/port before running.
