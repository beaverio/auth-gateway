# Auth flow transition plan (Keycloak → userId bootstrap)

Goal: On successful OIDC login, auth-gateway relays the access_token to internal-gateway, which queries identity-service by email and returns `{ userId: UUID }`. Auth-gateway logs the userId and then caches the session.

## Phase 0 – Preconditions / sanity checks
- [x] Keycloak: ensure `email` is present in the access_token (client scopes + mappers) and not only in id_token
- [x] Gateways/services: consistent `issuer-uri` and (if enforced) `aud`/`resource` match Keycloak client
- [x] Internal-gateway and identity-service: resource-server JWT validation enabled and working with current tokens

## Phase 1 – Contracts and routing (no behavior changes yet)
- [x] Define the bootstrap endpoint contract in identity-service
  - Method/Path: `POST /users/bootstrap` (no body)
  - Auth: required; identity-service extracts `email` from `@AuthenticationPrincipal Jwt`
  - Response: `200 { "userId": "<uuid>" }` if found; `201 { "userId": "<uuid>" }` if auto-provisioned (payload always `{ userId }`)
- [x] Internal-gateway route to identity-service
  - Path mapping: `/api/identity/users/bootstrap` → `http://identity-service:8002/users/bootstrap`
  - Preserve headers: forward `Authorization: Bearer <token>`; do not strip/overwrite it; strip only the `/api/identity` prefix
- [ ] Observability
  - [ ] Add structured logs for subject (`sub`/`preferred_username`) and `email` at gateway boundaries

## Phase 2 – Identity lookup implementation
- [ ] identity-service endpoint implementation per contract
  - [ ] Extract `email` from `@AuthenticationPrincipal Jwt`; no extra validation here (JWT is already validated)
  - [ ] Lookup user by email; if found return `200 { userId }`; if not found auto-provision and return `201 { userId }`
- [ ] Internal-gateway passthrough
  - [ ] Ensure `Authorization` preserved; add short timeout and clear 5xx/timeout handling

## Phase 3 – Auth-gateway bootstrap on login
- [ ] Hook on authentication success (post-login)
  - [ ] Retrieve current `OAuth2AuthorizedClient` and extract `access_token`
  - [ ] Call internal-gateway bootstrap endpoint with `Authorization: Bearer <token>`
  - [ ] Log `{ userId }` on success
  - [ ] Only after this step, proceed to cache/confirm session (existing Redis session remains the source of truth)
- [ ] Error strategy
  - [ ] Expect `201` on first login (auto-provision) and `200` on subsequent logins; log both
  - [ ] 401/403 from downstream: treat as session inconsistency → force re-auth
  - [ ] Timeouts/5xx: fail fast, log, and show friendly error page

## Phase 4 – Hardening & policy
- [ ] Keycloak
  - [ ] Confirm role/authority mappers if we’ll gate routes/permissions later
  - [ ] Token TTLs aligned with session TTL; refresh-token policy decided
- [ ] Gateways
  - [ ] Ensure 401/403 are propagated to client; don’t swallow
  - [ ] Remove Cookie headers towards internal services; preserve `Authorization`
  - [ ] Rate limit and basic circuit-breakers where applicable
- [ ] Services
  - [ ] Consistent authority mapping (if using roles): `realm_access.roles` or `resource_access[client].roles`
  - [ ] Robust JWT clock skew and JWKS refresh settings

## Phase 5 – E2E validation
- [ ] Happy path: first login → auto-provision in identity-service (201) → `{ userId }` logged → session cached
- [ ] Repeat requests use existing session without re-login; no duplicate identity bootstrap calls (200)
- [ ] Expired token path triggers re-auth or refresh strategy as designed
- [ ] Logs contain correlation ID, subject, email, and userId across gateway and service

## Open decisions
- [x] Auto-provision user in identity-service when not found; return `201 { userId }`
- [x] Where to extract email: identity-service extracts from JWT via `@AuthenticationPrincipal`
- [x] Minimal shape of bootstrap response: always `{ userId }` (status code conveys created vs found)
