# Phase 8 — Security and Identity

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 08 of 18

## Business Goal

Customers, admins, support users, and internal services need different access levels.

## Technical Goal

Secure APIs locally using realistic identity and authorization patterns.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| OAuth2 | Modern delegated authorization |
| OpenID Connect | User identity on top of OAuth2 |
| Keycloak | Local identity provider |
| JWT | Stateless access tokens |
| JWKS | Token signature verification needs public keys |
| JWKS key rotation | Signing keys change over time |
| JWT issuer validation | Tokens must come from the expected authority |
| JWT audience validation | Tokens must be meant for this API |
| JWT clock skew | Distributed systems disagree slightly on time |
| Refresh token flow | Sessions need renewal without re-login |
| Token rotation | Long-lived credentials need protection |
| Client credentials flow | Services need machine-to-machine auth |
| RBAC | Roles define broad permissions |
| ABAC | Attributes like tenant and ownership refine access |
| Method security | Authorization should protect use cases too |
| Token relay | Downstream services may need caller context |
| Service account permissions | Machine identities need least privilege |
| CORS | Browser clients need controlled cross-origin access |
| CSRF | Cookie-based flows need protection understanding |
| Security headers | HTTP responses should reduce browser risk |
| Sensitive data masking | Logs must not leak secrets or PII |
| PII classification | Data handling depends on sensitivity |
| Encryption in transit | Local TLS can model production constraints |
| Encryption at rest | Some data requires database-level or field-level protection |
| Field-level encryption | Highly sensitive fields need extra protection |
| Secret rotation | Credentials must be replaceable |
| OWASP Top 10 | Common app security risks need coverage |
| Audit trail | Sensitive actions need traceable records |

## Practical Implementation Tasks

- Run Keycloak in Docker Compose.
- Create realm, clients, users, roles, and groups.
- Secure REST APIs with JWT validation.
- Validate:
  - issuer
  - audience
  - expiry
  - not-before
  - signature
  - clock skew
- Add tenant ID claim.
- Add RBAC for admin/support/customer roles.
- Add ABAC ownership checks.
- Add method-level security with `@PreAuthorize`.
- Add service-to-service auth using client credentials.
- Add token relay for downstream calls.
- Add JWKS rotation test scenario.
- Add refresh token flow notes and local demo.
- Add audit log for sensitive operations:

```text
who did what, to which resource, when, from which client, with which correlation ID
```

- Add log masking for tokens, emails, and sensitive payment fields.
- Add CORS configuration.
- Add security headers.
- Add local secret rotation exercise.

## Architectural Decision

### Decision

Use Keycloak locally as the identity provider.

### Why

It teaches realistic authentication and authorization without paid cloud services.

### Alternatives Considered

| Alternative | Why Not Enough |
|---|---|
| Hardcoded users | Unrealistic and bypasses real token validation |
| Basic auth | Not representative of modern service security |
| SaaS identity provider | Violates local-first constraint |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Realistic identity flows | More local setup |
| Better security learning | More configuration |
| Service auth supported | Token lifecycle must be understood |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| JWT integration tests | Token validation is real |
| Authorization tests | Role and ownership rules work |
| Method security tests | Use cases are protected |
| JWKS rotation tests | Key changes do not break unexpectedly |
| Audit tests | Sensitive actions are recorded |
| Log masking tests | Secrets do not leak to logs |

Acceptance criteria:

- Customer users cannot access another tenant's invoices.
- Support users can view but not mutate protected resources unless allowed.
- Internal service calls use client credentials.
- Logs do not expose bearer tokens or sensitive payment fields.

## Staff Engineer Lens

- Authentication answers “who are you?” Authorization answers “what can you do?”
- Security must exist at boundaries and inside critical use cases.
- Auditability is a product requirement in billing, not only a compliance checkbox.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Threat model per boundary** — Add a small STRIDE-style table for REST APIs, admin APIs, webhooks, Kafka consumers, and database access.
- **Authorization matrix** — Document resource, action, role, tenant condition, and ownership condition for every protected operation.
- **Token lifecycle tests** — Test expired, not-before, wrong issuer, wrong audience, wrong algorithm, rotated key, and missing tenant claim cases.
- **Local TLS exercise** — Run the gateway or app with local TLS so certificate, truststore, and token validation interactions are understood.
- **Secret classification and rotation runbook** — Separate dev secrets, service credentials, token signing material, and provider webhook secrets.

---
