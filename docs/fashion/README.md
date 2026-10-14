# Panyaloka Fashion

Started 2026-10-14. The clothing section of Panyaloka, beside Panyaloka Fresh (grocery). It builds on the earlier plan in `~/personal/business_ideas/custom-cotton-clothing/` (custom-fit cotton, natural dyes, made to order).

## The idea

Customers design a kurti, top, or frock, give five measurements, pick a cotton and a plant-dyed shade, and the garment is cut and stitched for them. Nothing is stocked, so there is no unsold inventory, and the same "honest, natural" promise as Fresh carries across.

## How it sits under the parent brand

| | Fresh | Fashion |
|---|---|---|
| Final address | `fresh.panyaloka.com` | `fashion.panyaloka.com` |
| Dev site today | `/grocery/` | `/fashion/` |
| Front end | `store-web` | `fashion-web` (own look: indigo, madder, turmeric, serif headings) |
| Back end | `store-api` | Same `store-api`, endpoints under `/api/fashion/*` |
| Accounts | Shared | Shared: one sign-in, one customer record |

Why one back end: one account, one customer database, one deploy, and one place for payments later. Each section keeps its own front end, so it can move to its own subdomain without code changes (the build takes a base path).

Subdomains need an owned domain. Until `panyaloka.com` is bought, both sections live as folders on the dev site and share the sign-in because they share an origin. Once they are on separate subdomains, browser storage is no longer shared, so a sign-in will need either a cookie set for `.panyaloka.com` or a shared `accounts.panyaloka.com` page. That is the one piece of work the move to subdomains adds.

## What is built

- Catalog: 3 garments, 4 parts each (neckline, sleeve, length, fit) with priced choices, 4 cotton fabrics, 10 natural-dye shades. Loaded by migration `V10__fashion.sql`. Prices are illustrative.
- Design studio with a live sketch that redraws as each choice changes.
- Measurements in centimetres or inches, saved to the account. A rules-based body shape reading (round, pear, inverted triangle, hourglass, rectangle) marks the fits that tend to suit; it is a styling guide only, the cut always follows the numbers.
- Review with the server's price, delivery details, notes for the tailor, and an acknowledgement that shades vary and that a made-to-measure order can be cancelled only until the fabric is cut.
- Order tracking: placed, fabric cut, stitched, quality checked, on the way, delivered. Cancel button until cutting.
- Studio console at `/studio` for the tailoring partner: everything needed to cut each order, and a button for the next stage.
- 7 API tests in `FashionTest`.

## Not built yet

- Payment (orders say pay on delivery), and the studio console has no sign-in, the same open item as the grocery hub console.
- Fit from photos (3DLOOK or Bold Metrics in the original plan). Measurements are typed for now.
- Real prices, a real tailoring partner and dye house, and their lead times.
- Returns and alterations flow. The draft policy is in the custom-cotton-clothing folder.

## Claims to check before launch

The site says "coloured by plants" and names each dye. Natural dyeing still uses mordants (alum, iron), and the iron grey uses iron with myrobalan. Do not say "chemical-free" or "organic" without certification (GOTS for organic cotton). Have the wording checked, as with the food claims on Fresh.
