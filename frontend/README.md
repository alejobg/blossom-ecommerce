# Blossom E-commerce Frontend

React, TypeScript, and Vite frontend for the Blossom e-commerce application.
It provides a product catalog, authentication, cart, checkout, and order history.

## Getting started

Run these commands from `frontend/`:

```sh
npm install
npm run dev
npm test
```

Start the backend separately. Copy `.env.example` to `.env` to customize the API URL.

## Environment

| Variable | Default | Description |
| --- | --- | --- |
| `VITE_API_URL` | `http://localhost:8080/api/v1` | Backend API base URL |

## Source structure

- `src/api/`: HTTP client and API resources
- `src/components/`: reusable UI components
- `src/context/`: authentication and cart state
- `src/hooks/`: reusable hooks
- `src/pages/`: application screens
- `src/routes/`: routing and access guards
- `src/types/`: API and application types
- `src/utils/`: shared utilities
- `src/test/`: test setup
