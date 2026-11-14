# Blossom E-commerce — Frontend

Technical assessment. Clean, readable code matters more than extra features.

## Stack
- React 18 + Vite + TypeScript (strict mode)
- Tailwind CSS for styling, responsive (mobile first)
- React Router for navigation
- Context API for global state (auth and cart). No Redux.
- axios for HTTP, with a single configured instance in `src/api/client.ts`
- react-hook-form + zod for form validation
- Vitest + React Testing Library for tests

## Backend contract
- Base URL: `http://localhost:8080/api/v1` (from `VITE_API_URL` env var)
- Auth: JWT returned by `POST /auth/login`; send it as `Authorization: Bearer <token>`.
  Store it in localStorage; on 401, clear the session and redirect to login.
- Errors follow RFC 7807: `{ title, detail, status, errors? }`, where `errors` maps field -> message.
  Show `detail` to the user and map `errors` onto form fields.
- Lists are paginated: `{ content, page, size, totalElements, totalPages }`.
- Always check the real DTOs in `backend/src/main/java/**/dto/` before writing types. Never invent field names.

## Folder structure (inside frontend/src)
- `api/` — axios client and one file per resource (products.ts, auth.ts, orders.ts)
- `types/` — TypeScript types mirroring the backend DTOs
- `context/` — AuthContext, CartContext
- `hooks/` — reusable hooks (useProducts, useAuth...)
- `pages/` — one component per screen
- `components/` — small reusable UI pieces
- `routes/` — router setup and ProtectedRoute

## Rules
- Small, focused components. No component above ~150 lines.
- Every data-fetching screen handles loading, error and empty states.
- No `any`. Explicit types for props and API responses.
- Code, comments and identifiers in English.
- All user-facing text (labels, buttons, messages, validation errors, empty states) in English.

## Required screens
1. Login and Register
2. Product list with filters (name, category, min/max price) and pagination
3. Product detail
4. Shopping cart (add/remove, change quantity, total) — cart lives in CartContext, persisted in localStorage
5. Order history for the authenticated user (protected route)

## Git
- This repo is shared by two agents: Claude Code (backend) and Copilot (frontend).
- Stage only paths under frontend/ or .github/.
  Never use `git add .` or `git add -A`. Never stage backend/, docs/ or CLAUDE.md.
- Do not push unless the user asks.