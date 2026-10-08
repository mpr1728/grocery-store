# 03. App experience

## Principles

1. **Fast on a mid-range Android phone on patchy mobile data.** That is the real test device, not a new iPhone on office Wi-Fi.
2. **Clean.** One accent color, large product photos, plenty of white space, no banners fighting each other, no countdown timers.
3. **Honest.** All-in price visible early, no pre-ticked add-ons, no fake scarcity. India's consumer rules on dark patterns make this a legal point as well as a brand point.
4. **Three taps to reorder.** Most grocery orders repeat. The app should treat a repeat as the normal case.
5. **Regional language is first class.** Telugu, Hindi, and English at launch for a Hyderabad pilot, with voice search.

## Speed budgets

| Moment | Budget |
|---|---|
| Cold start to usable home screen | Under 2 seconds |
| Search results after typing stops | Under 300 milliseconds |
| Add to cart feedback | Instant (update the screen first, sync after) |
| Checkout to order confirmed | Under 3 seconds after payment returns |
| App download size | Under 25 MB |

How to hit them: cache the catalog for the customer's hub on the device, keep the cart on the device and sync in the background, serve images in modern formats at the right size from a content delivery network, and keep the home screen to one request.

## Customer app screens

| Screen | What it does |
|---|---|
| Home | Address and delivery promise at the top. "Your refill" card (items the household is likely running low on). Today's fresh kitchen menu for this hub. Categories. |
| Search | Text and voice, tolerant of spelling and of regional names (bhindi, bendakaya, okra, lady's finger all find the same item). |
| Product | Photo, price per unit, source and harvest or made-at date, test report link, ingredients with the banned list check, ripeness choice where relevant. |
| Fresh kitchen | Menu for this hub and this daypart. Each dish shows made-at time and use-by time. Pre-order for a later daypart. |
| Cart | All-in price. Substitution preference per item (best match, ask me, do not substitute). Delivery choice: now, slot, or add to tomorrow's morning run. |
| Checkout | UPI first, then cards and wallets. Cash on delivery is optional and can be limited by order value. |
| Tracking | Picking, packing, on the way, with the rider on a map. One tap to call or message. |
| Refill and subscriptions | Daily and weekly subscriptions with pause, skip, and vacation mode. Suggested refill basket that the customer edits and confirms. |
| Help | Report an issue per item with a photo. Refund without argument for fresh quality complaints, within a clear limit. |

## AI in the customer experience

Keep it useful and quiet. No chatbot blocking the home screen.

- **Refill card:** predicted from the household's own buying rhythm.
- **Meal to cart:** "Dinner for four tonight, vegetarian, under 600 rupees" returns two or three options, each a mix of ready-made items and ingredients, and adds them to the cart.
- **Smart substitutions:** when an item is short, offer the closest match on brand, size, and diet rules.
- **Label help:** plain-language summary of an ingredient list, with allergens flagged against the household profile.
- **Voice ordering in a regional language**, which also helps older family members.

Every AI suggestion is a suggestion. The customer confirms before anything is ordered or charged.

## Staff apps

| App | User | Core jobs |
|---|---|---|
| Picker app | Pickers and packers | Guided pick path, scan bin and item, weigh loose produce, flag short or damaged items, print bag labels |
| Receiving app | Quality staff | Scan purchase order, weigh, grade, record temperature, photo, accept or reject |
| Kitchen display | Kitchen | Today's production plan by daypart, batch labels, live pre-orders |
| Rider app | Riders | Assigned orders, route, gate rules per community, proof of delivery, cash handling if used |
| Hub console | Hub manager | Live orders, stock and expiry alerts, purchase order approvals, waste log, markdown approvals |

The existing admin dashboard in `grocery-ui` is the seed for the hub console.

## Technology choice for the apps

- Customer and staff mobile apps: React Native, because the repo already uses React and one codebase covers Android and iOS. Android comes first.
- Web: a light ordering site reusing the same components, mainly for discovery and for sharing links on WhatsApp.
- WhatsApp: order updates, reorder prompts, and support, since it is the default messaging channel for this customer.
