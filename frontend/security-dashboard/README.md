# Security dashboard

React + TypeScript + Vite SOC console. Talks only to api-service.

```bash
npm install
npm test
npm run build
npm run dev
```

http://localhost:5173 — Vite proxies `/api` to api-service `:8080` and attaches local HTTP Basic (`analyst` / `analyst_change_me` by default).
