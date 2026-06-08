# Phase 03 Extra — Country Tax Rules, Coupons, Promotions, and Pricing Policies

> **Attach to:** `phase-03-domain-driven-billing-model.md`  
> **Purpose:** Add missing domain-modeling work for tax and pricing complexity without introducing external tax providers, caching, Kafka, or service extraction.  
> **Architecture stance:** Still a modular monolith. This phase models the rules in the domain first.

---

## Why This Belongs in Phase 03

Phase 03 already introduces richer billing rules, discounts, taxes, value objects, domain services, invariants, state transitions, records, sealed classes, and domain events. Country tax rules and dynamic pricing/coupons are domain concerns first, so they should start here before being optimized, cached, externalized, or distributed later.

This extra should not turn Phase 03 into a full tax-compliance system. The goal is to model the concepts clearly enough that later phases can evolve them safely.

---

## Still Out of Scope Here

| Topic | Why Not Here | Later Phase |
|---|---|---|
| External tax provider integration | The domain model should exist before provider adapters. | Phase 04 or later integration/provider phase |
| Tax-rate caching | Caching is a consistency/performance topic. | Phase 09 — Caching, Read Models, and Performance |
| Country-complete VAT compliance | Too much legal/domain depth too early. | Later dedicated tax/billing expansion |
| Kafka events for tax/pricing changes | Async distribution is not introduced yet. | Phase 10/11 |
| Microservice extraction for pricing/tax | Boundaries are not validated yet. | Phase 14 or later |
| Rules engine | Too heavy before rule complexity proves it is needed. | Later, only if policy complexity justifies it |

---

## Extra Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Tax jurisdiction | Tax depends on country/region and billing address context. |
| Tax category | Different products may be taxed differently later. |
| Tax policy | Invoice generation needs a clear tax decision point. |
| Tax exemption | Some customers may be exempt or reverse-charged later. |
| Price policy | Pricing needs explicit rules instead of scattered `BigDecimal` logic. |
| Coupon | Customers can receive a specific discount code. |
| Promotion | Plans can have time-bound commercial offers. |
| Discount eligibility | Not every customer/subscription/plan can use every discount. |
| Discount stacking rule | Multiple discounts need deterministic behavior. |
| Promotion validity period | Commercial rules depend on time. |
| Pricing explanation | Invoices should explain how totals were calculated. |

---

## Workable Chunk 03.E1 — Tax Domain Vocabulary

### Goal

Create explicit domain types for tax instead of hiding tax as a raw percentage or number inside invoice calculation.

### Implement

```text
billing/tax/domain/
  TaxJurisdiction
  TaxRate
  TaxCategory
  TaxPolicy
  TaxExemption
  TaxCalculation
  TaxLine
```

Suggested value objects:

```text
CountryCode
RegionCode
TaxRate
TaxAmount
TaxJurisdiction
TaxRegistrationNumber
```

### Rules

- `TaxRate` cannot be negative.
- `TaxRate` should have an upper sanity limit, for example `0 <= rate <= 100`.
- `TaxAmount` must use the same currency as the taxable amount.
- Tax calculation must use the project-wide rounding policy.
- A tax calculation must produce an explanation, not only a number.

### Acceptance Gate

- Tax model has pure unit tests without Spring.
- Invalid tax rate cannot be constructed.
- Tax amount cannot mix currencies.
- Tax line appears in invoice calculation output.

---

## Workable Chunk 03.E2 — Simple Country Tax Strategy

### Goal

Implement a small local strategy that demonstrates country-specific behavior without claiming full VAT compliance.

### Implement

```text
TaxPolicy
  calculateTax(CustomerBillingProfile customer, Plan plan, Money netAmount, Clock clock)
```

Example local rules:

| Country | Example Rule |
|---|---|
| `RO` | Apply configured Romanian VAT rate. |
| `DE` | Apply configured German VAT rate. |
| `GB` | Apply configured UK VAT rate. |
| unknown | Use zero-tax fallback only in local/demo mode. |

### Add Configuration

```yaml
billing:
  tax:
    default-currency: EUR
    rates:
      RO: 0.19
      DE: 0.19
      GB: 0.20
```

### Guardrail

Do not describe this as production-ready VAT compliance. It is a learning implementation for modeling and tests.

### Acceptance Gate

- Same invoice net amount produces different tax lines for different billing countries.
- Unknown country behavior is explicit and tested.
- Tax configuration is validated at startup.
- Invoice totals show net, tax, and gross amounts.

---

## Workable Chunk 03.E3 — Customer Billing Profile

### Goal

Separate billing/tax identity from the basic customer CRUD model.

### Implement

```text
CustomerBillingProfile
  customerId
  billingCountry
  billingRegion
  taxRegistrationNumber
  taxExempt
```

### Rules

- Billing country is required before issuing a taxable invoice.
- Tax exemption must be explicit, not inferred from missing data.
- Invoice stores tax decision details as a snapshot so later customer changes do not rewrite old invoices.

### Acceptance Gate

- Invoice generation fails with a business error if required billing tax data is missing.
- Generated invoice stores tax country, tax rate, and tax explanation snapshot.
- Changing customer billing country does not mutate previously issued invoices.

---

## Workable Chunk 03.E4 — Pricing Policy Boundary

### Goal

Move price calculation into a dedicated domain policy so catalog and invoice do not duplicate money logic.

### Implement

```text
billing/pricing/domain/
  PricePolicy
  PriceCalculation
  PriceComponent
  PriceAdjustment
  PricingExplanation
```

### Pricing Output

```text
PriceCalculation
  basePrice
  adjustments
  netAmount
  currency
  explanation
```

### Rules

- Every adjustment must have a reason.
- Negative final price is not allowed.
- Currency must remain consistent across all price components.
- Price calculation should depend on `Clock`, not direct `Instant.now()` calls.

### Acceptance Gate

- Price calculation is testable without Spring.
- Final price cannot become negative.
- Rounding rules are consistently applied.
- Invoice line item references the pricing explanation.

---

## Workable Chunk 03.E5 — Coupon Model

### Goal

Add coupon behavior as a domain concept, not as random controller parameters.

### Implement

```text
Coupon
  code
  discountType: PERCENTAGE | FIXED_AMOUNT
  value
  validFrom
  validUntil
  maxRedemptions
  active
```

### Rules

- Expired coupons cannot be applied.
- Inactive coupons cannot be applied.
- Percentage coupons must be between 0 and 100.
- Fixed-amount coupons must use the invoice currency.
- Coupon code comparison should be case-insensitive, but storage should be normalized.

### Acceptance Gate

- Valid coupon reduces invoice net amount.
- Expired coupon returns a business-rule violation.
- Fixed discount cannot make final amount negative.
- Coupon code normalization is tested.

---

## Workable Chunk 03.E6 — Promotion Model

### Goal

Model plan-level commercial offers independently from customer-entered coupon codes.

### Implement

```text
Promotion
  promotionId
  name
  planId
  discount
  validFrom
  validUntil
  priority
  active
```

### Rules

- A promotion applies only during its validity window.
- Promotion must target a plan or customer segment placeholder.
- Higher-priority promotion wins if stacking is disabled.
- Promotion decisions should be explainable.

### Acceptance Gate

- Active promotion applies automatically to eligible plan.
- Expired promotion is ignored.
- Conflicting promotions resolve deterministically.
- Price explanation lists the applied promotion.

---

## Workable Chunk 03.E7 — Discount Stacking Policy

### Goal

Avoid accidental double-discounting by making stacking rules explicit.

### Implement

```text
DiscountStackingPolicy
  NO_STACKING
  COUPON_OVERRIDES_PROMOTION
  BEST_DISCOUNT_WINS
  PROMOTION_THEN_COUPON
```

### Rules

- The selected policy must be configured explicitly.
- If multiple discounts are eligible, the final selection must be deterministic.
- Every rejected discount should have a reason in the pricing explanation.

### Acceptance Gate

- Two eligible discounts do not produce ambiguous results.
- Stacking policy is covered with parameterized tests.
- Pricing explanation shows selected and rejected discounts.

---

## Workable Chunk 03.E8 — Domain Events

### Goal

Capture important pricing/tax facts without publishing them externally yet.

### Add Local Domain Events

```text
PriceCalculated
TaxCalculated
CouponApplied
PromotionApplied
DiscountRejected
```

### Guardrail

These are local domain/application events only. Do not publish them to Kafka in Phase 03.

### Acceptance Gate

- Events are emitted internally or captured in tests.
- Events contain correlation ID and aggregate ID when available.
- No Kafka/outbox dependency is introduced in this phase.

---

## Testing Additions

| Test Type | What It Proves |
|---|---|
| Pure domain unit tests | Pricing/tax rules work without Spring. |
| Parameterized tests | Country tax rates and discount combinations are covered. |
| Property-style tests | Discounts never produce negative totals. |
| Clock-based tests | Promotion validity is deterministic. |
| Snapshot tests | Invoice stores pricing/tax explanation at issue time. |

---

## Acceptance Criteria for This Extra

- Tax calculation uses explicit tax domain objects.
- Invoice total contains net, tax, and gross amounts.
- Country-specific tax behavior exists as a simple local learning model.
- Coupon and promotion rules are modeled as domain policies.
- Discount stacking is deterministic and tested.
- Invoice stores pricing/tax calculation snapshots.
- No external provider, Kafka, microservice extraction, or rules engine is introduced.

---

## Staff Engineer Lens

- Tax and pricing rules are business policy, not formatting logic.
- A simple explicit model is better than a clever implicit calculation.
- Every financial number should be explainable after the fact.
