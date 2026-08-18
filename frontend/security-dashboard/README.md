# Security dashboard

React + TypeScript + Vite SOC console. Phase 1 is a routed shell with Tailwind and shadcn-style primitives. API calls other than dashboard health are not implemented yet.

```bash
npm install
npm run dev
```

http://localhost:5173 — Vite proxies `/api` to api-service `:8080`.

```bash
npm run build
npx playwright test   # requires browsers; optional in Phase 1
```
