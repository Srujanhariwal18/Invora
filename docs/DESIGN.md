# INVORA: UI/UX Frontend Specification & Design System

**Product:** Invora, Warehouse Inventory Stock IN/OUT & Threshold Alert System  
**Frontend stack:** HTML5, CSS3, vanilla JavaScript (ES modules), Chart.js, Three.js, Lucide icons. No framework and no build step. Served by Spring Boot from `src/main/resources/static`.  
**Backend:** Java 21 + Spring Boot REST API (see the tech stack spec)  
**Version:** 1.0  

> **Instruction to Antigravity:** Implement the entire frontend described here. It must feel like a premium warehouse control center, not a generic admin template. Data readability always comes before visual effects. Every button must work, navigate, open a modal, or be visibly disabled with a reason. Use realistic copy, never lorem ipsum. Where this document and the tech spec disagree about visual or interaction behavior, this document wins.

---

## 1. DESIGN VISION

**Feeling:** a calm, dark, industrial control room. Premium, minimal, data-rich, with restrained futuristic touches.

**Keywords:** industrial, precise, calm, trustworthy, cinematic only where it earns its place.

**Avoid:** Bootstrap look, giant rounded cards everywhere, neon floods, random animation, clutter, tiny unreadable text, 3D on every element.

### Design principles
1. **Data first.** The 3D scene and effects support the data and never replace it. Every status shown in 3D also exists as text.
2. **Color carries meaning.** Cyan is the brand accent and appears sparingly. Green, yellow, orange, and red are reserved for stock status only.
3. **Speed for operators.** Receiving and dispatching are frequent tasks. They must be doable quickly with the keyboard and with minimal clicks.
4. **Prevent mistakes.** Show consequences before confirming (remaining stock, resulting status). Confirm only risky actions.
5. **Calm motion.** Animation explains change; it never decorates or distracts.
6. **Everything has a state.** Loading, empty, error, success, and disabled states are designed for every screen.

---

## 2. DESIGN TOKENS

Define all tokens as CSS variables in `static/css/tokens.css`. No hardcoded colors elsewhere.

```css
:root {
  /* Surfaces */
  --bg: #05070A;
  --bg-elevated: #0A0E14;
  --panel: rgba(255,255,255,0.04);
  --panel-hover: rgba(255,255,255,0.07);
  --panel-solid: #0D1219;
  --border: rgba(255,255,255,0.10);
  --border-strong: rgba(255,255,255,0.18);

  /* Text */
  --text: #F8FAFC;
  --text-muted: #94A3B8;
  --text-faint: #64748B;

  /* Brand */
  --accent: #22D3EE;
  --accent-soft: rgba(34,211,238,0.12);
  --accent-glow: 0 0 24px rgba(34,211,238,0.25);

  /* Stock status (semantic only) */
  --st-normal: #22C55E;
  --st-low: #EAB308;
  --st-warning: #F97316;
  --st-critical: #EF4444;
  --st-out: #B91C1C;
  --st-inactive: #64748B;

  /* Feedback */
  --info: #38BDF8;
  --success: #22C55E;
  --danger: #EF4444;

  /* Shape and depth */
  --radius-sm: 8px;
  --radius: 12px;
  --radius-lg: 16px;
  --shadow: 0 8px 30px rgba(0,0,0,0.35);
  --blur: blur(14px);

  /* Spacing (4px base) */
  --s-1: 4px; --s-2: 8px; --s-3: 12px; --s-4: 16px;
  --s-5: 20px; --s-6: 24px; --s-8: 32px; --s-10: 40px; --s-12: 48px;

  /* Motion */
  --t-fast: 200ms; --t-med: 450ms; --t-slow: 900ms;
  --ease: cubic-bezier(0.22, 1, 0.36, 1);
}
```

Each status color also has a **soft background** (about 12% opacity) and a **border** (about 35% opacity) for badges.

**Contrast:** body text must meet WCAG AA (4.5:1) on all panels. `--text-faint` is for non-essential hints only, never for critical information.

**Light theme:** not required. Dark is the only theme. Structure tokens so a light theme could be added later.

---

## 3. TYPOGRAPHY

| Use | Font | Weight | Size |
|---|---|---|---|
| Body and tables | Inter | 400/500 | 14px (tables 13-14px) |
| Labels, small meta | Inter | 500 | 12px, never below 12px |
| Section headings | Space Grotesk | 600 | 18-22px |
| Page titles | Space Grotesk | 600 | 28-32px |
| Big KPI numbers | Space Grotesk | 700 | 32-44px, `font-variant-numeric: tabular-nums` |
| Code, SKUs, IDs | `ui-monospace`, JetBrains Mono fallback | 500 | 13px |

Rules:
- Load fonts from Google Fonts with `display=swap`, with system fallbacks (`system-ui, -apple-system, Segoe UI, sans-serif`).
- **Always use tabular numbers** for quantities so columns align and counters do not jitter.
- Uppercase only for system labels (small headings, status badges, nav section labels) with letter-spacing 0.06em. Never uppercase paragraphs.
- Line height 1.5 for body, 1.2 for headings. Max line length 70 characters for policy text.

---

## 4. LAYOUT SYSTEM

### App shell (all authenticated pages)

```text
┌──────────────────────────────────────────────────────────────────────┐
│ TOP BAR  [logo] [breadcrumbs]   [warehouse ▾] ●LIVE  🔔  ⌘K  [user ▾] │
├──────────┬───────────────────────────────────────────────────────────┤
│ SIDEBAR  │  PAGE HEADER (title, subtitle, primary action)            │
│          │  ───────────────────────────────────────────────────────  │
│ Dashboard│                                                           │
│ Warehouse│  PAGE CONTENT                                             │
│ Inventory│                                                           │
│ Stock IN │                                                           │
│ Stock OUT│                                                           │
│ Transfers│                                                           │
│ Alerts ③ │                                                           │
│ Analytics│                                                           │
│ Products │                                                           │
│ Suppliers│                                                           │
│ Activity │                                                           │
│ Users    │                                                           │
│ Settings │                                                           │
└──────────┴───────────────────────────────────────────────────────────┘
```

- **Sidebar:** 240px, collapsible to a 72px icon rail. Groups: *Operations* (Dashboard, Warehouse, Inventory, Stock IN, Stock OUT, Transfers), *Monitoring* (Alerts with a live count badge, Analytics, Activity), *Manage* (Products, Suppliers, Users, Settings). Items the role cannot use are hidden. Active item has an accent left bar and soft background.
- **Top bar (64px):** logo, breadcrumbs, warehouse selector (filters all data), live indicator, notification bell (shows alert count, opens a dropdown of latest alerts), command palette trigger (`Ctrl K`), user menu (profile, change password, Policy, sign out).
- **Content:** max width 1440px, centered, 24-32px padding. 12-column grid with 16-24px gaps.
- **Footer (slim):** version, "Security and Policy" link, last sync time.

### Breakpoints
| Name | Width | Behavior |
|---|---|---|
| Desktop XL | 1440+ | Full layout |
| Desktop | 1024-1439 | Full layout, narrower content |
| Tablet | 768-1023 | Sidebar becomes an icon rail, dashboard stacks to 2 columns |
| Mobile | below 768 | Sidebar becomes a bottom tab bar (Home, Inventory, Scan/Move, Alerts, More), tables become cards, 3D becomes the 2D map |

---

## 5. COMPONENT LIBRARY

Build as small reusable vanilla JS modules in `static/js/components/` with matching CSS. Each component documents its states.

### 5.1 Buttons
- **Primary** (accent fill, dark text), **Secondary** (glass with border), **Ghost** (text only), **Danger** (red outline, fills on hover).
- Sizes: sm 32px, md 40px, lg 48px.
- States: default, hover (lift 1px, soft glow on primary), active (press), focus-visible (2px accent ring with offset), loading (spinner replaces icon, width fixed, disabled), disabled (40% opacity with a tooltip explaining why).
- Icon buttons have `aria-label`. Minimum touch target 44px on mobile.

### 5.2 Status badge
Pill with a colored dot, label, soft background, and border.
`NORMAL`, `LOW`, `WARNING`, `CRITICAL`, `OUT OF STOCK`, `INACTIVE`. Never rely on color alone (the dot shape and the label text are always present). Critical and out-of-stock dots pulse subtly (opacity 0.6 to 1, 2s, disabled under reduced motion).

### 5.3 KPI card
Label (12px uppercase), big animated number, small delta or context line (for example "+12% vs yesterday"), optional mini sparkline (Chart.js, 60x24, no axes). Click navigates to the relevant page. Critical-alert KPI uses a subtle red border tint when above zero.

### 5.4 Glass panel
`background: var(--panel); border: 1px solid var(--border); backdrop-filter: var(--blur); border-radius: var(--radius-lg)`. Header row with title, optional actions, and a thin divider. Do not nest more than two panel levels.

### 5.5 Data table
- Sticky header, row hover highlight, zebra off (use borders), 48px rows (40px compact toggle).
- Sortable columns (arrow icon, `aria-sort`), column visibility menu, density toggle.
- Toolbar: debounced search (300ms), filter chips, bulk-selection bar (appears when rows are selected), export button.
- Pagination footer: page size (10/20/50/100), page numbers, total count.
- Row action menu (kebab). Row click opens the product drawer.
- Quantity cells right-aligned, tabular numbers. Status cell uses the status badge.
- Skeleton rows while loading. Friendly empty state with a "Clear filters" button when filters yield nothing.
- On mobile, each row turns into a stacked card (name, SKU, stock, status badge, primary action).

### 5.6 Form controls
- Labels above fields, helper text below, error text in red with an icon, `aria-describedby` linking error text.
- Inputs 40px high, glass background, accent focus ring.
- Searchable select (combobox) for products with SKU and current stock shown in each option.
- Cascading selects Warehouse, Zone, Rack. Racks show free capacity in each option, and disabled racks are marked.
- Number input with +/- steppers and quick-add chips (+10, +50, +100).
- Date picker native `<input type="date">` styled to match.
- Validate on blur and on submit. Focus moves to the first invalid field. Never clear user input on error.

### 5.6b Quantity counter
Animated count (`1,240 → 1,340`) using `requestAnimationFrame`, 600ms ease-out. Under reduced motion it switches instantly.

### 5.7 Modal and drawer
- Modal: centered, max 520px, scrim 60% black with blur, focus trap, Esc closes (unless destructive confirm in progress), returns focus to the trigger.
- Drawer: right side, 480px (full width on mobile), used for product details and rack details.
- Enter: fade and slide 250ms. `role="dialog"`, `aria-modal="true"`, labelled by its title.

### 5.8 Toast
Bottom-right stack (top on mobile). Types: success, info, warning, error. Auto-dismiss 5s (errors stay until closed), pause on hover, `role="status"` (errors use `role="alert"`). Max 3 visible.

### 5.9 Alert card
Left severity bar, severity badge, product name and SKU, location chip (rack code), stock vs threshold with a mini progress bar, "Estimated depletion: 1.2 days" (or "N/A"), recommended action text, status chip (Active, Acknowledged, Resolved), timestamp ("12 min ago", full time on hover), and buttons: **View rack**, **Restock**, **Acknowledge**, **Resolve** (role-aware; hidden if not permitted).

### 5.10 Stock progress bar
A horizontal bar with the threshold marker. Fill color follows the status. Shows `82 / threshold 100` text beside it.

### 5.11 Empty, error, and skeleton states
- **Empty:** icon, title, one sentence, one action. Example: *"No active alerts. Your warehouse is currently healthy. All inventory levels are above configured thresholds."*
- **Error:** *"Unable to load inventory. The warehouse data could not be synchronized."* with a **Retry** button. Never show raw errors or stack traces.
- **Skeleton:** shimmer blocks matching the real layout (1.4s loop, static under reduced motion).

### 5.12 Command palette
`Ctrl/Cmd + K` opens a centered glass modal with a search input. Groups: *Navigate*, *Actions*, *Products*. Arrow keys navigate, Enter runs, Esc closes. Actions: Add stock, Dispatch stock, Transfer stock, View alerts, Open warehouse, View analytics, Add product (admin only). Product search hits `/api/products?search=` with debounce. Only show commands the user's role allows. Show shortcut hints on the right.

### 5.13 Global search
Search field in the top bar (`/` focuses it). Results grouped: Products, Racks, Suppliers, Transactions. Each result shows an icon, primary text, secondary text, and goes to the right page or opens the relevant drawer.

### 5.14 Live indicator
`● LIVE  synced 2s ago`. Green dot when the SSE connection is open, amber `○ RECONNECTING` with polling fallback, red `○ OFFLINE` after repeated failures. The "synced" time updates every second.

---

## 6. PAGE SPECIFICATIONS

For each page: purpose, layout, key interactions, states.

### 6.1 Landing and intro (`/index.html`)
- Shown to unauthenticated users. Hero: **INVORA**, subtitle **Live inventory intelligence**, supporting line **Monitor. Move. Predict. Control.**, primary CTA **Enter command center** (goes to login or dashboard), secondary CTA **View warehouse** (goes to login, then the warehouse page).
- Background: near-black with a faint animated grid and a slowly rotating low-poly warehouse silhouette (lightweight Three.js or CSS). Should stay under 1.2s to first interactive.
- **Cinematic intro** (grid fades in, warehouse fades in, HUD text appears, counters tick) plays **once per session** (`sessionStorage` flag, which holds only a non-sensitive UI flag). Provide a visible **Skip** button. Skipped automatically under reduced motion.
- Loading text sequence on first app load: `INITIALIZING INVORA` then *Loading warehouse… Loading inventory… Connecting to live systems…* (show for the real loading duration, not a fake delay).

### 6.2 Login (`/login.html`)
- Centered glass card on the dark grid background. Fields: username or email, password (show/hide toggle). Button **Sign in**.
- Errors inline: *"Invalid username or password."* Lockout message: *"Too many attempts. Try again in 15 minutes."* Session-expired notice if redirected.
- Caps-lock warning. Enter submits. Autofocus on first field. `autocomplete` attributes set properly.
- First-login flows: forced password change modal, then policy acceptance modal.

### 6.3 Dashboard (`/dashboard.html`)

```text
┌─────────────────────────────────────────────────────────────────────┐
│ WAREHOUSE COMMAND CENTER                      Warehouse A ▾  Today ▾ │
│ Live inventory intelligence                                          │
│ [TOTAL STOCK] [STOCK IN TODAY] [STOCK OUT TODAY] [CRITICAL ALERTS]   │
├───────────────────────────────────────────────┬─────────────────────┤
│                                               │ LIVE ACTIVITY       │
│          3D WAREHOUSE (digital twin)          │ ● 21:42 Stock IN    │
│          HUD: utilization, active racks,      │   +250 Logitech M185│
│          critical, today's movement           │ ● 21:38 Stock OUT   │
│          [Reset][Top][Front][Focus alerts]    │   -80 HP Keyboard   │
│                                               │ ● 21:31 Alert       │
│                                               │ ● 21:25 Transfer    │
├───────────────────────────────────────────────┴─────────────────────┤
│ INVENTORY FLOW (IN / OUT / NET, 7d / 30d / 90d toggle)              │
├──────────────────────────────┬──────────────────────────────────────┤
│ TOP MOVING PRODUCTS          │ STOCK HEALTH (donut + legend counts)  │
└──────────────────────────────┴──────────────────────────────────────┘
```

- KPI numbers count up once on load (600ms), then update with a small flash when new data arrives.
- Live Activity: newest at top, slide-in animation (250ms), max 20 items, each item clickable (opens the related product or alert). Types have distinct icons and colors (IN green, OUT blue, alert orange/red, transfer cyan).
- The 3D panel loads lazily with a skeleton and "Loading warehouse…" text. If WebGL fails, show the 2D map instead (section 8.6).
- Chart tooltips show exact values. Legend items toggle series.
- Warehouse selector filters every widget.

### 6.4 Warehouse (`/warehouse.html`)
- Full-height 3D scene with a left or right **rack list panel** (searchable, sortable by status) so every rack is available as text.
- Top toolbar: view buttons (Reset, Top, Front, Focus alerts), mode switch **Stock level | Movement | Turnover | Alerts** (heatmap, build after core), legend.
- Click a rack (in the scene or list): camera glides to it (900ms), the **Rack detail drawer** opens:

```text
Rack A-04                       [HEALTHY / WARNING / ...]
Zone A · Warehouse A
Utilization   1,284 / 1,500   85.6%  ▓▓▓▓▓▓▓▓░░
Products in this rack (table: product, qty, threshold, status)
Recent movements (last 10)
[ Stock IN here ]  [ Dispatch from here ]  [ Transfer from here ]
```

- Admin-only: **Edit rack** (capacity, active). Inactive racks render gray.

### 6.5 Inventory (`/inventory.html`)
- Header with page title and **Export CSV** and **Add stock** buttons.
- Filter bar: search, category, warehouse, status (multi-select chips), "Show critical only" quick toggle.
- Data table (section 5.5). Columns: SKU, Product, Category, Location, Current stock, Threshold, Status, Last movement, Actions.
- Row actions: View details, Stock IN, Dispatch, Transfer, View in warehouse. Role-aware.
- Bulk actions (select rows): Export selected, and for managers Set threshold (modal). Show selected count.
- Product drawer: current stock total and per rack, threshold (editable by allowed roles), supplier, stockout estimate card, movement chart, transaction history table.

### 6.6 Stock IN, "Receiving Dock" (`/stock-in.html`)
Two-column layout: form on the left, live summary panel on the right.

Form fields: Product (combobox), SKU (auto, read-only), Supplier (defaults to the product's supplier), Quantity, Batch number, Expiry date (optional), Warehouse, Zone, Rack, Reference/PO number, Notes.

Right summary panel updates as the user types: product name, current total stock, quantity to add, **resulting total**, selected rack free capacity, and the resulting status badge.

Flow:
1. User fills the form. Button **Confirm stock in** stays disabled until valid.
2. On submit: button shows loading, request sent with a `requestId`.
3. On success: green confirmation panel with the animated change (`1,240 → 1,340`), transaction reference number, buttons **Receive another** and **View product**. A toast appears and the Live Activity and rack color update.
4. On failure: inline error with *"Transaction failed. No stock was added. Your inventory remains unchanged."* and **Try again**.

Rack over capacity shows an inline error before submit: *"Rack A-04 has room for 216 more units."*

### 6.7 Stock OUT, "Dispatch Stock" (`/stock-out.html`)
Same two-column structure. Fields: Product, SKU, Rack (shows available quantity per rack), Quantity, Destination, Order/Reference ID, Reason (select: Sale, Internal use, Damaged, Expired, Return to supplier, Other).

The summary panel calls the preview endpoint (debounced) and shows:

```text
CURRENT STOCK     420
REQUESTED OUT      80
REMAINING         340
THRESHOLD         100
STATUS            NORMAL
```

- Quantity above available shows an inline error and blocks submit: *"Only 60 units available in Rack A-04."*
- If the result would be WARNING, CRITICAL, or OUT OF STOCK, clicking confirm opens a warning modal:

```text
⚠ STOCK WILL FALL BELOW THRESHOLD
Current: 140     After dispatch: 60
Threshold: 100   Resulting status: WARNING
[ Cancel ]  [ Confirm dispatch ]
```

(Use the status the backend returns. 60 against threshold 100 is WARNING; CRITICAL starts at 50 or below with default settings.)

- Success shows the animated decrease, reference number, and any new or updated alert ("This created a WARNING alert").

### 6.8 Transfers (`/transfers.html`)
- Source card (warehouse, zone, rack, product, available quantity) → animated arrow/path → Destination card (warehouse, zone, rack, free capacity). Quantity input with a "max" button.
- Validation: source and destination must differ, quantity within available, destination capacity respected.
- Show before/after for both racks. On confirm, animate the path (600ms) and update both racks. Failure message: *"Transfer failed. Neither rack was changed."*
- Below: recent transfers table.

### 6.9 Alerts (`/alerts.html`)
- Summary strip: counts by severity (clickable filters).
- Filters: status tabs (Active, Acknowledged, Resolved, All), severity, search.
- List of alert cards sorted by severity then recency. Resolved alerts are visually muted.
- Acknowledge and Resolve open a small confirm popover with an optional note. After action the card animates to its new state and a toast appears.
- **View rack** jumps to the warehouse page with that rack focused. **Restock** opens Stock IN prefilled with the product and the reorder quantity.
- Empty state as in section 5.11.

### 6.10 Analytics (`/analytics.html`)
Tabs or stacked sections: **Inventory flow** (area/line: IN, OUT, net, with range toggle), **Stock health** (donut), **Top moving products** (horizontal bars), **Dead stock** (table with days since last movement), **Turnover** (line), **Warehouse utilization** (radial gauge plus per-warehouse bars, for example `Capacity 25,000 · Occupied 18,492 · 74%`), **Category distribution**, **Stockout estimates** (table: product, stock, daily outflow, estimated days, risk).

**What-if simulator** (build after core): left side sliders and inputs (Demand change -50% to +100%, Supplier delay 0-30 days, Expected restock quantity, Restock lead time), product selector. Right side shows a projected stock line chart over 90 days with the threshold line, plus result cards: *Stockout date/day*, *First threshold breach*, *Risk level*. A **Reset** button restores defaults. Label results *"Simulation based on historical average outflow"*. Never call it AI.

Every chart has a text alternative: a "View as table" toggle.

### 6.11 Products (`/products.html`)
- Table with search and category filter. **Add product** (admin) opens a drawer form: name, SKU, barcode, category, supplier, unit, unit price, threshold, reorder quantity, description, image URL.
- Inline validation, duplicate SKU error: *"This SKU already exists."*
- Edit and Deactivate (confirm modal explaining history is kept). Inactive products appear muted with an Inactive badge and are hidden by default behind a filter.

### 6.12 Suppliers (`/suppliers.html`)
Card grid or table: supplier, contact, products count, average lead time, total orders, on-time rate (progress bar). Click opens a detail drawer with products supplied and a performance chart. Admin can add and edit.

### 6.13 Activity (`/activity.html`)
Full timeline of movements with filters (type, user, date range, product). Grouped by day. Each entry shows type icon, quantity, product, location, user, reference number, and time. Pagination or "Load more".

### 6.14 Users (`/users.html`, ADMIN)
Table: name, username, email, role badge, status, last login, policy accepted. Actions: add user, edit role (confirm modal explaining sessions will end), deactivate, reset password (shows the temporary password once, with a copy button). Prevent removing the last admin (button disabled with tooltip).

### 6.15 Settings (`/settings.html`, ADMIN)
Sections: **Threshold rules** (low multiplier 1.5, critical multiplier 0.5, alert-on-LOW toggle, preview table that updates live showing example stock levels and the resulting statuses), **Forecast** (days of history), **Session** (read-only info), **Security events** link, **Audit log** link.

### 6.16 Audit log and Security events (ADMIN)
Filterable table (user, action, entity, date range). Row click shows the full metadata JSON in a drawer. Security events view pre-filters for failed logins, lockouts, access denied, and role changes.

### 6.17 Policy page (`/policy.html`)
Two-column: sticky table of contents (left), readable policy text (right, max width 70ch, comfortable line height). Sections: Acceptable Use, Password Policy, Data Privacy and Retention, Reporting a Security Issue, version and date. Acceptance modal appears on first login and on policy version change.

### 6.18 Error pages
**404** ("This rack isn't on the map. The page you're looking for doesn't exist." with a Back to dashboard button) and **403** ("You don't have permission to view this page." with the user's role shown and a Back button). **Session expired** returns to login with a notice.

---

## 7. STATUS AND DATA VISUALIZATION RULES

| Status | Color token | Badge text | 3D rack | Notes |
|---|---|---|---|---|
| NORMAL | `--st-normal` | Normal | Green | |
| LOW | `--st-low` | Low | Yellow | Approaching threshold |
| WARNING | `--st-warning` | Warning | Orange | At or below threshold |
| CRITICAL | `--st-critical` | Critical | Red | At or below 50% of threshold |
| OUT_OF_STOCK | `--st-out` | Out of stock | Red with outline | Zero stock |
| INACTIVE | `--st-inactive` | Inactive | Gray | |

Correct example: stock 82 with threshold 100 is **WARNING**. Use these correct examples in mock copy and screenshots.

Charts (Chart.js):
- Dark theme: transparent backgrounds, `--border` gridlines at low opacity, `--text-muted` tick labels.
- Series colors: IN = green, OUT = blue (`--info`), Net = accent cyan. Status charts use the status tokens.
- Animated draw-in 600ms on first render only. No animation on live updates beyond a subtle transition.
- Responsive, with a "View as table" fallback. Tooltips readable with high contrast.
- Never use red/green alone to separate two series (add shape or label differences for color-blind users).

---

## 8. 3D WAREHOUSE UX (Three.js)

### 8.1 Scene
Procedural low-poly: floor with faint grid, ceiling beams, zone markers with floating labels, racks (instanced shelves), pallets and boxes (`InstancedMesh`), receiving dock, dispatch area, a simple conveyor, one or two animated forklifts or AGVs on loops. Soft hemisphere and directional light, subtle fog. No paid assets.

### 8.2 Rack appearance
Rack frame in neutral graphite; the **status shows on a glowing strip/base and shelf accent**, using emissive material at moderate intensity (never blinding). Critical racks pulse gently (emissive 0.6 to 1.0, 2.4s). Inactive racks gray and unlit.

### 8.3 Interactions
- **Hover:** outline highlight, rack raises about 4%, tooltip near the cursor (rack code, stock/capacity, utilization %, status text). Cursor becomes a pointer.
- **Click:** camera glides to the rack (900ms ease), other racks dim slightly, the rack drawer opens. Esc or the close button restores the previous view.
- **Controls:** orbit (drag), zoom (wheel/pinch), pan (right drag/two fingers) with damping, with polar angle limits so users cannot go under the floor.
- **View buttons:** Reset, Top, Front, Focus alerts (cycles through critical racks with Next/Previous).
- Selecting an alert elsewhere in the app focuses its rack.

### 8.4 HUD overlay
Small, top-left, semi-transparent glass: warehouse name, utilization %, active racks `18 / 24`, critical count, today's movement `+1,284`. Never covers more than about 15% of the scene.

### 8.5 Heatmap mode (after core)
Switch between **Stock level, Movement, Turnover, Alerts**. Rack/floor-zone colors map a low to critical intensity scale (blue-gray to cyan to amber to red) with a legend (`LOW / MEDIUM / HIGH / CRITICAL`). Animated transition 500ms between modes.

### 8.6 Accessible 2D alternative
- A **2D map** (CSS grid of rack tiles matching `pos_x/pos_y`) with the same colors, tooltips, click-to-drawer, and the same rack list. Used automatically when WebGL is unavailable, on mobile (below 768px), or when the user toggles **2D view** (toggle always available, preference remembered in `localStorage`).
- Rack list is always present as text, keyboard accessible, and screen-reader friendly. Tiles are focusable buttons with `aria-label` like "Rack A-04, warning, 1,284 of 1,500 units".

### 8.7 Performance
- Load Three.js with dynamic `import()` only on pages that show the scene, after the first paint.
- Cap pixel ratio at 2. Pause rendering when the tab is hidden or the scene is off-screen (IntersectionObserver). Render on demand when idle (controls damping finished, no animation).
- Dispose geometries, materials, and textures on page leave. Target 60fps on a mid-range laptop and 30fps on low-end devices (reduce shadows and AGV animation if frame time is high).

---

## 9. MOTION SYSTEM

| Category | Duration | Used for |
|---|---|---|
| Fast | 200-350ms | hover, focus, button, dropdown, toast |
| Medium | 400-700ms | panel reveal, counters, chart draw, drawer, modal |
| Cinematic | 800-1500ms | camera glide, first-visit intro only |

Rules:
- Use the shared easing `--ease`. Prefer `transform` and `opacity` (GPU friendly).
- Animate in sequence, never everything at once. Page load: shell appears, then KPIs (stagger 60ms), then panels, then the 3D scene.
- Page transitions: content fade and 8px rise, 250ms.
- Specific effects: stock IN confirmation gets a brief green pulse on the counter; stock OUT gets a brief blue pulse; new alert slides into the activity list and the bell gets a one-time shake (1 cycle); critical badge pulses slowly.
- **`prefers-reduced-motion: reduce`:** disable counters, pulses, camera glides (jump instead), intro, parallax and shimmer; keep instant state changes. Implement via one CSS media query and a JS helper `motionAllowed()`.

---

## 10. COPY AND MICROCOPY

Tone: clear, calm, professional. Short sentences. Say what happened and what to do next.

| Situation | Copy |
|---|---|
| Stock IN success | "Stock added. Laptop Stand: 1,240 → 1,340 units." |
| Stock OUT success | "Dispatch confirmed. Remaining: 340 units." |
| Over-withdrawal | "Only 60 units available in Rack A-04." |
| Failed stock operation | "Transaction failed. No stock was changed. Your inventory remains unchanged." |
| Transfer failed | "Transfer failed. Neither rack was changed." |
| Load failure | "Unable to load inventory. The warehouse data could not be synchronized." + Retry |
| No alerts | "No active alerts. Your warehouse is currently healthy." |
| No search results | "No products match your filters." + Clear filters |
| Permission denied | "You don't have permission to do this. Ask an administrator if you need access." |
| Session expiring | "Your session will expire in 2 minutes." + Stay signed in |
| Deactivate product | "Deactivating hides this product from stock operations. Its history is kept." |

Use plain words ("Dispatch", "Receive", "Rack"), consistent terminology across the app (Stock IN/OUT in navigation, "Receiving Dock" and "Dispatch Stock" as page titles). Dates show relative ("2 min ago") with full timestamp on hover. Numbers use thousands separators.

---

## 11. ACCESSIBILITY (target WCAG 2.1 AA)

- Semantic HTML: `header`, `nav`, `main`, `aside`, `section`, correct heading order (one `h1` per page), real `button` and `a` elements, `table` with `th scope`.
- Full keyboard operation: logical tab order, visible focus ring (2px accent, 2px offset, never removed), skip-to-content link, arrow-key navigation in menus, palette, and tabs, Esc closes overlays.
- Keyboard shortcuts: `Ctrl/Cmd K` palette, `/` search, `g` then `d/i/a/w` go to Dashboard/Inventory/Alerts/Warehouse (disabled while typing in inputs). Show a shortcuts help modal (`?`).
- ARIA: labels on icon buttons, `aria-live="polite"` region for toasts and live counters (throttled), `aria-sort` on tables, `aria-current="page"` in nav, `role="dialog"` and focus trap on modals, `aria-busy` while loading.
- Forms: associated labels, errors announced and linked with `aria-describedby`, required fields marked in text (not just color).
- Never rely on color alone: status always includes text and an icon/dot shape.
- Text can be zoomed to 200% without breaking layout. Touch targets at least 44px on mobile.
- Charts and 3D have text alternatives (tables, rack list).
- Test with keyboard only, a screen reader pass on key flows (login, stock IN, stock OUT, alerts), and an automated check (axe) for each page.

---

## 12. RESPONSIVE BEHAVIOR

- **Tablet:** sidebar collapsed to rail, dashboard grid 2 columns, 3D scene full width above the activity panel, tables keep horizontal scroll with sticky first column.
- **Mobile:** bottom tab bar, top bar reduced (logo, warehouse, bell, avatar), KPI cards in a horizontal swipe row or 2x2 grid, tables become cards, 2D map instead of 3D, forms single column with a sticky bottom action bar, drawers and modals become full-screen sheets.
- **Mobile operator flows (priority):** a prominent **Move stock** screen with large buttons for Stock IN, Stock OUT, and Transfer; a product search field with a **Scan barcode** button (uses the camera via `BarcodeDetector` when supported; otherwise shows a disabled button with the explanation "Barcode scanning isn't supported on this device"); large numeric keypad inputs (`inputmode="numeric"`).
- Test at 360, 390, 768, 1024, 1440, and 1920px widths.

---

## 13. LOADING, EMPTY, AND ERROR STANDARDS

Every data region implements all of these states:
1. **Loading:** skeleton matching the layout (never a blank area or a lone spinner).
2. **Loaded:** the real content.
3. **Empty:** helpful message and next action.
4. **Error:** friendly message, **Retry** button, request failure never breaks the rest of the page.
5. **Refreshing:** keep old data visible, show a small "Updating…" indicator.
6. **Forbidden/Unauthorized:** redirect or message as in section 10.

Network failures during stock operations show the specific "No stock was changed" message. After a timeout on a stock operation, the UI checks the transaction by `requestId` before telling the user it failed, to avoid duplicate or lost entries.

---

## 14. FRONTEND CODE ARCHITECTURE

```text
static/
 ├── index.html, login.html, dashboard.html, warehouse.html, inventory.html,
 │   stock-in.html, stock-out.html, transfers.html, alerts.html, analytics.html,
 │   products.html, suppliers.html, activity.html, users.html, settings.html,
 │   audit.html, policy.html, 403.html, 404.html
 ├── css/
 │   ├── tokens.css        design tokens
 │   ├── base.css          reset, typography, utilities
 │   ├── layout.css        shell, grid, breakpoints
 │   ├── components.css    buttons, badges, tables, forms, modals, toasts...
 │   └── pages/*.css       page-specific styles (keep small)
 ├── js/
 │   ├── core/
 │   │   ├── api.js        fetch wrapper (CSRF header, error mapping, 401/403 handling)
 │   │   ├── auth.js       current user, role checks, route guard
 │   │   ├── sse.js        EventSource with reconnect + polling fallback
 │   │   ├── store.js      tiny pub/sub state (warehouse filter, alerts count)
 │   │   ├── format.js     numbers, dates, relative time
 │   │   ├── motion.js     motionAllowed(), counter animation
 │   │   └── a11y.js       focus trap, live region, shortcuts
 │   ├── components/       shell.js, toast.js, modal.js, drawer.js, table.js,
 │   │                     badge.js, kpi.js, alertCard.js, commandPalette.js,
 │   │                     globalSearch.js, combobox.js, stockProgress.js
 │   ├── charts/           flowChart.js, healthChart.js, topMoving.js, whatIf.js
 │   ├── three/            scene.js, racks.js, controls.js, hud.js, heatmap.js,
 │   │                     fallback2d.js
 │   └── pages/            dashboard.js, inventory.js, stockIn.js, stockOut.js, ...
 ├── vendor/               three.module.js, OrbitControls.js, chart.umd.js, lucide icons
 └── assets/               logo.svg, favicon, empty-state illustrations (inline SVG)
```

Rules:
- ES modules (`<script type="module">`), one entry file per page, shared code imported from `core/` and `components/`.
- `api.js` is the only place that calls `fetch`. It attaches the CSRF token, parses the standard error body, and converts failures to friendly messages.
- Render dynamic text with `textContent` or a safe template helper that escapes HTML. Never `innerHTML` with API data.
- The frontend never contains business rules. Thresholds, status, and stock validation come from the backend (the preview endpoint powers the stock-out summary).
- Use event delegation for tables, keep DOM updates minimal, debounce search (300ms), and cancel in-flight requests with `AbortController` when a newer one starts.
- No sensitive data in `localStorage`. Allowed there: the 2D/3D preference, table density and column visibility, sidebar collapsed state. `sessionStorage`: the intro-seen flag.
- Keep each file focused and under about 300 lines where practical, with short comments on non-obvious logic.

---

## 15. PERFORMANCE TARGETS

- First contentful paint under 1.5s on a typical laptop, interactive under 2.5s (3D loads after).
- Dashboard shell and KPIs render before the 3D scene.
- Lazy-load Three.js and Chart.js only where used. Defer non-critical scripts.
- Images optimized (prefer inline SVG icons), fonts with `display=swap` and preconnect.
- Avoid layout shift: reserve space for charts, KPIs, and the 3D canvas (CLS under 0.1).
- Table pagination at the server. Never load all inventory at once.
- SSE updates are batched (at most one DOM update per animation frame per widget).

---

## 16. DELIVERABLE CHECKLIST (UI/UX acceptance)

**Visual**
- [ ] Dark glass design system implemented through tokens, with no hardcoded colors
- [ ] Cyan used sparingly, status colors used only for stock status
- [ ] Typography and tabular numbers consistent across all pages
- [ ] Does not look like a generic Bootstrap admin template

**Pages and flows**
- [ ] All pages in section 6 exist and match their descriptions
- [ ] Stock IN shows resulting total and rack capacity live, with the animated confirmation
- [ ] Stock OUT shows the preview summary and the below-threshold warning modal
- [ ] Transfers show before/after for both racks
- [ ] Alerts can be acknowledged, resolved, and jump to the rack or Stock IN
- [ ] Command palette and global search work and respect roles

**3D and data**
- [ ] 3D scene is lazy-loaded, racks colored by real status, hover/click/camera views work
- [ ] 2D fallback appears with WebGL off and on mobile, with the same data and interactions
- [ ] Every 3D status is also available as text
- [ ] Charts have "View as table" alternatives

**States and quality**
- [ ] Loading skeletons, empty states, and friendly errors on every data region
- [ ] No raw errors, stack traces, or lorem ipsum anywhere
- [ ] Reduced motion fully respected
- [ ] Keyboard-only use works end to end, focus rings visible, axe finds no critical issues
- [ ] Responsive at 360, 768, 1024, 1440px; mobile has operator flows
- [ ] Every button works, navigates, opens something, or is disabled with a tooltip reason
- [ ] No console errors on any page

---

## 17. BUILD ORDER (frontend)

1. Tokens, base CSS, app shell (sidebar, top bar, breadcrumbs), `api.js`, auth guard, toast, modal, drawer
2. Login and dashboard skeleton with KPI cards and live indicator
3. Reusable data table, badges, forms, combobox
4. Inventory page, product drawer, Products page
5. Stock IN, Stock OUT (with preview and warning modal), Transfers
6. Alerts page and alert cards, notification bell, SSE live updates, activity timeline
7. Dashboard charts and Analytics page
8. 3D warehouse, rack drawer, 2D fallback, Warehouse page
9. Command palette, global search, shortcuts, policy page, users/settings/audit pages
10. Polish: motion, intro (once per session, skippable), responsive and mobile flows, accessibility pass
11. Extras if time allows: heatmap modes, what-if simulator

Do not spend time on the intro animation or heatmap while core flows are missing.
