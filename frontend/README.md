# FresherRadar – Stage 1 (Frontend)

Frontend-only build using HTML5, CSS3 and vanilla JavaScript. No frameworks, no build step, no dependencies.

> **Sample data notice:** every record in `script.js` is a made-up demo record (fictional companies, `example.com` apply links). They are not real job openings. The UI shows a "Demo data" banner whenever records have `isDemo: true`.

## Run locally

Any of these works:

```bash
# Option 1: just open the file
open index.html            # macOS  (Windows: double-click index.html)

# Option 2: a tiny local server (recommended, behaves like production)
python3 -m http.server 5500
# then visit http://localhost:5500
```

Handy URLs while reviewing:
- `index.html?demoError=1` previews the error state (with a working "Try again" button).
- Press `/` anywhere to focus the search box, `Esc` to close the details panel.

## Files

| File | Responsibility |
|---|---|
| `index.html` | Page structure: sidebar, top bar, overview (stats + insight panels), filters, job list, states, companies, about, footer, details drawer |
| `style.css` | Design tokens (colours, spacing) at the top, then one section per component, then responsive rules |
| `script.js` | Config, sample data, data access, filter logic, rendering, events |

## UI architecture

`script.js` reads top to bottom in the order data flows:

1. **CONFIG**: one switch (`USE_SAMPLE_DATA`) decides sample data vs. real API.
2. **SAMPLE_JOBS**: the only place fake data lives.
3. **`fetchJobs()`**: the only function that knows where data comes from.
4. **State**: `allJobs` (everything loaded) and `filters` (what the user chose).
5. **`getFilteredJobs()`**: a pure function that applies search, filters and sorting. Think of it as a `WHERE ... ORDER BY` over `allJobs`.
6. **Render functions**: turn data into HTML (`createJobCardHtml`, `renderStats`, `renderCompanies`, ...).
7. **Events**: one place (`bindEvents`) wires the inputs to the state and re-renders.

The flow is always: user action → update `filters` → `refreshJobList()` → `getFilteredJobs()` → render.

Anything that changes a filter from outside the dropdowns (sidebar quick views, overview panels, filter chips) updates `filters` and then calls `syncControlsFromFilters()` so the visible controls always match the state.

## Job object (the contract with the backend)

| Field | Type | Notes |
|---|---|---|
| `id` | number or string | unique |
| `title` | string | |
| `company` | string | |
| `location` | string | city, or `"Remote (India)"` |
| `category` | string | e.g. `"Software Engineering"`, `"AI / ML"` |
| `employmentType` | `"Full-time"` or `"Internship"` | |
| `experience` | `"0 years"`, `"0-1 years"` or `"0-2 years"` | |
| `postedAt` | ISO-8601 string | e.g. `"2026-11-14T09:30:00Z"` |
| `source` | string | where the job was originally found |
| `applyUrl` | string | direct link, http/https only |
| `description` | string | |
| `skills` | string[] | |
| `eligibility` | string[] | |
| `isDemo` | boolean | optional, only for sample records |

Expected response of `GET /api/jobs`:

```json
{ "lastUpdated": "2026-11-16T02:00:00Z", "jobs": [ { "id": 1, "title": "..." } ] }
```

## Connecting to Spring Boot later

1. In `script.js` set `USE_SAMPLE_DATA: false` and point `API_BASE_URL` to your server.
2. Create a `JobDto` whose field names match the table above. Jackson serialises Java camelCase fields as-is (`employmentType`, `postedAt`, `applyUrl`).
3. Return `{ lastUpdated, jobs }` from `GET /api/jobs`. Use `Instant` for dates; it serialises to ISO-8601.
4. Allow the browser to call the API, either with `@CrossOrigin(origins = "http://localhost:5500")` on the controller or a `WebMvcConfigurer`. Or skip CORS entirely by copying the three files into `src/main/resources/static/` so Spring Boot serves them from the same origin (then use `API_BASE_URL: "/api"`).
5. Nothing else in the frontend changes. Filtering currently runs in the browser, which is fine for a few hundred jobs. If the list grows to thousands, move search and filters to query parameters (`/api/jobs?city=Pune&type=Internship`) and have `fetchJobs(filters)` pass them through.

## Design decisions worth knowing

- **Inter via one Google Fonts `<link>`, with a system font stack as fallback.** No icon library: all icons are small inline SVGs. If the font fails to load (offline), the page falls back cleanly. Remove the three font `<link>` tags in `index.html` to go fully dependency-free.
- **One restrained accent colour** on a neutral palette, with borders instead of heavy shadows (the Linear/Vercel look). Colour is also reserved for meaning: green = fresh/updated, amber = demo warning, red = error.
- **Details open in a side drawer, not a new page.** The user keeps their scroll position and filters. It has Previous/Next buttons (and ← → keys) that walk through the current results, and a pinned Apply button. On mobile it becomes full-screen.
- **Sidebar app shell.** Sections (Overview, Jobs, Companies, About) with scroll-spy highlighting, plus *quick views* (New today, Internships, Full-time, Remote) that act as toggleable filters with live counts. Below 1024px it becomes a slide-in menu.
- **Overview panels are clickable.** Top roles, top locations and the full-time/internship mix are computed from the loaded jobs (like `GROUP BY` in SQL) and filter the list when clicked, so they are controls, not decoration. They work unchanged with API data.
- **Active filters are visible and removable** as chips under the search box; dropdowns with a value are highlighted.
- **Cards are compact and scannable**: title and company first, four metadata chips, then date, source and a primary Apply button. Clicking the card opens details; clicking Apply goes straight to the source.
- **Companies and About are real sections.** Clicking a company filters the job list to it, so nav items are not decorative.
- **"Show more" instead of pagination or infinite scroll.** Simple, predictable and works for 200+ jobs.
- **Safety by default.** All text is escaped before reaching `innerHTML`, and apply links must be http/https. This matters once data comes from scraped sources.
- **Accessibility basics:** keyboard focus styles, focus trapped in the drawer, `Esc` to close, `aria-live` result count, reduced-motion support.
- **No search-engine or LLM logic here.** Classification, de-duplication and the 24h refresh belong to the backend stages. The UI only displays `category` and `lastUpdated`.
