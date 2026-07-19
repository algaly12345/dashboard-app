# Estate Control — Real Estate Admin Dashboard

A Spring Boot back-office for the real-estate app described in the brief:
listings (`estates`), end users (`users`, with `agents` / `service_providers`
role profiles), `categories` and `zones`. Ships bilingual out of the box —
Arabic (RTL, default) and English (LTR) — switchable from the top bar.

## Stack
- Java 17, Spring Boot 3.3
- Spring Security (form login against the existing `admins` table)
- Spring Data JPA / MySQL (maps onto your **existing** schema — `ddl-auto=validate`, nothing is auto-created or altered)
- Thymeleaf + Bootstrap 5 (RTL build swapped in automatically for Arabic)

## Project layout
```
src/main/java/com/realestate/admin/
  entity/            JPA entities mapped 1:1 to the tables you supplied
  repository/        Spring Data repositories (search/filter/pagination queries)
  service/           AdminUserDetailsService (login) + UserDetails wrapper
  config/            SecurityConfig (form login) + LocaleConfig (AR/EN switch)
  controller/web/    Thymeleaf page controllers (dashboard, estates, users, catalog, login)
  controller/api/    REST API controllers (JSON, versioned under /api/v1) - see "REST API" below
  dto/api/           Response DTOs used only by the REST API layer
src/main/resources/
  templates/     Thymeleaf pages + fragments/ (head, sidebar, topbar)
  static/css/    custom.css — design tokens, layout, components
  i18n/          messages.properties (en) / messages_ar.properties (ar)
```

The split between `controller.web` and `controller.api` is deliberate: the
Thymeleaf pages and the JSON API are two independent front doors onto the
same repositories/entities. A future mobile app, a JS frontend, or
integrations can be built entirely against `/api/v1/**` without touching a
single HTML template, and vice versa.

## REST API
A first, read-only slice lives under `/api/v1`:
```
GET /api/v1/estates?q=&city=&category=&status=&page=0&size=20
GET /api/v1/estates/{id}
GET /api/v1/users?q=&userType=&page=0&size=20
GET /api/v1/users/{id}
```
Responses are DTO-shaped (see `dto/api/`), not raw entities - keeps the
payload small and decoupled from the database schema. These endpoints
currently sit behind the same session-based admin login as the dashboard
(`SecurityConfig`'s `anyRequest().authenticated()` covers `/api/**` too).
**Before opening this to a non-admin client** (a public mobile app, a
third-party integration), swap that for a stateless scheme - an API key
filter or JWT - so callers aren't forced through the HTML login form.
Write endpoints (POST/PUT/DELETE) aren't built yet; add them the same way,
one `@RestController` per resource under `controller/api/`.

## 1. Configure the database
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/YOUR_DB?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Riyadh
spring.datasource.username=YOUR_USER
spring.datasource.password=YOUR_PASSWORD
```
`ddl-auto=validate` is intentional: the app checks the mapped entities
against your existing tables at boot and refuses to start on a mismatch,
but it will never create/drop/alter anything for you.

## 2. Create your first admin
The `admins` table stores its own bcrypt hash — the same format Laravel
produces (`$2y$...`), which Spring's `BCryptPasswordEncoder` reads natively,
so nothing changes for accounts that already exist there. To add a new one:

```sql
-- password below is "ChangeMe123!" hashed with bcrypt (cost 10)
INSERT INTO admins (phone, name, email, password, created_at, updated_at)
VALUES ('+966500000000', 'Super Admin', 'admin@example.com',
        '$2y$10$3Q1s0m1F1eYFq0k7m3H8UuP0m2v6iF2m2gk9zjq2m1B3y8B0m1B3y', NOW(), NOW());
```
Generate a real hash instead of the placeholder above, e.g. with Node
(`bcrypt.hashSync('ChangeMe123!', 10)`) or an online bcrypt generator you trust.

## 3. Run
```bash
mvn spring-boot:run
```
Then open `http://localhost:8080` → redirects to `/login`.
Sign in with the admin's **email or phone** + password.

## Pages included
- **Dashboard** — KPI cards (total/active listings, for-sale/for-rent split, new this week, users, categories, zones) and two bar-list breakdowns (by category, by city), plus the 6 latest listings.
- **Listings** (`/estates`) — search by title/city/district/advertiser, filter by city/category/status/purpose/estate type/virtual tour/price range, paginated table, view + edit buttons per row. A "3D tour" badge shows automatically whenever `ar_path` is non-empty.
- **Listing details** (`/estates/{id}`) — read-only, full-page view: hero image + key facts, description, room breakdown (parsed from the `property` JSON column), photo gallery, deed/license info, advertiser card, location with a Google Maps link.
- **Edit listing** (`/estates/{id}/edit`) — grouped form (basic info, pricing & size, location, media & virtual tour, advertiser contact). Only the fields shown are ever written back — the REGA/MOJ legal fields (deed numbers, license numbers, boundary limits, etc.) are read-only here and untouched on save, on purpose.
- **Users** (`/users`) — search by name/phone, filter by user type, paginated table, view button per row.
- **User details** (`/users/{id}`) — account info (name/phone/email/status) plus, conditionally, an **agent/marketer profile** (from `agents`) or a **service-provider profile** (from `service_providers`) — whichever the account actually has — both editable inline in the same form. Shows a live count of listings added (agents) or service offers added (providers).
- **Services** (`/offers`) — card-grid view of every service offer (price or discount type), with advanced filters (search, status, offer type, **service type** — real names from `service_types`, **zone** — via `offer_zone`, **estate category** — via `category_offer`), quick approve/reject actions right on the card, and a full edit screen (`/offers/{id}/edit`).
- **Listings map** (`/estates/map`, linked from the Listings filter bar) — Google Maps view of listings with saved coordinates, filterable by **zone / city / district (cascading) / property type / advertiser** entirely over AJAX (jQuery) so the page never fully reloads. Results are paginated (12 per page) and the list + map markers + pagination all update together on every filter change - keeps this light even with a large `estates` table, since nothing tries to plot every listing at once. Needs `map_api_key` set under **Settings**; without it, the page still shows the filterable, paginated list, just without the map itself.
- **Categories** (`/categories`) and **Zones** (`/zones`) — reference data views. Each zone card shows how many listings (`estates.zone_id`) and how many distinct service offers (`offer_zone`) are in that zone.
- **Reports** (`/reports`) — every user complaint filed against a listing (`reports`), shown as cards with the reason, details, a live preview of the reported listing (image/city, linking to `/estates/{id}`), the reporter's name (or "Anonymous" — `user_id` is `0`/unset for most existing rows), and a date/reason/listing-ID filter bar.
- **Notifications** (`/notifications`) — history of marketing push notifications, and a compose screen (`/notifications/compose`) to target by **zone**, **estate category**, and **audience** (marketers / service providers / everyone), with a live phone-mockup preview.
- **Settings** (`/settings`) — a curated, sectioned editor over the legacy `business_settings` key/value store: company info, brand colors, app store links, legacy push/FCM fields, registration & verification toggles, map/regional config, and the legal content pages (About/Terms/Privacy, raw HTML). `type` isn't unique in the existing data (many duplicate rows accumulated over time) — this page always reads/writes the **most recent** row per key (`SettingsService`), updating it in place instead of adding another duplicate.

## Push notifications (Firebase Cloud Messaging)
Sending is **topic-based**, not per-device-token — there's no device-token
table in this schema, so targeting works by having the Android/iOS app
subscribe each device to FCM topics on login, using this exact naming:
```
zone_{zoneId}          e.g. zone_5
category_{categoryId}  e.g. category_12
role_agent  |  role_provider
```
When you compose a notification with a zone/category/audience selected,
the admin builds an FCM **condition** combining whichever of those you
picked (e.g. `'zone_5' in topics && 'role_agent' in topics`) and sends to
that; leaving everything on "All" sends to a broadcast `all_users` topic.
**The mobile app has to actually call `subscribeToTopic(...)` for these to
reach anyone** - this project can't do that from the server side.

### One-time setup to enable real delivery
1. Firebase Console → your `abaad-8b6a0` project → ⚙️ **Project Settings** →
   **Service Accounts** → **Generate new private key**. This downloads a
   JSON file — treat it like a password, never commit it or paste it
   anywhere (including chat).
2. Put that file at `src/main/resources/firebase-service-account.json`
   (already covered by `.gitignore`).
3. In `application.yml`, set `app.firebase.enabled: true`.

Until all three steps are done, `/notifications/compose` still works and
still logs every attempt to `notification_apps` — it just reports
"not configured" instead of actually calling the FCM API, so the rest of
the app is unaffected either way.

Every notification is logged to `notification_apps` regardless of whether
delivery succeeded (`status` = 1 sent / 0 not delivered). The table has no
`category_id` column, so the category filter used is recorded in `type`
(e.g. `category:12`) purely for the admin's own history — it doesn't affect
anything downstream.
- **Property Reports** (`/property-reports`) — a printable, professional portfolio report with an **advanced filter bar (zone / category / marketer)** that recomputes every stat below it: gradient header with a "Print / Export PDF" button (`window.print()` + `@media print` rules that hide the sidebar/topbar), KPI summary (totals, portfolio sale value, average sale price), a 12-month new-listings trend chart, ranked breakdown tables by **city** and by **zone** (count / share / average price each), a category breakdown, and a **top marketers** leaderboard (who posted the listings, via `estates.user_id`). This is different from `/reports` — that page is user complaints, this one is portfolio analytics.
- **Provider Reports** (`/provider-reports`) — the same report style with its own **advanced filter bar (zone / service type / provider)**, built entirely from the service-offers data: total offers/providers, approval status breakdown, average discount/price, a 12-month new-offers trend, a top-providers leaderboard, and breakdowns by service type and by zone.

Both report filters work by fetching the matching `Estate`/`Offer` rows once (`EstateRepository.findForReport` / `OfferRepository.findForReport`) and computing every downstream stat (counts, averages, group-bys, monthly trend) in Java from that in-memory slice, rather than maintaining a separate hand-filtered SQL aggregate query per stat. This keeps the numbers always consistent with whatever filter combination is selected.

## Design system v2
The UI was rebuilt around a distinct violet/indigo + coral palette with
glass surfaces (blurred topbar), gradient KPI icons, and real motion:
- KPI numbers count up on load (`static/js/app.js`)
- Bar-chart fills animate in
- Buttons/icon-buttons have a ripple on click
- Cards lift and glow on hover, sidebar has an animated active-item indicator
No JS framework or build step involved — it's one small vanilla script
(`app.js`) loaded via the shared `<head>` fragment, so it applies to every
page automatically.

## Dashboard period filter
`/dashboard?period=day|week|month|year` (pills at the top of the page)
control the "New Listings" KPI window: day = since midnight today, week =
rolling 7 days, month = since the 1st of the current month, year = since
Jan 1st. All other KPIs (totals) are period-independent. The "now" anchor
for these windows is the most recent listing's `created_at`, not the real
server clock — see the comment in `DashboardController.dashboard()` for why
(historical/staging data would otherwise make every period bucket read 0).

## Offers / service-provider activity
`offers` has no `user_id` column - its creator is identified by
`phone_provider`, matched against `users.phone`. `OfferRepository` exposes
`countByPhoneProvider` / `countGroupedByPhoneProvider` for that, used on the
users list/details pages and the dashboard's "Service Offers" KPI.
`offer_zone` (offer_id, zone_id) still powers the per-zone service counts
on the Zones page; `category_offer` (offer_id, category_id) and `service_types`
power the estate-category and service-type filters on `/offers`.
(`OfferUser`/`OfferUserRepository` were an earlier, schema-guessing stand-in
before the real `offers` table was shared - they're harmless leftovers,
unused for counting now, safe to delete if unneeded.)

The dashboard has a dedicated **Services & providers** row: a stacked status
bar (pending/approved/rejected), a top-service-types breakdown, and a top-5
providers leaderboard (name resolved by matching `phone_provider` back to
`users.phone`; shows "Unregistered number" if no match).

## Bilingual / RTL
Language is a cookie-backed locale (`?lang=ar` / `?lang=en` on any page,
picked up by `LocaleChangeInterceptor`). The `<html>` tag's `dir` attribute
and the Bootstrap CSS build (`bootstrap.min.css` vs `bootstrap.rtl.min.css`)
follow the active locale automatically — every page, not just login.

## Extending
- Add more mapped columns to `Estate.java` the same way (e.g. for a listing
  detail page) — the physical table has ~70 columns; only the ones the
  dashboard reads today are mapped.
- **Mixed-case columns & naming strategy.** A few columns in `estates` are
  genuinely camelCase in the DB (`advertiserName`, `phoneNumber`, `isValid`,
  and others like `adLicenseUrl`, `titleDeedTypeName`, `numberOfRooms`...).
  Spring Boot's *default* physical naming strategy (`SpringPhysicalNamingStrategy`)
  force-rewrites every column name to snake_case — even ones already given
  explicitly via `@Column(name = "...")` — so `advertiserName` silently
  becomes `advertiser_name` and schema validation fails. This project
  disables that behavior in `application.yml`:
  ```yaml
  spring.jpa.hibernate.naming.physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
  ```
  With Hibernate's standard strategy, whatever you put in `@Column(name = "...")`
  is used verbatim — no quoting tricks needed, just spell it exactly as the
  DB column is spelled.
- `Agent` / `ServiceProvider` are already mapped (1:1 on `user_id`) if you
  want a "role profile" tab on the user detail page next.
- To add a new language, add `i18n/messages_xx.properties` and add a link in
  `fragments/topbar.html`.
