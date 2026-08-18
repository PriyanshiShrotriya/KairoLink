# KairoLink — Design System

Visual identity for the platform. The direction: **"dusk commute"** — the calm, in-motion feeling of a shared ride home as the city lights come on. Trustworthy and grounded (deep indigo, like the road at dusk), with warm amber as the human, energetic accent (headlights/taillights, the moment a ride gets confirmed).

This avoids generic "SaaS blue" and cookie-cutter pastel palettes — it's built around the actual subject: roads, motion, shared trips, dusk-to-night commuting.

---

## 1. Color & Theme

### 1.1 Core Palette

| Token | Hex | Usage |
|---|---|---|
| `--kl-indigo` (Primary) | `#1B2A4A` | Navbar, headers, primary buttons, driver-side accents, footer |
| `--kl-indigo-light` | `#2E4272` | Hover states, secondary surfaces on dark backgrounds |
| `--kl-amber` (Accent) | `#F2A93B` | Primary CTAs ("Book Now", "Publish Ride"), highlights, confirmed states |
| `--kl-amber-dark` | `#D48F22` | Hover/active state for amber elements |
| `--kl-teal` (Trust/Eco) | `#2E8B74` | Success states, ratings, eco/sustainability messaging, "seats available" |
| `--kl-coral` (Alert) | `#E15554` | Errors, cancellations, destructive actions |
| `--kl-cloud` (Background) | `#F7F8FA` | Page background |
| `--kl-surface` | `#FFFFFF` | Cards, forms, modals |
| `--kl-charcoal` (Text) | `#1F2229` | Primary text |
| `--kl-slate` (Muted text) | `#5C6270` | Secondary text, labels, placeholders |
| `--kl-border` | `#E3E6EB` | Dividers, input borders, card outlines |

### 1.2 Semantic / Status Colors

Maps directly to `RideStatus` and `BookingStatus` enums — keep this consistent everywhere a status badge appears.

| Status | Color | Token |
|---|---|---|
| Active / Confirmed | Teal | `--kl-teal` |
| Pending | Amber | `--kl-amber` |
| Ongoing | Indigo (light) | `--kl-indigo-light` |
| Completed | Slate (muted, done) | `--kl-slate` |
| Cancelled / Rejected | Coral | `--kl-coral` |

### 1.3 CSS Variables (drop into `style.css`)

```css
:root {
  /* Core */
  --kl-indigo: #1B2A4A;
  --kl-indigo-light: #2E4272;
  --kl-amber: #F2A93B;
  --kl-amber-dark: #D48F22;
  --kl-teal: #2E8B74;
  --kl-coral: #E15554;

  /* Neutrals */
  --kl-cloud: #F7F8FA;
  --kl-surface: #FFFFFF;
  --kl-charcoal: #1F2229;
  --kl-slate: #5C6270;
  --kl-border: #E3E6EB;

  /* Radius & Shadow (keep restrained — not a rounded/bubbly product) */
  --kl-radius: 8px;
  --kl-shadow-sm: 0 1px 2px rgba(27, 42, 74, 0.06);
  --kl-shadow-md: 0 4px 12px rgba(27, 42, 74, 0.10);
}
```

### 1.4 Usage Principles

- **Indigo dominates structure** (nav, headers, footer) — it's the "road," the constant.
- **Amber is earned, not everywhere.** Reserve it for the primary action on any given screen (one amber CTA per view, not five). It should feel like the moment a ride locks in.
- **Teal signals "safe to proceed"** — availability, good ratings, confirmed bookings. Never use it for anything neutral.
- **Coral is only for things that need attention or reversal** — never decorative.
- Keep large surfaces on `--kl-cloud` / `--kl-surface`, not indigo — indigo is for structure and accents, not backgrounds of content-heavy pages (JSP forms, tables) which need to stay legible.
- Maintain **WCAG AA contrast** minimum: white text on `--kl-indigo` passes; charcoal text on `--kl-cloud`/`--kl-surface` passes; avoid amber text on white (fails contrast) — amber is for backgrounds/borders/icons, not body text.

---

## 2. Fonts

| Role | Typeface | Why |
|---|---|---|
| **Display** (headings, hero text, ride prices) | **Sora** | Geometric but warm, has a bit of mobility/tech personality without being cold — good for a product about motion |
| **Body** (paragraphs, forms, nav, buttons) | **Inter** | Extremely legible at small sizes, neutral workhorse — right choice for form-heavy JSP pages (login, booking, search results) |
| **Mono / Utility** (ride codes, prices in tables, timestamps, seat counts) | **JetBrains Mono** | Gives numeric/data content a "ticket stub" precision feel — fitting for a booking confirmation or fare breakdown |

**Loading (Google Fonts, add to `header.jsp` or a shared layout include):**

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Sora:wght@400;600;700&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
```

```css
:root {
  --font-display: 'Sora', sans-serif;
  --font-body: 'Inter', sans-serif;
  --font-mono: 'JetBrains Mono', monospace;
}
```

**Fallback stack (if Google Fonts CDN is blocked/unavailable):**
```css
--font-display: 'Sora', 'Segoe UI', system-ui, sans-serif;
--font-body: 'Inter', 'Segoe UI', system-ui, sans-serif;
--font-mono: 'JetBrains Mono', 'Consolas', monospace;
```

---

## 3. Typography

### 3.1 Type Scale

| Element | Font | Size | Weight | Line Height | Usage |
|---|---|---|---|---|---|
| H1 | Sora | 2.5rem (40px) | 700 | 1.15 | Page hero titles ("Find your ride") |
| H2 | Sora | 2rem (32px) | 600 | 1.2 | Section headers ("Available Rides") |
| H3 | Sora | 1.5rem (24px) | 600 | 1.25 | Card titles, dashboard section headers |
| H4 | Sora | 1.25rem (20px) | 600 | 1.3 | Sub-section, modal titles |
| Body Large | Inter | 1.125rem (18px) | 400 | 1.6 | Intro/lead text |
| Body | Inter | 1rem (16px) | 400 | 1.6 | Default paragraph, form labels, table content |
| Body Small | Inter | 0.875rem (14px) | 400 | 1.5 | Helper text, timestamps, captions |
| Micro / Label | Inter | 0.75rem (12px) | 500 (uppercase, letter-spacing 0.04em) | 1.4 | Status badges, form field labels |
| Numeric / Mono | JetBrains Mono | 1rem–1.25rem | 500 | 1.4 | Ride price, seat count, booking ID |
| Button Text | Inter | 0.9375rem (15px) | 600 | 1 | All buttons/CTAs |

### 3.2 CSS Implementation

```css
h1 { font-family: var(--font-display); font-size: 2.5rem; font-weight: 700; line-height: 1.15; color: var(--kl-charcoal); }
h2 { font-family: var(--font-display); font-size: 2rem; font-weight: 600; line-height: 1.2; color: var(--kl-charcoal); }
h3 { font-family: var(--font-display); font-size: 1.5rem; font-weight: 600; line-height: 1.25; color: var(--kl-charcoal); }
h4 { font-family: var(--font-display); font-size: 1.25rem; font-weight: 600; line-height: 1.3; color: var(--kl-charcoal); }

body, p, label, td, input, textarea, select {
  font-family: var(--font-body);
  font-size: 1rem;
  line-height: 1.6;
  color: var(--kl-charcoal);
}

.text-small { font-size: 0.875rem; color: var(--kl-slate); }

.label-micro {
  font-family: var(--font-body);
  font-size: 0.75rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--kl-slate);
}

.price, .ride-code, .seat-count, .timestamp {
  font-family: var(--font-mono);
  font-weight: 500;
}

.btn, button {
  font-family: var(--font-body);
  font-size: 0.9375rem;
  font-weight: 600;
  letter-spacing: 0.01em;
}
```

### 3.3 Typography Principles

- **Display font (Sora) only for headings and hero moments** — never body copy, never form fields. It carries the personality; overusing it dilutes that.
- **Numbers get the mono treatment.** A ride price (`₹240`), a booking ID (`#KL-8841`), or a seat count should visually read as "data," distinct from prose — reinforces trust and precision, especially important for a product handling money and bookings.
- **Status badges use the micro/label style** (uppercase, letter-spaced, small) paired with the semantic status colors from Section 1.2 — e.g. a "CONFIRMED" badge in teal, "PENDING" in amber.
- Keep line length readable on JSP content pages — max `~75ch` for body paragraphs (ride descriptions, terms, admin notes).
- Don't mix more than 2 weights of Sora on one screen (600 for most headings, 700 reserved for the single largest hero heading only).

---

## 4. Component Tone Reference

Quick gut-check for applying the system consistently:

| Component | Background | Text | Accent |
|---|---|---|---|
| Navbar | `--kl-indigo` | White | Amber (active link underline) |
| Primary Button | `--kl-amber` | `--kl-indigo` (dark text on amber, not white) | — |
| Secondary Button | Transparent, `--kl-indigo` border | `--kl-indigo` | — |
| Ride Card | `--kl-surface` | `--kl-charcoal` | Teal seat-availability tag |
| Status Badge (Confirmed) | Teal tint (`#2E8B74` at 12% opacity) | `--kl-teal` | — |
| Status Badge (Pending) | Amber tint | `--kl-amber-dark` | — |
| Status Badge (Cancelled) | Coral tint | `--kl-coral` | — |
| Footer | `--kl-indigo` | `--kl-slate`-tinted white (`#C7CCDA`) | — |

---

## 5. Signature Element

The one recurring visual motif: a **thin amber "route line"** — a 2px horizontal accent line, subtly animated (a soft left-to-right draw-in on load) — used under section headers and as a divider on ride cards. It's a small, restrained nod to "a route being traced," tying back to the product's actual subject without turning into decoration. Use it sparingly: once per major section, never as a repeated ornamental pattern.