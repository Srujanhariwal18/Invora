# INVORA: Security and Policy Document

**System:** Invora, Warehouse Inventory Stock IN/OUT & Threshold Alert System
**Stack:** Java 21, Spring Boot 3.x, Spring Security, MySQL 8, Flyway, HTML/CSS/JS frontend
**Version:** 1.0
**Applies to:** developers, administrators, and all application users

> **Instruction to Antigravity:** Implement every control in this document in the application. Sections marked **[IMPLEMENT]** must exist in code or configuration. Sections marked **[POLICY]** must be shown to users in the in-app Policy page (section 20). Where this document and the main tech spec disagree, this document wins for anything security-related.

---

## 1. PURPOSE AND SCOPE

This document defines how Invora protects inventory data, user accounts, and system integrity, and the rules users must follow. It covers authentication, authorization, data protection, secure coding, auditing, operations, and acceptable use.

**Assets to protect**
1. **Inventory integrity:** stock counts must always be accurate and never negative.
2. **Account credentials:** passwords and sessions.
3. **Business data:** products, suppliers, prices, stock movements.
4. **Audit trail:** must be complete and tamper-resistant.
5. **Availability:** staff must be able to record stock movements during operations.

**Out of scope:** physical warehouse security, network hardware, and the hosting provider's infrastructure (the deployer is responsible for those).

---

## 2. SECURITY PRINCIPLES

1. **Least privilege:** each role gets only the access it needs.
2. **Defense in depth:** validate in the UI, the API, the service layer, and the database.
3. **Server is the authority:** the browser is never trusted. Hiding a button is not security.
4. **Secure by default:** new features start denied until a role is explicitly granted.
5. **Fail safely:** on error, no partial stock change occurs and no internal details are shown.
6. **Accountability:** every important action is linked to a user and a timestamp.
7. **No secrets in code:** credentials come from environment variables only.

---

## 3. AUTHENTICATION [IMPLEMENT]

- Login with username or email plus password via Spring Security form login.
- Passwords stored only as **BCrypt** hashes (strength 12). Never store, log, or return plain text or hashes in any API response.
- Generic login error: *"Invalid username or password."* Do not reveal which part was wrong.
- **Account lockout:** after 5 consecutive failed attempts, lock the account for 15 minutes. Track with `failed_attempts` and `locked_until` columns on `users`. Reset the counter on success. Each lockout writes an audit entry.
- Disabled users (`active = false`) cannot log in.
- All login successes, failures, and lockouts are audited (`LOGIN`, `LOGIN_FAILED`, `ACCOUNT_LOCKED`).
- Login endpoint rate limit: maximum 10 attempts per minute per IP (simple in-memory limiter such as a Bucket4j or custom filter).
- No default accounts in production. Demo users exist only under the `dev` Spring profile and have `must_change_password = true`; the app forces a password change at first login.

### Password policy [POLICY]
- Minimum **10 characters**, with at least one uppercase letter, one lowercase letter, one digit, and one symbol.
- Must not contain the username or email.
- Must not match the user''s last 3 passwords (store hashes in `password_history`).
- Block a small list of common passwords (for example `Password@123`, `Admin@12345`).
- Users must never share passwords. Admins never ask for a password.
- Password change requires the current password.
- Admin password reset sets a temporary password with `must_change_password = true`.

---

## 4. SESSION MANAGEMENT [IMPLEMENT]

- Server-side session with `JSESSIONID` cookie: `HttpOnly`, `Secure` (when served over HTTPS), `SameSite=Lax`.
- Idle timeout: **30 minutes**. Absolute timeout: **12 hours**.
- Session fixation protection: new session id on login (Spring default, keep enabled).
- Maximum **2 concurrent sessions** per user. The oldest is expired.
- Logout invalidates the session server-side and clears the cookie.
- Warn the user in the UI 2 minutes before idle expiry, with a "Stay signed in" button that pings `/api/auth/me`.

---

## 5. AUTHORIZATION AND ROLES [IMPLEMENT]

Role-based access control (RBAC) using the `UserRole` enum. Enforce on the server with `@PreAuthorize` and URL rules. The default rule is `denyAll`; routes are opened explicitly.

| Capability | ADMIN | WAREHOUSE_MANAGER | STAFF | VIEWER |
|---|:-:|:-:|:-:|:-:|
| View dashboard, inventory, racks, 3D view | Yes | Yes | Yes | Yes |
| View analytics and alerts | Yes | Yes | Limited (alert badge only) | Yes |
| Stock IN / Stock OUT / Transfer | Yes | Yes | Yes | No |
| Acknowledge / resolve alerts | Yes | Yes | No | No |
| Change product thresholds | Yes | Yes | No | No |
| Create / edit / deactivate products | Yes | No | No | No |
| Manage warehouses, zones, racks, suppliers | Yes | No | No | No |
| Manage users and roles | Yes | No | No | No |
| View audit log | Yes | No | No | No |
| Change system settings | Yes | No | No | No |
| Export inventory CSV | Yes | Yes | No | Yes |

Rules:
- Return **403 Forbidden** for authenticated-but-not-allowed and **401 Unauthorized** for unauthenticated requests. Both with a friendly JSON body.
- An admin cannot delete or demote themselves, and the system must always keep at least one active ADMIN.
- Role changes take effect immediately and invalidate that user''s active sessions.
- The frontend hides unauthorized controls using `/api/auth/me`, but never relies on this.
- Prevent **IDOR**: every endpoint that takes an id must still check the caller''s role. Never trust ids from the client for authorization decisions.

---

## 6. INPUT VALIDATION AND INJECTION PROTECTION [IMPLEMENT]

- Every request DTO uses Jakarta Bean Validation (`@NotNull`, `@Positive`, `@Size`, `@Pattern`, `@Email`).
- Stock quantities: integer, `> 0`, with a sensible upper bound (for example 1,000,000) to prevent absurd values and overflow.
- SKU pattern, name length, and text field length limits enforced in DTOs and mirrored in database column sizes.
- **SQL injection:** use Spring Data JPA and parameterized queries only. Never concatenate user input into SQL or JPQL. Sorting parameters are checked against a whitelist of allowed column names.
- **XSS:** the frontend never inserts API data with `innerHTML`. Use `textContent` or an escaping helper. Allow no user-supplied HTML. Product image URLs must be `https://` or relative and are validated.
- **CSV injection:** when exporting CSV, prefix any cell starting with `=`, `+`, `-`, or `@` with a single quote.
- File uploads (product images, if implemented): allow only PNG/JPEG/WebP, max 2 MB, check content type and magic bytes, store outside the web root or rename to random names, never execute.
- Reject unknown JSON fields or ignore them safely; never bind request bodies directly to entities (use DTOs, which prevents mass assignment).

---

## 7. API AND WEB SECURITY [IMPLEMENT]

- **CSRF protection enabled** for all state-changing requests (POST, PUT, PATCH, DELETE). Use `CookieCsrfTokenRepository.withHttpOnlyFalse()` so the frontend JS can send `X-XSRF-TOKEN`.
- **Security headers** on every response:
  - `Content-Security-Policy`: `default-src ''self''; script-src ''self'' https://cdn.jsdelivr.net; style-src ''self'' ''unsafe-inline'' https://fonts.googleapis.com; font-src ''self'' https://fonts.gstatic.com; img-src ''self'' data:; connect-src ''self''; frame-ancestors ''none''` (tighten further by vendoring libraries locally, which is preferred)
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Permissions-Policy: camera=(), microphone=(), geolocation=()`
  - `Strict-Transport-Security` when running behind HTTPS
- **HTTPS required in production.** Redirect HTTP to HTTPS.
- **CORS:** same-origin only by default. If a separate frontend origin is ever needed, allow only that explicit origin, never `*` with credentials.
- Do not expose Spring Actuator endpoints publicly. Expose only `/actuator/health`, and require ADMIN for any others.
- Swagger UI available only in the `dev` profile or for ADMIN.
- Error responses use the standard error body. Disable stack traces in responses (`server.error.include-stacktrace=never`, `include-message=never` for unexpected errors).
- Request size limits: max body 1 MB (except image uploads), max page size 100.
- Do not use predictable references for sensitive actions; transaction reference numbers are fine for display but never grant access.

---

## 8. INVENTORY INTEGRITY CONTROLS [IMPLEMENT]

These protect the most important asset, accurate stock.

1. All stock operations run in a single `@Transactional` service method. Either everything is saved or nothing is.
2. **Negative stock is impossible:** service validation, a pessimistic row lock, an optimistic `@Version` field, and a database `CHECK (quantity >= 0)`.
3. Transfers lock inventory rows in a consistent order (ascending rack id) to avoid deadlocks.
4. Stock transactions (`stock_transactions`) are **append-only**. There are no update or delete endpoints. Corrections are made with a new `ADJUST` transaction that requires a mandatory reason and an ADMIN or WAREHOUSE_MANAGER role.
5. Each transaction stores `balance_after`, the user, and the timestamp.
6. Idempotency: Stock IN/OUT/Transfer requests carry a client-generated `requestId`. A duplicate `requestId` (for example from a double-click or retry) returns the original result and does not apply twice. Store it with a unique index.
7. Product deactivation instead of deletion, so history stays valid. Database foreign keys prevent deleting products or racks that have stock or history.
8. Threshold changes are audited with old and new values.

---

## 9. AUDIT LOGGING [IMPLEMENT]

Record these actions in `audit_logs` (user, action, entity, entity id, description, JSON metadata, timestamp):

`LOGIN`, `LOGIN_FAILED`, `ACCOUNT_LOCKED`, `LOGOUT`, `PASSWORD_CHANGED`, `PASSWORD_RESET`, `USER_CREATED`, `USER_UPDATED`, `USER_DEACTIVATED`, `ROLE_CHANGED`, `PRODUCT_CREATED`, `PRODUCT_UPDATED`, `PRODUCT_DEACTIVATED`, `THRESHOLD_UPDATED`, `STOCK_IN`, `STOCK_OUT`, `STOCK_TRANSFER`, `STOCK_ADJUSTED`, `ALERT_ACKNOWLEDGED`, `ALERT_RESOLVED`, `WAREHOUSE_CHANGED`, `RACK_CHANGED`, `SETTINGS_CHANGED`, `ACCESS_DENIED`, `EXPORT_DATA`.

Rules:
- Audit rows for stock operations are written in the **same transaction** as the stock change.
- The audit table is **append-only**: the application database user has `INSERT` and `SELECT` but not `UPDATE` or `DELETE` on `audit_logs` (document the SQL grants in the README). No delete endpoint exists.
- Never write passwords, hashes, session ids, or tokens into audit metadata or application logs.
- Only ADMIN can view audit logs, with filters (user, action, date) and pagination.
- Retention: keep audit logs for at least **12 months**.

---

## 10. DATABASE SECURITY [IMPLEMENT]

- The application connects with a **dedicated MySQL user** with only the privileges it needs (`SELECT, INSERT, UPDATE, DELETE` on the Invora schema; `INSERT, SELECT` only on `audit_logs` and `stock_transactions`). Flyway migrations run with a separate, higher-privilege user, or at deploy time.
- No `root` user in application configuration.
- Credentials via environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`). Provide `.env.example`; `.env` is in `.gitignore`.
- Foreign keys, unique constraints, and `CHECK` constraints enabled (MySQL 8.0.16+ enforces CHECK).
- Database not exposed to the public internet. In Docker Compose, bind MySQL to `127.0.0.1` only.
- Use TLS for the database connection in production (`useSSL=true`).
- Encrypted backups (see section 14).

---

## 11. SECRETS AND CONFIGURATION [IMPLEMENT]

- No secrets, passwords, or keys in source code, front-end files, logs, or Git history.
- Spring profiles: `dev` (demo data, Swagger, verbose errors off) and `prod` (no demo data, Swagger off, secure cookies on, strict headers).
- Fail startup in `prod` if required environment variables are missing.
- If a secret is ever committed, rotate it immediately.

---

## 12. FRONTEND SECURITY [IMPLEMENT]

- No sensitive data in `localStorage` or `sessionStorage`. Authentication relies on the HttpOnly session cookie only.
- Escape or use `textContent` for all dynamic data. Never use `eval`, `new Function`, or inline event handlers (keeps the CSP strict).
- Vendor third-party libraries (Three.js, Chart.js, Lucide) into `static/vendor/` with pinned versions, and avoid loading scripts from random CDNs. If a CDN is used, add Subresource Integrity (`integrity` and `crossorigin` attributes).
- The 3D scene and charts only display data. They never decide stock rules.
- On `401` responses, redirect to the login page. On `403`, show a friendly "You don''t have permission" message.
- Disable submit buttons while a request is running to prevent duplicate submissions (the server also enforces idempotency).

---

## 13. LOGGING AND MONITORING [IMPLEMENT]

- Application logs (SLF4J) at INFO for normal operation, WARN for security events (failed logins, access denied, validation abuse), ERROR for unexpected exceptions.
- Include a request correlation id in logs.
- Never log request bodies containing passwords, or full session ids.
- Provide an admin-visible **Security Events** view (filtered audit log: failed logins, lockouts, access denied, role changes).
- Alert (log at WARN and show in the Security Events view) when: 10+ failed logins from one IP in 10 minutes, repeated 403s from one user, or an unusually large number of stock OUT actions by one user in a short window.
- Health endpoint `/actuator/health` for uptime checks.

---

## 14. BACKUP AND RECOVERY [POLICY]

- Daily automated MySQL backup (`mysqldump` or managed snapshots), retained for 30 days, with a weekly copy kept for 12 weeks.
- Backups are encrypted and stored separately from the database server.
- Test restoring a backup at least once per quarter.
- Targets: **RPO** (maximum data loss) of 24 hours, **RTO** (time to restore) of 4 hours.
- Document the restore steps in the README.
- Flyway migrations are versioned and never edited after release. Use a new migration for each change.

---

## 15. DEPENDENCY AND CODE SECURITY [IMPLEMENT]

- Use current stable versions of Java, Spring Boot, and all libraries. Review updates monthly.
- Run an automated dependency vulnerability check (OWASP Dependency-Check Maven plugin or GitHub Dependabot). Fix high and critical findings before release.
- No unused dependencies.
- Code review required before merging to the main branch.
- Follow the OWASP Top 10 as a checklist (see section 19).
- Do not use deprecated or insecure APIs (for example weak hashes such as MD5 or SHA-1 for passwords).

---

## 16. INCIDENT RESPONSE [POLICY]

1. **Detect:** from alerts, audit review, or user reports.
2. **Contain:** disable affected accounts (set `active = false`), invalidate sessions, restrict network access if needed.
3. **Investigate:** use audit logs and application logs to find what happened, who was affected, and what data changed.
4. **Recover:** correct stock using `ADJUST` transactions (with reasons), restore from backup if required, rotate any exposed secrets.
5. **Notify:** inform the system owner and affected users promptly.
6. **Review:** write a short post-incident report (cause, impact, fix, prevention) within 5 working days.

Users must report suspected account compromise, wrong stock movements, or suspicious behavior to an ADMIN immediately.

---

## 17. DATA PRIVACY AND RETENTION [POLICY]

**Data collected:** user name, email, role, login timestamps, actions performed (audit log), and business inventory data. Invora does not collect payment card data or sensitive personal data.

**Use:** operating the inventory system, security, and accountability. Data is not sold or shared with third parties.

**Retention:**

| Data | Retention |
|---|---|
| Stock transactions and transfers | Minimum 7 years (business and accounting records) |
| Audit logs | Minimum 12 months |
| Alerts | 24 months |
| Deactivated user accounts | Kept for audit integrity; personal details can be anonymized on request, but the user id stays linked to historical records |
| Application logs | 90 days |
| Backups | 30 days daily, 12 weeks weekly |

**User rights:** users can ask an ADMIN to correct their profile data. Deleting records that are required for audit or accounting is not allowed. Such records are anonymized instead.

---

## 18. ACCEPTABLE USE POLICY [POLICY]

Users must:
- Use their own account only and keep their password private.
- Record stock movements accurately and promptly, with the correct product, quantity, and reference.
- Enter a truthful reason when making adjustments or dispatching stock.
- Log out on shared devices.
- Report errors, suspicious activity, or lost access immediately.

Users must not:
- Share accounts or credentials.
- Record fake, backdated, or duplicate movements to hide shortages.
- Try to bypass permissions, probe the system for vulnerabilities, or access data outside their role.
- Export business data for non-business purposes.
- Use automated scripts against the application without ADMIN approval.

Breaking this policy may lead to account suspension, disciplinary action, and where appropriate legal action. All actions are logged.

---

## 19. SECURITY TESTING AND ACCEPTANCE CHECKLIST

Antigravity must implement automated tests (JUnit 5, MockMvc) and confirm each item:

**Authentication and session**
- [ ] Passwords are stored as BCrypt hashes; no plain text anywhere
- [ ] Wrong credentials return the generic message
- [ ] 5 failed logins lock the account for 15 minutes
- [ ] Idle timeout and logout invalidate the session
- [ ] Demo users forced to change password on first login

**Authorization**
- [ ] Each role can access only what the section 5 table allows (test every endpoint per role)
- [ ] Unauthenticated requests return 401, unauthorized roles return 403
- [ ] The last active ADMIN cannot be removed or demoted
- [ ] Role change invalidates sessions

**Input and web security**
- [ ] Requests with invalid quantities (0, negative, huge, non-numeric) are rejected
- [ ] Attempted SQL injection strings in search and sort parameters are harmless
- [ ] Script tags in product names are displayed as text and never executed
- [ ] CSRF token is required for POST/PUT/PATCH/DELETE
- [ ] Security headers present on responses
- [ ] CSV export neutralizes formula injection

**Inventory integrity**
- [ ] Stock OUT greater than available is rejected, and stock stays unchanged
- [ ] Parallel stock-outs never produce negative stock
- [ ] A failed transfer changes neither rack
- [ ] Duplicate `requestId` does not apply twice
- [ ] No endpoint exists to edit or delete stock transactions or audit logs

**Audit and operations**
- [ ] Every action in section 9 creates an audit record
- [ ] Audit rows for stock changes roll back together with failed stock changes
- [ ] No passwords or tokens appear in logs or audit metadata
- [ ] Error responses contain no stack traces or SQL
- [ ] `prod` profile has Swagger off, demo data off, secure cookies on
- [ ] No secrets committed to the repository

---

## 20. IN-APP POLICY PAGE [IMPLEMENT]

Add a page `/policy.html`, linked from the footer and the user menu, visible to every logged-in user. It displays, in readable sections with a sticky table of contents:

1. Acceptable Use Policy (section 18)
2. Password Policy (section 3)
3. Data Privacy and Retention (section 17)
4. How to Report a Security Issue (section 16)
5. Version number and last-updated date

Behavior:
- On first login, and again whenever the policy version changes, show a modal asking the user to read and accept the Acceptable Use Policy. Store `policy_version_accepted` and `policy_accepted_at` on the user and write a `POLICY_ACCEPTED` audit entry.
- The page uses the same dark glass design system as the rest of the app, with readable text sizes and good contrast.
- ADMIN can see which users have accepted the current version in the Users page.

---

## 21. ROLES AND RESPONSIBILITIES [POLICY]

| Role | Security responsibility |
|---|---|
| ADMIN | Manage accounts and roles, review audit and security events, run backups and restore tests, handle incidents |
| WAREHOUSE_MANAGER | Review alerts and stock adjustments, approve corrections, report suspicious activity |
| STAFF | Record accurate movements, protect their credentials |
| VIEWER | Protect their credentials, use data only for business purposes |
| Developers | Follow this document, review code, keep dependencies updated, never commit secrets |

---

## 22. DOCUMENT CONTROL

- **Owner:** System Administrator
- **Review cycle:** every 6 months, and after any security incident or major release
- **Changes:** update the version number, update the in-app policy version (which re-triggers user acceptance), and record the change in the README changelog

| Version | Date | Change |
|---|---|---|
| 1.0 | 2026-10-06 | Initial release — first complete security and policy document |

---

## 23. SUMMARY FOR ANTIGRAVITY

Implement, in this priority order:
1. BCrypt authentication, session settings, lockout, and the RBAC matrix
2. Server-side validation, CSRF, security headers, and error handling that leaks nothing
3. Transactional, locked, idempotent stock operations with append-only transactions
4. Complete audit logging (append-only)
5. Environment-based secrets and `dev`/`prod` profiles
6. Security tests from section 19
7. The in-app Policy page with acceptance tracking
8. Security Events view, backups documentation, and dependency scanning

Do not skip a control because it is not visible in the UI. Security is judged by what the server enforces.
