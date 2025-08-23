# Auth flow transition plan (Keycloak → userId bootstrap)

Goal: On successful OIDC login, auth-gateway relays the access_token to internal-gateway, which queries identity-service by email and returns `{ userId: UUID }`. Auth-gateway logs the userId and then caches the session.

## Phase 0 – Preconditions / sanity checks
- [x] Keycloak: ensure `email` is present in the access_token (client scopes + mappers) and not only in id_token
- [x] Gateways/services: consistent `issuer-uri` and (if enforced) `aud`/`resource` match Keycloak client
- [x] Internal-gateway and identity-service: resource-server JWT validation enabled and working with current tokens

## Phase 1 – Contracts and routing (no behavior changes yet)
- [ ] Define the bootstrap endpoint contract in identity-service
  - Request: authenticated call with `Authorization: Bearer <access_token>` containing an `email` claim
  - Response: `200 { "userId": "<uuid>" }` if found; `201 { "userId": "<uuid>" }` if auto-provisioned (payload shape is always `{ userId }`)
  - Decision: 
    - [ ] Auto-provision user when not found (return 201 + payload)
- [ ] Internal-gateway route to identity-service
  - [ ] Path: `/api/identity/claims->user` (or similar) and forward `Authorization` header unchanged
  - [ ] Strip only path prefixes as needed; do not strip `Authorization`
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

## Open decisions
- [x] Auto-provision user in identity-service when not found; return `201 { userId }`
- [x] Where to extract email: identity-service extracts from JWT via `@AuthenticationPrincipal`
- [x] Minimal shape of bootstrap response: always `{ userId }` (status code conveys created vs found)
