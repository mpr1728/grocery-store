# 01. Concept and market

## Positioning

"The grocery store you do not have to double-check."

Quick-commerce apps in India compete on speed and discount. Organic specialists compete on purity but are slow, narrow, and expensive. The gap is a store that is trusted like a specialist, complete enough for the full weekly basket, and fast enough for tonight's dinner.

| | Quick commerce | Organic specialist | This store |
|---|---|---|---|
| Main promise | Minutes | Purity | Trust, fresh, and fast enough |
| Range | Very wide, anything that sells | Narrow | Curated full basket |
| Fresh produce | Variable quality | Good, limited | Graded, residue-tested, traceable |
| Ready-made food | Packaged snacks, partner kitchens | Rare | Own kitchen, local menu |
| Pricing | Discount led, fees added at checkout | Premium | Fair premium, all-in price shown early |

## Target customer

Primary: dual-income households, age 28 to 45, in gated communities and premium apartments. They already order groceries on an app several times a week, they worry about adulteration and pesticide residue, and they are short on cooking time.

Secondary:

- Parents of young children (clean ingredients, fresh purees, lunchbox items).
- Health-focused singles and couples (millets, high protein, low sugar).
- Older parents living with or near the primary customer (familiar regional food, simple app, phone ordering).

## What "Whole Foods-like" means here

The parts worth copying, and how each is adapted:

1. **A banned list.** Publish the ingredients and practices the store will not sell: for example hydrogenated fats, artificial colors, and a named set of preservatives and sweeteners. Start small and enforceable. A short list that is always true is worth more than a long one with exceptions.
2. **Produce you can trace.** Each batch of fruit and vegetables links to a farm or farmer group and a harvest date. Sample batches go for pesticide residue testing at an accredited lab, and results are shown in the app.
3. **Staples tested for adulteration.** Milk, ghee, honey, turmeric, chilli powder, and oils are the categories Indian buyers distrust most. Test them and show the report.
4. **Private label.** A house brand for staples (atta, dals, rice, cold-pressed oils, spices, ghee) carries the trust promise and the best margin.
5. **Prepared food as a reason to visit.** In India this is not a salad bar. It is fresh batters, cut vegetables, chutneys, curries, tiffins, and meal kits. See the menu logic in [04-ai-logic.md](04-ai-logic.md).
6. **Local sourcing with names.** Regional farmer producer organizations, local dairies, and small makers, shown with their story.

What not to copy: large-format stores, a huge imported range, and United States price levels. Rent, basket sizes, and buying frequency in India favor a small hub and frequent small orders.

## Range plan

About 2,500 to 3,500 items at maturity, about 800 to 1,200 at launch.

| Category | Role | Notes |
|---|---|---|
| Fruit and vegetables | Trust builder, daily habit | Graded, three ripeness options for key fruit, cut and cleaned options |
| Dairy, eggs, bread | Daily habit | Morning subscription delivery before 7 am |
| Staples (atta, rice, dal, oil, spices) | Basket size, margin through private label | Tested, with the report visible |
| Ready-made and fresh kitchen | Differentiator, highest margin | Location-specific, changes by daypart |
| Meat, fish, poultry | Basket size in non-vegetarian markets | Separate cold chain and packing, clearly marked |
| Snacks, beverages, breakfast | Completeness | Only items that pass the banned list |
| Baby, personal care, home care | Completeness | Tight selection, clean-label focus |
| Regional and festival | Delight, relevance | Rotates with the calendar |

## Pricing and membership

- Price staples close to market so the basket does not feel punished. Earn margin on private label, fresh, and ready-made.
- Show the all-in price (item, delivery, handling, taxes) before checkout, not on the last screen.
- Membership (working idea): a monthly fee for free delivery, morning subscription, and member prices on private label. The existing `membership_plan` field and the membership screen in this repo are the starting point.

## Open questions for validation

- Will this customer pay roughly 5 to 15 percent more than quick commerce for tested and traceable food? Test with a real pre-order pilot, not a survey.
- Which three categories create the trust story fastest in the pilot neighborhood? My guess is produce, milk, and fresh batters.
- Is the kitchen a traffic driver or a distraction in the first three months?
