# India launch design

Design for an app-first, premium natural grocery store in India. The inspiration is the Whole Foods idea (curated range, strict quality standards, strong fresh and prepared food), rebuilt for Indian households, Indian food habits, and fast delivery from a neighborhood hub.

Status: first draft, 2026-10-08. Numbers are planning assumptions, not researched market data. Each one is marked and needs validation before money is committed.

Working name: Trivarna ("three colours"). Web-screened only, not cleared against the trademark registry. See [07-brand-name.md](07-brand-name.md).

## The idea in five lines

1. One neighborhood hub (a dark store with a kitchen and a small pickup counter) serves homes within about 3 km.
2. The range is curated, about 2,500 to 3,500 items, and every item passes a published quality standard. Customers trust the shelf, so they do not have to read every label.
3. A fresh kitchen inside the hub makes ready-made food that changes by location, daypart, and season.
4. The app is clean and fast: reorder in three taps, honest prices, no dark patterns.
5. AI runs the back of the house: what to restock, what to cook, how to route deliveries, and when a household is about to run out.

## Documents

| File | What it covers |
|---|---|
| [01-concept-and-market.md](01-concept-and-market.md) | Positioning, target customer, quality standards, range, how it differs from quick commerce |
| [02-store-and-operations.md](02-store-and-operations.md) | Hub layout, kitchen, sourcing, cold chain, staffing, delivery model |
| [03-app-experience.md](03-app-experience.md) | Customer app screens, speed budgets, staff apps |
| [04-ai-logic.md](04-ai-logic.md) | Inventory refill, household refill, delivery, location-specific ready-made menu |
| [05-architecture.md](05-architecture.md) | Services, data model, how this repo evolves, AWS notes |
| [06-launch-plan.md](06-launch-plan.md) | Phases, illustrative unit economics, compliance checklist, risks |
| [07-brand-name.md](07-brand-name.md) | Working name Trivarna, trademark and domain screen, alternatives, clearance steps |

## Working assumptions I made (change any of these)

| Assumption | Why I picked it | Impact if changed |
|---|---|---|
| Pilot city is Hyderabad, west corridor (Gachibowli, Kondapur, Financial District) | Dense gated communities with high-income households make delivery cheap and word of mouth fast | Menu examples, sourcing, and language defaults change; the model does not |
| Delivery promise is "within 30 minutes, or a slot you choose" | Reliable and fresh beats a 10 minute race against funded quick-commerce players | A 10 minute promise needs more hubs and a smaller radius |
| Vegetarian and non-vegetarian both sold, with separate storage and packing | Hyderabad demand; many households still want strict separation | Veg-only simplifies cold chain and kitchen |
| One hub first, own riders at peak plus a logistics partner for overflow | Control of the experience while volume is small | Fully outsourced delivery lowers fixed cost and control |
| Indian-owned and Indian-operated entity | Foreign investment rules restrict inventory-based e-commerce and multi-brand retail | See the ownership note in the launch plan; this needs professional advice early |

## Decisions needed from you

1. City and neighborhood for the pilot.
2. Who owns the Indian entity and where the founders are tax resident (this drives the legal structure).
3. Starting budget range, which sets whether phase 1 is a full hub or a smaller kitchen-plus-produce pilot.
4. Brand name: confirm Trivarna after the registry search in [07-brand-name.md](07-brand-name.md), and pick the domain.
5. Vegetarian only, or both.
