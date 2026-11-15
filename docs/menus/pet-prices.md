# Pet prices screen

What pets sell for between players, by rating and rarity (luna, 2026-11-15). `/tacklebox prices`, or settings > tools > "pet prices". Not the pet store's payout: that is `display/pet-profit.md`.

Credit: the prices are the FishOnMC community's pet price sheet, a Google Sheet (`docs.google.com/spreadsheets/d/1mmOt9-hvCfT3flCekELM7F3FcOrbygwKGOrNyLwA2dk`); its author is not named on it. The screen shows the credit and the address under the table. The screen and the once a day download are FOE+'s idea (`itshaxr/FOE-Plus`, `PetPriceSheetHandler`), which builds on FOE-R (DannyPX) and FOECollab (Kimberlyow). No code was copied.

## Key files

- `menu/PetPrices`: the CSV parser, the table, the "is this the sheet" check, when a download is due (pure, the tests run these), and the copy on disk and the download.
- `screen/PetPricesScreen`: the table and the credit.
- Saved: `petPricesAt` (when the last good download was) and `config/tacklebox/petprices.csv` (the sheet as downloaded).

## How it works

- **Download**: only when the screen opens, in the background, from the sheet's CSV export (`/export?format=csv&gid=0`). At most one a day: a good copy is kept for 24 hours, over restarts too. A failed download is tried again when the screen opens ten minutes or more later. The request carries nothing about the player (no custom headers).
- **Check**: the answer has to be status 200, under 200 kB, no markup at its start, and at least two rows of two filled cells; otherwise it is thrown away (an error or sign-in page) and the old copy stays.
- **Table**: every cell exactly as the sheet writes it (the common column says `125` where the others say `250k`; that is the sheet's). Only the spaces around a cell go, columns that are empty in every row are dropped, the sheet's empty row stays as a gap. The sheet on 2026-11-15: `Rating (%)`, `Common` to `Mythic`, `Max Scale/Luck`, a notes column; bands `<70%`, `70-75%`, `75-79%`, then every percent from 80 to 100, then a `20%` row.
- **Screen**: the header row in the accent, the rating column dim, alternate rows faintly lighter. The table shrinks to fit the window, never grows. Under it: "prices from the fishonmc community's pet price sheet · updated 3h ago" and the sheet's address. Before the first download: "downloading the price sheet", or that it failed.

## Gotchas

- The notes column makes the table wide; on a small window it is scaled down.
- If the sheet's owner changes its layout the screen shows whatever is there; nothing is looked up by column.

## Tests

`menu/PetPricesTest` (CSV quotes and line ends, the table, error pages, the once a day rule, the age text), `menu/PetPricesDiskTest` (the real sheet of 2026-11-15 from `src/test/resources/dumps`, the copy on disk), `PetPricesIntegrationTest` (a download checked, shown, its time through a save, no second download that day).

Not run in game yet: the screen's look and scaling, the download itself from inside the game (the same address was fetched with curl on 2026-11-15 and answered with the CSV).
